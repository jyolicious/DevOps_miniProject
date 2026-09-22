# Community Health Survey System

A small web-based Community Health Survey System developed as a
15-week DevOps mini-project.

The system provides digital management of community health survey
records with CRUD operations, search, role-based workflow and a
summary dashboard.

---

## Project Information

**Project:** Community Health Survey System

**Type:** DevOps Mini-Project

**Build Tool:** Maven

**Backend:** Spring Boot

**Database:** MySQL

**Frontend:** Thymeleaf, HTML, CSS

**Language:** Java

---

# 1. Problem Statement

Community health survey information is often collected through paper
forms, spreadsheets, or disconnected records. This makes it difficult
to maintain consistent records, search previous entries, update survey
status and generate quick summaries.

The Community Health Survey System provides a centralized web-based
application for recording, viewing, updating and searching survey
records.

---

# 2. Objectives

The main objectives are:

- Create and manage community health survey records.
- View and search survey records.
- Support a role-based survey workflow.
- Provide a dashboard with survey summary counts.
- Store survey data in a relational database.
- Automate build and testing using Maven and Jenkins.
- Validate critical browser journeys using Selenium.
- Package and deploy the application using Docker.
- Automate environment configuration using Ansible.

---

# 3. Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Java |
| Framework | Spring Boot |
| Build Tool | Maven |
| Frontend | Thymeleaf, HTML, CSS |
| Database | MySQL |
| ORM | Spring Data JPA / Hibernate |
| Version Control | Git / GitHub |
| CI/CD | Jenkins |
| Browser Testing | Selenium WebDriver |
| Containerization | Docker |
| Provisioning | Ansible |

---

# 4. Application Architecture

The application follows a layered architecture:

```text
Browser
   |
   v
Controller Layer
   |
   v
Service Layer
   |
   v
Repository Layer
   |
   v
Spring Data JPA
   |
   v
MySQL Database