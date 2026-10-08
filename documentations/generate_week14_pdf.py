from datetime import date
from html import escape
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    Image,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parent.parent
ANSIBLE = ROOT / "community-health-survey" / "chss" / "ansible"
EVIDENCE = ROOT / "documentations" / "week14-evidence"
LOG = ANSIBLE / "week14-execution-log.txt"
OUT = ROOT / "documentations" / "Week14_Automation_Reliability.pdf"
PAGE_W, PAGE_H = A4
INK = colors.HexColor("#182230")
BLUE = colors.HexColor("#155EEF")
PALE = colors.HexColor("#EFF4FF")
MUTED = colors.HexColor("#526071")
GRID = colors.HexColor("#D0D5DD")
CONTENT_WIDTH = PAGE_W - 36 * mm

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="CoverTitle14", parent=styles["Title"], fontName="Helvetica-Bold", fontSize=28, leading=34, textColor=INK, alignment=TA_LEFT, spaceAfter=10))
styles.add(ParagraphStyle(name="CoverSub14", parent=styles["Normal"], fontSize=14, leading=21, textColor=MUTED))
styles.add(ParagraphStyle(name="H1x14", parent=styles["Heading1"], fontName="Helvetica-Bold", fontSize=19, leading=24, textColor=INK, spaceBefore=3, spaceAfter=10))
styles.add(ParagraphStyle(name="H2x14", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=11.5, leading=15, textColor=BLUE, spaceBefore=8, spaceAfter=5))
styles.add(ParagraphStyle(name="Bodyx14", parent=styles["BodyText"], fontSize=9, leading=13, textColor=INK, spaceAfter=6))
styles.add(ParagraphStyle(name="Smallx14", parent=styles["BodyText"], fontSize=7.8, leading=10.5, textColor=MUTED, spaceAfter=4))
styles.add(ParagraphStyle(name="Cellx14", parent=styles["BodyText"], fontSize=7.1, leading=9, textColor=INK))
styles.add(ParagraphStyle(name="CellHeadx14", parent=styles["BodyText"], fontName="Helvetica-Bold", fontSize=7.2, leading=9, textColor=colors.white))
styles.add(ParagraphStyle(name="Codex14", fontName="Courier", fontSize=6.7, leading=8.4, backColor=colors.HexColor("#F2F4F7"), borderColor=GRID, borderWidth=0.5, borderPadding=6, textColor=INK, splitLongWords=1))
styles.add(ParagraphStyle(name="Callout14", parent=styles["BodyText"], fontSize=9.3, leading=13, textColor=INK, backColor=PALE, borderColor=BLUE, borderWidth=0.7, borderPadding=8, spaceBefore=5, spaceAfter=7))
styles.add(ParagraphStyle(name="Caption14", parent=styles["BodyText"], fontSize=7.5, leading=10, textColor=MUTED, alignment=TA_CENTER, spaceBefore=3, spaceAfter=7))


def para(value, style="Bodyx14"):
    return Paragraph(value, styles[style])


def code(value):
    return Preformatted(value.strip("\n"), styles["Codex14"], maxLineLength=106)


def section(marker):
    heading = f"=== {marker} ==="
    start = LOG_TEXT.rfind(heading)
    if start < 0:
        raise ValueError(f"Required real execution-log section is missing: {marker}")
    end = LOG_TEXT.find("\n===", start + len(heading))
    return LOG_TEXT[start:] if end < 0 else LOG_TEXT[start:end]


def recap(marker):
    text = section(marker)
    lines = text.splitlines()
    selected = []
    in_play = False
    for line in lines:
        if line.startswith("PLAY ["):
            in_play = True
        if in_play and (
            line.startswith(("PLAY [", "TASK [", "PLAY RECAP", "localhost"))
            or line.startswith(("ok:", "changed:", "skipping:", "FAILED - RETRYING:"))
            or line.startswith("Command exit code:")
        ):
            selected.append(line)
    return "\n".join(selected)


def selected_lines(marker, predicates):
    lines = section(marker).splitlines()
    return "\n".join(line for line in lines if any(predicate in line for predicate in predicates))


def screenshot(filename, caption, max_height=88 * mm):
    path = EVIDENCE / filename
    if not path.is_file():
        raise FileNotFoundError(f"Required genuine evidence screenshot is missing: {path}")
    image = Image(str(path))
    scale = min(CONTENT_WIDTH / image.imageWidth, max_height / image.imageHeight)
    image.drawWidth = image.imageWidth * scale
    image.drawHeight = image.imageHeight * scale
    return [image, para(escape(caption), "Caption14")]


if not LOG.is_file():
    raise FileNotFoundError(f"Execution log not found: {LOG}")

