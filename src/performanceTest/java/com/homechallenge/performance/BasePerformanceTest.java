package com.homechallenge.performance;

import com.homechallenge.driver.DriverManager;
import com.homechallenge.junit.DriverLifecycleExtension;
import com.homechallenge.junit.MobileEvidenceExtension;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({DriverLifecycleExtension.class, MobileEvidenceExtension.class})
abstract class BasePerformanceTest {
    protected AndroidDriver driver;

    @BeforeEach
    void setUp() {
        DriverManager driverManager = DriverManager.getInstance();
        driver = driverManager.getDriver();
        driverManager.resetApplicationState();
    }
}
