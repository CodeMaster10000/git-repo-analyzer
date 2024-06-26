package com.codemaster.git_repo_analyzer.scraper;

import com.codemaster.git_repo_analyzer.event.EventStatus;
import com.codemaster.git_repo_analyzer.event.EventType;
import com.codemaster.git_repo_analyzer.event.RepositoryClonedEvent;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public final class GitRepoClonerService {

  private final ThreadPoolTaskExecutor cloneTaskExecutor;

  private final ApplicationEventPublisher eventPublisher;

  private final String cloneTargetDirectory;

  private static final String REPO_DATA_PATH="repo-data/repositories.xml";

  private static final Logger logger = LoggerFactory.getLogger(GitRepoClonerService.class);


  public GitRepoClonerService(
      ThreadPoolTaskExecutor cloneTaskExecutor,
      ApplicationEventPublisher eventPublisher,
      @Value("${clone-target-directory}") String cloneTargetDirectory) {
    this.cloneTaskExecutor = cloneTaskExecutor;
    this.eventPublisher = eventPublisher;
    this.cloneTargetDirectory = cloneTargetDirectory;
  }

  public void execute(int jobId) {
    XmlConfigParser.getRepositoriesInfo(REPO_DATA_PATH)
        .stream()
        .map(repoInfo -> createCloneRepositoryTask(jobId, repoInfo))
        .forEach(cloneTaskExecutor::submit);
  }

  private Runnable createCloneRepositoryTask(int jobId, RepositoryInfo repoInfo) {
    return () -> {
      Path repoDirPath = Paths.get(cloneTargetDirectory, repoInfo.repoName());
      publishRepositoryClonedEvent(repoDirPath.toString(), EventStatus.IN_PROGRESS, jobId);
      try {
        if (Files.exists(repoDirPath)) {
          logger.info("Folder already exists: {}. Beginning drop operation", repoDirPath);
          FileUtils.deleteDirectory(new File(repoDirPath.toString()));
        }
        Files.createDirectories(repoDirPath.getParent());
        List<String> command = List.of("git", "clone", repoInfo.repoUrl(), repoDirPath.toString());
        Process process = createProcess(command);
        int exitCode = process.waitFor();
        validateProcessState(repoDirPath.toString(), exitCode, jobId);
      } catch (Exception e) {
        logger.error("Something went wrong while cloning, cause: {}", e.getMessage());
        publishRepositoryClonedEvent(repoDirPath.toString(), EventStatus.FAILED, jobId);
      }
    };
  }

  private void validateProcessState(String path, int exitCode, int jobId) {
    if (exitCode == 0) {
      logger.info("Cloned: {}", path);
      publishRepositoryClonedEvent(path, EventStatus.SUCCEEDED, jobId);
    } else {
      String errorMessage = "Failed to clone";
      logger.error("{} {}: Exit code {}", errorMessage, path, exitCode);
      throw new GitCloneException(errorMessage);
    }
  }

  private static Process createProcess(List<String> command) throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.inheritIO();
    return processBuilder.start();
  }

  private void publishRepositoryClonedEvent(String repoPath, EventStatus status, int jobId) {
    RepositoryClonedEvent repositoryClonedEvent = new RepositoryClonedEvent(
        this,
        EventType.CLONED,
        status.equals(EventStatus.FAILED) ? "could not clone" : "start cloning process",
        status,
        Timestamp.from(Instant.now()),
        UUID.randomUUID(),
        jobId,
        repoPath
    );
    eventPublisher.publishEvent(repositoryClonedEvent);
  }

}
