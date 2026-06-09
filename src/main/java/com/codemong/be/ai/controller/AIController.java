package com.codemong.be.ai.controller;

import com.codemong.be.ai.service.AIService;
import com.codemong.be.github.service.GithubService;
import com.codemong.be.rag.service.RAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AIController {
    private final AIService aiService;
    private final RAGService ragService;
    private final GithubService githubService;





}
