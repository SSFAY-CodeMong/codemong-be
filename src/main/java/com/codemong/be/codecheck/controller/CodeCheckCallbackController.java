package com.codemong.be.codecheck.controller;

import com.codemong.be.codecheck.dto.CodeCheckCallbackRequest;
import com.codemong.be.codecheck.dto.CodeCheckStartResponse;
import com.codemong.be.codecheck.dto.CodeCheckStatusResponse;
import com.codemong.be.codecheck.service.CodeCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/code-check")
public class CodeCheckCallbackController {

    private final CodeCheckService codeCheckService;

    @PostMapping("/repositories/{repositoryId}/steps/{step}")
    public ResponseEntity<CodeCheckStartResponse> startCheck(
            @PathVariable Long repositoryId,
            @PathVariable Long step,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.accepted().body(codeCheckService.startAsyncCheck(repositoryId, step, userId));
    }

    @GetMapping("/checks/{checkId}")
    public ResponseEntity<CodeCheckStatusResponse> getCheckStatus(
            @PathVariable String checkId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(codeCheckService.getAsyncCheckStatus(checkId, userId));
    }

    @PostMapping("/github-actions/callback")
    public ResponseEntity<Void> receiveGithubActionsCallback(
            @RequestBody CodeCheckCallbackRequest request
    ) {
        codeCheckService.receiveGithubActionsCallback(request);
        return ResponseEntity.ok().build();
    }
}