LOG_TEXT = LOG.read_text(encoding="utf-8")
required_sections = (
    "B. First provisioning and stable deployment",
    "D. First HTTP health check",
    "E. Second Ansible execution (idempotency)",
    "E1. Manage the Docker Buildx build prerequisite",
    "E2. Post-prerequisite idempotency check",
    "F. Build an actual image from the current committed source revision",
    "G. Deploy the locally built commit-specific candidate",
    "H. Verify candidate image and application response",
    "I. Roll back to the previous stable Week 12 image",
    "J. Verify stable recovery, startup logs, and endpoint",
)
for required in required_sections:
    section(required)

doc = BaseDocTemplate(
    str(OUT),
    pagesize=A4,
    rightMargin=18 * mm,
    leftMargin=18 * mm,
    topMargin=22 * mm,
    bottomMargin=17 * mm,
    title="Week 14 - Automated Provisioning and Reliability Validation",
    author="Community Health Survey System",
)
frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="normal")


def on_page(canvas, current_doc):
    canvas.saveState()
    if current_doc.page > 1:
        canvas.setStrokeColor(GRID)
        canvas.line(18 * mm, PAGE_H - 14 * mm, PAGE_W - 18 * mm, PAGE_H - 14 * mm)
        canvas.setFont("Helvetica", 7.5)
        canvas.setFillColor(MUTED)
        canvas.drawString(18 * mm, PAGE_H - 10.5 * mm, "CHSS | WEEK 14 AUTOMATION & RELIABILITY")
        canvas.drawRightString(PAGE_W - 18 * mm, 10.5 * mm, f"Page {current_doc.page}")
    canvas.restoreState()


doc.addPageTemplates(PageTemplate(id="main", frames=frame, onPage=on_page))
story = []

story += [
    Spacer(1, 26 * mm),
    para("DEVOPS PROJECT / WEEK 14", "H2x14"),
    Spacer(1, 5 * mm),
    para("Automated Provisioning<br/>and Reliability Validation", "CoverTitle14"),
    para("Community Health Survey System", "CoverSub14"),
    Spacer(1, 15 * mm),
]
cover_rows = [
    ("APPROACH", "Ansible-managed Docker deployment"),
    ("TARGET", "Ubuntu 26.04.1 LTS on WSL2"),
    ("STABLE RELEASE", "Published image jyotsnakasi/chss-app:build-20"),
    ("LOCAL CANDIDATE", "Built from committed revision d7560b59fc5d"),
    ("PORT", "Host TCP 18084 -> container TCP 8081"),
]
cover = Table(
    [[para(escape(key), "CellHeadx14"), para(escape(value), "Cellx14")] for key, value in cover_rows],
    colWidths=[37 * mm, 130 * mm],
)
cover.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (0, -1), INK),
    ("BACKGROUND", (1, 0), (1, -1), colors.white),
    ("BOX", (0, 0), (-1, -1), 0.7, GRID),
    ("INNERGRID", (0, 0), (-1, -1), 0.4, GRID),
    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ("LEFTPADDING", (0, 0), (-1, -1), 8),
    ("RIGHTPADDING", (0, 0), (-1, -1), 7),
    ("TOPPADDING", (0, 0), (-1, -1), 7),
    ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
]))
story += [
    cover,
    Spacer(1, 16 * mm),
    para(f"Evidence date: {date.today().strftime('%d %B %Y')}. This report uses actual Ansible, Docker, HTTP, and browser evidence. The alternate image is a local build, not a newly published Docker Hub tag.", "Smallx14"),
    PageBreak(),
]

story += [
    para("1. Objective and target architecture", "H1x14"),
    para("Automate CHSS deployment to a real Linux target, verify the application endpoint, demonstrate a no-change repeat run, and exercise a local candidate release followed by rollback to the known stable image."),
    para("The controller and target are the same Ubuntu 26.04.1 WSL2 distribution. Ansible uses a local inventory connection and manages the Ubuntu distro's Docker Engine; it does not target Windows Docker Desktop or its internal docker-desktop WSL distribution. The container is isolated as chss-week14 and published only on host port 18084."),
    para("Registry discovery found build-20 as the only active published CHSS tag. To avoid inventing a release, the candidate uses the explicit local tag local/chss-app:git-d7560b59fc5d and carries the full source revision label. It is evaluated as a current-source build, not described as a newer published release.", "Callout14"),
    para("2. Configuration specification", "H1x14"),
]

