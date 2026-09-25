# Flyway + Envers + Docker — Extensie backend (sub-proiectul 1)

> Extinde backend-ul demo-ului CRUD + MCP (deja complet, spec:
> `2026-09-10-employee-crud-mcp-demo-design.md`) cu management de schemă
> via Flyway, audit istoric via Hibernate Envers, și un docker-compose
> complet pentru testare peste PostgreSQL real.

## Scop

Backend-ul folosește azi `ddl-auto: update` (Hibernate creează/modifică
schema automat) și rulează exclusiv pe H2. Această extensie:

1. Trece managementul schemei la Flyway (migrații SQL versionate,
   `ddl-auto: validate`).
2. Adaugă audit istoric (cine/când s-a schimbat o entitate) via Hibernate
   Envers, pe `Employee` și `AppUser`.
3. Adaugă un `docker-compose` complet (Postgres + aplicația
   containerizată) ca să se poată testa tot stack-ul peste o bază de date
   reală, nu doar H2.

## Managementul schemei — Flyway

- `spring.jpa.hibernate.ddl-auto` trece din `update` în `validate`:
  Hibernate verifică la pornire că maparea Java corespunde schemei reale,
  dar nu o mai modifică — Flyway e singura sursă de adevăr pentru schemă.
- Migrațiile (`src/main/resources/db/migration/`) sunt SQL portabil
  (tipuri ANSI standard, coloane `IDENTITY` conform SQL:2003) — aceleași
  fișiere rulează neschimbate pe H2 local și pe PostgreSQL în Docker.
- Testele existente (`@SpringBootTest`) rulează și ele migrațiile Flyway
  la pornirea contextului, pe H2 in-memory — orice greșeală de migrație
  pică toată suita de teste imediat, fără infrastructură suplimentară.
- Coloanele exacte generate de Hibernate/Envers NU sunt ghicite dinainte:
  planul de implementare include un pas explicit în care Hibernate
  generează temporar schema reală (`ddl-auto: create` + export DDL),
  DDL-ul e inspectat și copiat exact în migrația Flyway, apoi `ddl-auto`
  comută definitiv pe `validate`.

## Audit istoric — Hibernate Envers

- `Employee` și `AppUser` devin `@Audited`.
- Envers generează automat (via schema temporară exportată, ca mai sus)
  câte un tabel `_aud` pentru fiecare entitate auditată, plus un tabel
  unic `revinfo` (revizie: id generat, timestamp) — configurația implicită
  Envers, fără informație despre utilizator la acest pas (extensie
  naturală ulterioară, în afara scopului curent).
- Migrațiile Flyway pentru tabelele `_aud`/`revinfo` sunt separate de
  migrațiile pentru tabelele principale (fișiere distincte), ca cele două
  concepte (schema de business vs. schema de audit) să rămână ușor de
  distins în istoricul migrațiilor.

## Docker

- `docker-compose.yml` cu două servicii:
  - `db`: `postgres:16-alpine`, cu volum persistent și variabile de mediu
    pentru user/parolă/nume bază de date.
  - `app`: build local dintr-un `Dockerfile` multi-stage (etapă Maven
    pentru build, apoi imagine JRE minimă pentru rulare) — aplicația
    completă, containerizată.
- Un profil Spring nou, `docker` (`application-docker.yml`), activ doar
  în container, suprascrie:
  - Conexiunea la baza de date: hostname-ul serviciului `db` (nu
    `localhost`), user/parolă din variabile de mediu, driver PostgreSQL.
  - `server.address: 0.0.0.0` — obligatoriu: profilul implicit leagă
    aplicația exclusiv la `127.0.0.1` (decizie de securitate asumată
    pentru demo-ul local), dar într-un container asta ar face aplicația
    inaccesibilă din exterior prin port-mapping-ul Docker.
- Profilul implicit (fără Docker, `mvn spring-boot:run` local) rămâne
  neschimbat: H2, `127.0.0.1`, exact ca azi.
- `pom.xml` capătă dependința `org.postgresql:postgresql` (scope
  `runtime`), pe lângă H2 existent — ambele drivere coexistă, alese prin
  profilul activ.

## Verificare

Pas manual, documentat explicit (nu testare automată nouă — Testcontainers
ar depăși scopul cerut): `docker-compose up --build`, apoi login prin API
(sau prin Angular, pornit separat local) și un ciclu CRUD complet pe
angajați, ca să se confirme că Flyway + Envers + rețeaua containerizată
funcționează împreună peste PostgreSQL real. Testele automate existente
(JUnit, rulate pe H2) rămân neschimbate ca acoperire funcțională; noi
teste JUnit se adaugă doar pentru comportamentul specific Envers (o
modificare pe `Employee` produce o revizie de audit cu valorile vechi).

## În afara scopului

- Username-ul autorului unei modificări în tabelul `revinfo` (Envers
  rămâne la configurația implicită, doar timestamp).
- Testcontainers sau orice testare automată împotriva PostgreSQL —
  verificarea pe Postgres e manuală, via `docker-compose up`.
- Migrarea completă la PostgreSQL ca bază de date implicită — H2 rămâne
  default pentru rularea locală simplă, neschimbată.
- Orchestrare Docker mai avansată (Kubernetes, healthchecks complexe,
  CI/CD) — un `docker-compose.yml` funcțional e suficient pentru scopul
  "să pot testa că merge tot".
