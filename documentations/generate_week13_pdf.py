from pathlib import Path
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
)


OUT = Path(__file__).with_name("Week13_Configuration_Management.pdf")
PAGE_W, PAGE_H = A4
INK = colors.HexColor("#182230")
BLUE = colors.HexColor("#155EEF")
PALE = colors.HexColor("#EFF4FF")
MUTED = colors.HexColor("#526071")
GRID = colors.HexColor("#D0D5DD")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="CoverTitle", parent=styles["Title"], fontName="Helvetica-Bold", fontSize=29, leading=35, textColor=INK, alignment=TA_LEFT, spaceAfter=10))
styles.add(ParagraphStyle(name="CoverSub", parent=styles["Normal"], fontSize=14, leading=21, textColor=MUTED))
styles.add(ParagraphStyle(name="H1x", parent=styles["Heading1"], fontName="Helvetica-Bold", fontSize=20, leading=25, textColor=INK, spaceBefore=4, spaceAfter=12))
styles.add(ParagraphStyle(name="H2x", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=12, leading=16, textColor=BLUE, spaceBefore=9, spaceAfter=5))
styles.add(ParagraphStyle(name="Bodyx", parent=styles["BodyText"], fontSize=9.5, leading=14, textColor=INK, spaceAfter=7))
styles.add(ParagraphStyle(name="Smallx", parent=styles["BodyText"], fontSize=8, leading=11, textColor=MUTED))
styles.add(ParagraphStyle(name="Cellx", parent=styles["BodyText"], fontSize=7.2, leading=9, textColor=INK))
styles.add(ParagraphStyle(name="CellHeadx", parent=styles["BodyText"], fontName="Helvetica-Bold", fontSize=7.3, leading=9, textColor=colors.white))
styles.add(ParagraphStyle(name="Codex", fontName="Courier", fontSize=7.3, leading=9.3, backColor=colors.HexColor("#F2F4F7"), borderColor=GRID, borderWidth=0.5, borderPadding=7, textColor=INK, splitLongWords=1))
styles.add(ParagraphStyle(name="Callout", parent=styles["BodyText"], fontSize=10, leading=15, textColor=INK, backColor=PALE, borderColor=BLUE, borderWidth=0.7, borderPadding=9, spaceBefore=6, spaceAfter=9))


def para(value, style="Bodyx"):
    return Paragraph(value, styles[style])


def code(value):
    return Preformatted(value.strip("\n"), styles["Codex"], maxLineLength=108)


def on_page(canvas, doc):
    canvas.saveState()
    if doc.page > 1:
        canvas.setStrokeColor(GRID)
        canvas.line(18 * mm, PAGE_H - 15 * mm, PAGE_W - 18 * mm, PAGE_H - 15 * mm)
        canvas.setFont("Helvetica", 8)
        canvas.setFillColor(MUTED)
        canvas.drawString(18 * mm, PAGE_H - 11 * mm, "CHSS | WEEK 13 CONFIGURATION MANAGEMENT")
        canvas.drawRightString(PAGE_W - 18 * mm, 11 * mm, f"Page {doc.page}")
    canvas.restoreState()


doc = BaseDocTemplate(str(OUT), pagesize=A4, rightMargin=18 * mm, leftMargin=18 * mm, topMargin=23 * mm, bottomMargin=18 * mm, title="Week 13 - Configuration Management", author="Community Health Survey System")
frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="normal")
doc.addPageTemplates(PageTemplate(id="main", frames=frame, onPage=on_page))
story = []

