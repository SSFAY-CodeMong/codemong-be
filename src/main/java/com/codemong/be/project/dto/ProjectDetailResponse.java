package com.codemong.be.project.dto;

import com.codemong.be.project.entity.Project;
import com.codemong.be.project.entity.ProjectType;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectDetailResponse(
        Long id,
        String name,
        String title,
        ProjectType type,
        String description,
        String difficulty,
        String category,
        long steps,
        List<String> goals,
        List<String> stacks,
        LocalDateTime createdAt
) {
    public static ProjectDetailResponse from(Project project, long steps) {
        String category = project.getType() == ProjectType.BE ? "Backend" : "Frontend";
        List<String> stacks = project.getType() == ProjectType.BE
                ? List.of("Spring Boot", "JPA", "GitHub Actions", "REST API")
                : List.of("Vue", "JavaScript", "CSS", "REST API");
        return new ProjectDetailResponse(
                project.getId(),
                project.getName(),
                project.getName(),
                project.getType(),
                project.getDescription(),
                "Basic",
                category,
                steps,
                List.of("요구사항을 단계별로 구현합니다.", "GitHub 저장소에서 작업합니다.", "검사 결과를 확인하고 다음 단계로 이동합니다."),
                stacks,
                project.getCreatedAt()
        );
    }
}
