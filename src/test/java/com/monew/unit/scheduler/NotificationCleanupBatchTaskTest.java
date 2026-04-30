package com.monew.unit.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.monew.scheduler.task.NotificationCleanupBatchTask;
import com.monew.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupBatchTaskTest {

  @Mock
  private NotificationService notificationService;

  @Mock
  private JobRepository jobRepository;

  @Mock
  private PlatformTransactionManager transactionManager;

  @InjectMocks
  private NotificationCleanupBatchTask batchTask;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(batchTask, "cron", "0 0 3 * * *");
  }

  @Test
  @DisplayName("메타데이터 검증 - 크론 표현식과 Job 이름이 올바르게 반환된다")
  void metadata_ReturnsCorrectValues() {
    assertThat(batchTask.getCron()).isEqualTo("0 0 3 * * *");
    assertThat(batchTask.getJobName()).isEqualTo("notificationCleanupJob");
  }

  @Test
  @DisplayName("Job 생성 검증 - 올바른 이름의 Job 객체가 생성된다")
  void getJob_CreatesJobSuccessfully() {
    Job job = batchTask.getJob();

    assertThat(job).isNotNull();
    assertThat(job.getName()).isEqualTo("notificationCleanupJob");
  }

  @Test
  @DisplayName("Tasklet 로직 검증 - 알림 삭제 서비스가 정상적으로 호출되고 FINISHED를 반환한다")
  void notificationCleanupStep_ExecutesTaskletSuccessfully() throws Exception {
    long expectedDeletedCount = 5L;
    given(notificationService.deleteOldConfirmedNotifications()).willReturn(expectedDeletedCount);

    Step step = ReflectionTestUtils.invokeMethod(batchTask, "notificationCleanupStep");

    TaskletStep taskletStep = (TaskletStep) step;
    Tasklet tasklet = taskletStep.getTasklet();

    RepeatStatus status = tasklet.execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(notificationService).deleteOldConfirmedNotifications();
  }
}
