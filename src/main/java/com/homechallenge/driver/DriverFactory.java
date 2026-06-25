package com.homechallenge.driver;

import com.homechallenge.config.AppConfig;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

import java.time.Duration;

public final class DriverFactory {
    private DriverFactory() {
    }

    public static AndroidDriver createDriver() {
        AppConfig config = AppConfig.fromEnvironment();

        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setAutomationName("UiAutomator2")
                .setDeviceName(config.deviceName())
                .setAppPackage(config.appPackage())
                .setAppActivity(config.appActivity())
                .setNoReset(config.skipAppInstall())
                .setFullReset(false)
                .setAutoGrantPermissions(true)
                .setNewCommandTimeout(Duration.ofSeconds(config.newCommandTimeoutSeconds()));

        if (!config.skipAppInstall()) {
            options.setApp(config.appPath());
            options.setCapability("appium:androidInstallTimeout", 600000);
        }

        if (!config.platformVersion().isBlank()) {
            options.setPlatformVersion(config.platformVersion());
        }

        options.setCapability("appium:appWaitActivity", "*");
        options.setCapability("appium:adbExecTimeout", 600000);
        options.setCapability("appium:disableWindowAnimation", true);
        options.setCapability("appium:unicodeKeyboard", true);
        options.setCapability("appium:resetKeyboard", false);
        options.setCapability("appium:skipLogcatCapture", true);
        options.setCapability("appium:settings[waitForIdleTimeout]", 1000);
        options.setCapability("appium:settings[waitForSelectorTimeout]", 10000);
        options.setCapability("appium:uiautomator2ServerInstallTimeout", 300000);
        options.setCapability("appium:uiautomator2ServerLaunchTimeout", 300000);

        return new AndroidDriver(config.serverUrl(), options);
    }
}
