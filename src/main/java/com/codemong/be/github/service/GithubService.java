package com.codemong.be.github.service;

import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import java.util.Map;

public interface GithubService {

    String connectTest(String param);
    GHMyself gitConnectTest();
    Map<String, GHRepository> gitRepositoryTest();
    GHRepository gitGenerateTest();
    GHRef gitGenerateBranchTest(String repoName);
}
