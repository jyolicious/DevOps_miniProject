# Week 13 — CHSS configuration management with Ansible

## Architecture and target

The managed node is **Ubuntu 26.04.1 LTS on WSL2**, installed locally on the Windows development machine. Ubuntu has systemd as PID 1 and runs Ansible against itself through a local connection. This is a real Linux distribution, not a container or the internal `docker-desktop` distro. Ansible Core 2.20.1 is installed in Ubuntu. The Ansible run was invoked as root because this local node needs package and system-service configuration; the play itself does not use redundant `become`.

The Ubuntu node has its own Docker Engine (`docker.io` package, version 29.1.3). Docker Desktop's separate Engine (29.7.2) and its existing containers were not changed. The Week 12 CHSS image already contains the Java 17 runtime and runs as its image-level `chss` user. This play configures the Docker host prerequisites; it does not deploy an application container.

## Configuration specification

| Requirement | Target state | Ansible resource | Verification |
|---|---|---|---|
| OS | Debian-family Linux; actual target Ubuntu 26.04.1 LTS | `assert` on `ansible_facts.os_family` | Passes during both runs |
| Controller/module runtime | Ansible Core 2.20.1; Python 3.14.4 | Installed in Ubuntu before playbook; `python3` package state is enforced | `ansible --version`; fact gathering and modules |
| Host packages | `ca-certificates`, `docker.io`, `python3` present | `ansible.builtin.apt`, `state: present` | First run installed missing packages; second run no changes |
| Java | Java 17 JRE in CHSS image (`eclipse-temurin:17-jre-jammy`) | No host install | Existing Dockerfile and Week 11/12 runtime evidence; host is Docker-only |
| Maven | Maven 3.9.9 in image build stage; repo includes Maven Wrapper scripts | No host install | Dockerfile build stage and `pom.xml` Java 17 property |
| Users/groups | No host app user is needed. The application runs as `chss` inside the image. Local Ansible runs as root for host provisioning. | No user module: creating a duplicate account is unnecessary | `id` on target; Dockerfile defines the image user |
| Directories | No host app/config/log/data directory is required by the current Docker deployment | None | Playbook has no directory task; `/app` belongs to the image |
| Files | No host application file or secret-bearing config is required | None | Playbook has no file/template task; no credentials are stored |
| Container port | TCP 8081 is the CHSS application port inside its container | Not opened on host by this play | Dockerfile `EXPOSE 8081`; application default `server.port=8081` |
| Deployment host port | Week 12 maps host TCP 18082 to container 8081 | Not opened or bound by this host-prerequisite play | Existing deployment script; app is not deployed on this WSL engine |
| Other ports | Jenkins 8082 and Tomcat 8083 belong to separate Windows services | Not managed | Jenkinsfile and deployment docs |
| Database | Week 12 uses in-memory H2; no MySQL server/client is required on this target | None | Docker deployment configuration; no MySQL service installed |
| Services | Ubuntu systemd `docker.service` enabled and active | `ansible.builtin.service`, started/enabled | First and second execution; `systemctl` and Docker CLI verification |
| CHSS service | No host systemd CHSS service; the existing CD flow runs a container | None | Week 12 deployment script; no app container created by this play |

## Inventory

`inventory.ini` defines `localhost` in the `chss_servers` group and uses Ansible's `local` connection with `/usr/bin/python3`. This avoids SSH setup for a same-node WSL target. The host inventory contains no guessed address or credentials.

## Playbook

`site.yml` gathers facts and asserts a Debian-family OS, installs the three host packages with APT, ensures Docker is enabled and running using the service module, and uses a read-only Docker version command plus an assertion to verify the Engine. An optional URI task checks `/login` only when `chss_verify_deployed_app=true`; it defaults off because no CHSS app was deployed on this isolated WSL Engine. There are no handlers because no configuration file is changed and service state is set directly. The app deployment, firewall, MySQL, host users, directories, and secret files are outside this target's requirements.

## Prerequisites and execution

The WSL distro has systemd enabled, Python 3, and APT. Ansible Core was installed from Ubuntu's package repositories. From PowerShell, the commands used were:

