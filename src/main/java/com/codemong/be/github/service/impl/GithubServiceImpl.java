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

    private static final String ANSWER_REF = "main";
    private static final long MAX_CODE_FILE_BYTES = 1024 * 1024;

    private final KmsService kmsService;
    private final ProjectRepository projectRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final SetupRepository setupRepository;
    private final ProcessRepository processRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;


    @Override
    public Map<String, GHRepository> getRepositories(Long userId) {
        User user = findUser(userId);
        try {
            String decryptToken = kmsService.decrypt(user.getGithubToken());
            GitHub gitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            return gitHub.getMyself().getRepositories();
        } catch (IOException e) {
            log.error("Failed to fetch GitHub repositories", e);
            throw new CustomException(ErrorCode.GITHUB_REPOSITORY_FETCH_FAILED);
        }
    }

    @Override
    public GithubRepository createProjectRepository(Long userId, Long projectId, RepositoryInitRequest request) {
        User user = findUser(userId);
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

            log.info("GitHub repository 생성 : {}", repositoryName);

            GHRepository createdRepository = gitHub.createRepository(repositoryName)
                    .description("Codemong project repository")
                    .private_(false)
                    .autoInit(true)
                    .create();
            GithubRepository savedRepository = githubRepositoryRepository.save(new GithubRepository(
                    user,
                    project,
                    createdRepository.getName(),
                    createdRepository.getHtmlUrl().toString()
            ));

            initializeRepositoryStep(user, project, savedRepository, createdRepository, request);

            return savedRepository;
        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to create GitHub project repository", e);
            throw new CustomException(ErrorCode.GITHUB_REPOSITORY_CREATE_FAILED);
        }
    }

    @Override
    @Transactional
    public RepositoryDeleteResponse deleteRepository(Long userId, Long repositoryId) {
        User user = findUser(userId);
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

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
    }

    @Override
    public Map<String, String> getBranchContents(Long repositoryId, Long step, Long userId)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("유저를 찾을 수 없습니다."));
        GithubRepository githubRepository = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(()-> new RuntimeException("레포지토리를 찾을 수 없습니다."));
        ProjectType type = githubRepository.getProject().getType();
        String repoName = githubRepository.getName();
        String branchName = branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.GITHUB_BRANCH_BASE_NOT_FOUND))
                .getName();
        try {
            String decryptToken = kmsService.decrypt(user.getGithubToken());
            GitHub github = GitHub.connectUsingOAuth(decryptToken);
            GHRepository repo = github.getMyself().getRepository(repoName);

            return repo.readZip(
                    inputStream -> collectZipContents(inputStream, type),
                    branchName
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean validateRepoOwner(Long repositoryId, Long userId) {
        GithubRepository curRepo = githubRepositoryRepository.findGithubRepositoryById(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPOSITORY_NOT_FOUND));

        if (!curRepo.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.REPOSITORY_ACCESS_DENIED);
        }

        return true;
    }

    @Override
    public Boolean validateIsSuccess(Long repositoryId) {
        Branch curBranch = branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.GITHUB_BRANCH_BASE_NOT_FOUND));

        return curBranch.isSuccess();

    }

    @Override
    @Transactional
    public Branch createNextStepBranch(Long repositoryId, Long userId, RepositoryInitRequest request) {
        GithubRepository repository = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPOSITORY_NOT_FOUND));

        if (!repository.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.REPOSITORY_ACCESS_DENIED);
        }

        Branch currentBranch = branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.GITHUB_BRANCH_BASE_NOT_FOUND));

        if (!currentBranch.isSuccess()) {
            throw new CustomException(ErrorCode.BRANCH_NOT_SUCCESS);
        }

        String projectId = normalizeRepositoryName(repository.getProject().getName());
        ProjectType track = resolveTrack(repository.getProject(), request);

        try {
            String nextAnswerStepId = resolveNextStepId(currentBranch, request, projectId, track);
            String nextStep = extractStepPrefix(nextAnswerStepId);
            String nextBranchName = buildBranchName(repository.getProject().getName(), nextStep);
            String decryptToken = kmsService.decrypt(repository.getUser().getGithubToken());
            GitHub gitHub = new GitHubBuilder().withOAuthToken(decryptToken).build();
            GHRepository remoteRepository = gitHub.getMyself().getRepository(repository.getName());
            String currentBranchLatestSha = getBranchSha(remoteRepository, currentBranch.getName());
            GHRef createdBranch = createBranch(remoteRepository, nextBranchName, currentBranchLatestSha);

            ProjectType starterTrack = oppositeTrack(track);
            copyAnswerRepositoryContents(
                    remoteRepository,
                    nextBranchName,
                    projectId,
                    nextAnswerStepId,
                    starterTrack,
                    "starter"
            );

            String finalBranchSha = remoteRepository.getRef("heads/" + nextBranchName).getObject().getSha();
            processRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId)
                    .ifPresent(process -> process.updateCurrentStep(nextStep));

            return branchRepository.save(new Branch(
                    repository.getUser(),
                    repository,
                    nextBranchName,
                    nextStep,
                    finalBranchSha == null ? createdBranch.getObject().getSha() : finalBranchSha
            ));
        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.GITHUB_BRANCH_CREATE_FAILED);
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
        ProjectType track = resolveTrack(project, request);
        String projectId = normalizeRepositoryName(project.getName());
        String answerStepId = resolveStepId(request, projectId, track);
        String startStep = extractStepPrefix(answerStepId);
        String branchName = buildBranchName(project.getName(), startStep);


        // 브랜치 생성
        GHRef createdBranch = createBranch(createdRepository, branchName);

        copyAnswerRepositoryContents(createdRepository, branchName, projectId, answerStepId, ProjectType.BE, "starter");
        copyAnswerRepositoryContents(createdRepository, branchName, projectId, answerStepId, ProjectType.FE, "starter");

        String finalBranchSha = createdRepository.getRef("heads/" + branchName).getObject().getSha();
        String answerMainSha = getAnswerMainSha();

        setupRepository.save(new Setup(project, startStep, track, answerMainSha));
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
            return createBranch(repository, branchName, baseSha);
        } catch (GHFileNotFoundException e) {
            log.error("Base branch ref not found. repository={}, targetBranch={}", repository.getFullName(), branchName, e);
            throw new CustomException(ErrorCode.GITHUB_BRANCH_BASE_NOT_FOUND);
        } catch (IOException e) {
            log.error("Failed to create branch. repository={}, branch={}", repository.getFullName(), branchName, e);
            throw new CustomException(ErrorCode.GITHUB_BRANCH_CREATE_FAILED);
        }
    }

    private GHRef createBranch(GHRepository repository, String branchName, String baseSha) throws IOException {
        try {
            return repository.createRef("refs/heads/" + branchName, baseSha);
        } catch (GHFileNotFoundException e) {
            log.error("Base branch ref not found. repository={}, targetBranch={}", repository.getFullName(), branchName, e);
            throw new CustomException(ErrorCode.GITHUB_BRANCH_BASE_NOT_FOUND);
        } catch (IOException e) {
            log.error("Failed to create branch. repository={}, branch={}", repository.getFullName(), branchName, e);
            throw new CustomException(ErrorCode.GITHUB_BRANCH_CREATE_FAILED);
        }
    }

    private String getBranchSha(GHRepository repository, String branchName) throws IOException {
        try {
            return repository.getRef("heads/" + branchName).getObject().getSha();
        } catch (GHFileNotFoundException e) {
            log.error("Branch ref not found. repository={}, branch={}", repository.getFullName(), branchName, e);
            throw new CustomException(ErrorCode.GITHUB_BRANCH_BASE_NOT_FOUND);
        } catch (IOException e) {
            log.error("Failed to fetch branch ref. repository={}, branch={}", repository.getFullName(), branchName, e);
            throw new CustomException(ErrorCode.GITHUB_BRANCH_CREATE_FAILED);
        }
    }

    private void copyAnswerRepositoryContents(
            GHRepository targetRepository,
            String targetBranchName,
            String projectId,
            String stepId,
            ProjectType track,
            String artifact
    ) throws IOException {
        validateAnswerRepositoryToken();
        GitHub answerGitHub = new GitHubBuilder().withOAuthToken(answerToken).build();
        GHRepository answerRepository = getAnswerRepository(answerGitHub);
        String sourcePath = buildAnswerPath(track, projectId, stepId, artifact);
        String targetRootPath = buildTargetRootPath(track);
        try {
            copyDirectory(answerRepository, targetRepository, sourcePath, sourcePath, targetRootPath, ANSWER_REF, targetBranchName);
        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.GITHUB_ANSWER_CODE_COPY_FAILED);
        }
    }

    private void copyDirectory(
            GHRepository sourceRepository,
            GHRepository targetRepository,
            String path,
            String sourceRootPath,
            String targetRootPath,
            String sourceBranchName,
            String targetBranchName
    ) throws IOException {
        List<GHContent> contents = sourceRepository.getDirectoryContent(path, sourceBranchName);
        for (GHContent content : contents) {
            if (content.isDirectory()) {
                copyDirectory(sourceRepository, targetRepository, content.getPath(), sourceRootPath, targetRootPath, sourceBranchName, targetBranchName);
                continue;
            }

            if (content.isFile()) {
                String targetPath = buildTargetPath(removeSourceRoot(content.getPath(), sourceRootPath), targetRootPath);
                upsertContent(targetRepository, targetBranchName, targetPath, content.read().readAllBytes());
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

    private String getAnswerMainSha() throws IOException {
        validateAnswerRepositoryToken();
        GitHub answerGitHub = new GitHubBuilder().withOAuthToken(answerToken).build();
        GHRepository answerRepository = getAnswerRepository(answerGitHub);
        return answerRepository.getRef("heads/" + ANSWER_REF).getObject().getSha();
    }

    private GHRepository getAnswerRepository(GitHub answerGitHub) throws IOException {
        try {
            return answerGitHub.getRepository(answerRepositoryName);
        } catch (GHFileNotFoundException e) {
            log.error("Answer repository not found or inaccessible. repository={}", answerRepositoryName, e);
            throw new CustomException(ErrorCode.GITHUB_ANSWER_REPOSITORY_NOT_FOUND);
        }
    }

    private String formatStep(Long step) {
        return String.format("step%02d", step);
    }

    static String buildAnswerPath(ProjectType track, String projectId, String stepId, String artifact) {
        String trackPath = track == ProjectType.BE ? "backend" : "frontend";
        return trackPath + "/" + projectId + "/" + stepId + "/" + artifact;
    }

    static String buildTargetRootPath(ProjectType track) {
        return track == ProjectType.FE ? "frontend" : "";
    }

    static String buildTargetPath(String relativePath, String targetRootPath) {
        if (!StringUtils.hasText(targetRootPath)) {
            return relativePath;
        }
        return targetRootPath + "/" + relativePath;
    }

    private String removeSourceRoot(String sourcePath, String sourceRootPath) {
        String prefix = sourceRootPath.endsWith("/") ? sourceRootPath : sourceRootPath + "/";
        if (!sourcePath.startsWith(prefix)) {
            return sourcePath;
        }
        return sourcePath.substring(prefix.length());
    }

    private String resolveStepId(RepositoryInitRequest request, String projectId, ProjectType track) throws IOException {
        if (StringUtils.hasText(request.getStepId())) {
            return request.getStepId();
        }
        return findAnswerStepId(projectId, track, formatStep(request.getStartStep()));
    }

    private String resolveNextStepId(
            Branch currentBranch,
            RepositoryInitRequest request,
            String projectId,
            ProjectType track
    ) throws IOException {
        if (request != null && StringUtils.hasText(request.getStepId())) {
            return request.getStepId();
        }

        int nextStepNumber = Integer.parseInt(currentBranch.getStep().substring(4, 6)) + 1;
        return findAnswerStepId(projectId, track, formatStep((long) nextStepNumber));
    }

    private ProjectType resolveTrack(Project project, RepositoryInitRequest request) {
        if (request != null && request.getTrack() != null) {
            return request.getTrack();
        }
        if (request != null && request.getType() != null) {
            return request.getType();
        }
        return project.getType();
    }

    private ProjectType oppositeTrack(ProjectType track) {
        return track == ProjectType.BE ? ProjectType.FE : ProjectType.BE;
    }

    private String findAnswerStepId(String projectId, ProjectType track, String stepPrefix) throws IOException {
        validateAnswerRepositoryToken();
        GitHub answerGitHub = new GitHubBuilder().withOAuthToken(answerToken).build();
        GHRepository answerRepository = getAnswerRepository(answerGitHub);
        String projectPath = buildAnswerProjectPath(track, projectId);

        return answerRepository.getDirectoryContent(projectPath, ANSWER_REF)
                .stream()
                .filter(GHContent::isDirectory)
                .map(GHContent::getName)
                .filter(name -> name.equals(stepPrefix) || name.startsWith(stepPrefix + "-"))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.GITHUB_ANSWER_CODE_COPY_FAILED));
    }

    private String buildAnswerProjectPath(ProjectType track, String projectId) {
        return (track == ProjectType.BE ? "backend" : "frontend") + "/" + projectId;
    }

    private String buildBranchName(String projectName, String step) {
        return normalizeRepositoryName(projectName) + "-" + extractStepPrefix(step);
    }

    static String extractStepPrefix(String stepId) {
        if (stepId != null && stepId.matches("^step\\d{2}.*")) {
            return stepId.substring(0, 6);
        }
        return stepId;
    }

    private void validateRepositoryInitRequest(RepositoryInitRequest request) {
        if (request == null || (!StringUtils.hasText(request.getStepId()) && request.getStartStep() == null)) {
            throw new CustomException(ErrorCode.INVALID_REPOSITORY_REQUEST);
        }

        if (request.getStartStep() != null && (request.getStartStep() < 1 || request.getStartStep() > 5)) {
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

    private Map<String, String> collectZipContents(InputStream inputStream, ProjectType type) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String filePath = removeArchiveRoot(entry.getName());
                if (!checkContentPath(filePath, type) || !isCodeFile(filePath) || entry.getSize() > MAX_CODE_FILE_BYTES) {
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

    static boolean checkContentPath(String path, ProjectType type) {
        String filter = (ProjectType.BE.equals(type)) ? "frontend" : "src";
        return !path.equals(filter) && !path.startsWith(filter + "/");
    }

    private boolean isCodeFile(String path) {
        return path.endsWith(".java")
                || path.endsWith(".json")
                || path.endsWith(".xml")
                || path.endsWith(".yml")
                || path.endsWith(".yaml")
                || path.endsWith(".properties")
                || path.endsWith(".gradle")
                || path.endsWith(".ts")
                || path.endsWith(".tsx")
                || path.endsWith(".jsx")
                || path.endsWith(".html")
                || path.endsWith(".js")
                || path.endsWith(".css");
    }
}
