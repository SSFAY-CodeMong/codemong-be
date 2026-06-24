package com.codemong.be.report.service;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.branch.repository.BranchRepository;
import com.codemong.be.feedback.repository.FeedbackRepository;
import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.report.dto.BranchFeedback;
import com.codemong.be.report.dto.FinalReportResult;
import com.codemong.be.report.dto.ReportResponse;
import com.codemong.be.report.dto.ReportSummary;
import com.codemong.be.report.entity.Report;
import com.codemong.be.report.repository.ReportRepository;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final ReportRepository reportRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final BranchRepository branchRepository;
    private final FeedbackRepository feedbackRepository;
    private final ChatClient chatClient;

    @Async
    @Transactional
    public void getReport(Long repositoryId, Long userId) {
        GithubRepository repository = githubRepositoryRepository.findByIdWithUserAndProject(repositoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPOSITORY_NOT_FOUND));

        validateRepositoryOwner(repository, userId);

        List<BranchFeedback> branchFeedbacks = branchRepository.findByRepository_IdOrderByCreatedAtAsc(repositoryId)
                .stream()
                .map(this::findLatestFeedback)
                .filter(Objects::nonNull)
                .toList();

        if (branchFeedbacks.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REPOSITORY_REQUEST);
        }

        FinalReportResult result = generateFinalReport(repository, branchFeedbacks);
        Report report = upsertReport(repository, result.content(), result.score());
        reportRepository.save(report);
    }

    public List<ReportResponse> getReportList(Long userId) {
        return reportRepository.getReportList(userId)
                .stream()
                .map(this::toReportResponse)
                .toList();
    }

    private ReportResponse toReportResponse(ReportSummary summary) {
        return new ReportResponse(
                summary.id(),
                summary.repositoryId(),
                summary.projectName(),
                summary.content(),
                summary.score(),
                summary.createdAt(),
                feedbackRepository.findLatestDetailsByRepositoryId(summary.repositoryId())
        );
    }

    private void validateRepositoryOwner(GithubRepository repository, Long userId) {
        if (!repository.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.REPOSITORY_ACCESS_DENIED);
        }
    }

    private BranchFeedback findLatestFeedback(Branch branch) {
        return feedbackRepository.findFirstByBranch_IdOrderByCreatedAtDesc(branch.getId())
                .map(feedback -> new BranchFeedback(
                        branch.getStep(),
                        branch.getName(),
                        feedback.getContent()
                ))
                .orElse(null);
    }

    private FinalReportResult generateFinalReport(
            GithubRepository repository,
            List<BranchFeedback> branchFeedbacks
    ) {
        String feedbacks = branchFeedbacks.stream()
                .map(feedback -> """
                        [BRANCH]
                        step: %d
                        name: %s
                        
                        [FEEDBACK]
                        %s
                        """.formatted(
                        feedback.step(),
                        feedback.branchName(),
                        feedback.content()
                ))
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = """
                너는 Java/Spring Boot 백엔드 학습 프로젝트의 최종 평가자다.
                
                사용자가 하나의 레포지토리에서 단계별 브랜치를 완성했고,
                각 브랜치의 최신 피드백이 입력으로 제공된다.
                제공된 피드백만 근거로 프로젝트 전체 최종 평가를 작성한다.
                확인할 수 없는 내용은 추측하지 않는다.
                
                반드시 아래 형식으로만 응답한다.
                
                [SCORE]
                0부터 100 사이의 정수
                
                [CONTENT]
                1. 전체 요약
                2. 잘한 점
                3. 반복적으로 나타난 문제점
                4. 최종 개선 방향
                5. 최종 총평
                """;

        String userPrompt = """
                [REPOSITORY]
                name: %s
                project: %s
                
                [BRANCH_FEEDBACKS]
                %s
                """.formatted(
                repository.getName(),
                repository.getProject().getName(),
                feedbacks
        );

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        return parseFinalReport(answer);
    }

    private FinalReportResult parseFinalReport(String answer) {
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("최종 평가 생성 결과가 비어 있습니다.");
        }

        int score = parseScore(answer);
        String content = parseContent(answer);

        if (content.isBlank()) {
            throw new IllegalStateException("최종 평가 본문을 파싱할 수 없습니다.");
        }

        return new FinalReportResult(score, content);
    }

    private int parseScore(String answer) {
        Pattern pattern = Pattern.compile("\\[SCORE\\]\\s*(\\d{1,3})");
        Matcher matcher = pattern.matcher(answer);

        if (!matcher.find()) {
            throw new IllegalStateException("최종 평가 점수를 파싱할 수 없습니다.");
        }

        int score = Integer.parseInt(matcher.group(1));
        return Math.max(0, Math.min(100, score));
    }

    private String parseContent(String answer) {
        Pattern pattern = Pattern.compile("\\[CONTENT\\]\\s*(.*)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(answer);

        if (!matcher.find()) {
            return "";
        }

        return matcher.group(1).strip();
    }

    private Report upsertReport(GithubRepository repository, String content, int score) {
        List<Report> reports = reportRepository.findByGithubRepository_IdOrderByCreatedAtDesc(repository.getId());

        if (reports.isEmpty()) {
            return createReportEntity(repository, content, score);
        }

        Report report = reports.get(0);
        updateReportEntity(report, content, score);

        if (reports.size() > 1) {
            reportRepository.deleteAll(reports.subList(1, reports.size()));
        }

        return report;
    }

    private Report createReportEntity(GithubRepository repository, String content, int score) {
        try {
            Constructor<Report> constructor = Report.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            Report report = constructor.newInstance();
            setField(report, "githubRepository", repository);
            setField(report, "content", content);
            setField(report, "score", score);

            return report;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Report 엔티티를 생성할 수 없습니다.", e);
        }
    }

    private void updateReportEntity(Report report, String content, int score) {
        try {
            setField(report, "content", content);
            setField(report, "score", score);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Report 엔티티를 수정할 수 없습니다.", e);
        }
    }

    private void setField(Report report, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = Report.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(report, value);
    }

}
