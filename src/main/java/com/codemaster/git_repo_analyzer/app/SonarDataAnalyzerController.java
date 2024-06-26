package com.codemaster.git_repo_analyzer.app;

import com.codemaster.git_repo_analyzer.event.DebtDataDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data")
final class SonarDataAnalyzerController {

  private final ApplicationEventService eventService;

  SonarDataAnalyzerController(ApplicationEventService eventService) {
    this.eventService = eventService;
  }

  @GetMapping("/debt")
  public DebtDataDto getLatestProjectDebtInformation() {
    return eventService.getLatestProjectDebtInformation();
  }

}
