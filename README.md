# StreamHub

A web-based video browsing system that combines a personal video library (Jellyfin-style) with social video features (YouTube-style) — upload, organize, and stream your own videos, and like, comment on, and interact with videos from others.

**Group ID:** 2026-Y2-S1-MLB-B2G2-07
**Module:** SE2030 Software Engineering & Database Design and Development (DDD)
**Year 2, Semester 1, 2026**

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot (REST API), Java 17, Maven |
| Frontend | React (Vite) |
| Database | Microsoft SQL Server, via Spring Data JPA |
| Auth | Spring Security with JWT |
| File Storage | Local storage with byte-range video streaming |

## Team

| Student ID | Name | Function |
|---|---|---|
| IT25101967 | Jayalath E.P.A.P. | |
| IT25102896 | Weerasinghe W.M.V.K. | |
| IT25103985 | Chandrasena H.L.L.S. | |
| IT25101962 | Nawarathna N.S.D. | |
| IT25104019 | Kawdawatta P.M.A.I.D. | |
| IT25101060 | Rajapaksha R.M.H.N. | |

## The Six Major Functions

Each function owns its own database entity and its own folder in both the backend and frontend.

| # | Function | Owns Entity |
|---|---|---|
| 1 | Video Upload & Management (includes in-app player) | Video |
| 2 | Category Management | Category |
| 3 | Playlist & Content Organization | Playlist |
| 4 | Social Interaction (likes, comments) | Comment, Like |
| 5 | Report & Moderation Management | Report |
| 6 | Watch History & Recommendation Management | WatchHistory |

## Project Structure

```
streamhub/
  backend/        Spring Boot project (feature-based package structure)
  frontend/       React project (feature-based folder structure)
  docs/           ER diagrams, reports, meeting notes
  README.md
```

Backend and frontend are both organized by feature, not by layer — each function has its own folder containing everything it needs (entity, repository, service, controller, DTOs on the backend; API calls, components, and pages on the frontend). Full details are in `docs/StreamHub_Full_Project_Guide.docx` and `docs/StreamHub_Full_Setup_Guide.docx`.

## Getting Started

New to the project? Follow `docs/StreamHub_Full_Setup_Guide.docx` for the complete walkthrough — installing tools, setting up SQL Server locally, cloning, running the backend and frontend, and confirming everything connects.

Quick version, once tools are installed:

```bash
git clone https://github.com/<owner-username>/streamhub.git
cd streamhub
git checkout dev
```

Backend:
```bash
cd backend
# create backend/src/main/resources/application-local.properties with your own SQL Server login
mvnw spring-boot:run
```

Frontend:
```bash
cd frontend
npm install
npm run dev
```

## Branching Workflow

- `main` — always working, demo-ready code only. Nobody pushes here directly.
- `dev` — integration branch. Everyone merges finished work here first.
- `feature/<function>-<name>` — personal working branch, e.g. `feature/video-upload-jayalath`.

Both `main` and `dev` are protected — all changes go through a pull request with at least one review before merging.

## Database

The database design is developed and graded separately under the DDD module — fully normalized to 3NF, with indexing and constraints documented. The schema script is kept up to date in `docs/schema.sql`; whoever changes the schema updates that file in the same pull request.

## License

Academic project for SE2030, SLIIT. Not licensed for external use or distribution.
