# Home Test Mobile

Mobile automation challenge for the Android APK `app-home-test-mobile.apk`.

The project contains two implementations over the same Appium page objects:

- Plain JUnit 5 suite: `src/test/java`
- Cucumber suite: `src/cucumberTest/java` and `src/cucumberTest/resources/features`

## Requirements

The preferred execution path is Docker native:

- Docker Desktop with Docker Compose
- Docker/WSL virtualization enabled
- `/dev/kvm` is optional but recommended on Linux for faster emulator performance

No Java, Gradle, Android SDK, Appium, or emulator installation is required on the host when using Docker.

The APK is intentionally ignored by git because it is larger than GitHub's regular file limit. Download or copy it locally into the project root before running the suites:

```text
app-home-test-mobile.apk
```

## Run With Docker

Start the Android emulator/Appium service:

```bash
docker compose up -d android
```

The Android container installs `app-home-test-mobile.apk` once during its health check. The test containers reuse the installed app so Appium does not reinstall the large APK for every scenario.

Run the suite without Cucumber:

```bash
docker compose up --abort-on-container-exit --exit-code-from tests tests
```

Run the Cucumber suite:

```bash
docker compose --profile cucumber up --abort-on-container-exit --exit-code-from tests-cucumber tests-cucumber
```

Run both suites:

```bash
docker compose --profile all up --abort-on-container-exit --exit-code-from tests-all tests-all
```

Run the performance smoke suite:

```bash
docker compose --profile performance run --rm tests-performance
```

Generate the Allure HTML report after any run:

```bash
docker compose --profile report run --rm allure-report
```

Open the generated report from:

```text
build/reports/allure-report/allureReport/index.html
```

Watch the emulator through noVNC:

```text
http://localhost:6080/vnc.html?autoconnect=true&resize=scale
```

On Windows/Docker Desktop the project builds a small local Docker image that lets the emulator run without `/dev/kvm`.
It uses Android 11 and software acceleration for compatibility. Linux hosts with KVM can remove the local Dockerfile patch and use the upstream image directly if faster boot time is needed.

Appium is exposed on:

```text
http://localhost:4723/
```

The Docker Appium server starts with `--relaxed-security` so the failure handler can collect `dumpsys` and `ps` diagnostics through `mobile: shell`.

Stop and clean the Docker environment:

```bash
docker compose down -v
```

## GitHub Actions CI

The repository includes two GitHub Actions workflows:

- `CI`: runs on push and pull request to `main`; compiles all Java source sets with Dockerized Gradle.
- `Mobile E2E`: manual workflow that downloads the APK, starts the Docker Android/Appium stack, runs the selected mobile suite, generates Allure, and uploads test evidence.

The APK is not committed to git. Configure one of these APK sources before running `Mobile E2E`:

Option A, recommended when the original release asset has a stable URL:

```bash
gh secret set APK_DOWNLOAD_URL --body "https://example.com/path/to/app-home-test-mobile.apk"
gh variable set APK_SHA256 --body "***REMOVED***"
```

Option B, use a GitHub Release asset in this same repository:

```bash
gh release create mobile-apk app-home-test-mobile.apk --title "Mobile APK" --notes "APK consumed by the Mobile E2E workflow."
gh variable set APK_RELEASE_TAG --body "mobile-apk"
gh variable set APK_SHA256 --body "***REMOVED***"
```

Run the mobile workflow from GitHub Actions and choose one suite:

- `plain`
- `cucumber`
- `all`
- `performance`

Allure results, screenshots, animated recordings, JUnit/Cucumber reports, and Docker logs are uploaded as workflow artifacts.

### Slack Notifications

Both workflows can notify Slack through an incoming webhook. Notifications are optional: if the secret is missing, the Slack reporter is skipped and the workflow continues.

Configure this secret:

```bash
gh secret set SLACK_WEBHOOK_URL --body "https://hooks.slack.com/services/..."
```

Notification payloads include:

- workflow and job name
- selected mobile suite
- status
- repository, branch, short commit, and actor
- direct GitHub Actions run/artifact link

## Run Locally Without Docker

If you already have Java 17, Gradle, Android SDK, an Android emulator/device, and Appium 2 installed:

```bash
gradle clean test
gradle clean cucumberTest
gradle clean allMobileTests
```

Default capabilities can be overridden with system properties:

```bash
gradle clean test \
  -Dappium.serverUrl=http://127.0.0.1:4723/ \
  -Dappium.app=/absolute/path/to/app-home-test-mobile.apk \
  -Dappium.deviceName="Android Emulator" \
  -Dappium.platformVersion=11.0
```

Or with environment variables:

```bash
APPIUM_SERVER_URL=http://127.0.0.1:4723/ \
APP_PATH=/absolute/path/to/app-home-test-mobile.apk \
DEVICE_NAME="Android Emulator" \
ANDROID_PLATFORM_VERSION=11.0 \
gradle clean test
```

## Implemented Scenarios

- Scenario 1: successful login with `johndoe@email.com / 123`
- Scenario 2: missing credentials, incomplete credentials, and invalid credentials alerts
- Scenario 3: scroll catalog to `Twilight Glow` and validate its detail screen
- Scenario 4 bonus: registration form, date picker, terms checkbox, and success screen
- Registration navigation: login screen opens the sign-up form and shows all account fields

## Performance Tests

The performance suite is intentionally separate from the functional suite because Android software emulation on Windows can be slow and noisy.

- Login performance: valid credentials should reach the catalog within `PERF_LOGIN_BUDGET_SECONDS` (Docker default: `600`)
- Catalog performance: opening `Twilight Glow` should complete within `PERF_CATALOG_BUDGET_SECONDS` (Docker default: `420`)

Budgets can be tightened on a machine with KVM/hardware acceleration:

```bash
PERF_LOGIN_BUDGET_SECONDS=60 PERF_CATALOG_BUDGET_SECONDS=90 docker compose --profile performance run --rm tests-performance
```

## Evidence And Diagnostics

Allure results are written to:

```text
build/allure-results
```

Each JUnit/Cucumber scenario records:

- final screenshot (`image/png`)
- animated screen recording (`image/gif`) built from periodic screenshots
- Allure steps from Page Objects and Cucumber steps
- device/app metadata

On failure, the framework also attaches:

- first project stack frame with file and line number
- project stack trace
- current package/activity/session
- page source when available
- `dumpsys window`
- `dumpsys activity top`
- Android process list

## Reports

- Plain JUnit report: `build/reports/tests/test/index.html`
- Cucumber HTML report: `build/reports/cucumber/cucumber.html`
- Allure report: `build/reports/allure-report/allureReport/index.html`
- Cucumber JSON report: `build/reports/cucumber/cucumber.json`

## Repository Submission Notes

Create a new public GitHub repository, for example `home-test-mobile`, then push this project directly. Do not fork the original release repository.
