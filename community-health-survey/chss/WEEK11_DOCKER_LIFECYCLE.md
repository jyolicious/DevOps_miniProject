# Week 11 - Docker Image and Container Lifecycle

**Project:** Community Health Survey System (CHSS)
**Date:** 7 October 2026
**Docker Engine:** 29.7.2, Linux containers (Docker Desktop)

## Runtime design

The Maven project uses `packaging=war` and the Spring Boot Maven plugin. That plugin repackages the WAR as an executable Spring Boot archive, so the image runs the existing `community-health-survey.war` with `java -jar`. The Dockerfile uses a Maven/Java 17 build stage and a Java 17 JRE runtime stage. The runtime uses an unprivileged `chss` account.

The existing `application.properties` already reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `APP_PORT`. No database configuration or credentials were added to the image. `.dockerignore` excludes the local `application-dev.properties`, which is not needed for the default profile and should not be copied into an image.

For native Windows execution with MySQL on the same machine, `localhost:3306` is the normal host address. In a Docker Desktop container, `localhost` refers to the container, so set `DB_URL` to use `host.docker.internal`, for example:

```text
jdbc:mysql://host.docker.internal:3306/health_survey_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

For an actual MySQL run, provide the username and password at runtime, for example through a protected environment file. Do not place a real password in the Dockerfile, this report, or source control. No MySQL container was created.

## Build and image details

Working directory for Docker commands:

```text
community-health-survey/chss
```

### A. Build the image

```powershell
docker build --tag chss-app:week11 .
```

**Result:** `BUILD SUCCESS`; the multi-stage build compiled the application, skipped test execution, created the executable WAR, and created image `chss-app:week11`.

### B. Add a lifecycle tag

```powershell
docker tag chss-app:week11 chss-app:week11-lifecycle
```

**Result:** both tags refer to the same image.

### C. List the CHSS images

```powershell
docker images --format '{{.Repository}}:{{.Tag}}|{{.ID}}|{{.CreatedAt}}|{{.Size}}' chss-app
```

Observed image information:

| Field | Value |
|---|---|
| Repository | `chss-app` |
| Tags | `week11`, `week11-lifecycle` |
| Image ID | `sha256:95cd872e317b750fbef05b4ebd59f89d592ad5a76c75cce06babd4d3967346a4` |
| Created | `2026-10-07T15:43:31.394814619Z` (Docker reports UTC; approximately 21:13 IST) |
| Docker image list size | `474MB` disk usage |
| Image content size | `140,799,316` bytes (approximately 141MB), reported by inspect / image-tree output |

Docker's image-tree output distinguished 474MB local disk usage from 141MB image content size.

## Container lifecycle command log

The runtime environment did not have `DB_URL`, `DB_USERNAME`, or `DB_PASSWORD` set. To verify the application container without guessing MySQL credentials or writing to the existing database, the lifecycle run used an in-memory H2 database through the same environment-variable interface. This demonstrates the image, port, HTTP, and container lifecycle; it does **not** claim a live MySQL connection was tested.

The host port `18081` was selected to avoid the project's usual 8081 application port, Jenkins on 8082, and Tomcat on 8083.

### D. Run the container and map ports

```powershell
docker run --detach --name chss-container --publish 18081:8081 --env APP_PORT=8081 --env 'DB_URL=jdbc:h2:mem:week11_demo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE' --env DB_USERNAME=sa --env DB_PASSWORD= chss-app:week11
```

**Result:** container ID prefix `2abbd2d84043`.

### E. List the running container

```powershell
docker ps --filter name=chss-container --format 'ID={{.ID}}|IMAGE={{.Image}}|PORTS={{.Ports}}|STATUS={{.Status}}|NAMES={{.Names}}'
```

Observed running-container evidence:

```text
ID=2abbd2d84043|IMAGE=chss-app:week11|PORTS=0.0.0.0:18081->8081/tcp, [::]:18081->8081/tcp|STATUS=Up|NAMES=chss-container
```

### F. Inspect the container

```powershell
docker inspect --format 'ID={{.Id}}|Image={{.Config.Image}}|PortBindings={{json .HostConfig.PortBindings}}|Status={{.State.Status}}|Name={{.Name}}' chss-container
```

Observed values: image `chss-app:week11`; container port `8081/tcp` bound to host port `18081`; state `running`; name `/chss-container`.

### G. View application logs

```powershell
docker logs --tail 80 chss-container
```

**Result:** Spring Boot started on Java 17; embedded Tomcat listened on port 8081; Hikari connected to the in-memory H2 URL; the application logged `Started CommunityHealthSurveyApplication`. The database log output contained no MySQL credentials.

### H. Test the mapped application port

```powershell
Invoke-WebRequest -Uri 'http://localhost:18081/login' -UseBasicParsing -TimeoutSec 15
```

**Result:** HTTP `200`; content type `text/html;charset=UTF-8`; the returned page contained the login form.

### I. Stop the container

```powershell
docker stop chss-container
```

**Result:** Docker returned `chss-container`; `docker ps -a` showed it exited with status 143.

### J. Restart the same container

```powershell
docker start chss-container
```

**Result:** Docker returned `chss-container`; the original container ID was retained.

### K. Verify it is running after restart

The first immediate HTTP request ran before Spring completed startup. A bounded readiness poll then verified the service:

```powershell
$ready = $false
for ($i = 0; $i -lt 45; $i++) {
    try {
        $response = Invoke-WebRequest -Uri 'http://localhost:18081/login' -UseBasicParsing -TimeoutSec 3
        if ([int]$response.StatusCode -eq 200) { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw 'CHSS did not become HTTP-ready after restart' }
```

**Result:** HTTP `200`; `docker ps` showed the same container up with `0.0.0.0:18081->8081/tcp`.

### L. Stop the container after the restart check

```powershell
docker stop chss-container
```

**Result:** Docker returned `chss-container`.

### M. Remove the stopped container

```powershell
docker rm chss-container
```

**Result:** Docker returned `chss-container`. Only this Week 11 CHSS container was removed; the image and both CHSS tags were kept.

### N. Verify removal

```powershell
docker ps -a --filter name=chss-container --format '{{.ID}}|{{.Names}}|{{.Status}}'
```

**Result:** no rows returned. The image remains available under `chss-app:week11` and `chss-app:week11-lifecycle`.

## Validation notes

- The Docker build succeeded and verified the executable WAR exists before copying it to the runtime stage.
- The runtime is non-root and exposes container port 8081.
- HTTP returned 200 before and after stop/start.
- The named container was removed successfully; no unrelated container or image was removed.
- The local `mvnw.cmd` wrapper failed in this workstation's PowerShell with `icm: Cannot index into a null array`. The Docker build's Maven 3.9.9 / Java 17 builder completed the packaging successfully, so no source or wrapper workaround was needed.
- Docker Desktop's Linux engine was initially stopped. Starting Docker Desktop restored the engine; the image build and lifecycle then completed.
- A live MySQL connection still requires valid runtime `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` values. Supply those securely when running against MySQL; do not commit them.
