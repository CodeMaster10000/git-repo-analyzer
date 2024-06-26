package com.codemaster.git_repo_analyzer.sonar_analyzer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class MavenTemplateHandler {

  private static final String MVN_SONAR_COMMAND_TEMPLATE_UNIX =
      "%s/mvn -s %s sonar:sonar -Dsonar.host.url=%s -Dsonar.login=%s -DskipTests=true";

  private static final String MVN_SONAR_COMMAND_TEMPLATE_WINDOWS =
      "%s/mvn -s %s sonar:sonar -Dsonar.host.url=%s -Dsonar.login=%s -DskipTests=true";

  private final ShellProcessData shellProcessData;

  public MavenTemplateHandler(
      @Value("${maven-path}") String mavenPath,
      @Value("${maven-settings-xml-path}") String mavenSettingsPath,
      @Value("${sonarqube-url}") String sonarqubeUrl,
      @Value("${sonarqube-auth}") String sonarqubeAuth,
      @Value("${jdk-path}") String jdkPath) {

    shellProcessData = createShellProcessData(mavenPath, mavenSettingsPath, sonarqubeUrl, sonarqubeAuth, jdkPath);
  }

  private ShellProcessData createShellProcessData(String mavenPath, String mavenSettingsPath, String sonarqubeUrl, String sonarqubeAuth, String jdkPath) {
    String mvnCommand = initializeMvnCommand(mavenPath, mavenSettingsPath, sonarqubeAuth, sonarqubeUrl);
    OS_TYPE osType = determineOsType();
    return new ShellProcessData(jdkPath, mvnCommand, osType);
  }

  private static OS_TYPE determineOsType() {
    return System.getProperty("os.name").toLowerCase().contains("win")
        ? OS_TYPE.WINDOWS
        : OS_TYPE.UNIX;
  }

  private String initializeMvnCommand(String mavenPath, String mavenSettingsPath, String sonarqubeAuth, String sonarqubeUrl) {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) {
      return String.format(
          MVN_SONAR_COMMAND_TEMPLATE_WINDOWS,
          mavenPath,
          mavenSettingsPath,
          sonarqubeUrl,
          sonarqubeAuth);
    } else {
      return String.format(
          MVN_SONAR_COMMAND_TEMPLATE_UNIX,
          mavenPath,
          mavenSettingsPath,
          sonarqubeUrl,
          sonarqubeAuth);
    }
  }

  public ShellProcessData getShellProcessData() {
    return this.shellProcessData;
  }

}
