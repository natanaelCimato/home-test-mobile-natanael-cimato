package com.homechallenge.reporting;

import com.homechallenge.config.AppConfig;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MobileEvidence {
    private static final String PROJECT_PACKAGE = "com.homechallenge";
    private static final ThreadLocal<ScreenshotTimeline> RECORDING = new ThreadLocal<>();

    private MobileEvidence() {
    }

    public static void addEnvironmentLabels(AppConfig config) {
        Allure.label("platform", "Android");
        Allure.parameter("deviceName", config.deviceName());
        Allure.parameter("platformVersion", config.platformVersion());
        Allure.parameter("appPackage", config.appPackage());
        Allure.parameter("skipAppInstall", config.skipAppInstall());
    }

    public static void startVideo(AndroidDriver driver, String name) {
        if (!isVideoEnabled() || driver == null) {
            return;
        }

        try {
            ScreenshotTimeline timeline = new ScreenshotTimeline(driver, maxVideoSeconds(), frameIntervalMillis());
            RECORDING.set(timeline);
            timeline.start();
        } catch (Exception exception) {
            attachText("Screen recording start failed - " + name, exception.toString());
        }
    }

    public static void stopVideo(AndroidDriver driver, String name) {
        if (!isVideoEnabled() || driver == null) {
            return;
        }

        try {
            ScreenshotTimeline timeline = RECORDING.get();
            if (timeline != null) {
                timeline.stop();
                byte[] gif = timeline.toGif();
                if (gif.length > 0) {
                    Allure.addAttachment(name, "image/gif", new ByteArrayInputStream(gif), "gif");
                } else {
                    attachText("Screen recording empty - " + name, "No screenshots were captured for the timeline.");
                }
            }
        } catch (Exception exception) {
            attachText("Screen recording stop failed - " + name, exception.toString());
        } finally {
            RECORDING.remove();
        }
    }

    public static void attachFinalScreenshot(AndroidDriver driver, String name) {
        if (driver == null) {
            return;
        }

        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(screenshot), "png");
        } catch (Exception exception) {
            attachText("Screenshot failed - " + name, exception.toString());
        }
    }

    public static void attachPageSource(AndroidDriver driver, String name) {
        if (driver == null) {
            return;
        }

        try {
            Allure.addAttachment(name, "application/xml", driver.getPageSource(), "xml");
        } catch (Exception exception) {
            attachText("Page source failed - " + name, exception.toString());
        }
    }

    public static void attachFailureDiagnostics(AndroidDriver driver, String title, Throwable throwable) {
        StringBuilder diagnostics = new StringBuilder();
        diagnostics.append("Context: ").append(title).append(System.lineSeparator());

        if (throwable != null) {
            diagnostics.append("Exception: ")
                    .append(throwable.getClass().getName())
                    .append(": ")
                    .append(throwable.getMessage())
                    .append(System.lineSeparator());

            firstProjectFrame(throwable).ifPresent(frame -> diagnostics
                    .append("First project failure line: ")
                    .append(frame.getClassName())
                    .append(".")
                    .append(frame.getMethodName())
                    .append("(")
                    .append(frame.getFileName())
                    .append(":")
                    .append(frame.getLineNumber())
                    .append(")")
                    .append(System.lineSeparator()));

            diagnostics.append(System.lineSeparator()).append("Project stack:").append(System.lineSeparator());
            for (StackTraceElement frame : throwable.getStackTrace()) {
                if (frame.getClassName().startsWith(PROJECT_PACKAGE)) {
                    diagnostics.append("  at ").append(frame).append(System.lineSeparator());
                }
            }
        }

        diagnostics.append(System.lineSeparator()).append(deviceSummary(driver));
        attachText("failure-diagnostics", diagnostics.toString());
        attachPageSource(driver, "failure-page-source");
        attachDeviceShell(driver, "dumpsys-window", "dumpsys", List.of("window"));
        attachDeviceShell(driver, "dumpsys-activity-top", "dumpsys", List.of("activity", "top"));
        attachDeviceShell(driver, "android-processes", "ps", List.of("-A"));
    }

    public static void attachScenarioDiagnostics(AndroidDriver driver, String title) {
        attachText("scenario-context", title + System.lineSeparator() + deviceSummary(driver));
    }

    public static void attachText(String name, String content) {
        byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        Allure.addAttachment(name, "text/plain", new ByteArrayInputStream(bytes), "txt");
    }

    private static void attachDeviceShell(AndroidDriver driver, String name, String command, List<String> args) {
        if (driver == null) {
            return;
        }

        try {
            Object result = executeShell(driver, command, args, 60000);
            attachText(name, String.valueOf(result));
        } catch (Exception exception) {
            attachText(name + "-failed", exception.toString());
        }
    }

    private static String deviceSummary(AndroidDriver driver) {
        if (driver == null) {
            return "Driver: <not available>";
        }

        StringBuilder summary = new StringBuilder();
        try {
            summary.append("Session: ").append(driver.getSessionId()).append(System.lineSeparator());
        } catch (Exception ignored) {
            summary.append("Session: <not available>").append(System.lineSeparator());
        }

        try {
            summary.append("Current package: ").append(driver.getCurrentPackage()).append(System.lineSeparator());
        } catch (Exception ignored) {
            summary.append("Current package: <not available>").append(System.lineSeparator());
        }

        try {
            summary.append("Current activity: ").append(driver.currentActivity()).append(System.lineSeparator());
        } catch (Exception ignored) {
            summary.append("Current activity: <not available>").append(System.lineSeparator());
        }

        return summary.toString();
    }

    private static Optional<StackTraceElement> firstProjectFrame(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            for (StackTraceElement frame : current.getStackTrace()) {
                if (frame.getClassName().startsWith(PROJECT_PACKAGE)) {
                    return Optional.of(frame);
                }
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private static boolean isVideoEnabled() {
        return Boolean.parseBoolean(value("evidence.video.enabled", "RECORD_VIDEO", "false"));
    }

    private static long maxVideoSeconds() {
        return Long.parseLong(value("evidence.video.maxSeconds", "VIDEO_TIME_LIMIT_SECONDS", "60"));
    }

    private static long frameIntervalMillis() {
        return Long.parseLong(value("evidence.video.frameIntervalMillis", "VIDEO_FRAME_INTERVAL_MS", "10000"));
    }

    private static Object executeShell(AndroidDriver driver, String command, List<String> args, long timeoutMs) {
        return driver.executeScript("mobile: shell", Map.of(
                "command", command,
                "args", args,
                "timeout", timeoutMs
        ));
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

    private static final class ScreenshotTimeline {
        private final AndroidDriver driver;
        private final long maxSeconds;
        private final long frameIntervalMillis;
        private final List<byte[]> frames = Collections.synchronizedList(new ArrayList<>());
        private final AtomicBoolean running = new AtomicBoolean(false);
        private Thread worker;

        private ScreenshotTimeline(AndroidDriver driver, long maxSeconds, long frameIntervalMillis) {
            this.driver = driver;
            this.maxSeconds = maxSeconds;
            this.frameIntervalMillis = frameIntervalMillis;
        }

        private void start() {
            running.set(true);
            worker = new Thread(this::captureLoop, "allure-screenshot-timeline");
            worker.setDaemon(true);
            worker.start();
        }

        private void stop() throws InterruptedException {
            running.set(false);
            if (worker != null) {
                worker.join(15000);
            }
            captureFrame();
        }

        private byte[] toGif() throws IOException {
            List<byte[]> snapshot;
            synchronized (frames) {
                snapshot = new ArrayList<>(frames);
            }

            if (snapshot.isEmpty()) {
                return new byte[0];
            }

            return createGif(snapshot, Math.toIntExact(frameIntervalMillis));
        }

        private void captureLoop() {
            long deadline = System.nanoTime() + maxSeconds * 1_000_000_000L;
            while (running.get() && System.nanoTime() < deadline) {
                captureFrame();
                try {
                    Thread.sleep(frameIntervalMillis);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        private void captureFrame() {
            try {
                frames.add(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
            } catch (Exception ignored) {
                // Screenshots are opportunistic evidence and must never fail the test.
            }
        }
    }

    private static byte[] createGif(List<byte[]> pngFrames, int delayMillis) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        if (!writers.hasNext()) {
            throw new IOException("No GIF ImageIO writer is available.");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageWriter writer = writers.next();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            writer.prepareWriteSequence(null);

            for (byte[] frame : pngFrames) {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(frame));
                if (image != null) {
                    writer.writeToSequence(
                            new IIOImage(image, null, gifMetadata(writer, image, delayMillis)),
                            null
                    );
                }
            }

            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }

        return output.toByteArray();
    }

    private static IIOMetadata gifMetadata(ImageWriter writer, BufferedImage image, int delayMillis) throws IOException {
        ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromBufferedImageType(image.getType());
        IIOMetadata metadata = writer.getDefaultImageMetadata(imageType, null);
        String metadataFormat = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadataFormat);

        IIOMetadataNode graphicControl = getOrCreateNode(root, "GraphicControlExtension");
        graphicControl.setAttribute("disposalMethod", "none");
        graphicControl.setAttribute("userInputFlag", "FALSE");
        graphicControl.setAttribute("transparentColorFlag", "FALSE");
        graphicControl.setAttribute("delayTime", Integer.toString(Math.max(1, delayMillis / 10)));
        graphicControl.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode appExtensions = getOrCreateNode(root, "ApplicationExtensions");
        IIOMetadataNode appExtension = new IIOMetadataNode("ApplicationExtension");
        appExtension.setAttribute("applicationID", "NETSCAPE");
        appExtension.setAttribute("authenticationCode", "2.0");
        appExtension.setUserObject(new byte[]{0x1, 0x0, 0x0});
        appExtensions.appendChild(appExtension);

        metadata.setFromTree(metadataFormat, root);
        return metadata;
    }

    private static IIOMetadataNode getOrCreateNode(IIOMetadataNode root, String nodeName) {
        for (int index = 0; index < root.getLength(); index++) {
            if (root.item(index).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) root.item(index);
            }
        }

        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        root.appendChild(node);
        return node;
    }
}
