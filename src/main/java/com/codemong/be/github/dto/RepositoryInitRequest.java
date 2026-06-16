package com.codemong.be.github.dto;

import com.codemong.be.project.entity.ProjectType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryInitRequest {
    private Long startStep;
    private String stepId;
    private ProjectType track;
    private ProjectType type;
}
