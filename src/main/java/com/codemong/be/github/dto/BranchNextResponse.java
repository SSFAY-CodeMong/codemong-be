package com.codemong.be.github.dto;

import com.codemong.be.branch.entity.Branch;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BranchNextResponse {
    private Long id;
    private String name;
    private String step;
    private String sha;
    private boolean success;

    public static BranchNextResponse from(Branch branch) {
        return new BranchNextResponse(
                branch.getId(),
                branch.getName(),
                branch.getStep(),
                branch.getSha(),
                branch.isSuccess()
        );
    }
}
