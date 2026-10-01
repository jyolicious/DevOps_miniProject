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

**Status: not yet executed or committed.** No defect has been introduced at this point in the documented work. Do not treat the pipeline implementation or a local test as evidence of a Jenkins failure.

- Defect introduced:
- Defect commit:
- Selenium test expected to detect it:
- Jenkins failed build number and URL:
- Published failure report and screenshot:
- Evidence Package/Deploy were skipped:

These fields must be completed only after the failed Jenkins run is observed.

## Defect Correction

**Status: pending the deliberate-failure exercise.**

- Root cause:
- Corrected source file:
- Fix commit:
- Local regression result:

## Successful Rerun

**Status: not yet verified in Jenkins.**

- Jenkins build number and URL:
- Selenium result:
- JUnit report published:
- Package result:
- Tomcat deployment result and URL:

## Jenkins Execution and Evidence

The Jenkins login page at `http://localhost:8082` responded, but its JSON API returned HTTP 403 in this session. No Jenkins UI build was run or observed, and no Git push or Jenkins result is claimed here. The Jenkins operator must authenticate and run the configured `Community-Health-Survey-Pipeline` job after the relevant commits are available to its configured branch.

The evidence checklist in [`week10-evidence/README.md`](week10-evidence/README.md) lists the exact Jenkins screenshots to save. Do not fill build numbers or mark evidence complete until the corresponding run is observed.

## Conclusion

The repository pipeline is arranged to run the Selenium suite before package and deploy, publish test results, and fail the gate on a non-zero test result. Completion of Week 10's required Jenkins evidence still depends on authenticated failed and successful pipeline runs, including the controlled defect introduction and correction.
