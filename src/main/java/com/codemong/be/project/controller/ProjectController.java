package com.codemong.be.project.controller;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.project.dto.ProjectDetailResponse;
import com.codemong.be.project.dto.ProjectPreferenceCardResponse;
import com.codemong.be.project.dto.ProjectSpecResponse;
import com.codemong.be.project.dto.ProjectStepResponse;
import com.codemong.be.project.entity.Project;
import com.codemong.be.project.repository.ProjectRepository;
import com.codemong.be.project.repository.ProjectSpecRepository;
import com.codemong.be.project.repository.ProjectStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProjectStepRepository projectStepRepository;
    private final ProjectSpecRepository projectSpecRepository;

    @GetMapping
    public ResponseEntity<List<ProjectDetailResponse>> getProjects() {
        return ResponseEntity.ok(projectRepository.findAll().stream()
                .map(ProjectDetailResponse::from)
                .toList());
    }

    @GetMapping("/preference-cards")
    public ResponseEntity<List<ProjectPreferenceCardResponse>> getPreferenceCards() {
        return ResponseEntity.ok(List.of(
                new ProjectPreferenceCardResponse(
                        "spring-api",
                        "Spring API 리팩토링",
                        "Controller, Service, Repository 계층을 정리하며 백엔드 구조를 학습합니다.",
                        "BE",
                        "입문-중급",
                        "/run_report.png",
                        List.of("Spring", "REST API", "Layered")
                ),
                new ProjectPreferenceCardResponse(
                        "jpa-lab",
                        "JPA 연관관계 실험실",
                        "엔티티 관계, cascade, 트랜잭션 흐름을 작은 실습으로 익힙니다.",
                        "BE",
                        "중급",
                        "/dual-platform.png",
                        List.of("JPA", "Entity", "Transaction")
                ),
                new ProjectPreferenceCardResponse(
                        "rag-review",
                        "AI 코드 리뷰 챌린지",
                        "검사 결과와 코드 맥락을 바탕으로 AI 피드백을 개선하는 흐름을 경험합니다.",
                        "AI",
                        "중급",
                        "/chat.png",
                        List.of("AI Review", "RAG", "Feedback")
                ),
                new ProjectPreferenceCardResponse(
                        "daily-mong",
                        "DailyMong 루틴 학습",
                        "메일 기반 CS 문제와 코드 문제를 꾸준히 풀며 학습 루틴을 만듭니다.",
                        "CS",
                        "입문",
                        "/dailymong.png",
                        List.of("CS", "Mail", "Routine")
                )
        ));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetailResponse> getProject(@PathVariable Long projectId) {
        Project project = findProject(projectId);
        return ResponseEntity.ok(ProjectDetailResponse.from(project));
    }

    @GetMapping("/{projectId}/steps")
    public ResponseEntity<List<ProjectStepResponse>> getSteps(@PathVariable Long projectId) {
        findProject(projectId);
        return ResponseEntity.ok(projectStepRepository.findByProject_IdOrderByStepAsc(projectId).stream()
                .map(ProjectStepResponse::from)
                .toList());
    }

    @GetMapping("/{projectId}/steps/{step}/spec")
    public ResponseEntity<ProjectSpecResponse> getStepSpec(
            @PathVariable Long projectId,
            @PathVariable Integer step
    ) {
        Project project = findProject(projectId);
        return ResponseEntity.ok(projectSpecRepository.findByProject_IdAndStepAndType(projectId, step, project.getType())
                .map(ProjectSpecResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_SPEC_NOT_FOUND)));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }
}
