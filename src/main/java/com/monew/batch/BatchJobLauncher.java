package com.monew.batch;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchJobLauncher {

  private final JobLauncher jobLauncher;
  private final Job sampleJob;
  private final MeterRegistry meterRegistry;

  public void launchSampleJob() {
    try {
      meterRegistry.counter("batch.sample.launches").increment();
      jobLauncher.run(sampleJob, new JobParametersBuilder().addLong("time", System.currentTimeMillis()).toJobParameters());
    } catch (Exception e) {
      meterRegistry.counter("batch.sample.failures").increment();
      log.error("Failed to run sampleJob", e);
    }
  }
}

