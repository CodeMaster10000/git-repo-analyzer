package com.codemaster.git_repo_analyzer.scraper;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
class AsyncConfig {

  private ThreadPoolTaskExecutor cloneTaskExecutor;


  @Bean(name = "cloneTaskExecutor")
  public ThreadPoolTaskExecutor cloneTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.initialize();
    cloneTaskExecutor = executor;
    return executor;
  }

  @PreDestroy
  public void shutDownExecutor() {
    cloneTaskExecutor.shutdown();
  }

}
