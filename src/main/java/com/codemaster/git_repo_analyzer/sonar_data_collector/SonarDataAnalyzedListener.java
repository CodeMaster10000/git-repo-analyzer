package com.codemaster.git_repo_analyzer.sonar_data_collector;

import com.codemaster.git_repo_analyzer.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
final class SonarDataAnalyzedListener implements ApplicationListener<RepositoryAnalyzedEvent> {

  private static final Logger logger = LoggerFactory.getLogger(SonarDataAnalyzedListener.class);

  private final ApplicationEventPublisher eventPublisher;
  private final String sonarqubeUrl;
  private final String sonarqubeAuth;
  private final RestTemplate restTemplate;

  public SonarDataAnalyzedListener(ApplicationEventPublisher eventPublisher, @Value("${sonarqube-url}") String sonarqubeUrl,
      @Value("${sonarqube-auth}") String sonarqubeAuth,
      RestTemplate restTemplate) {
    this.eventPublisher = eventPublisher;
    this.sonarqubeUrl = sonarqubeUrl;
    this.sonarqubeAuth = sonarqubeAuth;
    this.restTemplate = restTemplate;
    setupRestTemplate();
  }

  @Override
  public void onApplicationEvent(RepositoryAnalyzedEvent event) {
    if (event.getEventStatus().equals(EventStatus.FAILED)) {
      publishDataAnalyzedEvent(event, EventStatus.FAILED);
    } else if (event.getEventStatus().equals(EventStatus.SUCCEEDED)) {
      processSonarProjectData(event);
    }
  }

  private void setupRestTemplate() {
    restTemplate.getInterceptors().add((request, body, execution) -> {
      String auth = sonarqubeAuth + ":";
      byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
      String authHeader = "Basic " + new String(encodedAuth);
      request.getHeaders().set("Authorization", authHeader);
      return execution.execute(request, body);
    });
  }

  private void processSonarProjectData(RepositoryAnalyzedEvent analyzedEvent) {
    publishDataAnalyzedEvent(analyzedEvent, EventStatus.IN_PROGRESS);
    String projectKey = analyzedEvent.getProjectKey();
    String apiUrl = String.format("%s/api/measures/component?component=%s&metricKeys=sqale_index", sonarqubeUrl, projectKey);
    try {
      HttpHeaders headers = new HttpHeaders();
      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<Map<String, Map<String, Object>>> response = restTemplate.exchange(
          apiUrl,
          HttpMethod.GET,
          entity, new ParameterizedTypeReference<>() {
          }
      );
      Map<String, Map<String, Object>> responseBody = response.getBody();
      if (responseBody != null) {
        extractDebt(responseBody, analyzedEvent);
      } else {
        logger.warn("No data received from SonarQube for project: {}", projectKey);
      }
    } catch (RestClientException e) {
      logException(projectKey, e);
      publishDataAnalyzedEvent(analyzedEvent, EventStatus.FAILED);
    }
  }

  private static void logException(String projectKey, RestClientException e) {
    switch (e) {
    case HttpClientErrorException clientError ->
        logger.error("Client error while fetching SonarQube data for project {}: {}", projectKey, clientError.getStatusCode());
    case HttpServerErrorException serverError ->
        logger.error("Server error while fetching SonarQube data for project {}: {}", projectKey, serverError.getStatusCode());
    default -> logger.error("Error fetching SonarQube data for project {}: {}", projectKey, e.getMessage());
    }
  }

  private void extractDebt(Map<String, Map<String, Object>> response, RepositoryAnalyzedEvent event) {
    Map<String, Object> component = response.get("component");
    if (component != null) {
      for (Map<String, Object> measure : getMeasures(component)) {
        if ("sqale_index".equals(measure.get("metric"))) {
          int debtMinutes = Integer.parseInt((String) measure.get("value"));
          int debtHours = convertMinutesToHours(debtMinutes);
          logger.info("Technical Debt: {}", debtHours);
          event.setMessage(String.valueOf(debtHours));
          publishDataAnalyzedEvent(event, EventStatus.SUCCEEDED);
          return;
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> getMeasures(Map<String, Object> component) {
    return (List<Map<String, Object>>) component.get("measures");
  }

  private int convertMinutesToHours(int debtMinutes) {
    if (debtMinutes < 1) return 0;
    return debtMinutes < 60 ? 1 : debtMinutes / 60;
  }

  private void publishDataAnalyzedEvent(RepositoryAnalyzedEvent event, EventStatus status) {
    DataAnalyzedEvent dataAnalyzedEvent = new DataAnalyzedEvent(
        this,
        EventType.DATA_COLLECTED,
        status.equals(EventStatus.FAILED) ? "could not gather data" : event.getMessage(),
        status,
        Timestamp.from(Instant.now()),
        UUID.randomUUID(),
        event.getJobId()
    );
    eventPublisher.publishEvent(dataAnalyzedEvent);
  }

}
