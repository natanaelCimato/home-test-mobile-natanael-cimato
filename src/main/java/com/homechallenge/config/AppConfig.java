package com.homechallenge.config;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;

public final class AppConfig {
    private static final String DEFAULT_APP_PACKAGE = "com.learnautomationapp";
    private static final String DEFAULT_APP_ACTIVITY = ".MainActivity";

    private final URL serverUrl;
    private final String appPath;
    private final String appPackage;
    private final String appActivity;
    private final String deviceName;
    private final String platformVersion;
    private final Duration waitTimeout;
    private final long newCommandTimeoutSeconds;
    private final boolean skipAppInstall;

    private AppConfig(
            URL serverUrl,
            String appPath,
            String appPackage,
            String appActivity,
            String deviceName,
            String platformVersion,
            Duration waitTimeout,
            long newCommandTimeoutSeconds,
            boolean skipAppInstall
    ) {
        this.serverUrl = serverUrl;
        this.appPath = appPath;
        this.appPackage = appPackage;
        this.appActivity = appActivity;
        this.deviceName = deviceName;
        this.platformVersion = platformVersion;
        this.waitTimeout = waitTimeout;
        this.newCommandTimeoutSeconds = newCommandTimeoutSeconds;
        this.skipAppInstall = skipAppInstall;
    }

    public static AppConfig fromEnvironment() {
        String appPath = value("appium.app", "APP_PATH", "app-home-test-mobile.apk");
        String resolvedAppPath = resolveAppPath(appPath);

        return new AppConfig(
                toUrl(value("appium.serverUrl", "APPIUM_SERVER_URL", "http://127.0.0.1:4723/")),
                resolvedAppPath,
                value("appium.appPackage", "APP_PACKAGE", DEFAULT_APP_PACKAGE),
                value("appium.appActivity", "APP_ACTIVITY", DEFAULT_APP_ACTIVITY),
                value("appium.deviceName", "DEVICE_NAME", "Android Emulator"),
                value("appium.platformVersion", "ANDROID_PLATFORM_VERSION", ""),
                Duration.ofSeconds(Long.parseLong(value("appium.waitSeconds", "WAIT_SECONDS", "20"))),
                Long.parseLong(value("appium.newCommandTimeoutSeconds", "NEW_COMMAND_TIMEOUT_SECONDS", "120")),
                Boolean.parseBoolean(value("appium.skipAppInstall", "SKIP_APP_INSTALL", "false"))
        );
    }

    public URL serverUrl() {
        return serverUrl;
    }

    public String appPath() {
        return appPath;
    }

    public String appPackage() {
        return appPackage;
    }

    public String appActivity() {
        return appActivity;
    }

    public String deviceName() {
        return deviceName;
    }

    public String platformVersion() {
        return platformVersion;
    }

    public Duration waitTimeout() {
        return waitTimeout;
    }

    public long newCommandTimeoutSeconds() {
        return newCommandTimeoutSeconds;
    }

    public boolean skipAppInstall() {
        return skipAppInstall;
    }

    private static String value(String systemProperty, String environmentVariable, String defaultValue) {
        String propertyValue = System.getProperty(systemProperty);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String environmentValue = System.getenv(environmentVariable);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        return defaultValue;
    }

    private static String resolveAppPath(String appPath) {
        Path path = Path.of(appPath);
        return path.isAbsolute() ? path.toString() : path.toAbsolutePath().normalize().toString();
    }

    private static URL toUrl(String rawUrl) {
        try {
            return URI.create(rawUrl).toURL();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid Appium server URL: " + rawUrl, exception);
        }
    }
}
