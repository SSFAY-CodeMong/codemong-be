package com.codemong.be.project.controller;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.project.dto.ProjectDetailResponse;
import com.codemong.be.project.dto.ProjectSpecResponse;
import com.codemong.be.project.dto.ProjectStepResponse;
import com.codemong.be.project.entity.Project;
import com.codemong.be.project.entity.ProjectStep;
import com.codemong.be.project.repository.ProjectRepository;
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

    @GetMapping
    public ResponseEntity<List<ProjectDetailResponse>> getProjects() {
        return ResponseEntity.ok(projectRepository.findAll().stream()
                .map(ProjectDetailResponse::from)
                .toList());
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
        findProject(projectId);
        ProjectStep projectStep = projectStepRepository.findByProject_IdAndStep(projectId, step)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
        return ResponseEntity.ok(ProjectSpecResponse.from(projectStep));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }
}
