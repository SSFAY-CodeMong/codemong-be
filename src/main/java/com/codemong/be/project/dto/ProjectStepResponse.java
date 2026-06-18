package com.codemong.be.project.dto;

import com.codemong.be.project.entity.Project;

public record ProjectStepResponse(
        Long projectId,
        Long step,
        String stepId,
        String title,
        String description
) {
    public static ProjectStepResponse from(Project project, long step) {
        String stepId = String.format("step%02d", step);
        return new ProjectStepResponse(
                project.getId(),
                step,
                stepId,
                "Step " + step,
                project.getName() + " " + stepId + " 요구사항을 구현합니다."
        );
    }
}
