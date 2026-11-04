# v2 Refaktoreringsmål

## Teknisk stack

| Komponent | v1 (nuvarande) | v2 (mål) |
|-----------|----------------|-----------|
| Spring Boot | 2.7.18 | 3.x |
| Java | 11 | 21 |
| Databasåtkomst | JdbcTemplate i controllers | JPA/Hibernate, repository-pattern |
| Frontend | Thymeleaf + Bootstrap 3 + jQuery | React 18 wizard |
| Auth | BankID mock (hardcoded) | BankID-mock lyft till egen service, utbytbar mot skarp integration senare |
| Lösenord | MD5 | bcrypt via Spring Security |
| Audit log | JSON-blob i TEXT-kolumn | Separat audit_log-tabell med index, hashkedja för manipulationsdetektion |
| Företagsvalidering | Mockad utan felhantering | Egen service med tydligt klientgränssnitt, mockat i MVP, utbytbart mot skarpa anrop |
| PII | Klartext | Krypterat på hot path (AES-256 eller motsvarande, C/C++ via JNA, se native/README.md) |
| Transaktioner | Ingen | @Transactional på service-lager |
| Session-check | Copy-paste i varje metod | Spring Security filter chain |

## Arkitekturmål v2

```
Browser (React 18)
  └── REST API → Spring Boot 3 (Java 21)
                  ├── SecurityFilterChain (session/JWT)
                  ├── Controller (tunn, ingen affärslogik)
                  ├── Service (@Transactional, affärslogik)
                  │    ├── ScoringService (konfigurerbara tröskeltar)
                  │    ├── AuditService (skriver till audit_log-tabell)
                  │    ├── CompanyValidationService (klientgränssnitt, mockat i MVP)
                  │    ├── BankIdService (mockad klient, utbytbar)
                  │    └── NotificationService (e-post via Spring Mail)
                  ├── Repository (JPA, Spring Data)
                  └── JNA Bridge → native/libresurs.so (PII-kryptering, audit-signering)
```

## Specifika refaktoreringsuppgifter

### 1. Extrahera ScoringService
- Flytta scoring-logiken från ApplicationController (800+ rader) till en separat `ScoringService`
- Definiera `ScoringThresholds` som en konfigurerbar klass (application.properties)
- Enhetstesta alla trösklar

### 2. Separat audit_log-tabell
```sql
CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT REFERENCES applications(id),
    ts TIMESTAMP NOT NULL DEFAULT NOW(),
    action VARCHAR(50) NOT NULL,
    actor VARCHAR(100),
    details JSONB,
    INDEX idx_audit_application_id (application_id),
    INDEX idx_audit_action (action)
);
```

### 3. Spring Security
- Ersätt session-check copy-paste med `SecurityFilterChain`
- Byt MD5 mot `BCryptPasswordEncoder`
- Implementera role-baserad åtkomst (`@PreAuthorize`)

### 4. JNA-integration (native/)
- Bygg C/C++-modul för kryptering av känsliga uppgifter (org.nr, personuppgifter, finansiell info) med AES-256 eller motsvarande, separat nyckellagring på hot path
- Bygg C/C++-modul för säker audit-signering: hashkedjor som upptäcker manipulation av audit-loggen
- Exponera båda via JNA bridge (`libresurs.so`)

### 5. BankID-mock som egen service
- Lyft BankID-mock ur `AuthController` till en egen service med tydligt klientgränssnitt
- Behåll mock (happy path) i v2, gör den utbytbar mot en skarp BankID-integration senare
- Validera juridisk firmatecknarbehörighet i mock-flödet

### 6. @Transactional
- Wrappa application-skapande (company + application + audit) i en transaktion
- Lägg till optimistic locking på applications-entitet

### 7. E-postnotifiering
- Implementera Spring Mail
- Skicka bekräftelse vid ansökan, notifiering vid beslut

### 8. Extern företagsvalidering som egen service
- Lyft företagsvalidering ur controller-lagret till en egen service med tydligt klientgränssnitt
- Mocka i MVP (registreringsstatus, moms- och F-skattestatus), gör utbytbar mot skarpa anrop senare
