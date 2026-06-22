package com.codemong.be.rag.controller;

import com.codemong.be.rag.service.RAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
public class RAGController {
    private final RAGService ragService;

    @PostMapping("/repositories/{repositoryId}/steps/{step}/save")
    public ResponseEntity<Void> save(
            @PathVariable Long repositoryId,
            @PathVariable Long step,
            @AuthenticationPrincipal Long userId){
        // 합의 되면 step 추가
        ragService.save(userId, repositoryId, Map.of());
        return ResponseEntity.noContent().build();
    }

}
