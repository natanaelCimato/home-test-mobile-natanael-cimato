package com.homechallenge.performance;

import java.util.ArrayList;
import java.util.List;

final class PerformanceMeter {
    private final PerformanceSettings settings;

    PerformanceMeter(PerformanceSettings settings) {
        this.settings = settings;
    }

    PerformanceResult measure(String operation, IterationFactory iterationFactory) {
        List<PerformanceResult.PerformanceSample> warmups = measurePhase(
                operation,
                "warmup",
                settings.warmupIterations(),
                iterationFactory
        );
        List<PerformanceResult.PerformanceSample> samples = measurePhase(
                operation,
                "sample",
                settings.sampleCount(),
                iterationFactory
        );

        return new PerformanceResult(operation, warmups, samples);
    }

    private List<PerformanceResult.PerformanceSample> measurePhase(
            String operation,
            String phase,
            int iterations,
            IterationFactory iterationFactory
    ) {
        List<PerformanceResult.PerformanceSample> samples = new ArrayList<>();
        for (int iteration = 1; iteration <= iterations; iteration++) {
            MeasuredAction action = iterationFactory.prepareIteration();
            long startedAt = System.nanoTime();
            action.run();
            samples.add(new PerformanceResult.PerformanceSample(
                    operation,
                    phase,
                    iteration,
                    System.nanoTime() - startedAt
            ));
        }
        return samples;
    }

    @FunctionalInterface
    interface IterationFactory {
        MeasuredAction prepareIteration();
    }

    @FunctionalInterface
    interface MeasuredAction {
        void run();
    }
}
