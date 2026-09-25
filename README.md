# Workshop AI — Employee CRUD + MCP Demo

Spring Boot 4.1.1 backend (REST + JWT, MCP server, Flyway-managed schema, Hibernate Envers audit history) with an Angular 21 frontend, built for an AI workshop.

## Running locally (H2, default)

```bash
mvn spring-boot:run
```

Uses a local H2 file database (`data/workshopai.mv.db`), auto-created on first run. Seeded accounts: `admin`/`admin123` (ADMIN), `user`/`user123` (USER).

**If you edit a file under `src/main/resources/db/migration/`, delete `data/workshopai.mv.db` (and `data/workshopai.trace.db` if present) before the next run.** Flyway validates the local database's migration history against the current migration files on every startup and refuses to start on a mismatch — this is a checksum check, not a schema diff, so even a comment-only edit to a migration file requires a fresh local database.

## Running tests

```bash
mvn test
```

Runs against an in-memory H2 database, with Flyway managing the schema there too — the same migrations that run against production databases.

## Running against PostgreSQL (Docker)

```bash
docker compose up --build
```

Starts PostgreSQL and the containerized app together, with Flyway running the same migrations against a real PostgreSQL database. See `docs/superpowers/specs/2026-09-12-flyway-envers-docker-design.md` for details.

## Static code analysis (SonarQube, local)

Start a local SonarQube server (separate compose file — not part of the app stack, so the demo stack stays lightweight):

```bash
docker compose -f docker-compose.sonar.yml up -d
```

Wait until it's healthy (first boot takes 1-3 minutes):

```bash
docker compose -f docker-compose.sonar.yml ps
```

