package com.codemaster.git_repo_analyzer.event;

import java.util.Map;

public record DebtDataDto(Map<String, Integer> debtPerProject, int overallDebt) {
}
