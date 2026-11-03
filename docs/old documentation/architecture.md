# Arkitektur – Resurs Kreditansökan v1

## Översikt

Spring Boot 2.7 monolith. All logik i controllers. Ingen service-lager. Ingen repository-lager.

## Stack

- **Backend:** Spring Boot 2.7.18, Java 11
- **Databas:** PostgreSQL 12
- **Template:** Thymeleaf + Bootstrap 3 (CDN)
- **Frontend:** jQuery multi-step form (steps.js)
- **ORM:** JdbcTemplate direkt i controllers
- **Auth:** BankID mock (hardcoded if-statement) + MD5-lösenord

## Komponentdiagram (textform)

```
Browser
  └── HTTP → Spring Boot (port 8083)
               ├── AuthController     → JdbcTemplate → PostgreSQL
               ├── ApplicationController → JdbcTemplate → PostgreSQL
               ├── DocumentController → /tmp/uploads/ + JdbcTemplate → PostgreSQL
               ├── StatusController   → JdbcTemplate → PostgreSQL
               └── BackofficeController → JdbcTemplate → PostgreSQL
```

## Kända arkitekturproblem (pedagogiska)

1. **Ingen service-lager** — all affärslogik direkt i controllers
2. **Audit log som JSON-blob** — `audit_log TEXT` på applications-raden, ingen separat tabell, ingen index
3. **Filer i /tmp** — rensas vid omstart, ej persistent
4. **BankID mock** — hårdkodade org.nummer i if-sats
5. **MD5-lösenord** — svag hash, ingen salt
6. **PII i klartext** — org.nummer, firmanamn, firmatecknare okrypterade
7. **SQL injection** — case worker-login bygger SQL med strängkonkatenering
8. **Ingen transaktion** — tre separata INSERTs utan BEGIN/COMMIT
9. **Magic numbers** — soliditetströskel är 0.25, 0.20 OCH 0.30 på olika ställen
10. **Session-check copy-pasteat** — `if (session.getAttribute("userId") == null)` i varje metod
