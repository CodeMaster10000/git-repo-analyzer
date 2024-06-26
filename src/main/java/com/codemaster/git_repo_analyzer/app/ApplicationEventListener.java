package com.codemaster.git_repo_analyzer.app;

import com.codemaster.git_repo_analyzer.event.ApplicationEventModule;
import com.codemaster.git_repo_analyzer.persistence.repository.ApplicationEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;


@Service
public final class ApplicationEventListener<T extends ApplicationEventModule> implements ApplicationListener<T> {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationEventListener.class);

  private final ApplicationEventRepository eventRepository;

  public ApplicationEventListener(ApplicationEventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  @Override
  public void onApplicationEvent(T event) {
    logger.info("Received event: {}", event.getMessage());
    eventRepository.save(ApplicationEventMapper.toEntity(event));
  }

}
