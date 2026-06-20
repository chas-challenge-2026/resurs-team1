# Pain Points – Resurs Kreditansökan v1

## Vad som fungerar

- **Grundläggande ansökningsflöde:** Företag loggar in (BankID mock) → fyller i finansiella nyckeltal → scoring körs → beslut returneras
- **Scoringbeslut:** APPROVED / UNDER_REVIEW / REJECTED med förklaring
- **Dokumentuppladdning:** Filer laddas upp och sparas (men parsas inte)
- **Handläggargränssnitt:** Karin kan se UNDER_REVIEW-ansökningar och fatta beslut
- **Audit log:** Händelser loggas (om än som JSON-blob)

## Vad som går sönder

### Samtida ansökningar
Ingen locking. Om två användare från samma org.nummer skickar ansökan samtidigt kan company-INSERT köras dubbelt (UNIQUE-constraint kastar exception) och lämna en halvfärdig application-rad.

### PDF-parsning saknas
Systemet accepterar PDF-filer men läser dem aldrig. Scoring baseras på manuellt inmatade siffror — ett fel i formuläret kan leda till felaktigt beslut. Årsredovisningens faktiska innehåll verifieras aldrig.

### Audit log inte sökbar
`audit_log TEXT` är ett JSON-blob. Går inte att köra `SELECT * FROM applications WHERE audit_log LIKE '%SCORING_RUN%'` effektivt. Full table scan. Kan inte aggregera händelsetyper.

### /tmp rensas
Uppladdade filer försvinner vid container-omstart. Dokument syns i DB men är nedladdningsbara 404.

### Magic numbers i scoring
Soliditetströskeln finns på tre olika ställen med tre olika värden (0.20, 0.25, 0.30). Ändring av affärsregel kräver sökning i hela filen.

### Session check copy-paste
10+ kopior av `if (session.getAttribute("userId") == null) return "redirect:/login"`. En Spring HandlerInterceptor hade hanterat detta på ett ställe.

### Ingen e-postnotifiering
Handläggaren fattar beslut → företaget får ingen notifiering. Måste logga in och kolla portalen manuellt.

### SQL injection
Case worker-login är sårbart. Orsakas av strängkonkatenering istället för PreparedStatement-parametrar.
