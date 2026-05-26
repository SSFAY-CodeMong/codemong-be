package com.codemong.be.github.controller;

import com.codemong.be.github.service.GithubService;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.global.jwt.JwtProvider;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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







    /**
     * [ DEV method ]
     * required delete
     *
     * @return connection status
     */
    @GetMapping("/connect-test")
    @Operation(summary = "Server 연결 테스트")
    public ResponseEntity<?> connectTest() {
        String result = githubService.connectTest("connection test");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(result + " : CONNECTED");
    }

    /**
     * [ DEV method ]
     * required delete
     *
     * @return connection status
     */
    @GetMapping("/git-connect-test")
    @Operation(summary = "Github 연결 테스트",
        description = """
                다음의 기능을 테스트합니다.
                1. Github <-> API Server 연결 테스트
                
                Response
                1. user information
                2. repo information
    """)
    public ResponseEntity<?> gitConnectTest() throws IOException {
        GHMyself me = githubService.gitConnectTest();
        Map<String, GHRepository> repos = githubService.gitRepositoryTest();

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("login", me.getLogin());
        body.put("name", me.getName());
        body.put("email", me.getEmail());
        body.put("url", me.getHtmlUrl());

        List<Map<String, Object>> repoInfos = repos.values().stream()
                .map(this::toRepoInfo)
                .toList();
        body.put("repos", repoInfos);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(body);
    }

    /**
     * [ DEV method ]
     * required delete
     *
     * @return connection status
     */
    @PostMapping("/git-generate-test")
    @Operation(summary = "Github Repository 생성 테스트",
            description = """
                다음의 기능을 테스트합니다.
                1. Github <-> API Server 생성 테스트
                Response
                1. user information
                2. repo information
    """)
    public ResponseEntity<?> gitGenerateTest() throws IOException {
        GHRepository repo = githubService.gitGenerateTest();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(repo.getName() + "가 생성되었습니다.");
    }

    /**
     * [ DEV method ]
     * required delete
     *
     * @return connection status
     */
    @PostMapping("/git-branch-generate-test")
    @Operation(summary = "Github Branch 생성 테스트",
            description = """
                다음의 기능을 테스트합니다.
                1. Github <-> API Server 생성 테스트
                Response
                1. user information
                2. repo information
    """)
    public ResponseEntity<?> gitGenerateBranchTest() throws IOException {
        try {
            GHRef branch = githubService.gitGenerateBranchTest("codemong-tester/test-repo0");
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(branch.getRef() + "가 생성되었습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
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

}