# Cover
story += [Spacer(1, 27 * mm), para("DEVOPS PROJECT / WEEK 13", "H2x"), Spacer(1, 5 * mm), para("Configuration<br/>Management", "CoverTitle"), para("Community Health Survey System", "CoverSub"), Spacer(1, 18 * mm)]
cover_table = Table([[para("ANSIBLE", "H2x"), para("Ubuntu 26.04.1 LTS on WSL2", "Bodyx")], [para("TARGET", "H2x"), para("Local Linux node, managed with Ansible's local connection", "Bodyx")], [para("RESULT", "H2x"), para("Successful first run; second run changed zero resources", "Bodyx")]], colWidths=[33 * mm, 120 * mm])
cover_table.setStyle(TableStyle([("BACKGROUND", (0, 0), (0, -1), PALE), ("BOX", (0, 0), (-1, -1), 0.7, GRID), ("INNERGRID", (0, 0), (-1, -1), 0.4, GRID), ("VALIGN", (0, 0), (-1, -1), "MIDDLE"), ("LEFTPADDING", (0, 0), (-1, -1), 10), ("RIGHTPADDING", (0, 0), (-1, -1), 8), ("TOPPADDING", (0, 0), (-1, -1), 10), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
story += [cover_table, Spacer(1, 22 * mm), para("Evidence date: 8 October 2026 (Asia/Calcutta). This report records the actual Ansible executions and target verification. No Jenkins job was run.", "Smallx"), PageBreak()]

# Objective, approach, target
story += [para("1. Objective and approach", "H1x"), para("Identify CHSS deployment-host prerequisites and express the correct target state as a repeatable Ansible playbook. The project deploys CHSS as a Docker image. The host therefore needs Docker Engine; Java and Maven belong to the container build/runtime images, not the host installation.")]
story += [para("Target-node environment", "H2x"), para("A usable Ubuntu WSL distribution was not present initially. WSL Ubuntu was installed non-interactively with <font name='Courier'>wsl --install --distribution Ubuntu --no-launch</font>. Ubuntu 26.04.1 LTS launched as a real WSL2 distribution with systemd as PID 1, Python 3, APT, and systemctl. Ansible Core 2.20.1 was installed from Ubuntu repositories. Ansible targets the same distro as localhost using a local connection; it is not a Docker container or Docker Desktop's internal distro.")]
story += [para("The Ubuntu WSL target has its own Docker Engine 29.1.3. Docker Desktop's separate Engine 29.7.2 and pre-existing containers were left untouched. The playbook provisions host prerequisites only; it does not deploy the application.", "Callout")]
story += [para("2. Configuration specification", "H1x")]

rows = [
    ("Requirement", "Target state", "Ansible resource / verification"),
    ("OS and Ansible", "Ubuntu 26.04.1 LTS; Ansible Core 2.20.1; Python 3", "OS-family assert; fact gathering; syntax and inventory checks passed"),
    ("Packages", "ca-certificates, docker.io, python3 installed", "APT state=present; first run changed packages, second run unchanged"),
    ("Java / Maven", "Java 17 JRE in image; Maven 3.9.9 in build stage", "No host install; Dockerfile and pom.xml are source of truth"),
    ("Users", "No host app user; container runs as image user chss", "No user task: duplicate host identity is unnecessary"),
    ("Directories / files", "No host app/config/log/data files required", "No file/directory tasks; no credentials created"),
    ("Ports", "8081 container app; 18082 deployment host map", "No host firewall or app deployment in this play"),
    ("Other ports", "8082 Jenkins, 8083 Tomcat are separate Windows services", "Not configured on Ubuntu target"),
    ("Database", "Week 12 Docker run uses in-memory H2", "No MySQL server/client or credentials on target"),
    ("Services", "Docker enabled and active under systemd", "service module; checked systemctl active/enabled and Docker CLI"),
]
table_data = [[para(escape(c), "CellHeadx") for c in rows[0]]] + [[para(escape(c), "Cellx") for c in row] for row in rows[1:]]
spec = Table(table_data, colWidths=[28 * mm, 66 * mm, 70 * mm], repeatRows=1, hAlign="LEFT")
spec.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, 0), INK), ("GRID", (0, 0), (-1, -1), 0.4, GRID), ("VALIGN", (0, 0), (-1, -1), "TOP"), ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F8FAFC")]), ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5), ("TOPPADDING", (0, 0), (-1, -1), 5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
story += [spec, PageBreak()]

# Detail
story += [para("3. Packages and runtime boundary", "H1x"), para("Ubuntu host packages managed by the playbook are ca-certificates, docker.io, and python3. Python 3 was present before Ansible could run and remains declared to make the target package state explicit. The actual package versions after execution were docker.io 29.1.3-0ubuntu4.1, ca-certificates 20260601~26.04.1, and python3 3.14.3-0ubuntu2.")]
story += [para("The CHSS Dockerfile uses Maven 3.9.9 with Temurin 17 to build the executable WAR, then Temurin 17 JRE to run it. No Java or Maven host install is required for this Docker deployment. Week 12 uses in-memory H2, so MySQL is not a prerequisite for this target.")]
story += [para("4. Users, directories, and files", "H1x"), para("Ansible ran as root on localhost because package and service state require administrative access. The playbook does not create a second host account. The Docker image already defines an unprivileged system account named chss and owns /app inside the container. No host-side CHSS application, data, log, or configuration directories are used by the existing container deployment. The playbook creates no files and embeds no credentials.")]
story += [para("5. Ports and services", "H1x"), para("TCP 8081 is the application's container port. Week 12 maps host TCP 18082 to container 8081. Jenkins uses 8082 and Tomcat uses 8083 on separate Windows services. This host-prerequisite play opens no ports and does not start CHSS or MySQL. It manages only Ubuntu's docker.service.")]
story += [para("6. Inventory", "H1x"), code("[chss_servers]\nlocalhost ansible_connection=local ansible_python_interpreter=/usr/bin/python3")]
story += [para("The Ansible inventory parser resolved localhost in the chss_servers group. No SSH endpoint, placeholder address, or secret is required.")]
story += [para("7. Playbook behavior", "H1x"), para("The play gathers facts and asserts the Debian family, installs the package list with ansible.builtin.apt, ensures docker.service is enabled and started with ansible.builtin.service, then checks the Engine using a read-only Docker version command and assertion. An optional URI task checks /login only if chss_verify_deployed_app=true; it is off by default because this playbook does not deploy the app. No handlers are needed because no file configuration changes trigger a service restart.")]
story += [PageBreak()]

# Execution evidence
story += [para("8. Real execution evidence", "H1x"), para("Environment setup and validation were performed in Ubuntu WSL. The Ansible output below is transcribed from the actual command responses. The first run emitted a facts deprecation warning; the playbook was corrected to use ansible_facts.os_family before the second run.")]
story += [para("First successful execution", "H2x"), code("PLAY [Configure prerequisites on the CHSS Docker deployment host] **************\n\nTASK [Gathering Facts] *********************************************************\nok: [localhost]\n\nTASK [Require a Debian-family Linux target] ************************************\n[DEPRECATION WARNING]: INJECT_FACTS_AS_VARS default to `True` is deprecated.\nUse `ansible_facts[\"fact_name\"]` (no `ansible_` prefix) instead.\nok: [localhost] => {\"changed\": false, \"msg\": \"All assertions passed\"}\n\nTASK [Install Docker and host verification prerequisites] **********************\nchanged: [localhost]\n\nTASK [Enable and start Docker Engine] ******************************************\nok: [localhost]\n\nTASK [Read Docker Engine version] **********************************************\nok: [localhost]\n\nTASK [Confirm Docker Engine is available] *************************************\nok: [localhost] => {\"changed\": false, \"msg\": \"All assertions passed\"}\n\nTASK [Check the CHSS deployment endpoint when an app is already deployed] ******\nskipping: [localhost]\n\nTASK [Report application endpoint verification mode] **************************\nok: [localhost] => {\"msg\": \"CHSS endpoint http://127.0.0.1:18082/login was not checked because no application deployment was requested.\"}\n\nPLAY RECAP *********************************************************************\nlocalhost : ok=7 changed=1 unreachable=0 failed=0 skipped=1 rescued=0 ignored=0")]
story += [para("Idempotency execution after the fact-variable correction", "H2x"), code("TASK [Gathering Facts] *********************************************************\nok: [localhost]\nTASK [Require a Debian-family Linux target] ************************************\nok: [localhost] => {\"changed\": false, \"msg\": \"All assertions passed\"}\nTASK [Install Docker and host verification prerequisites] **********************\nok: [localhost]\nTASK [Enable and start Docker Engine] ******************************************\nok: [localhost]\nTASK [Read Docker Engine version] **********************************************\nok: [localhost]\nTASK [Confirm Docker Engine is available] *************************************\nok: [localhost] => {\"changed\": false, \"msg\": \"All assertions passed\"}\nTASK [Check the CHSS deployment endpoint when an app is already deployed] ******\nskipping: [localhost]\nTASK [Report application endpoint verification mode] **************************\nok: [localhost] => {\"msg\": \"CHSS endpoint http://127.0.0.1:18082/login was not checked because no application deployment was requested.\"}\n\nPLAY RECAP *********************************************************************\nlocalhost : ok=7 changed=0 unreachable=0 failed=0 skipped=1 rescued=0 ignored=0")]
story += [para("The first run made one package-state change. The subsequent run changed zero resources, with no failed or unreachable hosts. The second run therefore verifies idempotency for the configured target state.", "Callout")]
story += [para("Continuation audit (8 October 2026)", "H2x"), para("The existing Ubuntu 26.04.1 WSL2 target was confirmed as a normal distribution, separate from docker-desktop. Inventory graph and syntax validation passed again. Two additional actual playbook executions each reported ok=7, changed=0, unreachable=0, failed=0, skipped=1. The Docker service remained active and enabled; the Ubuntu Docker Engine responded as Client=29.1.3 Server=29.1.3. Full continuation command output is preserved in community-health-survey/chss/ansible/execution-log.txt.")]
story += [para("9. Final verification and limits", "H1x"), para("Ansible inventory parsing and playbook syntax validation passed. Docker reports active and enabled; its Ubuntu Engine responds as version 29.1.3. Package versions were queried with dpkg-query. No host chss user was created. The optional CHSS endpoint task was skipped because this playbook prepares prerequisites rather than launching a container. Java 17 runtime behavior remains documented by the existing project image and Week 11/12 evidence.")]
story += [para("The execution log contains setup commands, validation results, both Ansible recaps, and post-run checks. No terminal screenshot was captured; the PDF uses the real command output transcript rather than a fabricated image. No Jenkins job was run, and existing Docker Desktop containers and unrelated working-tree changes were left alone.")]
story += [para("10. Conclusion", "H1x"), para("Week 13 now has a runnable, idempotent Ansible implementation against a real Ubuntu WSL2 target. The configuration reflects the project's Docker deployment boundary: it installs and verifies the Docker host service while avoiding unneeded host Java, Maven, MySQL, app users, directories, files, or ports.")]

doc.build(story)
print(f"Created {OUT} ({OUT.stat().st_size} bytes)")
