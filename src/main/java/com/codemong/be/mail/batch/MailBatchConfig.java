package com.codemong.be.mail.batch;

import com.codemong.be.mail.repository.MailSubscriptionRepository;
import com.codemong.be.mail.service.CodemongMailService;
import com.codemong.be.mail.service.MailContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MailBatchConfig {

    public static final String DAILY_RANDOM_MAIL_JOB_NAME = "dailyRandomMailJob";

    private final MailSubscriptionRepository mailSubscriptionRepository;
    private final CodemongMailService codemongMailService;
    private final MailContentService mailContentService;

    @Bean
    public Job dailyRandomMailJob(JobRepository jobRepository, Step dailyRandomMailStep) {
        return new JobBuilder(DAILY_RANDOM_MAIL_JOB_NAME, jobRepository)
                .start(dailyRandomMailStep)
                .build();
    }

    @Bean
    public Step dailyRandomMailStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("dailyRandomMailStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    mailSubscriptionRepository.findEnabledWithUser().forEach(subscription -> {
                        try {
                            if (!StringUtils.hasText(subscription.getUser().getEmail())) {
                                log.info("Mail skipped. userId={} has no email.", subscription.getUser().getId());
                                return;
                            }
                            var content = mailContentService.randomContent()
                                    .orElseThrow(() -> new IllegalStateException("메일 콘텐츠가 없습니다."));
                            codemongMailService.sendContentMail(subscription.getUser(), content);
                        } catch (Exception e) {
                            log.warn("Daily random mail failed. userId={}, reason={}",
                                    subscription.getUser().getId(), e.getMessage());
                        }
                    });
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
