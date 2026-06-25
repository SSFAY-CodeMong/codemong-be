package com.codemong.be.mail.service;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.mail.dto.SendCodeRequest;
import com.codemong.be.mail.dto.VerifyCodeRequest;
import com.codemong.be.mail.entity.MailQuestion;
import com.codemong.be.mail.entity.MailSendLog;
import com.codemong.be.mail.dto.MailContentResponse;
import com.codemong.be.mail.repository.MailSendLogRepository;
import com.codemong.be.user.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodemongMailService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JavaMailSender mailSender;
    private final MailSendLogRepository mailSendLogRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${codemong.mail.from:}")
    private String from;

    @Value("${codemong.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional
    public MailSendLog sendContentMail(User user, MailContentResponse content) {
        String recipient = user.getEmail();
        if (!StringUtils.hasText(recipient)) {
            return saveContentLog(user, content, false, "사용자 이메일이 없어 발송하지 않았습니다.");
        }

        String answerUrl = frontendBaseUrl + "/mail-service/contents?content=" + content.id();
        String html = """
                <h2>%s</h2>
                <p>오늘의 Codemong 메일 콘텐츠가 도착했습니다.</p>
                <p><strong>분류:</strong> %s</p>
                <p><a href="%s" style="display:inline-block;padding:10px 14px;background:#0f766e;color:#fff;text-decoration:none;border-radius:6px;">답변 보기</a></p>
                """.formatted(escape(content.title()), escape(content.type()), answerUrl);

        if (!isConfigured()) {
            return saveContentLog(user, content, false, "SMTP 설정이 없어 발송을 스킵했습니다.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(StringUtils.hasText(from) ? from : username);
            helper.setTo(recipient);
            helper.setSubject("[Codemong] " + content.title());
            helper.setText(html, true);
            mailSender.send(message);
            return saveContentLog(user, content, true, "발송 성공");
        } catch (Exception e) {
            log.warn("Content mail failed. userId={}, recipient={}, reason={}", user.getId(), recipient, e.getMessage());
            return saveContentLog(user, content, false, e.getMessage());
        }
    }

    @Transactional
    public MailSendLog sendDailyQuestionMail(User user, MailQuestion question) {
        String recipient = user.getEmail();
        if (!StringUtils.hasText(recipient)) {
            return saveLog(user, question, false, "사용자 이메일이 없어 발송하지 않았습니다.");
        }

        String html = """
                <h2>Codemong Mail Service</h2>
                <p>%s님, 오늘의 개발 질문이 도착했습니다.</p>
                <h3>%s</h3>
                <p><strong>카테고리:</strong> %s</p>
                <p><strong>난이도:</strong> %s</p>
                <p>%s</p>
                <p><a href="%s/mail-service" style="display:inline-block;padding:10px 14px;background:#0f766e;color:#fff;text-decoration:none;border-radius:6px;">메일 서비스 열기</a></p>
                """.formatted(
                user.getName() == null ? "Codemong 사용자" : user.getName(),
                question.getTitle(),
                question.getCategory().getName(),
                question.getDifficulty().name(),
                question.getContent(),
                frontendBaseUrl
        );

        if (!isConfigured()) {
            log.info("Mail skipped. SMTP is not configured. userId={}, questionId={}", user.getId(), question.getId());
            return saveLog(user, question, false, "SMTP 설정이 없어 발송을 스킵했습니다.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(StringUtils.hasText(from) ? from : username);
            helper.setTo(recipient);
            helper.setSubject("[Codemong] 오늘의 개발 질문이 도착했습니다.");
            helper.setText(html, true);
            mailSender.send(message);
            return saveLog(user, question, true, "발송 성공");
        } catch (Exception e) {
            log.warn("Mail failed. userId={}, recipient={}, reason={}", user.getId(), recipient, e.getMessage());
            return saveLog(user, question, false, e.getMessage());
        }
    }

    private MailSendLog saveLog(User user, MailQuestion question, boolean success, String message) {
        return mailSendLogRepository.save(new MailSendLog(
                user,
                question,
                "DAILY_RANDOM_QUESTION",
                user.getEmail() == null ? "" : user.getEmail(),
                success,
                message
        ));
    }

    private MailSendLog saveContentLog(User user, MailContentResponse content, boolean success, String message) {
        return mailSendLogRepository.save(new MailSendLog(
                user,
                null,
                "DAILY_MARKDOWN_CONTENT",
                user.getEmail() == null ? "" : user.getEmail(),
                success,
                message,
                content.title()
        ));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private boolean isConfigured() {
        return StringUtils.hasText(host) && StringUtils.hasText(username);
    }

    public void sendCodeMail(SendCodeRequest sendCodeRequest, Long userId) {

        String email = normalizeEmail(sendCodeRequest.email());
        String redisKey = userId + ":" + email;
        int randomNumber = SECURE_RANDOM.nextInt(900000) + 100000;
        String code = String.valueOf(randomNumber);

        String html = """
        <div style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; background-color: #ffffff;">
            <h2 style="margin: 0 0 16px; color: #111827; font-size: 22px;">
                CodeMong 이메일 변경 인증
            </h2>

            <p style="margin: 0 0 12px; color: #374151; font-size: 15px; line-height: 1.6;">
                안녕하세요. CodeMong 서비스입니다.
            </p>

            <p style="margin: 0 0 20px; color: #374151; font-size: 15px; line-height: 1.6;">
                이메일 변경을 진행하기 위해, 요청하신 이메일 주소가 본인 소유인지 확인하고 있습니다.
                아래 인증 코드를 입력해 이메일 인증을 완료해 주세요.
            </p>

            <div style="margin: 24px 0; padding: 18px; text-align: center; background-color: #f3f4f6; border-radius: 10px;">
                <p style="margin: 0 0 8px; color: #6b7280; font-size: 14px;">
                    인증 코드
                </p>
                <strong style="display: inline-block; color: #2563eb; font-size: 32px; letter-spacing: 6px;">
                    %s
                </strong>
            </div>

            <p style="margin: 0 0 12px; color: #374151; font-size: 14px; line-height: 1.6;">
                본인이 요청하지 않은 이메일 변경 인증이라면, 이 메일을 무시하셔도 됩니다.
            </p>

            <p style="margin: 20px 0 0; color: #9ca3af; font-size: 12px; line-height: 1.5;">
                본 메일은 발신 전용 메일입니다.
            </p>
        </div>
        """.formatted(code);

        if (!isConfigured()) {
            log.info("SMTP 설정이 없어 발송을 스킵했습니다.");
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(StringUtils.hasText(from) ? from : username);
            helper.setTo(email);
            helper.setSubject("[Codemong] " + "이메일 본인 인증");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("이메일 인증을 위한 메일 발송 성공");
        } catch (Exception e) {
            log.warn("Content mail failed. userId={}, recipient={}, reason={}", userId, email, e.getMessage());
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }

        redisTemplate.opsForValue().set(
                redisKey,
                code,
                3,
                TimeUnit.MINUTES
        );
    }

    public void verifyCode(VerifyCodeRequest verifyCodeRequest, Long userId) {
        String email = normalizeEmail(verifyCodeRequest.email());
        String code = verifyCodeRequest.code();
        String verifyRedisKey = userId + ":" + email;
        String correctCode = redisTemplate.opsForValue().get(verifyRedisKey);

        if(!Objects.equals(correctCode, code)){
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_FAILED);
        }

        String redisKey = verifyRedisKey + ":success";
        redisTemplate.opsForValue().set(
                redisKey,
                "success",
                3,
                TimeUnit.MINUTES
        );
        redisTemplate.delete(verifyRedisKey);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new CustomException(ErrorCode.INVALID_EMAIL);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
