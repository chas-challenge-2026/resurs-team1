# Kända buggar och säkerhetsproblem – v1

## Säkerhetsproblem

### 1. SQL Injection – CaseWorker-login
**Fil:** `AuthController.java`
**Kod:** `"SELECT * FROM case_workers WHERE email = '" + email + "' AND password_md5 = '" + md5 + "'"`
**Risk:** Fullständig databaskontroll via e-postfältet (t.ex. `' OR '1'='1`)

### 2. PII i klartext
**Fil:** `ApplicationController.java`, databas
**Problem:** Firmanamn, organisationsnummer och firmatecknare lagras okrypterat i PostgreSQL
**Kommentar i koden:** `// TODO: encrypt PII before go-live`

### 3. MD5-lösenord
**Fil:** `AuthController.java`, `infra/seed.sql`
**Problem:** MD5 är kryptografiskt bruten, inget salt, rainbow table-sårbar

### 4. BankID-mock som hårdkodad if-sats
**Fil:** `AuthController.java`
**Kod:** `if (orgNumber.equals("556000-1234") || orgNumber.equals("556000-5678"))`
**Risk:** Vem som helst som känner till ett giltigt org.nummer kan logga in

## Dataintegritsproblem

### 5. Ingen transaktion vid ansökningsskapande
**Fil:** `ApplicationController.java`, POST `/apply`
**Problem:** Tre separata INSERT-satser (company, application, audit_log) utan BEGIN/COMMIT.
Vid krasch halvvägs skapas inkonsistenta data.

### 6. Audit log ej sökbar/indexerad
**Problem:** `audit_log TEXT` är ett JSON-blob i applications-raden.
Omöjligt att söka efter händelsetyp, filtrera på datum eller göra aggregationer.
Uppdateras via manuell stränghackning: `currentLog.substring(0, currentLog.lastIndexOf("]"))`

## Affärslogik-buggar

### 7. Inkonsistenta soliditetströsklar
**Fil:** `ApplicationController.java`
- Rad ~115: `if (soliditet < 0.20)` → hard reject
- Rad ~122: `if (soliditet < 0.25)` → flagg
- Rad ~160: `if (soliditet < 0.30 && requestedAmount > 1000000)` → flagg (tredje tröskel!)
- `soliditetCategory()` metod: `if (soliditet < 0.15)` → KRITISK (fjärde tröskel, aldrig anropad)

### 8. PDF parsas inte
**Fil:** `DocumentController.java`
**Kommentar:** `// TODO: implement PDF parsing in v2 (see native/README.md)`
Filen sparas i /tmp/uploads/ men innehållet läses aldrig. Scoring baseras enbart på manuellt inmatade nyckeltal.

## Driftsproblem

### 9. Filer i /tmp
Uppladdade PDF-filer sparas i `/tmp/uploads/`. Rensas vid container-omstart.
Användare kan se dokument i DB men inte ladda ner dem efter omstart.

### 10. Ingen pagination
`BackofficeController.java` hämtar ALLA UNDER_REVIEW-ansökningar utan LIMIT.
Vid hög volym → minnesproblem och långsamma svar.
