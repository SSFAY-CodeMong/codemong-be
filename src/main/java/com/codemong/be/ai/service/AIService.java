package com.codemong.be.ai.service;

import com.codemong.be.ai.dto.CodeReviewResponse;
import com.codemong.be.feedback.service.FeedbackService;
import com.codemong.be.github.service.GithubService;
import com.codemong.be.rag.service.RAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIService {
    private final GithubService githubService;
    private final RAGService ragService;
    private final FeedbackService feedbackService;


    public CodeReviewResponse codeReview(Long projectId, Long step, Long userId) {
        // 1. 사용자의 코드 & 이전 대화 요약본 & *프로젝트 스텝별 설명 가져오기

        // TODO: 2. github actions 결과 받아오기

        // 3. LLM에 코드 보내기 with 부가정보(테스트 결과, 이전 대화 요약본, *프로젝트 스텝별 설명 등)

        // 4. LLM에 응답 대기 시간 동안 추가 질의 시 사용할 RAG를 위해 vectorDB 갱신

        // 5. 피드백 내용을 저장하기(요약)

        // 6. LLM 응답 반환하기

        return new CodeReviewResponse("asd");
    }
}
