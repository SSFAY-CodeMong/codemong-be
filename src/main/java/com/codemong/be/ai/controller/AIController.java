package com.codemong.be.ai.controller;

import com.codemong.be.ai.dto.CodeReviewResponse;
import com.codemong.be.ai.dto.UserQuestionRequest;
import com.codemong.be.ai.dto.UserQuestionResponse;
import com.codemong.be.ai.service.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AIController {
    private final AIService aiService;

    @PostMapping("/repositories/{repositoryId}/steps/{step}/review")
    public ResponseEntity<CodeReviewResponse> codeReview(
            @PathVariable Long repositoryId,
            @PathVariable Long step,
            @AuthenticationPrincipal Long userId
    ){
        CodeReviewResponse codeReviewResponse = aiService.codeReview(repositoryId, step, userId);

        return ResponseEntity.ok(codeReviewResponse);
    }

    @PostMapping("/repositories/{repositoryId}/questions")
    public ResponseEntity<UserQuestionResponse> userQuestion(
            @Valid @RequestBody UserQuestionRequest userQuestionsRequest,
            @PathVariable Long repositoryId,
            @AuthenticationPrincipal Long userId
    ){
        UserQuestionResponse userQuestionResponse = aiService.userQuestion(userQuestionsRequest, repositoryId, userId);
        return ResponseEntity.ok(userQuestionResponse);
    }



}
