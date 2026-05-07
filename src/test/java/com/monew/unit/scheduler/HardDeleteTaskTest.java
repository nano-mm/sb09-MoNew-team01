package com.monew.unit.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.monew.adapter.out.persistence.UserRepository;
import com.monew.scheduler.task.HardDeleteTask;
import java.time.Instant;
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
class HardDeleteTaskTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private JobRepository jobRepository;

  @Mock
  private PlatformTransactionManager transactionManager;

  @InjectMocks
  private HardDeleteTask hardDeleteTask;

  @BeforeEach
  void setUp() {
    // @Value 로 주입되는 cron 표현식을 리플렉션으로 수동 세팅
    ReflectionTestUtils.setField(hardDeleteTask, "cron", "0 0 3 * * *");
  }

  @Test
  @DisplayName("메타데이터 검증 - 크론 표현식과 Job 이름이 올바르게 반환된다")
  void metadata_ReturnsCorrectValues() {
    assertThat(hardDeleteTask.getCron()).isEqualTo("0 0 3 * * *");
    assertThat(hardDeleteTask.getJobName()).isEqualTo("hardDeleteJob");
  }

  @Test
  @DisplayName("Job 생성 검증 - 올바른 이름의 Job 객체가 생성된다")
  void getJob_CreatesJobSuccessfully() {
    Job job = hardDeleteTask.getJob();

    assertThat(job).isNotNull();
    assertThat(job.getName()).isEqualTo("hardDeleteJob");
  }

  @Test
  @DisplayName("Tasklet 정상 처리 - 물리 삭제가 완료되고 FINISHED를 반환한다")
  void userHardDeleteStep_ExecutesSuccessfully() throws Exception {
    given(userRepository.deleteSoftDeletedUsersOlderThan(any(Instant.class))).willReturn(10);

    Tasklet tasklet = extractTasklet("userHardDeleteStep");

    RepeatStatus status = tasklet.execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(userRepository).deleteSoftDeletedUsersOlderThan(any(Instant.class));
  }

  @Test
  @DisplayName("Tasklet 예외 처리 - 삭제 중 예외가 발생해도 FINISHED를 반환하며 종료된다")
  void userHardDeleteStep_HandlesExceptionSafely() throws Exception {
    given(userRepository.deleteSoftDeletedUsersOlderThan(any(Instant.class)))
        .willThrow(new RuntimeException("DB Connection Timeout"));

    Tasklet tasklet = extractTasklet("userHardDeleteStep");

    RepeatStatus status = tasklet.execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(userRepository).deleteSoftDeletedUsersOlderThan(any(Instant.class));
  }

  private Tasklet extractTasklet(String methodName) {
    Step step = ReflectionTestUtils.invokeMethod(hardDeleteTask, methodName);
    TaskletStep taskletStep = (TaskletStep) step;
    return taskletStep.getTasklet();
  }
}
