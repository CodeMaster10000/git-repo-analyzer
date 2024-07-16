# Git Repo Analyzer

Git Repo Analyzer is a Modulith, event-driven Java application that reads from a datasource of repositories.
For each repository url, creates a process that clones the repository locally, inside the repo,
it creates a SonarQube project for the project inside the repository.
Afterward, collects analysis for the project and extracts the technical debt for the project.

## Prerequisites

- Java 21
- Maven 3.9.7 and above
- Running Sonarqube instance

## Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/CodeMaster10000/git-repo-analyzer.git
   cd git-repo-analyzer
   ```

2. **Build the application**

    ```bash
    mvn clean install
    ```

3. **Setup**

   - Go to the repo-data directory and inside the repositories.xml file,
     append any repo urls of your choice in the format:
   
   ```xml
   <repository>
        <url>git-repo-url</url>
    </repository>
   ```
   
   - Inside the `application.properties` configure the settings for the `SonarQube` server instance

4. **Run the application**

   Either run the built version via `java -jar /target/*.jar`,
   Or through an IDE