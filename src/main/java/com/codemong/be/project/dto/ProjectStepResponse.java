package com.codemong.be.project.dto;

import com.codemong.be.project.entity.ProjectStep;

public record ProjectStepResponse(
        Long projectId,
        Long step,
        String stepId,
        String title,
        String description
) {
    public static ProjectStepResponse from(ProjectStep projectStep) {
        String stepId = String.format("step%02d", projectStep.getStep());
        return new ProjectStepResponse(
                projectStep.getProject().getId(),
                (long) projectStep.getStep(),
                stepId,
                "Step " + projectStep.getStep(),
                projectStep.getContent()
        );
    }
}
