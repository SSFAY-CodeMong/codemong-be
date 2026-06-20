package com.codemong.be.project.dto;

import com.codemong.be.project.entity.ProjectStep;

import java.util.List;

public record ProjectSpecResponse(
        Long projectId,
        Long step,
        String stepId,
        String title,
        String description,
        List<String> requirements
) {
    public static ProjectSpecResponse from(ProjectStep projectStep) {
        String stepId = String.format("step%02d", projectStep.getStep());
        return new ProjectSpecResponse(
                projectStep.getProject().getId(),
                (long) projectStep.getStep(),
                stepId,
                projectStep.getProject().getName() + " " + stepId,
                projectStep.getContent(),
                List.of(
                        "starter 코드의 TODO를 확인합니다.",
                        "요구사항에 맞게 기능을 구현합니다.",
                        "로컬 테스트를 통과하는지 검사합니다.",
                        "검사가 통과하면 다음 단계로 이동합니다."
                )
        );
    }
}
