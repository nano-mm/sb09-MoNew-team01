package com.monew.scheduler.task;

import com.monew.scheduler.BatchTask;
import com.monew.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupBatchTask implements BatchTask {

    private final NotificationService notificationService;

    @Value("${monew.batch.notification-cleanup.cron:0 0 3 * * *}")
    private String cron;

    @Override
    public void execute() {
        long deleted = notificationService.deleteOldConfirmedNotifications();
        log.info("[배치] 1주일 경과 확인 알림 삭제 완료. 삭제 건수={}", deleted);
    }

    @Override
    public String getCron() {
        return this.cron;
    }
}

