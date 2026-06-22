package com.codemong.be.report.controller;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.report.dto.ReportResponse;
import com.codemong.be.report.repository.ReportRepository;
import com.codemong.be.report.service.ReportService;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/repositories/{repositoryId}")
    public ResponseEntity<Void> getReport(
            @PathVariable Long repositoryId,
            @AuthenticationPrincipal Long userId
    ){
        reportService.getReport(repositoryId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<ReportResponse>> getReportList(
            @AuthenticationPrincipal Long userId
    ) {
        List<ReportResponse> reports = reportService.getReportList(userId);

        return ResponseEntity.ok(reports);
    }
}
