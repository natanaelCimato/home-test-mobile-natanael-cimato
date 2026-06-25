package com.homechallenge.performance;

import java.time.Duration;
import java.util.Locale;

final class PerformanceBudget {
    private final String operation;
    private final PerformanceSettings.AssertionMetric assertionMetric;
    private final Duration hardBudget;
    private final Duration targetBudget;

    PerformanceBudget(
            String operation,
            PerformanceSettings.AssertionMetric assertionMetric,
            Duration hardBudget,
            Duration targetBudget
    ) {
        this.operation = operation;
        this.assertionMetric = assertionMetric;
        this.hardBudget = hardBudget;
        this.targetBudget = targetBudget;
    }

    String operation() {
        return operation;
    }

    PerformanceSettings.AssertionMetric assertionMetric() {
        return assertionMetric;
    }

    Duration hardBudget() {
        return hardBudget;
    }

    Duration targetBudget() {
        return targetBudget;
    }

    double actualMillis(PerformanceResult result) {
        return assertionMetric.valueMillis(result);
    }

    boolean exceedsHardBudget(PerformanceResult result) {
        return actualMillis(result) > hardBudget.toMillis();
    }

    boolean exceedsTargetBudget(PerformanceResult result) {
        return actualMillis(result) > targetBudget.toMillis();
    }

    String actualSummary(PerformanceResult result) {
        return String.format(
                Locale.US,
                "%s %.1f ms (target %.0f ms, hard %.0f ms)",
                assertionMetric.displayName(),
                actualMillis(result),
                (double) targetBudget.toMillis(),
                (double) hardBudget.toMillis()
        );
    }
}
