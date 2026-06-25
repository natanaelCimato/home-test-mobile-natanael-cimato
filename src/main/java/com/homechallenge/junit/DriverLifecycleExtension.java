package com.homechallenge.junit;

import com.homechallenge.driver.DriverManager;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class DriverLifecycleExtension implements BeforeAllCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(DriverLifecycleExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot()
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent("driver-cleanup", key -> new DriverCleanup(), DriverCleanup.class);
    }

    private static final class DriverCleanup implements AutoCloseable {
        @Override
        public void close() {
            DriverManager.getInstance().quitDriver();
        }
    }
}
