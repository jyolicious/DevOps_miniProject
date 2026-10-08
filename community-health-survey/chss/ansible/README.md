# Week 13 — CHSS configuration management with Ansible

## Target and scope

The intended managed node is the **Linux Docker deployment host** used by the Week 12 deployment. Inspection on 8 October 2026 found a Windows development machine, WSL2 listing only a stopped internal `docker-desktop` distribution, no Ubuntu distribution, and a Docker CLI that could not connect to the stopped Docker Engine. There was no real, usable Linux CHSS target for this first run. The inventory is therefore an intentionally empty template; it does not invent a host. Add the actual Debian/Ubuntu deployment host to `inventory.ini` before applying the playbook.

The Week 12 Docker image already contains the Java 17 runtime and runs the application as its own unprivileged `chss` account. The host needs Docker, not a second Java installation, a host-side CHSS account, Maven, or a separate application service. The build pipeline uses Maven in its build environment. Maven Wrapper files are in the repository; they do not make Maven a deployment-host prerequisite.

## Configuration specification

| Requirement | Target state | Ansible resource | Verification |
|---|---|---|---|
| OS | Debian-family Linux (Ubuntu supported) | `assert` using gathered facts | Play fails explicitly on another OS family |
| Python | Python 3 available to Ansible modules | `apt` package `python3` (Python must be present to begin module execution) | Ansible fact gathering and module execution |
| Docker | Docker Engine installed, enabled at boot, and running | `apt` package `docker.io`; `service` state `started`, enabled | `docker version --format` plus assertion |
| Utilities | CA certificates available for trusted package/registry TLS; no extra HTTP utility is needed | `apt` package `ca-certificates` | Package manager reports installed state |
| Java | Java 17 JRE is in the CHSS runtime image | No host package; Dockerfile runtime `eclipse-temurin:17-jre-jammy` | Week 12 image inspection/runtime evidence; not duplicated on host |
| Maven | Build-time requirement only; project includes `mvnw` and `mvnw.cmd` | No host resource | `pom.xml` specifies Java 17; Docker build stage uses Maven 3.9.9 and Temurin 17 |
| Host users/groups | No extra host application user or group. Container uses system `chss`; Ansible connects as the named inventory user and escalates only for provisioning. | Inventory `ansible_user`; play `become: true` | SSH connection and sudo required on the real host |
| Directories | No host application/config/log/data directories are required by the current Docker deployment. `/app` belongs to the image. | None | Image Dockerfile and deployment script review |
| Files | No host app configuration or secret file is created. Runtime DB settings are supplied by deployment; never commit credentials. | None | Review task list; no templates/copy tasks |
| CHSS app port | Container listens on TCP 8081. | Not opened on the host directly | Dockerfile `EXPOSE 8081`; application `server.port` default 8081 |
| Deployment host port | TCP 18082 maps to container 8081 in Week 12. This is the only CHSS host port relevant to this deployment. | No firewall rule; deployment owns its published mapping | `deploy-docker.ps1` and `verify-docker-health.ps1`; optional HTTP check `/login` |
| Jenkins and Tomcat ports | 8082 and 8083 belong to separate Windows Jenkins/Tomcat services and are not configured on the Linux Docker host. | None | Jenkinsfile and project documentation |
| Database | No MySQL service on the target: Week 12 uses in-memory H2. | None | Week 11/12 deployment documentation; runtime DB settings remain external |
| Application service | No host systemd CHSS service; Jenkins starts the Docker container. | None | Week 12 Docker deployment script |

## Prerequisites

- Ansible controller on a supported Linux/Unix environment (or another supported control node) with `ansible-core` and the built-in `ansible.builtin` modules.
- A reachable Debian/Ubuntu Linux deployment host with SSH, Python 3 available for Ansible module execution, and an account that can use sudo. Docker Engine should be installable from the host's configured package repositories.
- For endpoint verification, a deployed CHSS container listening at `http://127.0.0.1:18082/login`.

The playbook uses `become` for package and service administration. It does not create a broad Docker group membership or add host users. Docker access is root-equivalent; grant it using the deployment platform's existing access policy.

## Inventory and execution

Edit `inventory.ini` and uncomment/update the example with the actual host and SSH user. Do not leave the documentation-only example address in place.

```ini
[chss_servers]
chss-deploy ansible_host=<real-linux-host> ansible_user=<authorized-user>
```

Check connectivity, then configure:

```sh
ansible-inventory -i inventory.ini --list
ansible chss_servers -i inventory.ini -m ansible.builtin.ping
ansible-playbook -i inventory.ini site.yml
```

If the app is already deployed and should be checked, pass `-e chss_verify_deployed_app=true`. A missing app is expected during host provisioning, so endpoint verification is off by default. The playbook checks Docker and installs only required host packages; it does not deploy CHSS or change firewall rules.

## Verification and idempotency

Ansible reports installed packages and the enabled/running Docker service; the playbook separately checks that the Docker Engine responds. The optional URI task requires HTTP 200 from `/login`. Port 8081 is internal to the container, and 18082 is the Docker-published host port. Ports 8082 and 8083 belong to other services and are not touched. No port is opened by this playbook.

APT package state and service state use idempotent Ansible modules. On a configured, unchanged host, a second run should report `changed=0`; the read-only version check is marked `changed_when: false`. The Docker daemon may itself report transient state outside Ansible's change accounting.

## First execution record

See [`execution-log.txt`](execution-log.txt). It records the actual environment inspection and Ansible invocation attempt. The playbook could not be applied because neither Ansible nor a usable Linux target was available; the log reports that limitation and does not claim successful configuration, endpoint verification, or an idempotency run.
