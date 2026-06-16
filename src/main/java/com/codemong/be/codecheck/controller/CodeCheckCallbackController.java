package com.codemong.be.codecheck.controller;

import com.codemong.be.codecheck.dto.CodeCheckCallbackRequest;
import com.codemong.be.codecheck.service.CodeCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/code-check")
public class CodeCheckCallbackController {

    private final CodeCheckService codeCheckService;

    @PostMapping("/github-actions/callback")
    public ResponseEntity<Void> receiveGithubActionsCallback(
            @RequestBody CodeCheckCallbackRequest request
    ) {
        codeCheckService.receiveGithubActionsCallback(request);
        return ResponseEntity.ok().build();
    }
}
