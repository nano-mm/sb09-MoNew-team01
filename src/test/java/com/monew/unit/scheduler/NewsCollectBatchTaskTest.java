package com.monew.unit.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.monew.scheduler.task.NewsCollectBatchTask;
import com.monew.application.service.ArticleService;
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
class NewsCollectBatchTaskTest {

  @Mock
  private ArticleService articleService;

  @Mock
  private JobRepository jobRepository;

  @Mock
  private PlatformTransactionManager transactionManager;

  @InjectMocks
  private NewsCollectBatchTask newsCollectBatchTask;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(newsCollectBatchTask, "cron", "0 0 * * * *");
  }

  @Test
  @DisplayName("메타데이터 검증 - 크론 표현식과 Job 이름이 올바르게 반환된다")
  void metadata_ReturnsCorrectValues() {
    assertThat(newsCollectBatchTask.getCron()).isEqualTo("0 0 * * * *");
    assertThat(newsCollectBatchTask.getJobName()).isEqualTo("newsCollectJob");
  }

  @Test
  @DisplayName("Job 생성 검증 - 올바른 이름의 Job 객체가 생성된다")
  void getJob_CreatesJobSuccessfully() {
    Job job = newsCollectBatchTask.getJob();

    assertThat(job).isNotNull();
    assertThat(job.getName()).isEqualTo("newsCollectJob");
  }

  @Test
  @DisplayName("Tasklet 정상 처리 - 뉴스 수집 서비스가 정상적으로 호출되고 FINISHED를 반환한다")
  void collectNewsStep_ExecutesSuccessfully() throws Exception {
    Tasklet tasklet = extractTasklet("collectNewsStep");

    RepeatStatus status = tasklet.execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(articleService).collect();
  }

  private Tasklet extractTasklet(String methodName) {
    Step step = ReflectionTestUtils.invokeMethod(newsCollectBatchTask, methodName);
    TaskletStep taskletStep = (TaskletStep) step;
    return taskletStep.getTasklet();
  }
}