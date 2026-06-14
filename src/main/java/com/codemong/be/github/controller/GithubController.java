package com.codemong.be.github.controller;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.github.dto.BranchNextResponse;
import com.codemong.be.github.dto.GithubRepositoryResponse;
import com.codemong.be.github.dto.RepositoryDeleteResponse;
import com.codemong.be.github.dto.RepositoryInitRequest;
import com.codemong.be.github.service.GithubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @Operation(summary = "프로젝트 GitHub Repository 생성")
    public ResponseEntity<?> createProjectRepository(
            @PathVariable Long projectId,
            @RequestBody RepositoryInitRequest requestDto,
            @AuthenticationPrincipal Long userId
    ) {
        GHRepository repository = githubService.createProjectRepository(userId, projectId, requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(GithubRepositoryResponse.from(repository));
    }

    @PostMapping("/repositories/{repositoryId}/next")
    @Operation(summary="사용자가 다음 단계로 넘어가기 시도")
    public ResponseEntity<?> createNextRepository(
            @PathVariable("repositoryId") Long repositoryId,
            @AuthenticationPrincipal Long userId
    ) {
        Branch branch = githubService.createNextStepBranch(repositoryId, userId);
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
