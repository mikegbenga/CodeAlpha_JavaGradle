# CodeAlpha_JavaGradle — Java Application using Gradle

**CodeAlpha DevOps Internship · Task 3** — Michael Gbenga Abajemito (Student ID: CA/DF1/326280)

A small Java REST API (a to-do list) built, tested and packaged with **Gradle**, with a full **CI/CD pipeline** in
GitHub Actions. Every push is compiled, tested and coverage-checked; pushes to `main` also publish a Docker image to
GitHub Container Registry, and tags (`v1.0.0`) produce a GitHub Release with the runnable jar.

```
 git push ──► GitHub Actions
              ├─ build   : wrapper check → ./gradlew build (compile · 18 tests · JaCoCo ≥ 80% · jars) → artifacts
              ├─ docker  : multi-stage image → ghcr.io/mikegbenga/codealpha-task-api → run it → smoke test
              └─ release : on tag v* → GitHub Release with the jar attached
```

## How each requirement is covered

| Requirement | Where |
|---|---|
| Automate Java builds using Gradle | [`build.gradle.kts`](build.gradle.kts) + Gradle wrapper (`./gradlew`), so everyone builds with Gradle 8.14.3 |
| Manage dependencies efficiently | [`gradle/libs.versions.toml`](gradle/libs.versions.toml) version catalog, `implementation` vs `testImplementation` scopes, dependency cache in CI, Dependabot ([`.github/dependabot.yml`](.github/dependabot.yml)) |
| Integrate CI/CD for continuous delivery | [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml): build → Docker image → GHCR → smoke test → release on tag |
| Streamline build & deployment | One command builds everything (`./gradlew build`); runnable fat jar; multi-stage [`Dockerfile`](Dockerfile) with a cached dependency layer |
| DevOps principles in Java | Tests and a coverage gate fail the build early, versioned artifacts, immutable images, non-root container with health check |

## The app

| Method | Path | What it does |
|---|---|---|
| GET | `/health` | `{"status":"UP","version":"…","tasks":…,"done":…}` |
| GET | `/api/tasks` | List tasks |
| POST | `/api/tasks` | `{"title":"…"}` → creates a task (201) |
| GET | `/api/tasks/{id}` | One task (404 if missing) |
| PUT | `/api/tasks/{id}/done` | Mark done |
| DELETE | `/api/tasks/{id}` | Delete (204) |

Plain Java 17+: the JDK's built-in HTTP server, records, `ConcurrentHashMap`. The one runtime dependency is **Gson** for JSON.

## Run it

Needs JDK 17 or newer. You don't need Gradle installed; the wrapper downloads the right version.

```bash
./gradlew build                     # compile, run 18 tests, check coverage, build jars
./gradlew run                       # start on http://localhost:8080
# or the single runnable jar
java -jar build/libs/codealpha-task-api-1.0.0-dev-all.jar

curl localhost:8080/health
curl -X POST -d '{"title":"Learn Gradle"}' localhost:8080/api/tasks
curl localhost:8080/api/tasks
curl -X PUT localhost:8080/api/tasks/1/done
```

On Windows use `gradlew.bat build` (or `.\gradlew build` in PowerShell).

With Docker:

```bash
docker build -t codealpha-task-api .
docker run --rm -p 8080:8080 codealpha-task-api
# or the image CI published
docker run --rm -p 8080:8080 ghcr.io/mikegbenga/codealpha-task-api:latest
```

## Useful Gradle tasks

| Command | What it does |
|---|---|
| `./gradlew tasks` | List every task |
| `./gradlew dependencies --configuration runtimeClasspath` | Show the dependency tree |
| `./gradlew test` | Run the tests (JaCoCo report runs straight after) |
| `./gradlew check` | Tests + coverage gate (fails under 80%) |
| `./gradlew fatJar` | Build `build/libs/*-all.jar` with Gson inside |
| `./gradlew build -PappVersion=1.2.3` | Build with a specific version (CI uses `1.0.<run number>`) |

Reports: `build/reports/tests/test/index.html` and `build/reports/jacoco/test/html/index.html`.

## Project layout

```
├── build.gradle.kts            plugins, tests, JaCoCo gate, fat jar
├── settings.gradle.kts
├── gradle/libs.versions.toml   all dependency versions in one place
├── gradlew, gradlew.bat, gradle/wrapper/   pinned Gradle 8.14.3
├── src/main/java/com/codealpha/tasks/
│   ├── App.java                entry point (PORT env var)
│   ├── TaskApi.java            HTTP routes
│   ├── TaskService.java        thread-safe in-memory store
│   ├── Task.java               record with validation
│   └── Json.java               Gson setup
├── src/test/java/com/codealpha/tasks/
│   ├── TaskServiceTest.java    unit tests (nested + parameterized)
│   └── TaskApiIT.java          starts the real server and calls it over HTTP
├── Dockerfile                  multi-stage, non-root, HEALTHCHECK
└── .github/
    ├── workflows/ci-cd.yml
    └── dependabot.yml
```

## Release a version

```bash
git tag v1.0.0 && git push origin v1.0.0
```

The pipeline builds and tests it, publishes `ghcr.io/mikegbenga/codealpha-task-api:1.0.0`, and creates a GitHub Release
with the jar attached.

## License
MIT
