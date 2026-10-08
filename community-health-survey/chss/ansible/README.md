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
