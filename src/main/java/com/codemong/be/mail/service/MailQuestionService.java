package com.codemong.be.mail.service;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.mail.dto.MailAnswerRequest;
import com.codemong.be.mail.dto.MailAnswerResponse;
import com.codemong.be.mail.dto.MailContentResponse;
import com.codemong.be.mail.dto.MailEvaluationResult;
import com.codemong.be.mail.dto.MailQuestionResponse;
import com.codemong.be.mail.entity.MailAnswer;
import com.codemong.be.mail.entity.MailQuestion;
import com.codemong.be.mail.repository.MailAnswerRepository;
import com.codemong.be.mail.repository.MailQuestionRepository;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailQuestionService {

    private final UserRepository userRepository;
    private final MailQuestionRepository mailQuestionRepository;
    private final MailAnswerRepository mailAnswerRepository;
    private final MailContentService mailContentService;
    private final ChatClient chatClient;

    @Transactional(readOnly = true)
    public MailQuestionResponse randomQuestion() {
        return mailQuestionRepository.findRandomQuestion()
                .map(MailQuestionResponse::from)
                .orElseThrow(() -> new IllegalStateException("메일 질문이 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<MailContentResponse> contents() {
        return mailContentService.list();
    }

    @Transactional(readOnly = true)
    public MailContentResponse content(String id) {
        return mailContentService.find(id)
                .orElseThrow(() -> new IllegalArgumentException("메일 콘텐츠를 찾을 수 없습니다."));
    }

    @Transactional
    public MailAnswerResponse submit(Long userId, Long questionId, MailAnswerRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
        MailQuestion question = mailQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));
        String answer = request.content() == null ? "" : request.content();
        String code = request.codeContent() == null ? "" : request.codeContent();
        MailEvaluationResult evaluation = evaluate(question, answer, code);
        MailAnswer saved = mailAnswerRepository.save(new MailAnswer(
                user,
                question,
                answer,
                code,
                evaluation.score(),
                evaluation.feedback(),
                evaluation.recommendedAnswer()
        ));
        return MailAnswerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<MailAnswerResponse> myAnswers(Long userId) {
        return mailAnswerRepository.findTop20ByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(MailAnswerResponse::from)
                .toList();
    }

    private MailEvaluationResult evaluate(MailQuestion question, String answer, String code) {
        if (!StringUtils.hasText(answer) && !StringUtils.hasText(code)) {
            return new MailEvaluationResult(0, "답변 또는 코드를 입력해야 합니다.", question.getModelAnswer());
        }
        try {
            String content = chatClient.prompt()
                    .system("""
                            너는 개발자 학습 서비스의 답변 평가자다.
                            한국어로 평가한다.
                            첫 줄은 반드시 SCORE: 0~100 형식으로 쓴다.
                            이후 FEEDBACK:, RECOMMENDED: 섹션을 작성한다.
                            코드 답변이 있으면 동작 가능성, API 사용, 예외 처리, 가독성을 함께 평가한다.
                            """)
                    .user("""
                            질문 제목: %s
                            질문 내용: %s
                            질문 타입: %s
                            난이도: %s
                            모범 답안: %s

                            사용자 설명 답변:
                            %s

                            사용자 코드 답변:
                            %s
                            """.formatted(
                            question.getTitle(),
                            question.getContent(),
                            question.getQuestionType(),
                            question.getDifficulty(),
                            question.getModelAnswer(),
                            answer,
                            code
                    ))
                    .options(OpenAiChatOptions.builder()
                            .temperature(1.0)
                            .maxCompletionTokens(1200)
                            .reasoningEffort("low")
                            .build())
                    .call()
                    .content();
            return parseEvaluation(content, question.getModelAnswer());
        } catch (Exception e) {
            log.warn("Mail answer AI evaluation failed. fallback used. reason={}", e.getMessage());
            int score = StringUtils.hasText(answer) || StringUtils.hasText(code) ? 60 : 0;
            return new MailEvaluationResult(score, "AI 평가에 실패해 기본 피드백으로 처리했습니다. 모범 답안과 비교해 핵심 키워드를 보강하세요.", question.getModelAnswer());
        }
    }

    private MailEvaluationResult parseEvaluation(String content, String recommendedAnswer) {
        if (!StringUtils.hasText(content)) {
            return new MailEvaluationResult(0, "AI 평가 응답이 비어 있습니다.", recommendedAnswer);
        }
        int score = 0;
        String firstLine = content.lines().findFirst().orElse("");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{1,3})").matcher(firstLine);
        if (matcher.find()) {
            score = Math.max(0, Math.min(100, Integer.parseInt(matcher.group(1))));
        }
        String recommended = recommendedAnswer;
        int recommendedIndex = content.indexOf("RECOMMENDED:");
        if (recommendedIndex >= 0) {
            recommended = content.substring(recommendedIndex + "RECOMMENDED:".length()).strip();
        }
        return new MailEvaluationResult(score, content, recommended);
    }
}
