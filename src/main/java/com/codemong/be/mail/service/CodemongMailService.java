package com.codemong.be.mail.service;

import com.codemong.be.mail.entity.MailQuestion;
import com.codemong.be.mail.entity.MailSendLog;
import com.codemong.be.mail.dto.MailContentResponse;
import com.codemong.be.mail.repository.MailSendLogRepository;
import com.codemong.be.user.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodemongMailService {

    private final JavaMailSender mailSender;
    private final MailSendLogRepository mailSendLogRepository;

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
}
