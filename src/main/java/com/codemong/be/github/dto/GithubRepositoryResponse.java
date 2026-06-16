package com.codemong.be.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.kohsuke.github.GHRepository;

import java.net.URL;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GithubRepositoryResponse {
    private String name;
    private String fullName;
    @JsonProperty("private")
    private boolean privateRepository;
    private URL url;
    private String defaultBranch;

    public static GithubRepositoryResponse from(GHRepository repository) {
        return new GithubRepositoryResponse(
                repository.getName(),
                repository.getFullName(),
                repository.isPrivate(),
                repository.getHtmlUrl(),
                repository.getDefaultBranch()
        );
    }
}
