package com.codemaster.git_repo_analyzer.scraper;

record RepositoryInfo(String repoUrl) {

  String repoName() {
    return repoUrl.substring(repoUrl.lastIndexOf('/') + 1, repoUrl.lastIndexOf('.'));
  }

}
