package com.codemong.be.github.controller;

import com.codemong.be.github.dto.RepositoryDeleteResponse;
import com.codemong.be.github.dto.RepositoryInitRequest;
import com.codemong.be.github.service.GithubService;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.global.jwt.JwtProvider;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/github")
@RequiredArgsConstructor
@Tag(name="Github API", description = "Github 내 정보 조회 및 레포 생성, 조회 등을 제공합니다.")
public class GithubController {

    private final GithubService githubService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @GetMapping("/repositories")
    @Operation(summary = "사용자 레포지토리 조회")
    public ResponseEntity<?> getRepositories(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        // TODO 1: 토큰 유효성 검사
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new CustomException(ErrorCode.MISSING_TOKEN);
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.INVALID_AUTH_HEADER);
        }

        String accessToken = authorizationHeader.substring(7);
        if (!jwtProvider.validateToken(accessToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // TODO 2: DB에 저장된 사용자의 token 가져오기
        Long userId = jwtProvider.getUserId(accessToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
        String userToken = user.getGithubToken(); // TODO: KMS 설정이 준비되면 복호화해서 사용해야 합니다.
        // TODO 3: 2에서 가져온 토큰으로 레포 목록 가져와서 return
        Map<String, GHRepository> repos = githubService.getRepositories(userToken);

        List<Map<String, Object>> repoInfos = repos.values().stream()
                .map(this::toRepoInfo)
                .toList();
        return ResponseEntity.ok(repoInfos);
    }

    @PostMapping("/repositories/{projectId}")
    @Operation(summary = "프로젝트 GitHub Repository 생성")
    public ResponseEntity<?> createProjectRepository(
            @PathVariable Long projectId,
            @RequestBody RepositoryInitRequest requestDto,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        User user = getAuthenticatedUser(authorizationHeader);
        GHRepository repository = githubService.createProjectRepository(user, projectId, requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toRepoInfo(repository));
    }

    @DeleteMapping("/repositories/{repositoryId}")
    @Operation(summary = "GitHub Repository 삭제")
    public ResponseEntity<?> deleteRepository(
            @PathVariable Long repositoryId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        User user = getAuthenticatedUser(authorizationHeader);
        RepositoryDeleteResponse response = githubService.deleteRepository(user, repositoryId);

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toRepoInfo(GHRepository repo) {
        Map<String, Object> repoInfo = new LinkedHashMap<>();
        repoInfo.put("name", repo.getName());
        repoInfo.put("fullName", repo.getFullName());
        repoInfo.put("private", repo.isPrivate());
        repoInfo.put("url", repo.getHtmlUrl());
        repoInfo.put("defaultBranch", repo.getDefaultBranch());
        return repoInfo;
    }

    private User getAuthenticatedUser(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new CustomException(ErrorCode.MISSING_TOKEN);
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.INVALID_AUTH_HEADER);
        }

        String accessToken = authorizationHeader.substring(7);
        if (!jwtProvider.validateToken(accessToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtProvider.getUserId(accessToken);
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
    }

}
