package com.codemong.be.admin.controller;

import com.codemong.be.admin.dto.AdminBanRequest;
import com.codemong.be.admin.dto.AdminEmailUpdateRequest;
import com.codemong.be.admin.dto.AdminLoginRequest;
import com.codemong.be.admin.dto.AdminLoginResponse;
import com.codemong.be.admin.dto.AdminMetricsResponse;
import com.codemong.be.admin.dto.AdminTaskLogResponse;
import com.codemong.be.admin.dto.AdminUserProgressResponse;
import com.codemong.be.admin.service.AdminAuthService;
import com.codemong.be.admin.service.AdminService;
import com.codemong.be.admin.service.AdminTaskLogService;
import com.codemong.be.github.dto.RepositoryDeleteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final AdminAuthService adminAuthService;
    private final AdminService adminService;
    private final AdminTaskLogService adminTaskLogService;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(new AdminLoginResponse(adminAuthService.login(request.username(), request.password())));
    }

    @GetMapping("/metrics")
    public ResponseEntity<AdminMetricsResponse> metrics(@RequestHeader(ADMIN_TOKEN_HEADER) String token) {
        adminAuthService.validate(token);
        return ResponseEntity.ok(adminService.getMetrics());
    }

    @GetMapping("/users/progress")
    public ResponseEntity<List<AdminUserProgressResponse>> userProgress(@RequestHeader(ADMIN_TOKEN_HEADER) String token) {
        adminAuthService.validate(token);
        return ResponseEntity.ok(adminService.getUserProgress());
    }

    @DeleteMapping("/repositories/{repositoryId}")
    public ResponseEntity<RepositoryDeleteResponse> forceDeleteRepository(
            @RequestHeader(ADMIN_TOKEN_HEADER) String token,
            @PathVariable Long repositoryId
    ) {
        adminAuthService.validate(token);
        return ResponseEntity.ok(adminService.forceDeleteRepository(repositoryId));
    }

    @PatchMapping("/users/{userId}/email")
    public ResponseEntity<Void> forceUpdateEmail(
            @RequestHeader(ADMIN_TOKEN_HEADER) String token,
            @PathVariable Long userId,
            @RequestBody AdminEmailUpdateRequest request
    ) {
        adminAuthService.validate(token);
        adminService.forceUpdateEmail(userId, request.email());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/ban")
    public ResponseEntity<Void> setUserBan(
            @RequestHeader(ADMIN_TOKEN_HEADER) String token,
            @PathVariable Long userId,
            @RequestBody AdminBanRequest request
    ) {
        adminAuthService.validate(token);
        adminService.setUserBan(userId, request.banned());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AdminTaskLogResponse>> logs(@RequestHeader(ADMIN_TOKEN_HEADER) String token) {
        adminAuthService.validate(token);
        return ResponseEntity.ok(adminTaskLogService.latest());
    }
}
