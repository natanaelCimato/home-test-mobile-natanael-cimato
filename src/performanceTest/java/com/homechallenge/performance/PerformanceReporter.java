package com.homechallenge.performance;

import com.homechallenge.reporting.MobileEvidence;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PerformanceReporter {
    private PerformanceReporter() {
    }

    static void publish(
            PerformanceResult result,
            PerformanceSettings settings,
            PerformanceBudget budget,
            AndroidDriver driver
    ) {
        Allure.parameter(result.operation() + " sample count", result.measuredSamples().size());
        Allure.parameter(result.operation() + " warmup iterations", result.warmupSamples().size());
        Allure.parameter(result.operation() + " assertion metric", budget.assertionMetric().displayName());
        Allure.parameter(result.operation() + " assertion ms", formatMillis(budget.actualMillis(result)));
        Allure.parameter(result.operation() + " target budget ms", budget.targetBudget().toMillis());
        Allure.parameter(result.operation() + " hard budget ms", budget.hardBudget().toMillis());
        Allure.parameter(result.operation() + " min ms", formatMillis(result.minMillis()));
        Allure.parameter(result.operation() + " median ms", formatMillis(result.medianMillis()));
        Allure.parameter(result.operation() + " p90 ms", formatMillis(result.percentileMillis(90)));
        Allure.parameter(result.operation() + " p95 ms", formatMillis(result.percentileMillis(95)));
        Allure.parameter(result.operation() + " max ms", formatMillis(result.maxMillis()));

        attach("performance-summary - " + result.operation(), "text/plain", "txt",
                result.summaryText(settings, budget));
        attach("performance-samples - " + result.operation(), "text/csv", "csv",
                result.samplesCsv());
        attach("performance-summary-json - " + result.operation(), "application/json", "json",
                result.summaryJson(settings, budget));

        writeReportFiles(result, settings, budget);
        PerformanceDeviceEvidence.attach(driver, result.operation(), settings);
    }

    static void assertWithinHardBudget(PerformanceResult result, PerformanceBudget budget) {
        assertTrue(
                !budget.exceedsHardBudget(result),
                result.operation() + " exceeded hard budget: " + budget.actualSummary(result)
        );
    }

    private static void writeReportFiles(PerformanceResult result, PerformanceSettings settings, PerformanceBudget budget) {
        try {
            Files.createDirectories(settings.reportDirectory());
            String fileName = safeFileName(result.operation());
            Files.writeString(
                    settings.reportDirectory().resolve(fileName + "-summary.txt"),
                    result.summaryText(settings, budget),
                    StandardCharsets.UTF_8
            );
            Files.writeString(
                    settings.reportDirectory().resolve(fileName + "-samples.csv"),
                    result.samplesCsv(),
                    StandardCharsets.UTF_8
            );
            Files.writeString(
                    settings.reportDirectory().resolve(fileName + "-summary.json"),
                    result.summaryJson(settings, budget),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            MobileEvidence.attachText("performance-report-write-failed - " + result.operation(), exception.toString());
        }
    }

    private static void attach(String name, String contentType, String extension, String content) {
        Allure.addAttachment(
                name,
                contentType,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                extension
        );
    }

    private static String safeFileName(String operation) {
        String sanitized = operation.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return sanitized.replaceAll("^-+|-+$", "");
    }

    private static String formatMillis(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
