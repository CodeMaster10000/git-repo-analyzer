package com.codemaster.git_repo_analyzer;

import com.codemaster.git_repo_analyzer.persistence.entity.ApplicationJobEntity;
import com.codemaster.git_repo_analyzer.persistence.repository.ApplicationJobRepository;
import com.codemaster.git_repo_analyzer.scraper.GitRepoClonerService;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;

@Component
@EnableScheduling
public class CloningEventScheduler {

  private final GitRepoClonerService gitRepoClonerService;

  private final ApplicationJobRepository jobRepository;

  public CloningEventScheduler(GitRepoClonerService gitRepoClonerService, ApplicationJobRepository jobRepository) {
    this.gitRepoClonerService = gitRepoClonerService;
    this.jobRepository = jobRepository;
  }

  //@Scheduled(cron = "0 0 1 * * ?") // Cron expression for 1 AM daily
  //@Scheduled() // Cron expression for 1 AM daily
  @PostConstruct
  public void initializeCloningProcess() {
    ApplicationJobEntity applicationJob = createJob();
    gitRepoClonerService.execute(applicationJob.getId());
  }

  private ApplicationJobEntity createJob() {
    return jobRepository.save(new ApplicationJobEntity(
        null, "Data Analyzer",
        Timestamp.from(Instant.now()), "gathers data from the cloned repositories",
        new HashSet<>()));
  }

}