```powershell
wsl --install --distribution Ubuntu --no-launch
wsl -d Ubuntu -u root -- apt-get update
wsl -d Ubuntu -u root -- apt-get install -y ansible-core
wsl -d Ubuntu -u root -- bash -lc 'cd /mnt/c/Users/Jyotsna/OneDrive/Desktop/DevOps/MiniProject/community-health-survey/chss/ansible && ansible-inventory -i inventory.ini --list'
wsl -d Ubuntu -u root -- bash -lc 'cd /mnt/c/Users/Jyotsna/OneDrive/Desktop/DevOps/MiniProject/community-health-survey/chss/ansible && ansible-playbook -i inventory.ini site.yml --syntax-check'
wsl -d Ubuntu -u root -- bash -lc 'cd /mnt/c/Users/Jyotsna/OneDrive/Desktop/DevOps/MiniProject/community-health-survey/chss/ansible && ansible-playbook -i inventory.ini site.yml'
```

The first actual playbook run reported `changed=1`, `failed=0`, `unreachable=0`. After correcting an Ansible fact deprecation warning, the second run reported `changed=0`, `failed=0`, `unreachable=0`, demonstrating idempotency. During the continuation audit, inventory graph and syntax validation passed again, and two additional real playbook runs each reported `changed=0`, `failed=0`, `unreachable=0`. The target remained Ubuntu 26.04.1 LTS on WSL2, with its Docker service active/enabled and its own Docker Engine responding. Full Ansible output and environment setup evidence are in [`execution-log.txt`](execution-log.txt).

## Limits of this run

This verifies a local Linux Docker host, package state, Docker service state, and Docker Engine response. It does not deploy CHSS, test a MySQL connection, or check the HTTP application endpoint because no CHSS container was started on this separate WSL Engine. Ports 8081/18082 and app runtime Java 17 are documented from the existing project configuration. The Windows Jenkins service was not run or changed.

## Week 14 — automated deployment, idempotency, and recovery

### Architecture and target

Week 14 uses a dedicated container on the **Ubuntu 26.04.1 LTS WSL2 distribution**, managed by Ansible Core 2.20.1 over a local connection. The Ubuntu distro runs its own Docker Engine 29.1.3; it is separate from Docker Desktop's `docker-desktop` distro and Engine. The isolated container is named `chss-week14`, with host TCP 18084 mapped to container TCP 8081. Windows could reach the app at the Ubuntu WSL2 address; `localhost:18084` on Windows was not the verified route.

Docker Hub discovery found one active published CHSS tag: `jyotsnakasi/chss-app:build-20`, manifest digest `sha256:36a23bd53295ec2f1f0168bcc579aca7bc4b895b340985a9340991421face45e`. No newer published CHSS tag was available. The alternate candidate was built locally from committed Git revision `d7560b59fc5df0eaa1331936c9b7a236b7f48abc` and tagged `local/chss-app:git-d7560b59fc5d`; it was not pushed or represented as a newer Docker Hub release.

### Configuration specification

| Area | Week 14 target state | Notes |
|---|---|---|
| Host packages | `ca-certificates`, `docker.io`, `docker-buildx`, `python3`, `python3-docker` | Buildx is included because this workflow builds a local candidate; it is not a CHSS runtime dependency |
| Java / Maven | Java 17 runtime and Maven 3.9.9 build stage in the image | No host Java or Maven installation is required |
| Users | No additional host app user; the image runs as its unprivileged `chss` account | The existing Dockerfile defines this account |
| Directories | No host CHSS data/config/log directories or bind mounts | The Week 14 app uses in-memory H2 |
| Files | No host application files or secret-bearing configuration files | Runtime values are non-secret environment settings |
| Ports | Host TCP 18084 -> container TCP 8081 | No MySQL 3306 listener is required |
| Services | Ubuntu `docker.service` enabled and started; CHSS runs as a container | No host CHSS systemd or MySQL service is created |

### Inventory and playbook

- [`week14_inventory.ini`](week14_inventory.ini) targets `localhost` using Ansible's local connection and `/usr/bin/python3`.
- [`week14_provision.yml`](week14_provision.yml) asserts a Debian-family target; refuses to replace a same-named container unless it has the Week 14 management label; installs host packages; ensures Docker is enabled/running; declares the image, ports, restart policy, H2 settings and release labels with `community.docker.docker_container`; and waits for `/login` to return HTTP 200.
- The playbook defaults to the published stable `build-20` image. `chss_image` can be overridden for candidate deployment and rollback. No application password or token is committed.