spec_rows = [
    ("Area", "Configured state", "Reason / verification"),
    ("Host packages", "ca-certificates, docker.io, docker-buildx, python3, python3-docker", "Docker Engine, Buildx candidate builds, HTTPS trust, Ansible Python runtime, Docker SDK module support"),
    ("Java / Maven", "Temurin Java 17 runtime; Maven 3.9.9 build stage inside the image", "No host Java/Maven install is needed for the Docker deployment"),
    ("Users", "No additional host user; image runs as unprivileged chss", "Container Dockerfile defines the runtime account"),
    ("Directories", "No host CHSS data/config/log directory; /app exists in image", "Deployment uses an in-memory H2 database and no host bind mounts"),
    ("Files", "No host config files or secrets created", "Runtime settings use non-secret environment values in the container"),
    ("Ports", "Host TCP 18084 -> container TCP 8081", "Isolated Week 14 endpoint; no host MySQL port is required"),
    ("Database", "H2 in-memory; not MySQL", "Matches the actual containerized demo deployment"),
    ("Services", "Ubuntu docker.service enabled and started; CHSS is a container", "No host systemd CHSS or MySQL service is fabricated"),
]
table = Table(
    [[para(escape(cell), "CellHeadx14") for cell in spec_rows[0]]]
    + [[para(escape(cell), "Cellx14") for cell in row] for row in spec_rows[1:]],
    colWidths=[29 * mm, 67 * mm, 71 * mm],
    repeatRows=1,
    hAlign="LEFT",
)
table.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (-1, 0), INK),
    ("GRID", (0, 0), (-1, -1), 0.4, GRID),
    ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F8FAFC")]),
    ("LEFTPADDING", (0, 0), (-1, -1), 5),
    ("RIGHTPADDING", (0, 0), (-1, -1), 5),
    ("TOPPADDING", (0, 0), (-1, -1), 4),
    ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
]))
story += [table, PageBreak()]

story += [
    para("3. Inventory and playbook", "H1x14"),
    para("The inventory targets localhost with Ansible's local connection and /usr/bin/python3. The playbook gathers facts, requires a Debian-family host, refuses to replace a same-named container not labelled as Week 14-managed, installs host prerequisites, starts/enables Docker, declares the container state with community.docker.docker_container, and waits for GET /login to return HTTP 200."),
    code("[chss_week14]\nlocalhost ansible_connection=local ansible_python_interpreter=/usr/bin/python3"),
    para("The container is configured with the selected image, image/release labels, restart policy, host-to-container port mapping, and H2 environment. The playbook does not store application passwords, expose MySQL, create an unnecessary host account, or create bind-mounted data directories."),
    para("Validation commands", "H2x14"),
    code("ansible-inventory -i week14_inventory.ini --graph\nansible-playbook -i week14_inventory.ini week14_provision.yml --syntax-check\nansible-playbook -i week14_inventory.ini week14_provision.yml --extra-vars chss_image=jyotsnakasi/chss-app:build-20"),
    para("Target environment", "H2x14"),
    code(section("Ansible, Ubuntu, Docker Engine, and service verification")),
    PageBreak(),
]

story += [
    para("4. First deployment and health verification", "H1x14"),
    para("The first real Ansible run deployed the published build-20 image. The playbook retried its application readiness check during startup and then completed successfully. The full command output remains in the execution log; the excerpt below is selected directly from that captured output."),
    code(recap("B. First provisioning and stable deployment")),
    para("The explicit target-side HTTP check also returned 200. A browser opened the same login endpoint over the Ubuntu WSL2 address; the genuine screenshot shows only the blank sign-in form.", "Callout14"),
    code(section("D. First HTTP health check")),
    *screenshot("04_first_provisioning.png", "Actual Windows Terminal output from the first Ansible deployment.", 78 * mm),
    PageBreak(),
]

story += [
    para("5. Idempotency and build prerequisite", "H1x14"),
    para("The second execution of the initial playbook reported changed=0. During source-build validation, Docker 29 reported its legacy-builder deprecation warning and the Docker CLI probe showed that Buildx was not installed. The capture helper was updated to preserve native stderr while still checking the process exit code, and the playbook now manages Ubuntu's docker-buildx package because this Week 14 workflow builds a local candidate."),
    code(recap("E. Second Ansible execution (idempotency)")),
    para("Ansible installed the missing build-only prerequisite, then a further run returned to changed=0. This final no-change run validates the current playbook after the package-list update.", "Callout14"),
    code(recap("E1. Manage the Docker Buildx build prerequisite")),
    code(recap("E2. Post-prerequisite idempotency check")),
    PageBreak(),
    para("6. Application response", "H1x14"),
    para("An explicit target-side HTTP health check returned 200. A browser opened the same login endpoint over the Ubuntu WSL2 address; the genuine screenshot shows only the blank sign-in form."),
    *screenshot("07_login_page.png", "Actual browser capture of the CHSS /login page served by the deployed WSL2 container.", 92 * mm),
    para("No test account was used, and no login credentials or seeded demo values are included in this evidence.", "Smallx14"),
    PageBreak(),
]