Open `http://localhost:9000` (default login `admin`/`admin`, you'll be asked to change it on first login). Create a local project (or let the scanner auto-create one) and generate a token: **My Account → Security → Generate Token**.

Run the analysis from the project root:

```bash
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<your-generated-token>
```

Results appear in the SonarQube UI at `http://localhost:9000` under the project. To stop SonarQube: `docker compose -f docker-compose.sonar.yml down` (add `-v` to also delete its data volume and start fresh next time).

## Dependency vulnerability scanning (OWASP Dependency-Check)

Scans every dependency (including transitive ones) against known CVEs from the National Vulnerability Database. Runs locally, no server/container needed — just a Maven plugin, already added to `pom.xml`.

**Requires a free NVD API key** (mandatory since 2023 — without one, the CVE database download fails with `Invalid API Key`). Get one at <https://nvd.nist.gov/developers/request-an-api-key> (instant, no cost), then run:

```bash
mvn verify -DskipTests -Dnvd.api.key=<your-nvd-api-key>
```

(Bound to the `verify` phase alongside Checkstyle/PMD/SpotBugs, so a plain `mvn verify -Dnvd.api.key=...` runs it too.) **First run downloads the NVD CVE database** (can take several minutes even with a key — it's a large dataset); subsequent runs are fast, using a local cache under `~/.m2/repository/org/owasp/dependency-check-data/`. To avoid retyping the key every time, set it once in `~/.m2/settings.xml` instead:

```xml
<settings>
  <profiles>
    <profile>
      <id>nvd</id>
      <properties>
        <nvd.api.key>your-nvd-api-key</nvd.api.key>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>nvd</activeProfile>
  </activeProfiles>
</settings>
```

Report: `target/dependency-check-report.html` (open directly in a browser) and `target/dependency-check-report.json`.

Currently configured as **report-only** (`failBuildOnCVSS: 11`, i.e. never fails the build — 11 is above the max CVSS score of 10). To turn it into a real gate that fails the build on serious vulnerabilities, lower `failBuildOnCVSS` in `pom.xml` (e.g. `7` fails only on High/Critical CVEs).

## Local Maven repository (Nexus, optional)

Start a local Nexus Repository Manager (separate compose file, same pattern as SonarQube — not part of the app stack):

```bash
docker compose -f docker-compose.nexus.yml up -d
```

First boot takes 1-3 minutes. Check status:

```bash
docker compose -f docker-compose.nexus.yml ps
```

Open `http://localhost:8081`. Initial admin login is `admin` with a generated password, retrieved with:

```bash
docker exec workshop_ai-nexus-1 cat /nexus-data/admin.password
```

(You'll be asked to change it and can disable anonymous access on first login — the setup wizard prompts for both.)

**To use it as a Maven mirror/cache for this project:** Nexus ships a built-in `maven-public` group repository that proxies Maven Central. Point Maven at it by adding a mirror to `~/.m2/settings.xml` (your global settings, not this project's `pom.xml` — this affects every Maven project on your machine, not just this one):

```xml
<settings>
  <mirrors>
    <mirror>
      <id>nexus</id>
      <mirrorOf>*</mirrorOf>
      <url>http://localhost:8081/repository/maven-public/</url>
    </mirror>
  </mirrors>
</settings>
```

After that, `mvn ...` in this project (or any other) resolves and caches dependencies through your local Nexus. Remove the `<mirror>` block to go back to resolving directly from Maven Central.

**Note:** the mirror above points at the *local Docker* Nexus (`localhost:8081`) from this section. Your actual, currently-active `~/.m2/settings.xml` mirror points instead at your real server, `https://nexus.georgesand.ro/repository/maven-public/` — that's the one actually in effect day to day (see the "Release & deploy pipeline" section below), and is also where `distributionManagement` in `pom.xml` publishes releases/snapshots to. The `localhost:8081` example here only matters if you deliberately switch back to the local Docker Nexus for offline/throwaway experimentation.

To stop Nexus: `docker compose -f docker-compose.nexus.yml down` (add `-v` to also delete its data volume/cache).

## Release & deploy pipeline (`deploy.sh` / `release.sh`)

Two scripts in the project root automate a full, gated release: build → static analysis → vulnerability gates → publish. They talk to **real, external servers** (not the local Docker containers from the sections above, which are only for casual local experimentation):

| Tool | URL | Used for |
|---|---|---|
| SonarQube | https://sonar.georgesand.ro | Code quality — Quality Gate must be `OK` |
| Dependency-Track | https://dtrack.georgesand.ro | Vulnerability scan of the generated SBOM — fails on Critical/High findings or policy violations |
| Nexus | https://nexus.georgesand.ro | Final destination of the deployed jar (`releases` or `snapshots` repo, chosen automatically by whether the version ends in `-SNAPSHOT`) |

### One-time setup

All credentials live in `~/.m2/settings.xml` (never in `pom.xml` or these scripts):

```xml
<settings>
  <servers>
    <server>
      <id>nexus</id>
      <username>admin</username>
      <password>YOUR_NEXUS_PASSWORD</password>
    </server>
    <server>
      <id>nvd</id>
      <password>YOUR_NVD_API_KEY</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>nexus</id>
      <properties>
        <dependency-track.apiKey>YOUR_DEPENDENCY_TRACK_API_KEY</dependency-track.apiKey>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>nexus</activeProfile>
  </activeProfiles>
</settings>
```

The SonarQube token is **not** stored in `settings.xml` — pass it as an environment variable each time (or export it once per shell session):

```bash
export SONAR_TOKEN=<your-sonar-token>   # My Account → Security → Generate Token, on sonar.georgesand.ro
```

### `./deploy.sh` — build, verify, publish once

```bash
export SONAR_TOKEN=<your-sonar-token>
./deploy.sh
```

What it does, in order (stops at the first failure — nothing bad ever reaches Nexus):
1. `mvn clean verify sonar:sonar` — compiles, runs tests, Checkstyle/PMD/OWASP Dependency-Check, generates the SBOM (`target/classes/META-INF/sbom/application.cdx.json`), runs the Sonar analysis.
2. Waits for the SonarQube **Quality Gate** result — aborts if not `OK`.
3. Uploads the SBOM to **Dependency-Track**, waits for processing, checks findings — aborts if Critical/High vulnerabilities (thresholds: `DTRACK_MAX_CRITICAL`, `DTRACK_MAX_HIGH`, both default `0`) or any failing policy violation are found. Link to the project's Dependency-Track page is printed either way.
4. `mvn deploy` — publishes the jar to Nexus.

Useful variations:
```bash
DRY_RUN=1 ./deploy.sh                  # run every check, skip the actual `mvn deploy`
DTRACK_MAX_CRITICAL=2 ./deploy.sh      # allow up to 2 critical findings before failing
```

### `./release.sh` — cut a release, then move on to the next dev version

```bash
export SONAR_TOKEN=<your-sonar-token>
./release.sh                # e.g. 1.0.1-SNAPSHOT → releases 1.0.1 → bumps pom.xml to 1.0.2-SNAPSHOT
./release.sh 2.0.0          # release exactly this version
./release.sh 2.0.0 2.1.0    # release 2.0.0, continue development as 2.1.0-SNAPSHOT
```

Asks for a `y/N` confirmation before doing anything. Sets the release version in `pom.xml`, runs the full `deploy.sh` pipeline, and — only if that succeeds — bumps `pom.xml` to the next SNAPSHOT version. If any gate fails, `pom.xml` is automatically restored to the version it had before the script ran, so a failed release never leaves the project half-versioned.

**No git repository exists in this project**, so there is no tag/commit step — "release" here means a real, gated artifact reached Nexus under a non-SNAPSHOT version.

### Where to look after a run

- Sonar dashboard: printed by `deploy.sh`, or `https://sonar.georgesand.ro/dashboard?id=ro.micronikgrupm.mcp.client:Workshop_AI`
- Dependency-Track project: printed by `deploy.sh` (a `https://dtrack.georgesand.ro/projects/<uuid>` link)
- Nexus: `https://nexus.georgesand.ro/#browse/browse:maven-releases` (or `maven-snapshots`)
- OWASP Dependency-Check HTML report (local, generated by the same `mvn verify`): `target/dependency-check-report.html`

## Frontend

See `frontend/README.md`.

## Workshop materials

Setup/participant guides: `docs/workshop-setup-guide-en.md` / `docs/workshop-setup-guide-fr.md`.
Presentation decks: `docs/workshop-ai-en.pptx` / `docs/workshop-ai-fr.pptx`.
