package com.codemong.be.mail.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailBatchScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier(MailBatchConfig.DAILY_RANDOM_MAIL_JOB_NAME)
    private final Job dailyRandomMailJob;

    @Scheduled(cron = "${codemong.mail.random-send-cron:0 */10 * * * *}", zone = "Asia/Seoul")
    public void launchDailyRandomMailJob() {
        try {
            jobLauncher.run(
                    dailyRandomMailJob,
                    new JobParametersBuilder()
                            .addLong("requestedAt", System.currentTimeMillis())
                            .toJobParameters()
            );
        } catch (Exception e) {
            log.warn("Daily random mail job launch failed. reason={}", e.getMessage());
        }
    }
}
