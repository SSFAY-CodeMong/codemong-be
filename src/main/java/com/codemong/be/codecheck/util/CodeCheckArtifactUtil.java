package com.codemong.be.codecheck.util;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeCheckArtifactUtil {

    private static final String TEST_RESULTS_ARTIFACT_NAME = "test-results";
    private static final Duration ARTIFACT_POLL_INTERVAL = Duration.ofSeconds(3);
    private static final Duration ARTIFACT_RESULT_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;

    public List<String> findFailedTests(String token, String fullRepositoryName, String runId) {
        Instant deadline = Instant.now().plus(ARTIFACT_RESULT_TIMEOUT);

        while (Instant.now().isBefore(deadline)) {
            JsonNode artifacts = fetchWorkflowArtifacts(token, fullRepositoryName, runId).path("artifacts");
            if (!artifacts.isArray()) {
                return List.of();
            }

            for (JsonNode artifact : artifacts) {
                if (!TEST_RESULTS_ARTIFACT_NAME.equals(artifact.path("name").asText())) {
                    continue;
                }
                if (artifact.path("expired").asBoolean(false)) {
                    return List.of();
                }

                byte[] archive = downloadArtifactArchive(token, artifact.path("archive_download_url").asText());
                return parseFailedTestsFromJUnitXmlArchive(archive);
            }

            try {
                Thread.sleep(ARTIFACT_POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_TIMEOUT);
            }
        }

        return List.of();
    }

    private JsonNode fetchWorkflowArtifacts(String token, String fullRepositoryName, String runId) {
        URI uri = URI.create("https://api.github.com/repos/" + fullRepositoryName
                + "/actions/runs/" + runId + "/artifacts?per_page=100");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            log.error("GitHub Actions artifact fetch failed. repository={}, runId={}", fullRepositoryName, runId, e);
            return objectMapper.createObjectNode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_TIMEOUT);
        }
    }

    private byte[] downloadArtifactArchive(String token, String archiveDownloadUrl) {
        if (!StringUtils.hasText(archiveDownloadUrl)) {
            return new byte[0];
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(archiveDownloadUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                String location = response.headers().firstValue("Location").orElse(null);
                if (!StringUtils.hasText(location)) {
                    return new byte[0];
                }

                HttpRequest redirectedRequest = HttpRequest.newBuilder(URI.create(location))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();
                response = httpClient.send(redirectedRequest, HttpResponse.BodyHandlers.ofByteArray());
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new byte[0];
            }
            return response.body();
        } catch (IOException e) {
            log.error("GitHub Actions artifact download failed. url={}", archiveDownloadUrl, e);
            return new byte[0];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.GITHUB_ACTIONS_RESULT_TIMEOUT);
        }
    }

    public List<String> parseFailedTestsFromJUnitXmlArchive(byte[] archive) {
        Set<String> failedTests = new LinkedHashSet<>();
        if (archive == null || archive.length == 0) {
            return List.of();
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".xml")) {
                    continue;
                }
                failedTests.addAll(parseFailedTestsFromJUnitXml(zipInputStream.readAllBytes()));
            }
        } catch (Exception e) {
            log.error("Failed to parse GitHub Actions test result artifact.", e);
            return List.of();
        }

        return new ArrayList<>(failedTests);
    }

    private List<String> parseFailedTestsFromJUnitXml(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);

        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        NodeList testCases = document.getElementsByTagName("testcase");
        List<String> failedTests = new ArrayList<>();

        for (int i = 0; i < testCases.getLength(); i++) {
            Element testCase = (Element) testCases.item(i);
            if (testCase.getElementsByTagName("failure").getLength() == 0
                    && testCase.getElementsByTagName("error").getLength() == 0) {
                continue;
            }

            String className = testCase.getAttribute("classname");
            String methodName = testCase.getAttribute("name");
            if (StringUtils.hasText(className) && StringUtils.hasText(methodName)) {
                failedTests.add(className + "." + methodName);
            }
        }

        return failedTests;
    }
}
