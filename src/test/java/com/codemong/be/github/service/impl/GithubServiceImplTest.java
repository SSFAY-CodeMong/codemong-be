package com.codemong.be.github.service.impl;

import com.codemong.be.project.entity.ProjectType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GithubServiceImplTest {

    @Test
    void buildAnswerPathUsesMainRepositoryFolderLayout() {
        String starterPath = GithubServiceImpl.buildAnswerPath(
                ProjectType.BE,
                "mmcafe",
                "step01-board-create-read",
                "starter"
        );
        String testsPath = GithubServiceImpl.buildAnswerPath(
                ProjectType.BE,
                "mmcafe",
                "step01-board-create-read",
                "tests"
        );
        String frontendStarterPath = GithubServiceImpl.buildAnswerPath(
                ProjectType.FE,
                "mmcafe",
                "step01-board-create-read",
                "starter"
        );

        assertThat(starterPath).isEqualTo("backend/mmcafe/step01-board-create-read/starter");
        assertThat(testsPath).isEqualTo("backend/mmcafe/step01-board-create-read/tests");
        assertThat(frontendStarterPath).isEqualTo("frontend/mmcafe/step01-board-create-read/starter");
    }

    @Test
    void buildTargetPathPlacesFrontendUnderFrontendDirectory() {
        assertThat(GithubServiceImpl.buildTargetRootPath(ProjectType.BE)).isBlank();
        assertThat(GithubServiceImpl.buildTargetRootPath(ProjectType.FE)).isEqualTo("frontend");

        assertThat(GithubServiceImpl.buildTargetPath("src/main/java/App.java", ""))
                .isEqualTo("src/main/java/App.java");
        assertThat(GithubServiceImpl.buildTargetPath("package.json", "frontend"))
                .isEqualTo("frontend/package.json");
        assertThat(GithubServiceImpl.buildTargetPath("src/App.tsx", "frontend"))
                .isEqualTo("frontend/src/App.tsx");
    }

    @Test
    void extractStepPrefixRemovesStepFolderSuffixForBranchName() {
        assertThat(GithubServiceImpl.extractStepPrefix("step02-board-listing"))
                .isEqualTo("step02");
        assertThat(GithubServiceImpl.extractStepPrefix("step01-board-create-read"))
                .isEqualTo("step01");
        assertThat(GithubServiceImpl.extractStepPrefix("step03"))
                .isEqualTo("step03");
    }

    @Test
    void extractStepNumberRemovesStepPrefixAndFolderSuffix() {
        assertThat(GithubServiceImpl.extractStepNumber("step02-board-listing"))
                .isEqualTo(2L);
        assertThat(GithubServiceImpl.extractStepNumber("step03"))
                .isEqualTo(3L);
    }
}
