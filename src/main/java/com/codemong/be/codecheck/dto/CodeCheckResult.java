package com.codemong.be.codecheck.dto;

import java.util.List;

public record CodeCheckResult(
        boolean passed,
        List<String> failedTests
) {
    public CodeCheckResult(boolean passed) {
        this(passed, List.of());
    }
}
