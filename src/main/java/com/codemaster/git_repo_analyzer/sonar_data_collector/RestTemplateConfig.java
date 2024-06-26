package com.codemaster.git_repo_analyzer.sonar_data_collector;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
class RestTemplateConfig {

  @Bean
   RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