story += [
    para("7. Current-source candidate and rollback", "H1x14"),
    para("The candidate was built from a clean Git archive of committed revision d7560b59fc5df0eaa1331936c9b7a236b7f48abc. Docker labels the local image with that full revision. It is not pushed to Docker Hub. The same Ansible playbook then deploys the candidate and verifies its container identity and HTTP response."),
    code(selected_lines("F. Build an actual image from the current committed source revision", ("github.com/docker/buildx", "local/chss-app:", "org.opencontainers.image.revision:", "Command exit code:"))),
    code(selected_lines("H. Verify candidate image and application response", ("Name=", "HTTP ", "Command exit code:"))),
    *screenshot("08_candidate_login_page.png", "Actual browser capture of the locally built candidate responding on /login.", 77 * mm),
    PageBreak(),
    para("8. Rollback and stable recovery", "H1x14"),
    para("Rollback uses the same playbook with the previous published build-20 image. The recovery stage verifies the stable image, running state, filtered startup milestones, and HTTP endpoint response.", "Callout14"),
    code(recap("I. Roll back to the previous stable Week 12 image")),
    code(selected_lines("J. Verify stable recovery, startup logs, and endpoint", ("ID=", "Name=", "Starting CommunityHealthSurveyApplication", "Tomcat started", "Started CommunityHealthSurveyApplication", "HTTP ", "Command exit code:"))),
    *screenshot("09_stable_recovery_login.png", "Actual browser capture after rollback to the published build-20 image.", 70 * mm),
    PageBreak(),
]

story += [
    para("9. Terminal evidence", "H1x14"),
    para("These images are genuine screen captures from the execution terminals. Each corresponds to a real command recorded in the execution log."),
    *screenshot("01_target_ubuntu.png", "Ubuntu WSL2, Ansible, Python Docker SDK, Docker Engine, and service checks.", 66 * mm),
    *screenshot("02_ansible_inventory.png", "Parsed Ansible inventory graph.", 66 * mm),
    *screenshot("03_playbook_syntax_check.png", "Successful Ansible syntax check.", 66 * mm),
    PageBreak(),
    para("10. Deployment evidence and lessons", "H1x14"),
    *screenshot("06_container_running.png", "Container inspection and filtered startup milestones. Long terminal lines are also preserved as text in the execution log.", 83 * mm),
    para("Problems encountered and resolution", "H2x14"),
    para("Two initial capture-helper errors occurred before provisioning: one used an unsupported PowerShell output parameter, and one contained an invalid text-normalization edit. Both failures are recorded in the execution log and preserved as original error screenshots. The helper was corrected before the successful runs; neither failed attempt changed the application target."),
    para("The WSL Docker service also received orderly stop signals between short command sessions, which interrupted the container and caused connection resets during early inspection. Journal output showed graceful Docker shutdown rather than an application crash or OOM event. The Ubuntu WSL target was kept available while the subsequent deployment, health, and lifecycle checks were performed; the final HTTP checks and recovery results are recorded below."),
    para("Limitations", "H2x14"),
    para("Docker Hub did not expose a newer CHSS tag during registry discovery. The alternate image is therefore a local, commit-labelled build, not a released or registry-published upgrade. The Dockerfile build uses -DskipTests, so this workflow verifies deployment and HTTP health but does not claim a Maven or Selenium test-suite pass. The Ubuntu target is WSL2 on a development workstation rather than a separate production server. Docker Desktop and unrelated Windows containers were not changed."),
    PageBreak(),
]

story += [
    para("11. Final verification", "H1x14"),
    para("Inventory parsing, Ansible syntax validation, first deployment, second-run idempotency, candidate response, rollback, and stable recovery are all represented by actual command output in the execution log. The browser screenshot confirms that the login page renders; the HTTP checks confirm the endpoint status without submitting credentials."),
    code(section("J. Verify stable recovery, startup logs, and endpoint")),
    para("The execution log is the authoritative transcript for command details and return codes. The PDF includes selected output and genuine screenshots for readable evidence. No secrets, passwords, access tokens, or personal data are intentionally included.", "Callout14"),
    para("12. Conclusion", "H1x14"),
    para("Week 14 demonstrates an Ansible-managed container release, real HTTP verification, repeat-run idempotency, deployment of a local current-source candidate, and recovery to the published stable image. All release claims are bounded by the registry evidence: build-20 is the published version, while the candidate remains local."),
]

doc.build(story)
print(f"Created {OUT} ({OUT.stat().st_size} bytes)")
