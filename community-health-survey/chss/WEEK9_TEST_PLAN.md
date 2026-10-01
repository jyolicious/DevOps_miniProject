# Week 9 Selenium Test Plan

## Objective

Week 9 validates critical Community Health Survey System user journeys with Selenium WebDriver and Chrome. The tests use explicit waits, synthetic data, and assertions, and are discoverable by Maven Surefire for later Jenkins integration.

## Environment

- Windows
- Java 17
- Spring Boot 3.2.5
- Chrome and Selenium WebDriver 4.25.0 (Selenium Manager resolves ChromeDriver)
- Maven
- Local application at `http://localhost:8081`
- MySQL-backed application data

Start the application and ensure the demo accounts are available before running Selenium tests. The tests do not start another Spring Boot server. Override the URL with `-Dselenium.base.url=http://host:port` when needed.

## Test Journeys

| Test ID | Journey | User Role | Expected Result |
|---|---|---|---|
| TC-S01 | Valid login | Surveyor | Dashboard displayed with role/user information |
| TC-S02 | Create survey | Surveyor | New synthetic survey saved and displayed |
| TC-S03 | Search survey | Surveyor | Matching synthetic survey displayed; search is case-insensitive |
| TC-S04 | Status update | Administrator | DRAFT → SUBMITTED → VERIFIED → CLOSED reflected in detail |
| TC-S05 | Dashboard summary | Surveyor/Health Officer | Summary cards and numeric counts displayed |

An additional invalid-login check runs with TC-S01.

## Test Data

Only synthetic values are entered. Each create/search/status journey uses a unique `SeleniumUser_<timestamp>` name; no real personal or medical data, database credentials, or database password are used. Records persist between test runs.

## Assertions

The suite checks successful login and user role, rejected invalid login, persisted survey creation, unique-name search results, each supported forward status transition, and presence of the five dashboard cards with numeric counts. Counts and pre-existing database rows are not hard-coded.

The status test follows the application's implemented administrator-only single-step workflow: DRAFT → SUBMITTED → VERIFIED → CLOSED. Invalid skips are rejected by the service.

## Failure Handling

JUnit failure lifecycle handling captures a Chrome screenshot automatically in `target/selenium-screenshots/`, using the test class, method, and timestamp in the filename. Maven Surefire writes standard reports under `target/surefire-reports/`.

## Execution

From the project root:

```powershell
community-health-survey\chss\..\..\mvnw.cmd -f community-health-survey\chss\pom.xml test
```

From `community-health-survey/chss`:

```powershell
..\..\mvnw.cmd -f pom.xml test
```

The application must already be running at `http://localhost:8081`; Chrome must be installed. Selenium Manager may need network access on its first driver resolution.

## Results

Executed on 2026-10-01 with the application at `http://localhost:8081` and Java 17:

- Focused: Maven `-f pom.xml -Dtest=LoginTest test` — 2 passed, 0 failed, 0 skipped.
- Full suite: Maven `-f pom.xml test` — 9 passed, 0 failed, 0 skipped (5 Selenium classes plus the existing `SurveyStatusTest`).
- Surefire reports are under `target/surefire-reports/`.
- The Windows wrapper command was attempted but failed before Maven startup in this shell (`icm: Cannot index into a null array`). The same Maven 3.9.16 distribution cached by the wrapper was invoked directly with the existing local repository configured. The standard wrapper command above remains the intended project command.

For the full Week 9 project report, see [Week9_Selenium_Testing.md](../../documentations/Week9_Selenium_Testing.md).
