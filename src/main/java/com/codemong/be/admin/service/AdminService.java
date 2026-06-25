package com.codemong.be.admin.service;

import com.codemong.be.admin.dto.AdminMetricsResponse;
import com.codemong.be.admin.dto.AdminRepositoryProgressResponse;
import com.codemong.be.admin.dto.AdminUserProgressResponse;
import com.codemong.be.branch.entity.Branch;
import com.codemong.be.branch.repository.BranchRepository;
import com.codemong.be.github.dto.RepositoryDeleteResponse;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.process.entity.Process;
import com.codemong.be.process.repository.ProcessRepository;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import com.codemong.be.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String USER_BAN_KEY_PREFIX = "BAN:";

    private final UserRepository userRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final BranchRepository branchRepository;
    private final ProcessRepository processRepository;
    private final UserService userService;
    private final AdminTaskLogService adminTaskLogService;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(readOnly = true)
    public List<AdminUserProgressResponse> getUserProgress() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(this::toUserProgress)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminMetricsResponse getMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemoryMb = runtime.maxMemory() / 1024 / 1024;
        return new AdminMetricsResponse(
                userRepository.count(),
                githubRepositoryRepository.count(),
                adminTaskLogService.runningCount(),
                adminTaskLogService.completedCount(),
                adminTaskLogService.successfulCount(),
                adminTaskLogService.failedCount(),
                usedMemoryMb,
                maxMemoryMb
        );
    }

    @Transactional
    public RepositoryDeleteResponse forceDeleteRepository(Long repositoryId) {
        GithubRepository repository = githubRepositoryRepository.findByIdWithUserAndProject(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPOSITORY_NOT_FOUND));
        Long ownerId = repository.getUser().getId();
        String repositoryName = repository.getName();
        String htmlUrl = repository.getHtmlUrl();
        String taskId = adminTaskLogService.start(
                "ADMIN_FORCE_DELETE_REPOSITORY",
                "관리자가 repository " + repositoryId + " DB 강제 삭제 호출.",
                ownerId,
                repositoryId,
                null,
                null
        );
        try {
            RepositoryDeleteResponse response = new RepositoryDeleteResponse(repositoryId, repositoryName, htmlUrl);
            githubRepositoryRepository.delete(repository);
            adminTaskLogService.complete(taskId, true, "dbOnlyDeletedRepositoryId=" + response.getDeletedRepositoryId());
            return response;
        } catch (RuntimeException e) {
            adminTaskLogService.complete(taskId, false, e.getClass().getSimpleName());
            throw e;
        }
    }

    public void setUserBan(Long userId, boolean banned) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
        String taskId = adminTaskLogService.start(
                banned ? "ADMIN_BAN_USER" : "ADMIN_UNBAN_USER",
                "관리자가 userId " + userId + (banned ? " 밴 처리 호출." : " 밴 해제 호출."),
                userId,
                null,
                null,
                null
        );
        try {
            if (banned) {
                redisTemplate.opsForValue().set(USER_BAN_KEY_PREFIX + userId, "true");
            } else {
                redisTemplate.delete(USER_BAN_KEY_PREFIX + userId);
            }
            adminTaskLogService.complete(taskId, true, "banned=" + banned);
        } catch (RuntimeException e) {
            adminTaskLogService.complete(taskId, false, e.getClass().getSimpleName());
            throw e;
        }
    }

    @Transactional
    public void forceUpdateEmail(Long userId, String email) {
        String taskId = adminTaskLogService.start(
                "ADMIN_UPDATE_EMAIL",
                "관리자가 userId " + userId + " 이메일 강제 변경 호출.",
                userId,
                null,
                null,
                null
        );
        try {
            userService.forceUpdateEmail(userId, email);
            adminTaskLogService.complete(taskId, true, "email=" + email);
        } catch (RuntimeException e) {
            adminTaskLogService.complete(taskId, false, e.getClass().getSimpleName());
            throw e;
        }
    }

    private AdminUserProgressResponse toUserProgress(User user) {
        List<AdminRepositoryProgressResponse> repositories = githubRepositoryRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toRepositoryProgress)
                .toList();
        return new AdminUserProgressResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getHtmlUrl(),
                isBanned(user.getId()),
                user.getCreatedAt(),
                repositories
        );
    }

    private boolean isBanned(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(USER_BAN_KEY_PREFIX + userId));
    }

    private AdminRepositoryProgressResponse toRepositoryProgress(GithubRepository repository) {
        Process process = processRepository.findTopByRepository_IdOrderByCreatedAtDesc(repository.getId()).orElse(null);
        Branch branch = branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repository.getId()).orElse(null);
        Long currentStep = process == null ? null : process.getCurrentStep();
        boolean completed = branch != null && branch.isSuccess()
                && currentStep != null
                && currentStep.equals((long) repository.getProject().getMaxStep());
        return new AdminRepositoryProgressResponse(
                repository.getId(),
                repository.getName(),
                repository.getHtmlUrl(),
                repository.getProject().getId(),
                repository.getProject().getName(),
                repository.getProject().getType().name(),
                repository.getProject().getMaxStep(),
                process == null ? null : process.getStartStep(),
                currentStep,
                branch == null ? null : branch.getId(),
                branch == null ? null : branch.getName(),
                branch != null && branch.isSuccess(),
                completed,
                repository.getCreatedAt()
        );
    }
}
