package com.codemong.be.github;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "GITHUB_TOKEN", matches = ".+")
class GithubApiIntegrationTest {

    private static final String TEST_REPOSITORY_PREFIX = "codemong-api-test-";
    private static final String TEST_BRANCH_NAME = "board-step01-test";

    @Test
    void connectToGithub() throws IOException {
        GitHub github = github();

        GHMyself myself = github.getMyself();

        assertThat(myself.getLogin()).isNotBlank();
    }

    @Test
    void getMyRepositories() throws IOException {
        GitHub github = github();

        Map<String, GHRepository> repositories = github.getMyself().getRepositories();

        assertThat(repositories).isNotNull();
    }

    @Test
    void createRepositoryAndBranch() throws IOException {
        GitHub github = github();
        GHRepository repository = null;

        try {
            repository = github.createRepository(TEST_REPOSITORY_PREFIX + Instant.now().toEpochMilli())
                    .description("Codemong GitHub API integration test repository")
                    .private_(true)
                    .autoInit(true)
                    .create();

            Map<String, GHBranch> branches = repository.getBranches();
            String baseBranchName = findBaseBranchName(repository, branches);
            String baseSha = repository.getRef("heads/" + baseBranchName).getObject().getSha();

            GHRef branch = repository.createRef("refs/heads/" + TEST_BRANCH_NAME, baseSha);

            assertThat(repository.getName()).startsWith(TEST_REPOSITORY_PREFIX);
            assertThat(branch.getRef()).isEqualTo("refs/heads/" + TEST_BRANCH_NAME);
        } finally {
            if (repository != null) {
                repository.delete();
            }
        }
    }

    private GitHub github() throws IOException {
        return new GitHubBuilder()
                .withOAuthToken(System.getenv("GITHUB_TOKEN"))
                .build();
    }

    private String findBaseBranchName(GHRepository repository, Map<String, GHBranch> branches) {
        String defaultBranch = repository.getDefaultBranch();
        if (defaultBranch != null && branches.containsKey(defaultBranch)) {
            return defaultBranch;
        }

        if (branches.containsKey("main")) {
            return "main";
        }

        if (branches.containsKey("master")) {
            return "master";
        }

        throw new IllegalStateException("Cannot find base branch for test repository.");
    }
}
