# Week 9 Documentation: Selenium Test Design and Local Execution

**Project:** Community Health Survey System (CHSS)
**Week:** 9
**Date:** 1 October 2026

## 1. Overview

Week 9 adds browser-level automated testing to the Community Health Survey System. Selenium WebDriver with Chrome validates the most important user journeys through the existing Thymeleaf UI. JUnit 5 and Maven Surefire discover and report the tests, providing a suite that can be integrated into the Jenkins pipeline in Week 10.

The application stack and Week 8 Jenkins deployment are unchanged. The tests run against an application already available at `http://localhost:8081`; they do not start a second Spring Boot server. Tomcat deployment on port 8083 is not part of this test run.

## 2. Goals and Scope

- Validate sign-in for a surveyor and rejection of invalid credentials.
- Verify survey creation and case-insensitive search using unique synthetic test names.
- Exercise the administrator's supported status workflow.
- Check dashboard summary labels and that their counts are numeric without assuming fixed database contents.
- Capture a screenshot automatically when a Selenium test fails.
- Keep the tests executable by the Maven Surefire lifecycle for future CI integration.

This suite validates the rendered UI and user-facing outcomes. It does not replace service-level unit tests, validate production data, or configure Jenkins. The existing `SurveyStatusTest` remains in place.

## 3. Environment and Dependencies

| Component | Configuration |
|---|---|
| Application | Spring Boot 3.2.5, Java 17, WAR packaging |
| UI | Thymeleaf, local URL `http://localhost:8081` |
| Browser | Google Chrome; ChromeDriver resolved by Selenium Manager |
| Browser automation | Selenium WebDriver 4.25.0, including Chrome support |
| Test framework | Existing JUnit 5 dependency from `spring-boot-starter-test` |
| Test execution | Maven Surefire through `mvn test` |
| Data store | Application's configured database; no database credentials are used by Selenium tests |

The known local demonstration accounts are `surveyor1`, `admin1`, and `officer1`. Tests use the surveyor and administrator accounts where their roles are required. Credentials are entered only for UI login and are not copied into configuration or database code.

## 4. Test Journeys

| ID | Journey | Role | Main checks |
|---|---|---|---|
| TC-S01 | Valid login | Surveyor | Dashboard and Survey Summary are visible; user and role appear. An additional invalid-login test verifies an error remains on the login page. |
| TC-S02 | Create survey | Surveyor | A uniquely named synthetic record is saved and appears in the records table with DRAFT status. |
| TC-S03 | Search survey | Surveyor | A unique record is created, searched by a lower-case version of its name, and returned by the case-insensitive search. |
| TC-S04 | Update status | Administrator | A synthetic draft record is advanced through SUBMITTED, VERIFIED, and CLOSED, with each resulting status checked. |
| TC-S05 | Dashboard summary | Surveyor | Total, Draft, Submitted, Verified, and Closed cards are visible and each count is numeric. |

### Status workflow

The status test follows the application's actual administrator-only workflow and `SurveyStatus` transition rules:

`DRAFT → SUBMITTED → VERIFIED → CLOSED`

The application allows one forward step at a time. The test selects each next state from the existing status form and checks the status shown on the record detail page after submission. It does not invent additional endpoints or controls.

## 5. Test Data and Repeatability

Create, search, and status tests use generated names in the form `SeleniumUser_<timestamp>`. Other values are synthetic placeholders such as `TestLocation_01` and `SyntheticCondition`; no real personal or medical information is entered. Test records persist in the application's database, so a new unique name is generated for each run. Assertions do not rely on a specific existing record or on fixed dashboard totals.

The date input is populated through its actual form field and a browser input/change event, which avoids locale-specific typing differences in Chrome. Explicit Selenium waits are used for page and result conditions; the suite does not use `Thread.sleep`.

## 6. Implementation

The tests are located in `community-health-survey/chss/src/test/java/com/chss/selenium/`:

- `BaseSeleniumTest.java` creates a Chrome session per test, uses Selenium Manager, configures the base URL, provides explicit waits and common synthetic-record helpers, and captures failure screenshots.
- `LoginTest.java` checks successful surveyor login and invalid login handling.
- `CreateSurveyTest.java` creates and verifies a new draft record.
- `SearchSurveyTest.java` verifies the application's case-insensitive name search.
- `StatusUpdateTest.java` runs the supported administrator transition sequence.
- `DashboardTest.java` checks the five summary cards and numeric values.

The selectors use IDs for login fields, names for form fields, and stable visible labels/table content where the Thymeleaf templates do not define IDs. No application template or controller behavior needed modification.

## 7. Failure Handling and Reports

The JUnit test watcher captures a screenshot on test failure. Files are named with the test class, method, and timestamp and written to:

`community-health-survey/chss/target/selenium-screenshots/`

Surefire's standard text and XML test reports are written to:

`community-health-survey/chss/target/surefire-reports/`

Screenshots are diagnostic artifacts and are produced when a Selenium test fails; successful runs do not need to generate them.

## 8. Execution Instructions

Before running the browser suite:

1. Start the CHSS application on `http://localhost:8081` and confirm it is reachable.
2. Ensure Chrome is installed and the local demonstration accounts are available.
3. From `community-health-survey/chss`, run:

```powershell
..\..\mvnw.cmd -f pom.xml test
```

To run only the login tests:

```powershell
..\..\mvnw.cmd -f pom.xml -Dtest=LoginTest test
```

The base URL defaults to `http://localhost:8081`. It can be overridden with the Java system property `-Dselenium.base.url=http://host:port`. Selenium Manager may require network access the first time it resolves a ChromeDriver version.

## 9. Execution Results

Executed on **1 October 2026** with Java 17, Chrome, and the application at `http://localhost:8081`:

| Run | Result |
|---|---|
| Focused `LoginTest` | 2 passed, 0 failed, 0 skipped |
| Full Maven test suite | 9 passed, 0 failed, 0 skipped |

The full suite includes six Selenium test methods across five Selenium classes and the three existing `SurveyStatusTest` unit tests. Surefire reports were generated under `target/surefire-reports/`.

In this workstation's shell, the Maven Wrapper command failed before Maven startup with `icm: Cannot index into a null array`. The same cached Maven 3.9.16 distribution was therefore invoked directly with the existing local Maven repository configured; the focused and full test runs both completed successfully. The wrapper command above remains the documented command for normal project environments. Chrome emitted a CDP compatibility warning for its installed version, but no CDP-specific API is used and all browser tests passed.

## 10. Week 10 Readiness

Maven Surefire discovers the Selenium classes during the normal `test` phase, and the application remains an external prerequisite. Week 10 can add a Jenkins stage that starts or targets the application, runs this Maven command, and retains Surefire reports and failure screenshots as build artifacts.

For step-by-step setup and test-case summary, see [`community-health-survey/chss/WEEK9_TEST_PLAN.md`](../community-health-survey/chss/WEEK9_TEST_PLAN.md).
