package com.codemong.be.project.dto;

import com.codemong.be.project.entity.Project;

import java.util.List;

public record ProjectSpecResponse(
        Long projectId,
        Long step,
        String stepId,
        String title,
        String description,
        List<String> requirements
) {
    public static ProjectSpecResponse from(Project project, long step) {
        String stepId = String.format("step%02d", step);
        return new ProjectSpecResponse(
                project.getId(),
                step,
                stepId,
                project.getName() + " " + stepId,
                project.getDescription(),
                List.of(
                        "starter 코드의 TODO를 확인합니다.",
                        "요구사항에 맞게 기능을 구현합니다.",
                        "로컬 테스트를 통과한 뒤 검사하기를 실행합니다.",
                        "검사가 통과하면 다음 단계로 이동합니다."
                )
        );
    }
}
