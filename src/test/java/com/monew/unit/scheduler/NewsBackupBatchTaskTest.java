package com.monew.unit.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.monew.scheduler.task.NewsBackupBatchTask;
import com.monew.service.ArticleBackupService;
import java.io.IOException;
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
class NewsBackupBatchTaskTest {

  @Mock
  private ArticleBackupService articleBackupService;

  @Mock
  private JobRepository jobRepository;

  @Mock
  private PlatformTransactionManager transactionManager;

  @InjectMocks
  private NewsBackupBatchTask newsBackupBatchTask;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(newsBackupBatchTask, "cron", "0 0 2 * * *");
  }

  @Test
  @DisplayName("메타데이터 검증 - 크론 표현식과 Job 이름이 올바르게 반환된다")
  void metadata_ReturnsCorrectValues() {
    assertThat(newsBackupBatchTask.getCron()).isEqualTo("0 0 2 * * *");
    assertThat(newsBackupBatchTask.getJobName()).isEqualTo("newsBackupJob");
  }

  @Test
  @DisplayName("Job 생성 검증 - 올바른 이름의 Job 객체가 생성된다")
  void getJob_CreatesJobSuccessfully() {
    Job job = newsBackupBatchTask.getJob();

    assertThat(job).isNotNull();
    assertThat(job.getName()).isEqualTo("newsBackupJob");
  }

  @Test
  @DisplayName("Tasklet 정상 처리 - 뉴스 백업 서비스가 정상적으로 호출되고 FINISHED를 반환한다")
  void articleBackupStep_ExecutesSuccessfully() throws Exception {
    Tasklet tasklet = extractTasklet("articleBackupStep");

    RepeatStatus status = tasklet.execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(articleBackupService).export();
  }

  @Test
  @DisplayName("Tasklet 예외 처리 - 백업 중 IOException 발생 시 RuntimeException으로 변환하여 던진다")
  void articleBackupStep_ThrowsRuntimeException_WhenIOExceptionOccurs() throws Exception {
    doThrow(new IOException("디스크 용량 부족")).when(articleBackupService).export();

    Tasklet tasklet = extractTasklet("articleBackupStep");

    assertThatThrownBy(() -> tasklet.execute(null, null))
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(IOException.class)
        .hasMessageContaining("디스크 용량 부족");
  }

  private Tasklet extractTasklet(String methodName) {
    Step step = ReflectionTestUtils.invokeMethod(newsBackupBatchTask, methodName);
    TaskletStep taskletStep = (TaskletStep) step;
    return taskletStep.getTasklet();
  }
}