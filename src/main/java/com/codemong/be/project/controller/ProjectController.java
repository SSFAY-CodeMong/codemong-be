package com.codemong.be.project.controller;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.project.dto.ProjectDetailResponse;
import com.codemong.be.project.dto.ProjectSpecResponse;
import com.codemong.be.project.dto.ProjectStepResponse;
import com.codemong.be.project.entity.Project;
import com.codemong.be.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.LongStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class ProjectController {

    private static final long DEFAULT_STEP_COUNT = 5L;

    private final ProjectRepository projectRepository;

    @GetMapping
    public ResponseEntity<List<ProjectDetailResponse>> getProjects() {
        return ResponseEntity.ok(projectRepository.findAll().stream()
                .map(project -> ProjectDetailResponse.from(project, DEFAULT_STEP_COUNT))
                .toList());
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetailResponse> getProject(@PathVariable Long projectId) {
        Project project = findProject(projectId);
        return ResponseEntity.ok(ProjectDetailResponse.from(project, DEFAULT_STEP_COUNT));
    }

    @GetMapping("/{projectId}/steps")
    public ResponseEntity<List<ProjectStepResponse>> getSteps(@PathVariable Long projectId) {
        Project project = findProject(projectId);
        return ResponseEntity.ok(LongStream.rangeClosed(1, DEFAULT_STEP_COUNT)
                .mapToObj(step -> ProjectStepResponse.from(project, step))
                .toList());
    }

    @GetMapping("/{projectId}/steps/{step}/spec")
    public ResponseEntity<ProjectSpecResponse> getStepSpec(
            @PathVariable Long projectId,
            @PathVariable Long step
    ) {
        Project project = findProject(projectId);
        return ResponseEntity.ok(ProjectSpecResponse.from(project, step));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }
}
