package com.codemong.be.mail.service;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import com.codemong.be.mail.dto.MailDashboardResponse;
import com.codemong.be.mail.dto.MailQuestionResponse;
import com.codemong.be.mail.dto.MailSendLogResponse;
import com.codemong.be.mail.dto.MailSubscriptionResponse;
import com.codemong.be.mail.entity.MailQuestion;
import com.codemong.be.mail.entity.MailSendLog;
import com.codemong.be.mail.entity.MailSubscription;
import com.codemong.be.mail.repository.MailQuestionRepository;
import com.codemong.be.mail.repository.MailSendLogRepository;
import com.codemong.be.mail.repository.MailSubscriptionRepository;
import com.codemong.be.user.entity.User;
import com.codemong.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MailSubscriptionService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final MailSubscriptionRepository mailSubscriptionRepository;
    private final MailQuestionRepository mailQuestionRepository;
    private final MailSendLogRepository mailSendLogRepository;
    private final CodemongMailService codemongMailService;
    private final MailContentService mailContentService;

    @Transactional(readOnly = true)
    public MailDashboardResponse dashboard(Long userId) {
        MailSubscription subscription = mailSubscriptionRepository.findByUser_Id(userId)
                .orElseGet(() -> new MailSubscription(findUser(userId), false));
        MailQuestion preview = mailQuestionRepository.findRandomQuestion().orElse(null);
        return new MailDashboardResponse(
                MailSubscriptionResponse.from(subscription),
                preview == null ? null : MailQuestionResponse.from(preview),
                mailContentService.randomContent().orElse(null),
                mailSendLogRepository.findTop20ByUser_IdOrderByCreatedAtDesc(userId).stream()
                        .map(MailSendLogResponse::from)
                        .toList()
        );
    }

    @Transactional
    public MailSubscriptionResponse updateSubscription(Long userId, boolean enabled, String email) {
        User user = findUser(userId);
        if (enabled && !StringUtils.hasText(user.getEmail())) {
            if (!StringUtils.hasText(email)) {
                throw new CustomException(ErrorCode.USER_EMAIL_REQUIRED);
            }
            String normalizedEmail = email.trim();
            if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
                throw new CustomException(ErrorCode.INVALID_EMAIL);
            }
            user.updateEmail(normalizedEmail);
        }
        MailSubscription subscription = mailSubscriptionRepository.findByUser_Id(userId)
                .orElseGet(() -> mailSubscriptionRepository.save(new MailSubscription(user, enabled)));
        subscription.updateEnabled(enabled);
        return MailSubscriptionResponse.from(subscription);
    }

    @Transactional
    public MailSendLogResponse sendTestMail(Long userId) {
        User user = findUser(userId);
        var content = mailContentService.randomContent()
                .orElseThrow(() -> new IllegalStateException("메일 콘텐츠가 없습니다."));
        MailSendLog log = codemongMailService.sendContentMail(user, content);
        return MailSendLogResponse.from(log);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
    }
}
