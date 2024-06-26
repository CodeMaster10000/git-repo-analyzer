package com.codemaster.git_repo_analyzer.sonar_analyzer;

import com.codemaster.git_repo_analyzer.event.EventStatus;
import com.codemaster.git_repo_analyzer.event.EventType;
import com.codemaster.git_repo_analyzer.event.RepositoryAnalyzedEvent;
import com.codemaster.git_repo_analyzer.event.RepositoryClonedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
final class RepositoryClonedListener implements ApplicationListener<RepositoryClonedEvent> {

  private static final Logger logger = LoggerFactory.getLogger(RepositoryClonedListener.class);

  private final ApplicationEventPublisher eventPublisher;

  private final MavenTemplateHandler mavenTemplateHandler;

  RepositoryClonedListener(ApplicationEventPublisher eventPublisher, MavenTemplateHandler mavenTemplateHandler) {
    this.eventPublisher = eventPublisher;
    this.mavenTemplateHandler = mavenTemplateHandler;
  }

  @Override
  public void onApplicationEvent(RepositoryClonedEvent event) {
    if (event.getEventStatus().equals(EventStatus.FAILED)) {
      publishRepositoryAnalyzedEvent(event, "", EventStatus.FAILED);
    } else if (event.getEventStatus().equals(EventStatus.SUCCEEDED)) {
      executeShellProcess(event);
    }
  }

  private void executeShellProcess(RepositoryClonedEvent clonedEvent) {
    try {
      publishRepositoryAnalyzedEvent(clonedEvent, "", EventStatus.IN_PROGRESS);
      String repositoryPath = clonedEvent.getLocalRepositoryPath();
      ShellProcessData shellProcessData = mavenTemplateHandler.getShellProcessData();
      List<String> commands = createProcessCommands(shellProcessData);
      ProcessBuilder processBuilder = createProcessBuilder(repositoryPath, commands, shellProcessData);
      File logFile = getProcessOutputLog(repositoryPath, processBuilder);
      Process process = processBuilder.start();
      int exitCode = process.waitFor();
      logger.info("Maven process exited with code: {}", exitCode);
      publishRepositoryAnalyzedEventWithKey(logFile, clonedEvent);
    } catch (Exception e) {
      logger.error("Analysis Process failed, cause: {}", e.getMessage());
      publishRepositoryAnalyzedEvent(clonedEvent, "", EventStatus.FAILED);
    }
  }

  private void publishRepositoryAnalyzedEventWithKey(File logFile, RepositoryClonedEvent event) {
    String projectKey = extractProjectKeyFromLogFile(logFile);
    if (projectKey != null) {
      publishRepositoryAnalyzedEvent(event, projectKey, EventStatus.SUCCEEDED);
    } else {
      throw new NullPointerException("project key is null");
    }
  }

  private static File getProcessOutputLog(String repositoryPath, ProcessBuilder processBuilder) {
    File logFile = new File(repositoryPath, "mvn_output.log");
    File errorFile = new File(repositoryPath, "mvn_error.log");
    processBuilder.redirectOutput(logFile);
    processBuilder.redirectError(errorFile);
    return logFile;
  }

  private static ProcessBuilder createProcessBuilder(String repositoryPath, List<String> commands, ShellProcessData shellProcessData) {
    ProcessBuilder processBuilder = new ProcessBuilder(commands);
    processBuilder.environment().put("JAVA_HOME", shellProcessData.jdkPath());
    processBuilder.directory(new File(repositoryPath));
    return processBuilder;
  }

  private static List<String> createProcessCommands(ShellProcessData shellProcessData) {
    List<String> commands = new ArrayList<>();
    if (shellProcessData.osType().equals(OS_TYPE.WINDOWS)) {
      commands.add("cmd.exe");
      commands.add("/c");
    } else {
      commands.add("/bin/sh");
      commands.add("-c");
    }
    commands.add(shellProcessData.mvnCommand());
    return commands;
  }

  private String extractProjectKeyFromLogFile(File logFile) {
    try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
      String line;
      while ((line = reader.readLine()) != null) {
        logger.info(line);
        if (line.contains("Project key: ")) {
          return line.split(": ")[1].trim();
        }
      }
    } catch (IOException e) {
      logger.error("Error reading log file, cause: {}", e.getMessage());
    }
    return null;
  }

  private void publishRepositoryAnalyzedEvent(RepositoryClonedEvent event, String projectKey, EventStatus status) {
    RepositoryAnalyzedEvent repositoryAnalyzedEvent = new RepositoryAnalyzedEvent(
        this,
        EventType.ANALYZED,
        status.equals(EventStatus.FAILED) ? "could not analyze repository" : "successfully analyzed",
        status,
        Timestamp.from(Instant.now()),
        UUID.randomUUID(),
        event.getJobId(),
        projectKey
    );
    eventPublisher.publishEvent(repositoryAnalyzedEvent);
  }

}
