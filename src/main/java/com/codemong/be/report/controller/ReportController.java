package com.codemong.be.report.controller;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.report.dto.ReportResponse;
import com.codemong.be.report.repository.ReportRepository;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportRepository reportRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;

    @GetMapping("/repositories/{repositoryId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReportResponse>> getReports(
            @PathVariable Long repositoryId,
            @AuthenticationPrincipal Long userId
    ) {
        GithubRepository repository = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPOSITORY_NOT_FOUND));
        if (!repository.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.REPOSITORY_ACCESS_DENIED);
        }

        return ResponseEntity.ok(reportRepository.findByGithubRepository_IdOrderByCreatedAtDesc(repositoryId)
                .stream()
                .map(ReportResponse::from)
                .toList());
    }
}
