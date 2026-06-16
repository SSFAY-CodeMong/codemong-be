package com.codemong.be.ai.service;

import com.codemong.be.ai.dto.CodeReviewResponse;
import com.codemong.be.ai.dto.UserQuestionRequest;
import com.codemong.be.ai.dto.UserQuestionResponse;
import com.codemong.be.branch.repository.BranchRepository;
import com.codemong.be.codecheck.dto.CodeCheckResult;
import com.codemong.be.codecheck.service.CodeCheckService;
import com.codemong.be.feedback.service.FeedbackService;
import com.codemong.be.github.service.GithubService;
import com.codemong.be.rag.service.RAGService;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {
    private final GithubService githubService;
    private final CodeCheckService codeCheckService;
    private final BranchRepository branchRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final RAGService ragService;
    private final FeedbackService feedbackService;
    private final ChatClient chatClient;


    public CodeReviewResponse codeReview(Long repositoryId, Long step, Long userId) {
        // 0. 요청 사용자가 레포지토리의 주인인지 확인
        if(!isOwner(userId, repositoryId)){
            throw new RuntimeException("레포지토리 소유자만이 검사하기를 요청할 수 있습니다.");
        }

        // 1. 사용자의 코드 & *프로젝트 스텝별 설명 가져오기
        Map<String, String> contents = githubService.getBranchContents(repositoryId, step, userId);

        // TODO: 2. github actions 결과 받아오기
        log.info("============================== actions call =============================");
        CodeCheckResult codeCheckResult = codeCheckService.runGithubActionsCheck(repositoryId, step, userId);
        boolean testPassed = codeCheckResult.passed();
        if (testPassed) { // 추후, 다음 스텝 넘어가기 수행 시 기준이되는 isSuccess 변경
            branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId)
                    .ifPresent(branch -> {
                        branch.markSuccess();
                        branchRepository.save(branch);
                    });
        }

        // 3. LLM에 코드 보내기 with 부가정보(테스트 결과, *프로젝트 스텝별 설명 등)
        // TODO: testPassed 값을 프롬프트 컨텍스트에 포함한다.

        // 4. LLM에 응답 대기 시간 동안 추가 질의 시 사용할 RAG를 위해 vectorDB 갱신
        ragService.save(userId, repositoryId, contents);

        // 5. 피드백 내용을 저장하기(요약)

        // 6. LLM 응답 반환하기

        return new CodeReviewResponse(true, List.of(), "CodeReviewResponse");
    }

    public UserQuestionResponse userQuestion(UserQuestionRequest userQuestionRequest, Long repositoryId, Long userId) {
        // 0. 요청 사용자가 레포지토리의 주인인지 확인
        if(!isOwner(userId, repositoryId)){
            throw new RuntimeException("레포지토리 소유자만이 검사하기를 요청할 수 있습니다.");
        }

        // 1. 사용자의 질의와 관련된 코드들, 사용자의 이전 대화기록 수집
        String question = userQuestionRequest.question();
        String context = ragService.searchSimilarCode(question, userId, repositoryId);
        // 2. LLM에 질의 하기
        String promptEngineering = """
                      너는 Java/Spring 코드 리뷰 도우미다.

                      답변 규칙:
                      - 한국어로 답변한다.
                      - 먼저 결론을 2문장 이내로 말한다.
                      - 그 다음 근거를 bullet로 정리한다.
                      - 모르는 내용은 추측하지 말고 "컨텍스트만으로는 알 수 없습니다"라고 말한다.
                      - 코드 예시는 필요한 경우에만 제공한다.
                      - 마지막에 답변 내용의 간단한 요약을 구분하여 제공한다.

                      답변 형식:
                      [결론]
                      ...

                      [근거]
                      - ...

                      [예시]
                      ```java
                      ...
                      ```
                      
                      [요약]
                      - ...
                      """;
        String userPrompt ="""
              아래 코드 컨텍스트를 참고해서 질문에 답변해줘.
              컨텍스트에 없는 내용은 추측하지 마.

              [코드 컨텍스트]
              %s

              [질문]
              %s
              """.formatted(context, question);
//        String answer = chatClient.prompt()
//                .system(promptEngineering) //역할/답변 형식/제약 조건은 system, 실제 질문은 user
//                .user(question) // rag 결과는 여기에 프롬프트로 추가해서 넣기
//                .call()
//                .content();
        log.debug("모델: gpt-5-mini\n\n[System Prompt]\n{}\n\n[User Prompt]\n{}"
                , promptEngineering, userPrompt);

        // 3. 피드백 내용 저장하기(요약)

        // 4. LLM 응답 반환하기

        return new UserQuestionResponse("userQuestionResponse");
    }

    private boolean isOwner(Long userId, Long repositoryId){
        GithubRepository repo = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(()-> new RuntimeException("레포지토리를 찾을 수 없습니다."));
        return userId.equals(repo.getUser().getId());
    }
}
