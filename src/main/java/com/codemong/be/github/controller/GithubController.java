package com.codemong.be.github.controller;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.github.dto.BranchNextResponse;
import com.codemong.be.github.dto.GithubRepositoryResponse;
import com.codemong.be.github.dto.LearningRepositoryResponse;
import com.codemong.be.github.dto.LearningRepositoryStatusResponse;
import com.codemong.be.github.dto.RepositoryCreatedResponse;
import com.codemong.be.github.dto.RepositoryDeleteResponse;
import com.codemong.be.github.dto.RepositoryInitRequest;
import com.codemong.be.github.service.GithubService;
import com.codemong.be.branch.repository.BranchRepository;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.process.repository.ProcessRepository;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/github")
@RequiredArgsConstructor
@Tag(name="Github API", description = "Github 내 정보 조회 및 레포 생성, 조회 등을 제공합니다.")
public class GithubController {

    private final GithubService githubService;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final ProcessRepository processRepository;
    private final BranchRepository branchRepository;

    @GetMapping("/repositories")
    @Operation(summary = "사용자 레포지토리 조회")
    public ResponseEntity<?> getRepositories(
            @AuthenticationPrincipal Long userId
    ){
        Map<String, GHRepository> repos = githubService.getRepositories(userId);

        List<GithubRepositoryResponse> repoInfos = repos.values().stream()
                .map(GithubRepositoryResponse::from)
                .toList();
        return ResponseEntity.ok(repoInfos);
    }

    @PostMapping("/repositories/{projectId}")
    @Transactional
    @Operation(summary = "프로젝트 GitHub Repository 생성")
    public ResponseEntity<?> createProjectRepository(
            @PathVariable Long projectId,
            @RequestBody RepositoryInitRequest requestDto,
            @AuthenticationPrincipal Long userId
    ) {
        GithubRepository repository = githubService.createProjectRepository(userId, projectId, requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RepositoryCreatedResponse.from(repository));
    }

    @GetMapping("/learning-repositories")
    @Transactional(readOnly = true)
    @Operation(summary = "내 학습 Repository 목록 조회")
    public ResponseEntity<List<LearningRepositoryResponse>> getLearningRepositories(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(githubRepositoryRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(LearningRepositoryResponse::from)
                .toList());
    }

    @GetMapping("/repositories/{repositoryId}/status")
    @Transactional(readOnly = true)
    @Operation(summary = "학습 Repository 진행 상태 조회")
    public ResponseEntity<LearningRepositoryStatusResponse> getLearningRepositoryStatus(
            @PathVariable Long repositoryId,
            @AuthenticationPrincipal Long userId
    ) {
        GithubRepository repository = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPOSITORY_NOT_FOUND));
        if (!repository.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.REPOSITORY_ACCESS_DENIED);
        }

        return ResponseEntity.ok(LearningRepositoryStatusResponse.from(
                repository,
                processRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId).orElse(null),
                branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId).orElse(null)
        ));
    }

    @GetMapping("/repositories/{repositoryId}/completed")
    @Transactional(readOnly = true)
    @Operation(summary = "프로젝트 완료 여부 조회")
    public ResponseEntity<Map<String, Object>> isRepositoryCompleted(
            @PathVariable Long repositoryId,
            @AuthenticationPrincipal Long userId
    ) {
        LearningRepositoryStatusResponse status = getLearningRepositoryStatus(repositoryId, userId).getBody();
        return ResponseEntity.ok(Map.of(
                "repositoryId", repositoryId,
                "completed", status != null && status.completed()
        ));
    }

    @PostMapping("/repositories/{repositoryId}/next")
    @Transactional
    @Operation(summary="사용자가 다음 단계로 넘어가기 시도")
    public ResponseEntity<?> createNextBranch(
            @PathVariable("repositoryId") Long repositoryId,
            @RequestBody(required = false) RepositoryInitRequest requestDto,
            @AuthenticationPrincipal Long userId
    ) {
        Branch branch = githubService.createNextStepBranch(repositoryId, userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(BranchNextResponse.from(branch));
    }

    @DeleteMapping("/repositories/{repositoryId}")
    @Operation(summary = "GitHub Repository 삭제")
    public ResponseEntity<?> deleteRepository(
            @PathVariable Long repositoryId,
            @AuthenticationPrincipal Long userId
    ) {
        RepositoryDeleteResponse response = githubService.deleteRepository(userId, repositoryId);

        return ResponseEntity.ok(response);
    }

}
