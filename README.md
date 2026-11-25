# Student Management

A Spring Boot application for registering, listing, updating, and deleting students through a Thymeleaf interface or REST API.

## Requirements

- Java 21 and Maven
- MySQL for local use, or Docker Compose

## Run locally

Create the `student_management` database in MySQL. Set your credentials in the shell rather than committing them to the application configuration:

```powershell
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "<your-mysql-password>"
mvn spring-boot:run
```

Open the UI at `http://localhost:8095`.

## Run with Docker Compose

Copy `.env.example` to `.env`, replace both sample values with strong passwords, then run:

```powershell
docker compose up --build
```

Compose starts MySQL and the application. Data is persisted in the `mysql-data` volume. Open `http://localhost:8095`.

## REST API

All endpoints use `/api/students` and JSON:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/students` | List students |
| `GET` | `/api/students/{id}` | Get one student |
| `POST` | `/api/students` | Create a student |
| `PUT` | `/api/students/{id}` | Update a student |
| `DELETE` | `/api/students/{id}` | Delete a student |

Example create request:

```json
{
  "name": "Anne-Marie O'Neil",
  "email": "anne.oneil@example.com",
  "course": "Java"
}
```

Invalid input returns `400`, duplicate email returns `409`, missing IDs return `404`, and successful creation returns `201` with a `Location` header.

## Tests, coverage, and SonarQube

Run unit/API tests and the JaCoCo 90% instruction and line coverage gates:

```powershell
mvn verify
```

Run Selenium browser tests as well (requires Chrome or Chromium):

```powershell
mvn verify -DrunSelenium=true
```

Selenium Manager downloads a matching driver automatically when available. If your environment blocks that download, install a matching ChromeDriver and set `CHROMEDRIVER_PATH` before running the command. GitHub Actions installs Chrome and ChromeDriver directly.

JaCoCo creates `target/site/jacoco/index.html`. The gate covers application logic and excludes the Spring Boot launcher and persistence model.

The GitHub Actions workflow runs verification and Selenium tests on pushes and pull requests to `main`. To enable its optional SonarQube scan, configure the repository variable `SONAR_HOST_URL` and secret `SONAR_TOKEN`. Analysis settings are in `sonar-project.properties`.
