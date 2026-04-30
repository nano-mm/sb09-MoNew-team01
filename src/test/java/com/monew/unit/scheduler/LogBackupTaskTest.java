package com.monew.unit.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.monew.scheduler.task.LogBackupTask;
import com.monew.storage.log.LogStorage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
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
class LogBackupTaskTest {

  @Mock
  private LogStorage logStorage;

  @Mock
  private JobRepository jobRepository;

  @Mock
  private PlatformTransactionManager transactionManager;

  @InjectMocks
  private LogBackupTask logBackupTask;

  private String yesterdayStr;
  private Path logDirPath;

  @BeforeEach
  void setUp() throws IOException {
    ReflectionTestUtils.setField(logBackupTask, "cron", "0 0 4 * * *");

    yesterdayStr = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    logDirPath = Paths.get("./logs/" + yesterdayStr);

    Files.createDirectories(logDirPath);

    Files.createFile(logDirPath.resolve("monew." + yesterdayStr + "_01.log"));
    Files.createFile(logDirPath.resolve("monew." + yesterdayStr + "_02.log"));
  }

  @AfterEach
  void tearDown() throws IOException {
    if (Files.exists(logDirPath)) {
      Files.walk(logDirPath)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  @Test
  @DisplayName("메타데이터 검증 - 크론 표현식과 Job 이름이 올바르게 반환된다")
  void metadata_ReturnsCorrectValues() {
    assertThat(logBackupTask.getCron()).isEqualTo("0 0 4 * * *");
    assertThat(logBackupTask.getJobName()).isEqualTo("logBackupJob");
  }

  @Test
  @DisplayName("Job 생성 검증 - 올바른 이름의 Job 객체가 생성된다")
  void getJob_CreatesJobSuccessfully() {
    Job job = logBackupTask.getJob();
    assertThat(job).isNotNull();
    assertThat(job.getName()).isEqualTo("logBackupJob");
  }

  @Test
  @DisplayName("Tasklet 정상 처리 - 존재하는 로그 파일들(01시, 02시)이 S3에 백업된다")
  void logBackupStep_ExecutesSuccessfully() throws Exception {
    Step step = ReflectionTestUtils.invokeMethod(logBackupTask, "logBackupStep");
    Tasklet tasklet = ((TaskletStep) step).getTasklet();

    RepeatStatus status = tasklet.execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);

    verify(logStorage, times(2)).backup(any(File.class), anyString());
  }

  @Test
  @DisplayName("이벤트 리스너 검증 - 서버 시작 시 어제자 로그 백업 이벤트가 정상 동작한다")
  void onApplicationReady_ExecutesBackup() {
    logBackupTask.onApplicationReady();

    verify(logStorage, times(2)).backup(any(File.class), anyString());
  }

  @Test
  @DisplayName("부분 실패 방어 로직 검증 - 하나의 파일 업로드 실패 시에도 다음 파일은 정상 업로드된다")
  void process_PartialFailure_ContinuesUploadingRemainingFiles() throws Exception {
    String s3Key01 = yesterdayStr + "/monew." + yesterdayStr + "_01.log";
    doThrow(new RuntimeException("S3 Network Error"))
        .when(logStorage).backup(any(File.class), eq(s3Key01));

    logBackupTask.onApplicationReady();

    verify(logStorage, times(2)).backup(any(File.class), anyString());
  }
}
