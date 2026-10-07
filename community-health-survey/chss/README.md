# Community Health Survey System

DevOps Mini-Project MVP — Week 3 project skeleton (PB-1), implementing the
architecture, data model and requirements documented in the Week 3 report.

## Stack
Java 17 · Spring Boot 3.2 (Web, Data JPA, Thymeleaf, Validation) · MySQL 8 · Maven

## Prerequisites
- JDK 17, `JAVA_HOME` set
- Maven 3.9+
- MySQL 8 running locally (**not required for the quick-start path below** — see H2 option)

## Quick start (no MySQL needed) — recommended first run
The project ships with an H2 file-database profile so you can run and test
everything today, then switch to MySQL later without changing any code.

```
mvn clean package
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Open http://localhost:8080 in your browser. Data is stored in `./data/`
(created automatically) and survives restarts. You can inspect the tables at
http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./data/healthsurveydb`,
user `sa`, blank password).

## Full setup with MySQL (switch to this once MySQL is installed)
1. In MySQL, either let the app create the schema (default), or create it yourself:
   ```sql
   CREATE DATABASE health_survey_db;
   ```
2. Edit `src/main/resources/application.properties` and set your local MySQL
   `spring.datasource.username` / `spring.datasource.password`.
3. Build:
   ```
   mvn clean package
   ```
4. Run (no `-Dspring-boot.run.profiles=dev` this time, so it uses the default
   MySQL-backed `application.properties`):
   ```
   mvn spring-boot:run
   ```
5. Open http://localhost:8080 — you'll be redirected to `/login`.

On first run, three demo users are seeded automatically (see console log),
one per role, all with password `password123`:

| Username    | Role          |
|-------------|---------------|
| surveyor1   | SURVEYOR      |
| admin1      | ADMINISTRATOR |
| officer1    | HEALTH_OFFICER|

These are placeholder credentials for local demo/testing only — not for
production use (see Week 1 constraint on dummy/anonymized data only).

## What's implemented (maps to Week 3 functional requirements)
| Route | Purpose | Requirement |
|---|---|---|
| `GET/POST /login`, `GET /logout` | Role-based login | FR-5 / US-5 |
| `GET /dashboard` | Summary counts by status | FR-4 / US-4 |
| `GET /surveys?query=` | List / search records | FR-2 / US-2 |
| `GET /surveys/new`, `POST /surveys` | Create record (starts DRAFT) | FR-1 / US-1 |
| `GET /surveys/{id}` | View record | FR-1 |
| `GET /surveys/{id}/edit`, `POST /surveys/{id}` | Update record details | FR-6 / PB-17 |
| `POST /surveys/{id}/status` | Change status (Administrator only, forward-only) | FR-3 / US-3 |

**Note on the Week 3 API list:** the Week 3 document also specced a JSON
`/api/**` surface. This skeleton implements the same functionality as
server-rendered Thymeleaf pages instead, because plain HTML pages are the
simplest target for the Selenium WebDriver journeys planned in Week 9. A thin
`/api/**` JSON layer can be added on top of the existing service layer later
without changing the architecture.

## Auth note
Login uses a lightweight session-based guard (`SessionAuthInterceptor`) with
BCrypt-hashed passwords, not the full Spring Security framework. Production-
grade identity management (OAuth2/SSO) is explicitly out of scope for this
MVP (Week 1, Section 7.2).

## Tests
```
mvn test
```
Includes the DRAFT → SUBMITTED → VERIFIED → CLOSED transition rules
(`SurveyStatusTest`) and Week 9 Selenium journeys under
`src/test/java/com/chss/selenium/`. For Selenium tests, start the application
separately at `http://localhost:8081` first; Maven tests do not launch a second
application instance. Selenium Manager resolves ChromeDriver. See
[`WEEK9_TEST_PLAN.md`](WEEK9_TEST_PLAN.md) for prerequisites and execution details.

## Week 12: Jenkins-Docker continuous deployment

The Jenkins pipeline checks out the selected `GIT_REVISION`, builds the Maven
project, runs the Selenium suite, and publishes JUnit reports before any Docker
stage can run. A successful test gate builds the existing Dockerfile and tags
the image as `<Docker Hub username>/chss-app:build-${BUILD_NUMBER}`. Jenkins
Credentials (`dockerhub-credentials`, username/password type, with a Docker Hub
Personal Access Token as the password) authenticates the push without storing
the token in source control. The pipeline verifies the pushed registry manifest,
replaces only the CHSS `chss-container` on host port 18082, checks container
metadata/log startup, and health-checks `http://localhost:18082/login`.

The Docker deployment uses an isolated in-memory H2 database for lifecycle
verification. MySQL connectivity from the container is not claimed by this
pipeline run; supply database settings securely if a MySQL-backed deployment is
required.

## Next steps (per the sprint plan)
- Week 4: initialize the Git/GitHub repository, branch policy, issue templates.
- Weeks 5–6: finish remaining MVP polish and Git collaboration workflow.
- Weeks 7–10: Jenkins CI, pipeline-as-code, Selenium continuous testing.
- Weeks 11–14: Dockerize, Jenkins CD, Ansible provisioning.
- Week 15: final release and documentation.
