package com.homechallenge.performance;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

final class PerformanceSettings {
    private static final int DEFAULT_SAMPLE_COUNT = 3;
    private static final int DEFAULT_WARMUP_ITERATIONS = 1;
    private static final long DEFAULT_LOGIN_HARD_BUDGET_MS = 600_000L;
    private static final long DEFAULT_CATALOG_HARD_BUDGET_MS = 420_000L;
    private static final long DEFAULT_LOGIN_TARGET_MS = 60_000L;
    private static final long DEFAULT_CATALOG_TARGET_MS = 90_000L;

    private final int sampleCount;
    private final int warmupIterations;
    private final AssertionMetric assertionMetric;
    private final Path reportDirectory;
    private final boolean collectDeviceEvidence;

    private PerformanceSettings(
            int sampleCount,
            int warmupIterations,
            AssertionMetric assertionMetric,
            Path reportDirectory,
            boolean collectDeviceEvidence
    ) {
        this.sampleCount = sampleCount;
        this.warmupIterations = warmupIterations;
        this.assertionMetric = assertionMetric;
        this.reportDirectory = reportDirectory;
        this.collectDeviceEvidence = collectDeviceEvidence;
    }

    static PerformanceSettings fromEnvironment() {
        return new PerformanceSettings(
                positiveInt("performance.sampleCount", "PERF_SAMPLE_COUNT", DEFAULT_SAMPLE_COUNT),
                nonNegativeInt("performance.warmupIterations", "PERF_WARMUP_ITERATIONS", DEFAULT_WARMUP_ITERATIONS),
                AssertionMetric.from(value("performance.assertionMetric", "PERF_ASSERTION_METRIC", "median")),
                Path.of(value("performance.reportDirectory", "PERF_REPORT_DIR", "build/reports/performance")).toAbsolutePath().normalize(),
                Boolean.parseBoolean(value("performance.collectDeviceEvidence", "PERF_COLLECT_DEVICE_EVIDENCE", "true"))
        );
    }

    int sampleCount() {
        return sampleCount;
    }

    int warmupIterations() {
        return warmupIterations;
    }

    Path reportDirectory() {
        return reportDirectory;
    }

    boolean collectDeviceEvidence() {
        return collectDeviceEvidence;
    }

    PerformanceBudget loginSubmitBudget() {
        return new PerformanceBudget(
                "login-submit-to-catalog",
                assertionMetric,
                durationMillisOrLegacySeconds(
                        "performance.loginSubmitBudgetMs",
                        "PERF_LOGIN_SUBMIT_BUDGET_MS",
                        "performance.loginBudgetSeconds",
                        "PERF_LOGIN_BUDGET_SECONDS",
                        DEFAULT_LOGIN_HARD_BUDGET_MS
                ),
                durationMillis(
                        "performance.loginSubmitTargetMs",
                        "PERF_LOGIN_SUBMIT_TARGET_MS",
                        DEFAULT_LOGIN_TARGET_MS
                )
        );
    }

    PerformanceBudget catalogItemOpenBudget() {
        return new PerformanceBudget(
                "catalog-item-open",
                assertionMetric,
                durationMillisOrLegacySeconds(
                        "performance.catalogItemOpenBudgetMs",
                        "PERF_CATALOG_ITEM_OPEN_BUDGET_MS",
                        "performance.catalogBudgetSeconds",
                        "PERF_CATALOG_BUDGET_SECONDS",
                        DEFAULT_CATALOG_HARD_BUDGET_MS
                ),
                durationMillis(
                        "performance.catalogItemOpenTargetMs",
                        "PERF_CATALOG_ITEM_OPEN_TARGET_MS",
                        DEFAULT_CATALOG_TARGET_MS
                )
        );
    }

    private static Duration durationMillisOrLegacySeconds(
            String millisProperty,
            String millisEnvironment,
            String legacySecondsProperty,
            String legacySecondsEnvironment,
            long defaultMillis
    ) {
        String millis = configuredValue(millisProperty, millisEnvironment);
        if (millis != null) {
            return Duration.ofMillis(positiveLong(millisProperty, millis));
        }

        String legacySeconds = configuredValue(legacySecondsProperty, legacySecondsEnvironment);
        if (legacySeconds != null) {
            return Duration.ofSeconds(positiveLong(legacySecondsProperty, legacySeconds));
        }

        return Duration.ofMillis(defaultMillis);
    }

    private static Duration durationMillis(String property, String environment, long defaultMillis) {
        return Duration.ofMillis(positiveLong(property, value(property, environment, Long.toString(defaultMillis))));
    }

    private static int positiveInt(String property, String environment, int defaultValue) {
        int parsed = integer(property, value(property, environment, Integer.toString(defaultValue)));
        if (parsed < 1) {
            throw new IllegalArgumentException(property + " must be greater than 0.");
        }
        return parsed;
    }

    private static int nonNegativeInt(String property, String environment, int defaultValue) {
        int parsed = integer(property, value(property, environment, Integer.toString(defaultValue)));
        if (parsed < 0) {
            throw new IllegalArgumentException(property + " must be 0 or greater.");
        }
        return parsed;
    }

    private static int integer(String property, String rawValue) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(property + " must be an integer, but was: " + rawValue, exception);
        }
    }

    private static long positiveLong(String property, String rawValue) {
        try {
            long parsed = Long.parseLong(rawValue);
            if (parsed < 1) {
                throw new IllegalArgumentException(property + " must be greater than 0.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(property + " must be a positive integer, but was: " + rawValue, exception);
        }
    }

    private static String value(String systemProperty, String environmentVariable, String defaultValue) {
        String configured = configuredValue(systemProperty, environmentVariable);
        return configured == null ? defaultValue : configured;
    }

    private static String configuredValue(String systemProperty, String environmentVariable) {
        String propertyValue = System.getProperty(systemProperty);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String environmentValue = System.getenv(environmentVariable);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        return null;
    }

    enum AssertionMetric {
        MIN("min"),
        MEDIAN("median"),
        P90("p90"),
        P95("p95"),
        MAX("max"),
        MEAN("mean");

        private final String displayName;

        AssertionMetric(String displayName) {
            this.displayName = displayName;
        }

        static AssertionMetric from(String rawValue) {
            String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
            for (AssertionMetric metric : values()) {
                if (metric.displayName.equals(normalized)) {
                    return metric;
                }
            }

            throw new IllegalArgumentException(
                    "performance.assertionMetric must be one of min, median, p90, p95, max, mean."
            );
        }

        double valueMillis(PerformanceResult result) {
            return switch (this) {
                case MIN -> result.minMillis();
                case MEDIAN -> result.medianMillis();
                case P90 -> result.percentileMillis(90);
                case P95 -> result.percentileMillis(95);
                case MAX -> result.maxMillis();
                case MEAN -> result.meanMillis();
            };
        }

        String displayName() {
            return displayName;
        }
    }
}
