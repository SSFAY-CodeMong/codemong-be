package com.codemong.be.project.dto;

import com.codemong.be.project.entity.ProjectSpec;
import com.codemong.be.project.entity.ProjectType;

import java.util.List;

public record ProjectSpecResponse(
        Long projectId,
        Long step,
        ProjectType type,
        String stepId,
        String title,
        String description,
        List<String> requirements
) {
    public static ProjectSpecResponse from(ProjectSpec projectSpec) {
        String stepId = String.format("step%02d", projectSpec.getStep());
        return new ProjectSpecResponse(
                projectSpec.getProject().getId(),
                (long) projectSpec.getStep(),
                projectSpec.getType(),
                stepId,
                projectSpec.getProject().getName() + " " + stepId,
                projectSpec.getContent(),
                List.of(
                        "starter 코드의 TODO를 확인합니다.",
                        "요구사항에 맞게 기능을 구현합니다.",
                        "로컬 테스트를 통과하는지 검사합니다.",
                        "검사가 통과하면 다음 단계로 이동합니다."
                )
        );
    }
}