### Commands

From PowerShell, with Ubuntu WSL2 available:

```powershell
wsl -d Ubuntu -u root --cd /mnt/c/Users/Jyotsna/OneDrive/Desktop/DevOps/MiniProject/community-health-survey/chss/ansible --exec ansible-inventory -i week14_inventory.ini --graph
wsl -d Ubuntu -u root --cd /mnt/c/Users/Jyotsna/OneDrive/Desktop/DevOps/MiniProject/community-health-survey/chss/ansible --exec ansible-playbook -i week14_inventory.ini week14_provision.yml --syntax-check
wsl -d Ubuntu -u root --cd /mnt/c/Users/Jyotsna/OneDrive/Desktop/DevOps/MiniProject/community-health-survey/chss/ansible --exec ansible-playbook -i week14_inventory.ini week14_provision.yml
wsl -d Ubuntu -u root --cd /mnt/c/Users/Jyotsna/OneDrive/Desktop/DevOps/MiniProject/community-health-survey/chss/ansible --exec ansible-playbook -i week14_inventory.ini week14_provision.yml --extra-vars "chss_image=local/chss-app:git-d7560b59fc5d"
wsl -d Ubuntu -u root --cd /mnt/c/Users/Jyotsna/OneDrive/Desktop/DevOps/MiniProject/community-health-survey/chss/ansible --exec ansible-playbook -i week14_inventory.ini week14_provision.yml --extra-vars "chss_image=jyotsnakasi/chss-app:build-20"
```

The alternate tag must first be built locally with Docker Buildx from the same clean committed source revision and full Git revision label used in the execution log. The rollback command selects the published `build-20` image again.

### Actual execution results

- Inventory graph and Ansible syntax validation passed.
- First deployment of `build-20` reported `changed=1`, `failed=0`, `unreachable=0`; the playbook's startup wait received HTTP 200 after three startup retries.
- A repeat run of the initial playbook reported `changed=0`. After the build workflow exposed the missing Docker Buildx plugin, `docker-buildx` was added to the managed package set. The Ansible update reported `changed=1`, and the following run reported `changed=0`.
- Docker Buildx 0.30.1 built the local candidate from committed revision `d7560b59fc5df0eaa1331936c9b7a236b7f48abc`, with image ID `sha256:fe893169cd4b7e31dd212725b0af72f105ea1cac81711b9580a002d74c6db72e`. The candidate deployment reported `changed=1`; its container was running and `/login` returned HTTP 200.
- Rollback to published `build-20` reported `changed=1`, `failed=0`, `unreachable=0`. Final recovery inspection showed the stable image ID `sha256:36a23bd53295ec2f1f0168bcc579aca7bc4b895b340985a9340991421face45e`, a running `chss-week14` container, filtered Java/Tomcat startup milestones, and HTTP 200. The browser page was also reloaded successfully after rollback.
- The Dockerfile uses `-DskipTests`; this workflow validates image build, deployment and HTTP health but does not claim a Maven or Selenium test-suite pass.

### Problems and limitations

The first candidate-build attempt exposed two real issues: Docker Buildx was absent, and the capture helper treated the Docker CLI's legacy-builder warning as a terminating PowerShell error. After adding Ubuntu's `docker-buildx` package to the Ansible-managed prerequisites and preserving native stderr while checking command exit codes, a later inspection template also needed correction. Each failed attempt and its resolution is recorded in the execution log; the stable deployment was retained until the candidate image was successfully built and verified.

The Ubuntu Docker service received graceful stop signals during early short-lived WSL command sessions, interrupting the container and causing connection resets. Journal inspection found no OOM kill or application crash. The final lifecycle checks were run while keeping the Ubuntu WSL target available. WSL2 is a development target, not a separate production host. The candidate is local only, and Docker Desktop plus unrelated Windows containers were not changed.

The complete real command transcript is [`week14-execution-log.txt`](week14-execution-log.txt). The report and genuine screenshots are in [`../../../documentations/Week14_Automation_Reliability.pdf`](../../../documentations/Week14_Automation_Reliability.pdf) and [`../../../documentations/week14-evidence/`](../../../documentations/week14-evidence/).
