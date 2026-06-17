package com.codemong.be.ai.service;

import com.codemong.be.ai.dto.CodeReviewResponse;
import com.codemong.be.ai.dto.UserQuestionRequest;
import com.codemong.be.ai.dto.UserQuestionResponse;
import com.codemong.be.branch.entity.Branch;
import com.codemong.be.branch.repository.BranchRepository;
import com.codemong.be.codecheck.dto.CodeCheckResult;
import com.codemong.be.codecheck.service.CodeCheckService;
import com.codemong.be.feedback.entity.Feedback;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        return new CodeReviewResponse(testPassed, codeCheckResult.failedTests(), "CodeReviewResponse");
    }

    public UserQuestionResponse userQuestion(UserQuestionRequest userQuestionRequest, Long repositoryId, Long userId) {
        // 0. 요청 사용자가 레포지토리의 주인인지 확인
        if(!isOwner(userId, repositoryId)){
            throw new RuntimeException("레포지토리 소유자만이 검사하기를 요청할 수 있습니다.");
        }

        // 1. 사용자의 질의와 관련된 코드들, 사용자의 이전 대화기록 수집
        String question = userQuestionRequest.question();
        String context = ragService.searchSimilarCode(question, userId, repositoryId);
        Branch curBranch = branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId)
                .orElseThrow(()-> new RuntimeException("TODO : 해당 브랜치가 없습니다."));
        Long branchId = curBranch.getId();
        String feedbackSummary = feedbackService.getLatestFeedback(branchId);

        // 2. LLM에 질의 하기
        String systemPrompt = """
            너는 Java/Spring 코드 설명과 코드 리뷰를 도와주는 백엔드 멘토다.
            
            출력은 반드시 아래 두 구역으로 나눈다.
            
            [USER_ANSWER]
            - 사용자에게 보여줄 답변이다.
            - 한국어로 답변한다.
            - 결론을 먼저 말한다.
            - 핵심 근거는 최대 3개 bullet로 설명한다.
            - 코드 예시는 꼭 필요할 때만 제공한다.
            - 250토큰 이내로 작성한다.
            
            [UPDATED_MEMORY_SUMMARY]
            - 사용자에게 보여주지 않고 DB에 저장할 내부 누적 요약이다.
            - 기존 누적 요약과 이번 질문/답변을 합쳐 최신 요약으로 갱신한다.
            - 다음 질문 이해에 필요한 내용만 남긴다.
            - 관련 클래스명, 메서드명, 사용자의 관심사를 포함한다.
            - 컨텍스트에 없는 사실은 추가하지 않는다.
            - 2~3문장 이내로 작성한다.
            - 100토큰 이내로 작성한다.
            """;

        String userPrompt = """
            아래 정보를 참고해서 사용자의 질문에 답변해줘.
            
            [기존 누적 요약]
            %s
            
            [코드 컨텍스트]
            %s
            
            [현재 질문]
            %s
            """.formatted(feedbackSummary, context, question);

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
        log.debug("모델: gpt-5-mini\n\n[System Prompt]\n{}\n\n[User Prompt]\n{}"
                , systemPrompt, userPrompt);

        // 3. 피드백 내용 저장하기(요약)
        String userAnswer = parsing(answer, "user");
        String updatedSummary = parsing(answer, "summary");
        feedbackService.save(curBranch, updatedSummary);

        // 4. LLM 응답 반환하기
        return new UserQuestionResponse(userAnswer);
    }

    private boolean isOwner(Long userId, Long repositoryId){
        GithubRepository repo = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(()-> new RuntimeException("레포지토리를 찾을 수 없습니다."));
        return userId.equals(repo.getUser().getId());
    }

    private String parsing(String answer, String type){
        if (answer == null || answer.isBlank()) {
            return "";
        }
        String regex = (type.equals("user")) ?
                "\\[USER_ANSWER\\]\\s*(.*?)(?=\\n\\[[A-Z_]+\\]|\\z)"
                : "\\[UPDATED_MEMORY_SUMMARY\\]\\s*(.*?)(?=\\n\\[[A-Z_]+\\]|\\z)";

        Pattern pattern = Pattern.compile(
                regex,
                Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(answer);

        if (!matcher.find()) {
            return "";
        }

        return matcher.group(1).strip();
    }
}
