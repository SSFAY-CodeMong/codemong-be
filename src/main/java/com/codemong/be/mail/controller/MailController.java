package com.codemong.be.mail.controller;

import com.codemong.be.mail.dto.MailDashboardResponse;
import com.codemong.be.mail.dto.MailAnswerRequest;
import com.codemong.be.mail.dto.MailAnswerResponse;
import com.codemong.be.mail.dto.MailContentResponse;
import com.codemong.be.mail.dto.MailQuestionResponse;
import com.codemong.be.mail.dto.MailSendLogResponse;
import com.codemong.be.mail.dto.MailSubscriptionRequest;
import com.codemong.be.mail.dto.MailSubscriptionResponse;
import com.codemong.be.mail.service.MailQuestionService;
import com.codemong.be.mail.service.MailSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
public class MailController {

    private final MailSubscriptionService mailSubscriptionService;
    private final MailQuestionService mailQuestionService;

    @GetMapping("/dashboard")
    public ResponseEntity<MailDashboardResponse> dashboard(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(mailSubscriptionService.dashboard(userId));
    }

    @PutMapping("/subscription")
    public ResponseEntity<MailSubscriptionResponse> updateSubscription(
            @AuthenticationPrincipal Long userId,
            @RequestBody MailSubscriptionRequest request
    ) {
        return ResponseEntity.ok(mailSubscriptionService.updateSubscription(userId, request.enabled(), request.email()));
    }

    @PostMapping("/send-test")
    public ResponseEntity<MailSendLogResponse> sendTest(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(mailSubscriptionService.sendTestMail(userId));
    }

    @GetMapping("/questions/random")
    public ResponseEntity<MailQuestionResponse> randomQuestion() {
        return ResponseEntity.ok(mailQuestionService.randomQuestion());
    }

    @PostMapping("/questions/{questionId}/answers")
    public ResponseEntity<MailAnswerResponse> submitAnswer(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long questionId,
            @RequestBody MailAnswerRequest request
    ) {
        return ResponseEntity.ok(mailQuestionService.submit(userId, questionId, request));
    }

    @GetMapping("/answers")
    public ResponseEntity<java.util.List<MailAnswerResponse>> answers(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(mailQuestionService.myAnswers(userId));
    }

    @GetMapping("/contents")
    public ResponseEntity<java.util.List<MailContentResponse>> contents() {
        return ResponseEntity.ok(mailQuestionService.contents());
    }

    @GetMapping("/contents/{track}/{name}")
    public ResponseEntity<MailContentResponse> content(@PathVariable String track, @PathVariable String name) {
        return ResponseEntity.ok(mailQuestionService.content(track + "/" + name));
    }
}
