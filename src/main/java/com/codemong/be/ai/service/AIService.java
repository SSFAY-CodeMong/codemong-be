package com.codemong.be.ai.service;

import com.codemong.be.ai.dto.CodeReviewResponse;
import com.codemong.be.ai.dto.UserQuestionRequest;
import com.codemong.be.ai.dto.UserQuestionResponse;
import com.codemong.be.branch.entity.Branch;
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
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

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
        String inputContents = mapToString(contents);
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

        // 3. LLM에 코드 보내기 with 부가정보(*프로젝트 스텝별 설명 등)
        String systemPrompt = """
                [SYSTEM_PROMPT]
                
                너는 Java/Spring Boot 백엔드 코드 리뷰어이자 멘토다.
                사용자가 제출한 전체 프로젝트 코드를 보고, 단순 실행 가능 여부뿐 아니라 유지보수성, 객체지향 설계, 계층 구조, 의존성 방향, 결합도, 응집도, SOLID 원칙, 예외 처리, 트랜잭션, 보안, 성능, 테스트 가능성까지 종합적으로 평가한다.
                
                반드시 코드에 근거해서 판단한다. 확인할 수 없는 내용은 추측하지 말고 “제공된 코드만으로는 확인하기 어렵다”고 말한다.
                문제점을 말할 때는 가능한 한 관련 파일 경로, 클래스명, 메서드명을 함께 언급한다.
                단순 비판이 아니라, 사용자가 바로 수정할 수 있도록 구체적인 개선 방향과 예시를 제시한다.
                
                응답은 반드시 한국어로 작성한다.
                초보 백엔드 개발자도 이해할 수 있게 설명하되, 기술적으로 중요한 개념은 정확히 사용한다.
                칭찬만 하거나 모호한 조언만 하지 말고, 우선순위가 높은 문제부터 구체적으로 짚는다.
                
                출력 형식은 반드시 아래 구조를 따른다.
                
                1. 전체 평가 요약
                
                * 코드의 전반적인 완성도
                * 가장 잘한 점
                * 가장 우선적으로 개선해야 할 점
                
                2. 동작 및 요구사항 관점
                
                * 코드가 의도한 기능을 수행할 가능성
                * 누락되었거나 위험해 보이는 로직
                * 예외 상황 처리 여부
                
                3. 설계 및 아키텍처 관점
                
                * Controller, Service, Repository 역할 분리
                * 계층 간 의존성 방향
                * 도메인 객체와 DTO 사용 적절성
                * 결합도와 응집도
                
                4. 객체지향 및 SOLID 관점
                
                * 단일 책임 원칙 위반 여부
                * 의존성 역전 또는 인터페이스 분리 필요 여부
                * 확장에 취약한 구조 여부
                
                5. Spring/JPA 관점
                
                * 트랜잭션 처리
                * 영속성 컨텍스트 사용
                * N+1 문제 가능성
                * 엔티티 설계 문제
                
                6. 보안 및 안정성 관점
                
                * 인증/인가 검증
                * 사용자 입력 검증
                * 민감 정보 노출 가능성
                * 예외 메시지 처리
                
                7. 개선 제안
                
                * 반드시 고쳐야 할 것
                * 고치면 좋은 것
                * 장기적으로 개선할 것
                
                8. 최종 점수
                
                * 동작 안정성: 0~10
                * 설계 품질: 0~10
                * 유지보수성: 0~10
                * Spring 활용도: 0~10
                * 총평
                
            """;

        String userPrompt = """
                [USER_PROMPT]
                
                아래는 사용자가 제출한 프로젝트 코드입니다.
                각 파일은 [FILE] 블록으로 구분되어 있으며, path는 파일 경로, content는 해당 파일의 코드입니다.
                
                이 코드를 전체적으로 리뷰해주세요.
                
                특히 다음 관점을 중요하게 봐주세요.
                
                * 실제로 기능이 정상 동작할 가능성이 있는지
                * Controller, Service, Repository의 책임 분리가 적절한지
                * 불필요한 결합이나 의존성이 있는지
                * SOLID 원칙을 위반하는 부분이 있는지
                * 예외 처리와 검증 로직이 충분한지
                * 트랜잭션 처리가 필요한 곳에 적용되어 있는지
                * 보안상 위험한 부분이 있는지
                * 테스트하기 어려운 구조가 있는지
                * 더 좋은 Spring Boot 코드로 개선할 수 있는 부분이 있는지
                
                문제점을 말할 때는 가능한 한 파일 경로와 클래스명을 함께 언급해주세요.
                수정 방향은 추상적으로 말하지 말고, 실제로 어떻게 바꾸면 좋은지 구체적으로 설명해주세요.
                
                [PROJECT_CODE]
                
                %s
                
            """.formatted(inputContents);

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(OpenAiChatOptions.builder()
                    .maxTokens(1000)
                    .build())
                .call()
                .content();


        // 4. LLM에 응답 대기 시간 동안 추가 질의 시 사용할 RAG를 위해 vectorDB 갱신
        ragService.save(userId, repositoryId, contents);

        // 5. LLM 응답 반환하기
        return new CodeReviewResponse(testPassed, codeCheckResult.failedTests(), answer);
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
                .user(userPrompt)
                .options(OpenAiChatOptions.builder()
                        .maxTokens(350)
                        .build())
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

    private String mapToString(Map<String, String> contents) {
        if (contents == null || contents.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("아래는 사용자의 프로젝트 코드 파일 목록과 각 파일의 내용입니다.\n");
        sb.append("각 파일은 [FILE] 블록으로 구분됩니다.\n\n");

        for (Map.Entry<String, String> entry : contents.entrySet()) {
            String filePath = entry.getKey();
            String content = entry.getValue();

            if (filePath == null || filePath.isBlank()) {
                continue;
            }

            sb.append("[FILE]\n");
            sb.append("path: ").append(filePath).append("\n");
            sb.append("content:\n");
            sb.append("```").append(resolveCodeFenceLanguage(filePath)).append("\n");

            if (content != null && !content.isBlank()) {
                sb.append(content.strip()).append("\n");
            }

            sb.append("```\n");
            sb.append("[/FILE]\n\n");
        }

        return sb.toString();
    }

    private String resolveCodeFenceLanguage(String filePath) {
        if (filePath == null) {
            return "";
        }

        if (filePath.endsWith(".java")) {
            return "java";
        }
        if (filePath.endsWith(".json")) {
            return "json";
        }
        if (filePath.endsWith(".xml")) {
            return "xml";
        }
        if (filePath.endsWith(".yml") || filePath.endsWith(".yaml")) {
            return "yaml";
        }
        if (filePath.endsWith(".properties")) {
            return "properties";
        }
        if (filePath.endsWith(".gradle")) {
            return "groovy";
        }
        if (filePath.endsWith(".js") || filePath.endsWith(".jsx")) {
            return "javascript";
        }
        if (filePath.endsWith(".ts") || filePath.endsWith(".tsx")) {
            return "typescript";
        }
        if (filePath.endsWith(".html")) {
            return "html";
        }
        if (filePath.endsWith(".css")) {
            return "css";
        }

        return "";
    }
}
