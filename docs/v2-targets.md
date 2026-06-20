# v2 Refaktoreringsmål

## Teknisk stack

| Komponent | v1 (nuvarande) | v2 (mål) |
|-----------|----------------|-----------|
| Spring Boot | 2.7.18 | 3.x |
| Java | 11 | 21 |
| Databasåtkomst | JdbcTemplate i controllers | JPA/Hibernate, repository-pattern |
| Frontend | Thymeleaf + Bootstrap 3 + jQuery | React 18 wizard |
| Auth | BankID mock (hardcoded) | Riktig BankID-integration |
| Lösenord | MD5 | bcrypt via Spring Security |
| Audit log | JSON-blob i TEXT-kolumn | Separat audit_log-tabell med index |
| PDF-parsning | Ej implementerad | C/C++ via JNA (se native/README.md) |
| PII | Klartext | Krypterat på hot path |
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
                  │    └── NotificationService (e-post via Spring Mail)
                  ├── Repository (JPA, Spring Data)
                  └── JNA Bridge → native/libresurs.so (PDF-parser, PII-kryptering)
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
- Bygg C/C++ PDF-parser för K2/K3-årsredovisningar
- Exponera via JNA bridge (`libresurs.so`)
- PII-kryptering på hot path med separat nyckellagring

### 5. Riktig BankID
- Integrera med Bankgirot/BankID Open API
- Implementera QR-kod-flöde
- Validera juridisk firmatecknarbehörighet

### 6. @Transactional
- Wrappa application-skapande (company + application + audit) i en transaktion
- Lägg till optimistic locking på applications-entitet

### 7. E-postnotifiering
- Implementera Spring Mail
- Skicka bekräftelse vid ansökan, notifiering vid beslut
