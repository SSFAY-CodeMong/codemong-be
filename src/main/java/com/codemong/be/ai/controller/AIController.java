package com.codemong.be.ai.controller;

import com.codemong.be.ai.dto.CodeReviewResponse;
import com.codemong.be.ai.service.AIService;
import com.codemong.be.github.service.GithubService;
import com.codemong.be.rag.service.RAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AIController {
    private final AIService aiService;

    @PostMapping("/projects/{projectId}/steps/{step}/review")
    public ResponseEntity<CodeReviewResponse> codeReview(
            @PathVariable Long projectId,
            @PathVariable Long step,
            @AuthenticationPrincipal Long userId
    ){
        CodeReviewResponse codeReviewResponse = aiService.codeReview(projectId, step, userId);

        return ResponseEntity.ok(codeReviewResponse);
    }




}
