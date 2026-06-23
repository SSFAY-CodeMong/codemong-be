package com.codemong.be.ai.service;

import com.codemong.be.ai.dto.CodeReviewResponse;
import com.codemong.be.ai.dto.FailedTestResponse;
import com.codemong.be.ai.dto.UserQuestionRequest;
import com.codemong.be.ai.dto.UserQuestionResponse;
import com.codemong.be.branch.entity.Branch;
import com.codemong.be.branch.repository.BranchRepository;
import com.codemong.be.chathistory.service.ChatHistoryService;
import com.codemong.be.codecheck.dto.CodeCheckResult;
import com.codemong.be.codecheck.service.CodeCheckService;
import com.codemong.be.feedback.service.FeedbackService;
import com.codemong.be.github.service.GithubService;
import com.codemong.be.project.entity.ProjectStep;
import com.codemong.be.project.entity.TestCode;
import com.codemong.be.project.repository.ProjectStepRepository;
import com.codemong.be.project.repository.TestCodeRepository;
import com.codemong.be.rag.service.RAGService;
import com.codemong.be.report.service.ReportService;
import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.repository.repository.GithubRepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final ChatClient chatClient;
    private final TestCodeRepository testCodeRepository;
    private final ChatHistoryService chatHistoryService;
    private final FeedbackService feedbackService;
    private final ReportService reportService;
    private final ProjectStepRepository projectStepRepository;


    public CodeReviewResponse codeReview(Long repositoryId, Long step, Long userId) {
        // 1. 사용자의 코드 & *프로젝트 스텝별 설명 가져오기
        GithubRepository githubRepository = githubRepositoryRepository.findById(repositoryId)
                .orElseThrow(()-> new RuntimeException("TODO : 레포지토리를 찾을 수 없습니다."));
        long start = System.nanoTime();
        Map<String, String> contents = githubService.getBranchContents(repositoryId, step, userId);
        String inputContents = mapToString(contents);
        ProjectStep projectStep = projectStepRepository.findByProject_IdAndStep(githubRepository.getProject().getId(), step.intValue())
                        .orElseThrow(()-> new RuntimeException("TODO: 스탭을 찾을 수 없습니다."));
        String stepInfo = projectStep.getContent();

        log.debug("[codeReview] owner check took {} ms", elapsedMs(start));

        // TODO: 2. github actions 결과 받아오기
        start = System.nanoTime();
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
        log.debug("[GithubActions] Github Actions took {} ms", elapsedMs(start));

        // 3. LLM에 코드 보내기 with 부가정보(*프로젝트 스텝별 설명 등)
        String systemPrompt = """
                너는 Java/Spring Boot 백엔드 코드 리뷰어이자 멘토다.
        
                사용자가 제출한 프로젝트 코드를 보고 다음 관점에서 평가한다.
        
                - 기능이 정상 동작할 가능성
                - Controller, Service, Repository의 역할 분리
                - 계층 간 의존성 방향, 결합도, 응집도
                - 객체지향 설계와 SOLID 원칙
                - 예외 처리, 입력 검증, 트랜잭션 처리
        
                반드시 제공된 코드에 근거해서 판단하고, 확인할 수 없는 내용은 추측하지 않는다.
                문제점은 가능한 한 파일 경로, 클래스명, 메서드명을 함께 언급한다.
                단순한 지적이 아니라 코드의 개선 방향을 제시한다.
                응답은 한국어로 작성하고, 초보 백엔드 개발자도 이해할 수 있게 설명한다.
        
                출력 형식 Markdown 형식이며, 내용은 아래를 따른다. 최종 점수는 백점 만점으로 계산하여 제시한다.
        
                1. 전체 요약
                2. 잘한 점
                3. 주요 문제점
                4. 개선 제안
                5. 최종 점수와 총평
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
                * 더 좋은 Spring Boot 코드로 개선할 수 있는 부분이 있는지
                
                문제점을 말할 때는 가능한 한 파일 경로와 클래스명을 함께 언급해주세요.
                수정 방향은 사용자가 직접 생각하고 수정할 수 있도록 코드를 제공하기보다는 방향을 제시해주세요.
                
                [PROJECT_CODE]
                
                %s
                
                아래는 현재 사용자가 구현해야 하는 스텝 요구사항입니다.
                이 요구사항을 기준으로 코드가 제대로 구현되었는지 중점적으로 리뷰해주세요.
                [CURRENT_STEP_REQUIREMENT]
                
                %s
                
            """.formatted(inputContents, stepInfo);

        start = System.nanoTime();
        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
//                .options(OpenAiChatOptions.builder()
//                    .maxCompletionTokens(4000)
//                    .reasoningEffort("low")
//                    .build())
                .call()
                .content();

        log.debug("[ChatClient Call] ChatClient Call took {} ms", elapsedMs(start));

        String feedbackContent = parseFinalReview(answer);

        // step 수정시 수정할 부분
        Branch curBranch = branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId)
                .orElseThrow(()-> new RuntimeException("TODO : 해당 브랜치가 없습니다."));

        feedbackService.save(curBranch, feedbackContent);

        boolean isSaved = false;

        // 4. LLM에 응답 대기 시간 동안 추가 질의 시 사용할 RAG를 위해 vectorDB 갱신
        start = System.nanoTime();
        try {
            ragService.save(userId, repositoryId, contents);
            isSaved = true;
        }catch (Exception e){
            log.error("RAG 저장 실패", e);
        }
        log.debug("[RAG Save] RAG Save took {} ms", elapsedMs(start));
        // 5. LLM 응답 반환하기
        List<FailedTestResponse> failedTestDetails = resolveFailedTestDetails(
                repositoryId,
                step,
                codeCheckResult.failedTests()
        );

        // 최종 스텝 && isSuccess => report 작성 비동기 처리
        Long curStep = curBranch.getStep();
        int maxStep = githubRepository.getProject().getMaxStep();
        if(testPassed && curStep.equals((long) maxStep)){
            reportService.getReport(repositoryId, userId);
        }

        return new CodeReviewResponse(testPassed, codeCheckResult.failedTests(), failedTestDetails, answer, isSaved);
    }


    public UserQuestionResponse userQuestion(UserQuestionRequest userQuestionRequest, Long repositoryId, Long userId) {
        // 1. 사용자의 질의와 관련된 코드들, 사용자의 이전 대화기록 수집
        String question = userQuestionRequest.question();
        String context = ragService.searchSimilarCode(question, userId, repositoryId);
        Branch curBranch = branchRepository.findTopByRepository_IdOrderByCreatedAtDesc(repositoryId)
                .orElseThrow(()-> new RuntimeException("TODO : 해당 브랜치가 없습니다."));
        Long branchId = curBranch.getId();
        String feedbackSummary = chatHistoryService.getLatestChatHistory(branchId);

        // 2. LLM에 질의 하기
        String systemPrompt = """
            너는 Java/Spring 코드 설명과 코드 리뷰를 도와주는 백엔드 멘토다.
            
            Java/Spring/html/css/js 등 웹 프로그래밍과 관련되지 않은 질문은 답변하지마.
            
            출력은 반드시 아래 두 구역으로 나눈다.
            
            [USER_ANSWER]
            - 사용자에게 보여줄 답변이다.
            - 한국어로 답변한다.
            - 핵심 근거는 최대 3개 bullet로 설명한다.
            - 코드 예시는 꼭 필요할 때만 제공한다.
            
            [UPDATED_MEMORY_SUMMARY]
            - 사용자에게 보여주지 않고 DB에 저장할 내부 누적 요약이다.
            - 기존 누적 요약과 이번 질문/답변을 합쳐 최신 요약으로 갱신한다.
            - 다음 질문 이해에 필요한 내용만 남긴다.
            - 관련 클래스명, 메서드명, 사용자의 관심사를 포함한다.
            - 컨텍스트에 없는 사실은 추가하지 않는다.
            - 2~3문장 이내로 작성한다.
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
//                .options(OpenAiChatOptions.builder()
//                        .maxCompletionTokens(350)
//                        .build())
                .call()
                .content();
        log.debug("모델: gpt-5-mini\n\n[System Prompt]\n{}\n\n[User Prompt]\n{}"
                , systemPrompt, userPrompt);

        // 3. 피드백 내용 저장하기(요약)
        String userAnswer = parsing(answer, "user");
        String updatedSummary = parsing(answer, "summary");
        chatHistoryService.save(curBranch, updatedSummary);

        // 4. LLM 응답 반환하기
        return new UserQuestionResponse(userAnswer);
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

    private List<FailedTestResponse> resolveFailedTestDetails(Long repositoryId, Long step, List<String> failedTests) {
        if (failedTests == null || failedTests.isEmpty()) {
            return List.of();
        }

        GithubRepository repository = githubRepositoryRepository.findByIdWithUserAndProject(repositoryId)
                .orElseThrow(() -> new RuntimeException("Repository not found."));
        int stepNumber = step == null ? 1 : step.intValue();
        List<String> methodNames = failedTests.stream()
                .map(this::normalizeMethodName)
                .distinct()
                .toList();
        Map<String, TestCode> testCodeByMethodName = testCodeRepository
                .findByProject_IdAndStepAndMethodNameIn(repository.getProject().getId(), stepNumber, methodNames)
                .stream()
                .collect(Collectors.toMap(TestCode::getMethodName, testCode -> testCode));

        return failedTests.stream()
                .map(failedTest -> {
                    String methodName = normalizeMethodName(failedTest);
                    TestCode testCode = testCodeByMethodName.get(methodName);
                    String description = testCode == null
                            ? "해당 테스트 메서드 설명이 아직 등록되어 있지 않습니다."
                            : testCode.getDescription();
                    return new FailedTestResponse(failedTest, methodName, description);
                })
                .toList();
    }

    private String normalizeMethodName(String failedTest) {
        if (failedTest == null || failedTest.isBlank()) {
            return "";
        }
        String trimmed = failedTest.trim();
        int lastDotIndex = trimmed.lastIndexOf('.');
        String methodName = lastDotIndex >= 0 ? trimmed.substring(lastDotIndex + 1) : trimmed;
        return methodName.replaceFirst("\\(.*\\)$", "");
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

    private long elapsedMs(long startNanoTime) {
        return (System.nanoTime() - startNanoTime) / 1_000_000;
    }

    private String parseFinalReview(String answer) {
        if (answer == null || answer.isBlank()) {
            return "";
        }

        String regex = "(?ms)^\\s*5\\.\\s*최종 점수와 총평\\s*\\R?(.*)\\z";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(answer);

        if (!matcher.find()) {
            return "";
        }

        return matcher.group(1).strip();
    }
}
