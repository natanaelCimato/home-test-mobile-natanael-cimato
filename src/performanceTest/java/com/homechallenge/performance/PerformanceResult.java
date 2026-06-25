package com.homechallenge.performance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class PerformanceResult {
    private final String operation;
    private final List<PerformanceSample> warmupSamples;
    private final List<PerformanceSample> measuredSamples;

    PerformanceResult(String operation, List<PerformanceSample> warmupSamples, List<PerformanceSample> measuredSamples) {
        if (measuredSamples.isEmpty()) {
            throw new IllegalArgumentException("At least one measured sample is required.");
        }

        this.operation = operation;
        this.warmupSamples = List.copyOf(warmupSamples);
        this.measuredSamples = List.copyOf(measuredSamples);
    }

    String operation() {
        return operation;
    }

    List<PerformanceSample> warmupSamples() {
        return warmupSamples;
    }

    List<PerformanceSample> measuredSamples() {
        return measuredSamples;
    }

    double minMillis() {
        return sortedMeasuredNanos().get(0) / 1_000_000.0;
    }

    double medianMillis() {
        List<Long> sorted = sortedMeasuredNanos();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle) / 1_000_000.0;
        }

        return ((sorted.get(middle - 1) + sorted.get(middle)) / 2.0) / 1_000_000.0;
    }

    double percentileMillis(int percentile) {
        if (percentile < 1 || percentile > 100) {
            throw new IllegalArgumentException("Percentile must be between 1 and 100.");
        }

        List<Long> sorted = sortedMeasuredNanos();
        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1))) / 1_000_000.0;
    }

    double maxMillis() {
        List<Long> sorted = sortedMeasuredNanos();
        return sorted.get(sorted.size() - 1) / 1_000_000.0;
    }

    double meanMillis() {
        return measuredSamples.stream()
                .mapToDouble(PerformanceSample::elapsedMillis)
                .average()
                .orElseThrow();
    }

    String summaryText(PerformanceSettings settings, PerformanceBudget budget) {
        return "Operation: " + operation + System.lineSeparator()
                + "Measured samples: " + measuredSamples.size() + System.lineSeparator()
                + "Warmup iterations: " + warmupSamples.size() + System.lineSeparator()
                + "Assertion metric: " + budget.assertionMetric().displayName() + System.lineSeparator()
                + "Assertion value: " + formatMillis(budget.actualMillis(this)) + " ms" + System.lineSeparator()
                + "Target budget: " + budget.targetBudget().toMillis() + " ms" + System.lineSeparator()
                + "Hard budget: " + budget.hardBudget().toMillis() + " ms" + System.lineSeparator()
                + "Target status: " + (budget.exceedsTargetBudget(this) ? "OVER_TARGET" : "WITHIN_TARGET") + System.lineSeparator()
                + "Hard status: " + (budget.exceedsHardBudget(this) ? "OVER_HARD_BUDGET" : "WITHIN_HARD_BUDGET") + System.lineSeparator()
                + "Report directory: " + settings.reportDirectory() + System.lineSeparator()
                + System.lineSeparator()
                + "Measured summary" + System.lineSeparator()
                + "min_ms=" + formatMillis(minMillis()) + System.lineSeparator()
                + "median_ms=" + formatMillis(medianMillis()) + System.lineSeparator()
                + "mean_ms=" + formatMillis(meanMillis()) + System.lineSeparator()
                + "p90_ms=" + formatMillis(percentileMillis(90)) + System.lineSeparator()
                + "p95_ms=" + formatMillis(percentileMillis(95)) + System.lineSeparator()
                + "max_ms=" + formatMillis(maxMillis()) + System.lineSeparator();
    }

    String samplesCsv() {
        StringBuilder csv = new StringBuilder("operation,phase,iteration,elapsed_ms").append(System.lineSeparator());
        for (PerformanceSample sample : warmupSamples) {
            appendSample(csv, sample);
        }
        for (PerformanceSample sample : measuredSamples) {
            appendSample(csv, sample);
        }
        return csv.toString();
    }

    String summaryJson(PerformanceSettings settings, PerformanceBudget budget) {
        return "{"
                + "\"operation\":\"" + escapeJson(operation) + "\","
                + "\"sampleCount\":" + measuredSamples.size() + ","
                + "\"warmupIterations\":" + warmupSamples.size() + ","
                + "\"assertionMetric\":\"" + budget.assertionMetric().displayName() + "\","
                + "\"assertionValueMs\":" + formatMillis(budget.actualMillis(this)) + ","
                + "\"targetBudgetMs\":" + budget.targetBudget().toMillis() + ","
                + "\"hardBudgetMs\":" + budget.hardBudget().toMillis() + ","
                + "\"targetStatus\":\"" + (budget.exceedsTargetBudget(this) ? "OVER_TARGET" : "WITHIN_TARGET") + "\","
                + "\"hardStatus\":\"" + (budget.exceedsHardBudget(this) ? "OVER_HARD_BUDGET" : "WITHIN_HARD_BUDGET") + "\","
                + "\"reportDirectory\":\"" + escapeJson(settings.reportDirectory().toString()) + "\","
                + "\"summary\":{"
                + "\"minMs\":" + formatMillis(minMillis()) + ","
                + "\"medianMs\":" + formatMillis(medianMillis()) + ","
                + "\"meanMs\":" + formatMillis(meanMillis()) + ","
                + "\"p90Ms\":" + formatMillis(percentileMillis(90)) + ","
                + "\"p95Ms\":" + formatMillis(percentileMillis(95)) + ","
                + "\"maxMs\":" + formatMillis(maxMillis())
                + "},"
                + "\"samples\":" + samplesJson()
                + "}";
    }

    private List<Long> sortedMeasuredNanos() {
        List<Long> sorted = new ArrayList<>();
        for (PerformanceSample sample : measuredSamples) {
            sorted.add(sample.elapsedNanos());
        }
        sorted.sort(Comparator.naturalOrder());
        return sorted;
    }

    private String samplesJson() {
        StringBuilder json = new StringBuilder("[");
        List<PerformanceSample> allSamples = new ArrayList<>(warmupSamples);
        allSamples.addAll(measuredSamples);

        for (int index = 0; index < allSamples.size(); index++) {
            PerformanceSample sample = allSamples.get(index);
            if (index > 0) {
                json.append(",");
            }

            json.append("{")
                    .append("\"phase\":\"").append(sample.phase()).append("\",")
                    .append("\"iteration\":").append(sample.iteration()).append(",")
                    .append("\"elapsedMs\":").append(formatMillis(sample.elapsedMillis()))
                    .append("}");
        }

        json.append("]");
        return json.toString();
    }

    private static void appendSample(StringBuilder csv, PerformanceSample sample) {
        csv.append(sample.operation()).append(",")
                .append(sample.phase()).append(",")
                .append(sample.iteration()).append(",")
                .append(formatMillis(sample.elapsedMillis()))
                .append(System.lineSeparator());
    }

    private static String formatMillis(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    record PerformanceSample(String operation, String phase, int iteration, long elapsedNanos) {
        double elapsedMillis() {
            return elapsedNanos / 1_000_000.0;
        }
    }
}
