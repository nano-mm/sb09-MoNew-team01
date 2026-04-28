package com.monew.scheduler.task;

import com.monew.scheduler.BatchTask;
import com.monew.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupBatchTask implements BatchTask {

    private final NotificationService notificationService;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${monew.batch.notification-cleanup.cron:0 0 3 * * *}")
    private String cron;

    @Override
    public String getCron() {
        return this.cron;
    }

    @Override
    public String getJobName() {
        return "notificationCleanupJob";
    }

    @Override
    public Job getJob() {
        return new JobBuilder(this.getJobName(), jobRepository)
            .start(notificationCleanupStep())
            .build();
    }

    private Step notificationCleanupStep() {
        return new StepBuilder("notificationCleanupStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {

                long deleted = notificationService.deleteOldConfirmedNotifications();
                log.info("[배치] 1주일 경과 확인 알림 삭제 완료. 삭제 건수={}", deleted);

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

}

