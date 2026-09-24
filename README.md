# Resurs Kreditansökan

B2B-kreditansökningsportal för Resurs Bank. Företag ansöker om kredit, laddar upp årsredovisning, och får ett kreditbeslut baserat på finansiella nyckeltal.

## Snabbstart

```bash
cd infra && docker compose up
```

Öppna [http://localhost:8083](http://localhost:8083)

### API-dokumentation
Swagger UI nås på denna länk: http://localhost:8083/swagger-ui/index.html

### Testinloggningar

| Roll | Uppgifter | FirmaTeknare(Företag)                     |
|------|-----------|-------------------------------------------|
| Företag (Fasen Elteknik AB) | Org.nr: `556000-1234` | Anders Karlsson<br/>Pers#: `750312-1234` |
| Företag (Ögonfransar AB) | Org.nr: `556000-5678` | Maria Svensson<br/>Pers#: `820624-5678` |
| Företag (Gunnar Kruts Dynamit AB) | Org.nr: `556000-7777` | Johan Berg<br/>Pers#: `660930-7777` |
| Företag (Frukt och grönt Göteborg) | Org.nr: `556000-9999` | Erik Lindqvist<br/>Pers#: `681105-9999` |
| Handläggare | `karin@resurs.se` / `password123` | |

## Mappstruktur

```
backend/ResursPortal/   ← Spring Boot 2.7 Maven-projekt
  src/main/java/se/comerit/resurs/
    ResursPortalApplication.java
    controller/
      AuthController.java        ← BankID mock + MD5-login
      ApplicationController.java ← 800+ rader scoring-logik inline
      DocumentController.java    ← filuppladdning (PDF parsas ej)
      StatusController.java      ← status + hårdkodade ETAer
      BackofficeController.java  ← handläggargränssnitt
  src/main/resources/
    application.properties
    templates/                   ← Thymeleaf + Bootstrap 3
    static/steps.js              ← jQuery multi-step logic

infra/
  docker-compose.yml             ← PostgreSQL + Spring Boot
  seed.sql                       ← schema + seed-data

native/
  README.md                      ← v2 C/C++ moduler (PII-kryptering, audit-signering)

docs/
  architecture.md
  known-bugs.md
  README-pain-points.md
  v2-targets.md
```

## Avsiktliga anti-patterns (pedagogiska)

Detta är en **v1 spaghetti-kodbas** avsedd för studenter att refaktorera till v2.

Se `docs/known-bugs.md` för fullständig lista. Highlights:

1. BankID mock som hårdkodad if-sats
2. 800+ raders scoring-metod inline i controller
3. SQL injection i handläggare-login
4. Audit log som JSON-blob (ingen separat tabell)
5. JdbcTemplate direkt i varje controller
6. PDF sparas men parsas aldrig
7. PII i klartext
8. MD5-lösenord
9. Ingen transaktion vid ansökningsskapande
10. Session-check copy-pastad i varje metod

## Vad ska ni bygga

Se `docs/v2-targets.md`.
