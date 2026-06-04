package com.codemong.be.github.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDeleteResponse {
    private Long deletedRepositoryId;
    private String name;
    private String htmlUrl;
}

/*
        body.put("deletedRepositoryId", latestRepository.getId());
        body.put("name", latestRepository.getName());
        body.put("url", latestRepository.getHtmlUrl());
 */