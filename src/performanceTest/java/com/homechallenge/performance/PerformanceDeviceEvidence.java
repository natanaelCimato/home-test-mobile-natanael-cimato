package com.homechallenge.performance;

import com.homechallenge.config.AppConfig;
import com.homechallenge.reporting.MobileEvidence;
import io.appium.java_client.android.AndroidDriver;

import java.util.List;
import java.util.Map;

final class PerformanceDeviceEvidence {
    private PerformanceDeviceEvidence() {
    }

    static void attach(AndroidDriver driver, String operation, PerformanceSettings settings) {
        if (!settings.collectDeviceEvidence() || driver == null) {
            return;
        }

        AppConfig config = AppConfig.fromEnvironment();
        MobileEvidence.attachText("performance-device-context - " + operation, deviceContext(driver, config));
        attachShell(driver, "performance-meminfo - " + operation, "dumpsys", List.of("meminfo", config.appPackage()), 20_000);
        attachShell(driver, "performance-gfxinfo - " + operation, "dumpsys", List.of("gfxinfo", config.appPackage()), 20_000);
    }

    private static String deviceContext(AndroidDriver driver, AppConfig config) {
        StringBuilder context = new StringBuilder();
        context.append("App package: ").append(config.appPackage()).append(System.lineSeparator());
        context.append("Device name: ").append(config.deviceName()).append(System.lineSeparator());
        context.append("Platform version: ").append(config.platformVersion()).append(System.lineSeparator());
        context.append("Window animation scale: ")
                .append(shellValue(driver, "settings", List.of("get", "global", "window_animation_scale")))
                .append(System.lineSeparator());
        context.append("Transition animation scale: ")
                .append(shellValue(driver, "settings", List.of("get", "global", "transition_animation_scale")))
                .append(System.lineSeparator());
        context.append("Animator duration scale: ")
                .append(shellValue(driver, "settings", List.of("get", "global", "animator_duration_scale")))
                .append(System.lineSeparator());

        try {
            context.append("Current package: ").append(driver.getCurrentPackage()).append(System.lineSeparator());
        } catch (Exception exception) {
            context.append("Current package: <not available>").append(System.lineSeparator());
        }

        try {
            context.append("Current activity: ").append(driver.currentActivity()).append(System.lineSeparator());
        } catch (Exception exception) {
            context.append("Current activity: <not available>").append(System.lineSeparator());
        }

        return context.toString();
    }

    private static void attachShell(AndroidDriver driver, String name, String command, List<String> args, long timeoutMs) {
        try {
            MobileEvidence.attachText(name, String.valueOf(executeShell(driver, command, args, timeoutMs)));
        } catch (Exception exception) {
            MobileEvidence.attachText(name + "-failed", exception.toString());
        }
    }

    private static String shellValue(AndroidDriver driver, String command, List<String> args) {
        try {
            return String.valueOf(executeShell(driver, command, args, 5_000)).trim();
        } catch (Exception exception) {
            return "<not available: " + exception.getClass().getSimpleName() + ">";
        }
    }

    private static Object executeShell(AndroidDriver driver, String command, List<String> args, long timeoutMs) {
        return driver.executeScript("mobile: shell", Map.of(
                "command", command,
                "args", args,
                "timeout", timeoutMs
        ));
    }
}
