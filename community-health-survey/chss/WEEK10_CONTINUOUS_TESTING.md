# Week 10 — Continuous Testing in Jenkins

## Objective

Integrate the Week 9 Selenium regression suite into the existing Windows Jenkins Pipeline job. Publish Maven/Surefire test reports and Selenium failure screenshots, and prevent packaging and deployment when a browser test fails.

The Week 8 Tomcat deployment stage and the Week 7 Freestyle job are retained. The test server uses a separate temporary port and an isolated in-memory H2 database, so the local application, MySQL credentials, and Tomcat deployment are not used as test infrastructure.

## Pipeline Flow

```text
Checkout
  → Build (clean test-compile)
  → Start Test Application (temporary Spring Boot process)
  → Selenium Tests
  → Publish Test Reports and screenshots
  → Stop Test Application
  → Package
  → Deploy to Tomcat on port 8083
```

The temporary app defaults to port 8181. This avoids collision with the local application on 8081, Jenkins on 8082, and the Week 8 Tomcat deployment on 8083. The port is held in the Jenkinsfile environment and can be changed there if the agent uses it already. The startup script refuses to attach to an existing listener, records only the process it starts, waits for `/login`, and the pipeline stops that process in both the normal stage and the `post { always }` cleanup.

The test-only Spring profile (`application-selenium.properties`) uses an in-memory H2 database and demo-user seeding. It contains no MySQL URL, username, or password. The application runs from the source project with the temporary profile and is not deployed to Tomcat.

If Maven/Surefire returns a non-zero code, the report stage first publishes any XML and screenshot artifacts, then deliberately fails the stage. Declarative Pipeline therefore skips Package and Deploy. Maven's test exit code is retained; it is never converted into success. The `post { always }` publisher is a fallback for failures before the report stage.

### Local verification

The temporary startup and cleanup scripts were exercised locally. The full suite passed against the temporary H2 app on port 8181: **9 tests, 0 failures, 0 errors, 0 skipped**. Selenium Manager needed network access to resolve ChromeDriver. The temporary process was stopped afterward. This validates the application profile and test target locally; it does not validate Jenkins Pipeline behavior.

## Selenium Suite

The pipeline runs all tests in `community-health-survey/chss/src/test/java/com/chss/selenium/`:

1. Valid surveyor login (plus invalid-credential handling).
2. Surveyor creates a unique synthetic survey record.
3. Surveyor searches for a unique record using case-insensitive name search.
4. Administrator advances a survey through DRAFT → SUBMITTED → VERIFIED → CLOSED.
5. Dashboard summary cards display numeric counts.

The `BaseSeleniumTest` URL precedence is the `selenium.base.url` Java system property, then `SELENIUM_BASE_URL`, then the Week 9 default `http://localhost:8081`. Jenkins supplies the environment variable for its isolated app port; Week 9 local execution keeps its default.

## Test Reports

Surefire XML files are read from:

`community-health-survey/chss/target/surefire-reports/*.xml`

The Pipeline uses Jenkins' `junit` publisher so failed tests appear in the job's test report. The XML files and any failure screenshots are archived as build artifacts. The Jenkins JUnit plugin must be installed and enabled on the controller.

## Failure Screenshots

Week 9 automatically captures browser screenshots after Selenium test failures under:

`community-health-survey/chss/target/selenium-screenshots/`

The pipeline archives `*.png` from this directory, including when the Selenium stage fails. No screenshot binary is checked into Git; use the failed Jenkins build's artifacts for evidence.

## Deliberate Defect

**Local defect exercise completed; Jenkins failure evidence is still pending.**

- Defect introduced: the dashboard heading was changed from `Survey Summary` to `Survey Summery`.
- Defect commit: `6e12503` (`Week 10: Introduce dashboard defect for Selenium gate`).
- Selenium test: `DashboardTest.dashboardDisplaysNumericSurveySummary`.
- Local result: Maven failed as expected; the test timed out waiting for `Survey Summary`. Surefire recorded one test error. The local failure screenshot is in `target/selenium-screenshots/`.
- Jenkins failed build number and URL:
- Jenkins JUnit report and archived screenshot: pending an authenticated Jenkins run.
- Evidence Package/Deploy were skipped: pending an authenticated Jenkins run.

The Jenkins build number, report publication, and skipped-stage result must be completed only after the failed Jenkins run is observed.

## Defect Correction

**Local correction completed; Jenkins correction evidence is pending.**

- Root cause: the misspelled dashboard heading did not match the text asserted by the Selenium journey.
- Correction: restore `Survey Summary` in `src/main/resources/templates/dashboard.html`.
- Fix commit: `ebaa259` (`Week 10: Fix dashboard heading detected by Selenium`).
- Local regression result: 9 tests passed, 0 failures, 0 errors, 0 skipped.

## Successful Rerun

**Status: not yet verified in Jenkins.**

- Jenkins build number and URL:
- Local post-fix Selenium/JUnit result: 9 passed, 0 failures, 0 errors, 0 skipped.
- Jenkins Selenium result: pending.
- Jenkins JUnit report published: pending.
- Jenkins Package result: pending.
- Jenkins Tomcat deployment result and URL: pending.

## Jenkins Execution and Evidence

The Jenkins login page at `http://localhost:8082` responded, but its JSON API returned HTTP 403 in this session. No Jenkins UI build was run or observed, and no Jenkins result is claimed here. The integration commit is `853244d`; the deliberate defect and correction commits are `6e12503` and `ebaa259`; the test-results documentation commit is `3bfdfd8`. These commits were pushed to `origin/main`.

To produce the required Jenkins defect and recovery runs, first run the job on `main` and capture its result. Then configure the job's Git branch specifier to the deliberate-defect revision `6e12503`, run it, and capture the failing test/report plus skipped Package/Deploy stages. Restore the branch specifier to `*/main`, save, and run again for the corrected commit. Jenkins must have its JUnit plugin, Java 17, Chrome, network access for Selenium Manager on first use, and access to the configured Tomcat installation. The Tomcat port was not listening when repository changes were prepared, so verify that the Deploy stage starts it successfully.

The evidence checklist in [`week10-evidence/README.md`](week10-evidence/README.md) lists the exact Jenkins screenshots to save. Do not fill build numbers or mark evidence complete until the corresponding run is observed.

## Conclusion

The repository pipeline is arranged to run the Selenium suite before package and deploy, publish test results, and fail the gate on a non-zero test result. Completion of Week 10's required Jenkins evidence still depends on authenticated failed and successful pipeline runs, including the controlled defect introduction and correction.
