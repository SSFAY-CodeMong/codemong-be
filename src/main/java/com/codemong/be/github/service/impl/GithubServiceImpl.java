package com.codemong.be.github.service.impl;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.branch.repository.BranchRepository;
import com.codemong.be.github.dto.RepositoryInitRequest;
import com.codemong.be.github.dto.RepositoryDeleteResponse;
import com.codemong.be.github.service.GithubService;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.global.kms.KmsService;
import com.codemong.be.process.entity.Process;
import com.codemong.be.process.repository.ProcessRepository;
import com.codemong.be.project.entity.Project;
import com.codemong.be.project.entity.ProjectType;
import com.codemong.be.project.repository.ProjectRepository;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import com.codemong.be.setup.entity.Setup;
import com.codemong.be.setup.repository.SetupRepository;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubServiceImpl implements GithubService {

    @Value("${github.answer.token}")
    private String answerToken;

    @Value("${github.answer.repository}")
    private String answerRepositoryName;

    private static final long MAX_CODE_FILE_BYTES = 1024 * 1024;

    private final KmsService kmsService;
    private final ProjectRepository projectRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final SetupRepository setupRepository;
    private final ProcessRepository processRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;


    @Override
    public Map<String, GHRepository> getRepositories(String token) {
        try {
            String decryptToken = kmsService.decrypt(token);
            GitHub gitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            return gitHub.getMyself().getRepositories();
        } catch (IOException e) {
            log.error("Failed to fetch GitHub repositories", e);
            throw new CustomException(ErrorCode.GITHUB_REPOSITORY_FETCH_FAILED);
        }
    }

    @Override
    public GHRepository createProjectRepository(User user, Long projectId, RepositoryInitRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
        validateRepositoryInitRequest(request);

        try {
            String decryptToken = kmsService.decrypt(user.getGithubToken());
            GitHub gitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            Map<String, GHRepository> repositories = gitHub.getMyself().getRepositories();

            String repositoryNamePrefix = "codemong-" + normalizeRepositoryName(project.getName());
            int repoNum = 0;
            String repositoryName = repositoryNamePrefix + "-" + repoNum;
            while (repositories.containsKey(repositoryName)) {
                repoNum++;
                repositoryName = repositoryNamePrefix + "-" + repoNum;
            }

            log.info("Creating GitHub repository: {}", repositoryName);

            GHRepository createdRepository = gitHub.createRepository(repositoryName)
                    .description("Codemong project repository")
                    .private_(true)
                    .autoInit(true)
                    .create();
            GithubRepository savedRepository = githubRepositoryRepository.save(new GithubRepository(
                    user,
                    project,
                    createdRepository.getName(),
                    createdRepository.getHtmlUrl().toString()
            ));

            initializeRepositoryStep(user, project, savedRepository, createdRepository, request);

            return createdRepository;
        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to create GitHub project repository", e);
            throw new CustomException(ErrorCode.GITHUB_REPOSITORY_CREATE_FAILED);
        }
    }

    @Override
    @Transactional
    public RepositoryDeleteResponse deleteRepository(User user, Long repositoryId) {
        GithubRepository repository = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPOSITORY_NOT_FOUND));
        if (!repository.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.REPOSITORY_NOT_FOUND);
        }

        try {
            String decryptToken = kmsService.decrypt(user.getGithubToken());
            GitHub gitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            String login = gitHub.getMyself().getLogin();
            GHRepository remoteRepository = gitHub.getRepository(login + "/" + repository.getName());
            remoteRepository.delete();

            RepositoryDeleteResponse response = new RepositoryDeleteResponse(
                    repository.getId(),
                    repository.getName(),
                    repository.getHtmlUrl()
            );
            branchRepository.deleteByRepository(repository);
            processRepository.deleteByRepository(repository);
            githubRepositoryRepository.delete(repository);

            return response;
        } catch (CustomException e) {
            throw e;
        } catch (GHFileNotFoundException e) {
            log.error("GitHub repository not found: {}", repository.getName(), e);
            throw new CustomException(ErrorCode.REPOSITORY_NOT_FOUND);
        } catch (IOException e) {
            log.error("Failed to delete GitHub project repository: {}", repository.getName(), e);
            throw new CustomException(ErrorCode.GITHUB_REPOSITORY_DELETE_FAILED);
        }
    }

    @Override
    public Map<String, String> getBranchContents(Long repositoryId, Long step, Long userId)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("유저를 찾을 수 없습니다."));
        GithubRepository githubRepository = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(()-> new RuntimeException("레포지토리를 찾을 수 없습니다."));
        String repoName = githubRepository.getName();
        String branchName = (step>=10) ? "step-"+step : "step-0"+step;
        try {
            String decryptToken = kmsService.decrypt(user.getGithubToken());
            GitHub github = GitHub.connectUsingOAuth(decryptToken);
            GHRepository repo = github.getMyself().getRepository(repoName);

            return repo.readZip(this::collectZipContents, branchName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String normalizeRepositoryName(String projectName) {
        String normalized = projectName.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (normalized.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REPOSITORY_NAME);
        }

        return normalized;
    }

    private void initializeRepositoryStep(
            User user,
            Project project,
            GithubRepository savedRepository,
            GHRepository createdRepository,
            RepositoryInitRequest request
    ) throws IOException {
        String startStep = formatStep(request.getStartStep());
        String frontendAnswerStep = formatStep(request.getStartStep());
        String backendAnswerStep = formatStep(resolveBackendAnswerStep(request.getStartStep()));
        String branchName = buildBranchName(project.getName(), startStep);

        log.info(
                "Initializing repository. projectId={}, startStep={}, frontendAnswerStep={}, backendAnswerStep={}, targetBranch={}",
                project.getId(),
                startStep,
                frontendAnswerStep,
                backendAnswerStep,
                branchName
        );

        GHRef createdBranch = createBranch(createdRepository, branchName);
        copyAnswerRepositoryContents(createdRepository, branchName, frontendAnswerStep, ProjectType.FE);
        copyAnswerRepositoryContents(createdRepository, branchName, backendAnswerStep, ProjectType.BE);

        String finalBranchSha = createdRepository.getRef("heads/" + branchName).getObject().getSha();
        String frontendAnswerSha = getAnswerBranchSha(frontendAnswerStep);
        String backendAnswerSha = getAnswerBranchSha(backendAnswerStep);

        setupRepository.save(new Setup(project, frontendAnswerStep, ProjectType.FE, frontendAnswerSha));
        setupRepository.save(new Setup(project, backendAnswerStep, ProjectType.BE, backendAnswerSha));
        processRepository.save(new Process(user, savedRepository, startStep, startStep));
        branchRepository.save(new Branch(
                user,
                savedRepository,
                branchName,
                startStep,
                finalBranchSha == null ? createdBranch.getObject().getSha() : finalBranchSha
        ));
    }

    private GHRef createBranch(GHRepository repository, String branchName) throws IOException {
        try {
            String baseBranchName = repository.getDefaultBranch();
            GHRef baseRef = repository.getRef("heads/" + baseBranchName);
            String baseSha = baseRef.getObject().getSha();
            return repository.createRef("refs/heads/" + branchName, baseSha);
        } catch (GHFileNotFoundException e) {
            log.error("Base branch ref not found. repository={}, targetBranch={}", repository.getFullName(), branchName, e);
            throw new CustomException(ErrorCode.GITHUB_BRANCH_BASE_NOT_FOUND);
        } catch (IOException e) {
            log.error("Failed to create branch. repository={}, branch={}", repository.getFullName(), branchName, e);
            throw new CustomException(ErrorCode.GITHUB_BRANCH_CREATE_FAILED);
        }
    }

    private void copyAnswerRepositoryContents(
            GHRepository targetRepository,
            String targetBranchName,
            String answerStep,
            ProjectType type
    ) throws IOException {
        validateAnswerRepositoryToken();
        GitHub answerGitHub = new GitHubBuilder().withOAuthToken(answerToken).build();
        GHRepository answerRepository = getAnswerRepository(answerGitHub);
        log.info(
                "Copying answer contents. sourceRepository={}, sourceBranch={}, sourcePath={}, targetRepository={}, targetBranch={}",
                answerRepositoryName,
                answerStep,
                type.name(),
                targetRepository.getFullName(),
                targetBranchName
        );
        try {
            copyDirectory(answerRepository, targetRepository, type.name(), answerStep, targetBranchName);
        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            log.error(
                    "Failed to copy answer contents. sourceRepository={}, sourceBranch={}, sourcePath={}, targetRepository={}, targetBranch={}",
                    answerRepositoryName,
                    answerStep,
                    type.name(),
                    targetRepository.getFullName(),
                    targetBranchName,
                    e
            );
            throw new CustomException(ErrorCode.GITHUB_ANSWER_CODE_COPY_FAILED);
        }
    }

    private void copyDirectory(
            GHRepository sourceRepository,
            GHRepository targetRepository,
            String path,
            String sourceBranchName,
            String targetBranchName
    ) throws IOException {
        List<GHContent> contents = sourceRepository.getDirectoryContent(path, sourceBranchName);
        for (GHContent content : contents) {
            if (content.isDirectory()) {
                copyDirectory(sourceRepository, targetRepository, content.getPath(), sourceBranchName, targetBranchName);
                continue;
            }

            if (content.isFile()) {
                upsertContent(targetRepository, targetBranchName, content.getPath(), content.read().readAllBytes());
            }
        }
    }

    private void upsertContent(GHRepository repository, String branchName, String path, byte[] content) throws IOException {
        try {
            GHContent existingContent = repository.getFileContent(path, branchName);
            existingContent.update(content, "Initialize answer code: " + path, branchName);
        } catch (GHFileNotFoundException e) {
            repository.createContent()
                    .branch(branchName)
                    .path(path)
                    .content(content)
                    .message("Initialize answer code: " + path)
                    .commit();
        }
    }

    private String getAnswerBranchSha(String answerStep) throws IOException {
        validateAnswerRepositoryToken();
        GitHub answerGitHub = new GitHubBuilder().withOAuthToken(answerToken).build();
        GHRepository answerRepository = getAnswerRepository(answerGitHub);
        return answerRepository.getRef("heads/" + answerStep).getObject().getSha();
    }

    private GHRepository getAnswerRepository(GitHub answerGitHub) throws IOException {
        try {
            return answerGitHub.getRepository(answerRepositoryName);
        } catch (GHFileNotFoundException e) {
            log.error("Answer repository not found or inaccessible. repository={}", answerRepositoryName, e);
            throw new CustomException(ErrorCode.GITHUB_ANSWER_REPOSITORY_NOT_FOUND);
        }
    }

    private long resolveBackendAnswerStep(Long startStep) {
        return Math.max(1, startStep - 1);
    }

    private String formatStep(Long step) {
        return String.format("step%02d", step);
    }

    private String buildBranchName(String projectName, String step) {
        // 0612 수정 ) 합의 내용에 의거, 브랜치명에 날짜 제거
        return normalizeRepositoryName(projectName) + "-" + step;
    }

    private void validateRepositoryInitRequest(RepositoryInitRequest request) {
        if (request == null || request.getStartStep() == null) {
            throw new CustomException(ErrorCode.INVALID_REPOSITORY_REQUEST);
        }

        if (request.getStartStep() < 1 || request.getStartStep() > 5) {
            throw new CustomException(ErrorCode.INVALID_REPOSITORY_REQUEST);
        }
    }

    private void validateAnswerRepositoryToken() {
        if (!StringUtils.hasText(answerToken)) {
            throw new CustomException(ErrorCode.GITHUB_ANSWER_TOKEN_MISSING);
        }
    }


    private String findBaseBranchName(GHRepository repo, Map<String, GHBranch> branches) {
        String defaultBranch = repo.getDefaultBranch();
        if (defaultBranch != null && branches.containsKey(defaultBranch)) {
            return defaultBranch;
        }

        if (branches.containsKey("main")) {
            return "main";
        }

        if (branches.containsKey("master")) {
            return "master";
        }

        throw new CustomException(ErrorCode.GITHUB_BRANCH_BASE_NOT_FOUND);
    }

    private Map<String, String> collectZipContents(InputStream inputStream) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String filePath = removeArchiveRoot(entry.getName());
                if (!isCodeFile(filePath) || entry.getSize() > MAX_CODE_FILE_BYTES) {
                    continue;
                }

                String fileText = readZipEntryText(zipInputStream);
                if (fileText == null) {
                    continue;
                }
                result.put(filePath, fileText);
            }
        }

        return result;
    }

    private String readZipEntryText(ZipInputStream zipInputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long totalBytes = 0;
        int readBytes;

        while ((readBytes = zipInputStream.read(buffer)) != -1) {
            totalBytes += readBytes;
            if (totalBytes > MAX_CODE_FILE_BYTES) {
                while (zipInputStream.read(buffer) != -1) {
                    // Drain this zip entry so the next entry can be read normally.
                }
                return null;
            }
            outputStream.write(buffer, 0, readBytes);
        }

        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private String removeArchiveRoot(String entryName) {
        int firstSlashIndex = entryName.indexOf('/');
        if (firstSlashIndex == -1 || firstSlashIndex == entryName.length() - 1) {
            return entryName;
        }
        return entryName.substring(firstSlashIndex + 1);
    }


    private boolean isCodeFile(String path) {
        return path.endsWith(".java")
                || path.endsWith(".json")
                || path.endsWith(".xml")
                || path.endsWith(".yml")
                || path.endsWith(".yaml")
                || path.endsWith(".properties")
                || path.endsWith(".gradle");
    }
}
