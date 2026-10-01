# Week 10 Jenkins Evidence Checklist

Save screenshots or exported Jenkins artifacts here after running the pipeline. No Jenkins build evidence has been captured yet.

The local defect exercise has been verified: the temporary `Survey Summery` heading made `DashboardTest.dashboardDisplaysNumericSurveySummary` time out, and the corrected full local suite passed. Its Surefire report and screenshot are available in the project's ignored `target/` directory. These are local artifacts, not Jenkins evidence.

- [ ] Pipeline stage view showing Selenium before Package and Deploy.
- [ ] Jenkins JUnit report for the baseline run.
- [ ] Deliberately broken application commit and its Selenium failure details.
- [ ] Failed build overview showing `FAILURE`.
- [ ] Failed run's JUnit details and archived Selenium screenshot.
- [ ] Failed stage view showing Package and Deploy skipped.
- [ ] Defect-fix commit in Git history.
- [ ] Successful rerun overview and Selenium JUnit report.
- [ ] Successful Package and Deploy stages, plus the deployed Tomcat URL responding.

Suggested capture locations: `01-pipeline-stages.png`, `02-baseline-junit.png`, `03-deliberate-failure.png`, `04-failed-junit-and-screenshot.png`, `05-package-deploy-skipped.png`, `06-defect-fix-commit.png`, and `07-successful-rerun-deploy.png`.

Do not store database credentials, Jenkins secrets, or unrelated personal records in screenshots or exported evidence.
