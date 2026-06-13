package com.codemong.be.ai.service;

import com.codemong.be.ai.dto.CodeReviewResponse;
import com.codemong.be.ai.dto.UserQuestionRequest;
import com.codemong.be.ai.dto.UserQuestionResponse;
import com.codemong.be.codecheck.dto.CodeCheckResult;
import com.codemong.be.codecheck.service.CodeCheckService;
import com.codemong.be.feedback.service.FeedbackService;
import com.codemong.be.github.service.GithubService;
import com.codemong.be.rag.service.RAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIService {
    private final GithubService githubService;
    private final CodeCheckService codeCheckService;
    private final RAGService ragService;
    private final FeedbackService feedbackService;
    private final ChatClient chatClient;


    public CodeReviewResponse codeReview(Long repositoryId, Long step, Long userId) {
        // 1. 사용자의 코드 & *프로젝트 스텝별 설명 가져오기
        Map<String, String> contents = githubService.getBranchContents(repositoryId, step, userId);

        // TODO: 2. github actions 결과 받아오기
        CodeCheckResult codeCheckResult = codeCheckService.runGithubActionsCheck(repositoryId, step, userId);
        boolean testPassed = codeCheckResult.passed();

        // 3. LLM에 코드 보내기 with 부가정보(테스트 결과, *프로젝트 스텝별 설명 등)
        // TODO: testPassed 값을 프롬프트 컨텍스트에 포함한다.

        // 4. LLM에 응답 대기 시간 동안 추가 질의 시 사용할 RAG를 위해 vectorDB 갱신
        ragService.save(userId, repositoryId, contents);

        // 5. 피드백 내용을 저장하기(요약)

        // 6. LLM 응답 반환하기

        return new CodeReviewResponse("CodeReviewResponse");
    }

    public UserQuestionResponse userQuestion(UserQuestionRequest userQuestionRequest, Long repositoryId, Long userId) {
        // 1. 사용자의 질의와 관련된 코드들, 사용자의 이전 대화기록 수집
        String question = userQuestionRequest.question();
        String context = ragService.searchSimilarCode(question, userId, repositoryId);
        // 2. LLM에 질의 하기

        // 3. 피드백 내용 저장하기(요약)

        // 4. LLM 응답 반환하기

        return new UserQuestionResponse("userQuestionResponse");
    }
}
