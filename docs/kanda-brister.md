# Kända brister och avgränsningar

Det här fungerar inte fullt ut, eller har vi medvetet valt bort, i den nuvarande versionen.

## Kända brister

| Brist | Konsekvens | Hur vi hanterar det nu |
|---|---|---|
| **Uppladdade filer sparas i `/tmp`** (v1-bugg #9, `DocumentService.java`) | Dokument försvinner när backend-containern startas om. | Fungerar för demo. Behöver flyttas till permanent lagring (volym eller objektlagring). |
| **Uppladdade PDF:er läses inte maskinellt** (v1-bugg #8) | Scoringen tar inte hänsyn till innehållet i dokumenten. | Handläggaren granskar dokumenten manuellt. Inte del av v2-scopet. |
| **Ärenden med skyddad identitet kan inte scoras automatiskt** | Externa register lämnar inte ut uppgifterna, så ärendet hamnar alltid hos en handläggare. | Ärendet markeras som känsligt och går till manuell granskning, på samma sätt som när ett externt register är nere. Ett automatiskt utskick till kunden har vi inte hunnit med. |
| **Lösenord till databasen står i klartext i `docker-compose.yml`** | Okej lokalt, men inte i skarp drift. | Riktiga hemligheter ska ligga i Docker secrets eller ett valv i drift, se [DRIFT.md](../DRIFT.md). |

## Mockade externa tjänster

Vi har inga avtal med de riktiga tjänsterna, så de är mockade bakom interface och kan bytas mot skarpa anrop utan att resten av koden ändras (se [beslutsloggen](beslutslogg.md#backend-java)):

- **BankID**: inloggning och signering
- **UC**: kreditupplysning
- **Bolagsverket**: företags- och finansdata
- **SPAR/Navet**: skyddad identitet

## Medvetna avgränsningar

| Avgränsning | Varför |
|---|---|
| Ränteberäkning | Kräver ombyggnad av scoring-motorn och beslut om räntesatser. Stretch goal. |
| Fritextsvar på komplettering | Dokumentuppladdning täcker det mesta. Stretch goal. |
| Sökning på org.nr i handläggarvyn | MVP är en lista över ärenden som behöver granskas. |
| Endpoint för att ändra scoring-gränsvärden | Ändras sällan och kan ändras i koden. |
| Endpoint för att köra om scoring på ett befintligt ärende (#321) | Inte hunnet. |
| Varningen "Att låna kostar pengar" (#381) | Stretch goal, inte hunnet. |
| Redis som delad cache (#284) | Caching och rate limiting körs i minnet, vilket räcker för en instans. Redis behövs först när vi skalar ut till flera instanser. |
| Avbryta en pågående företagsinloggning | Formuläret kan återställas, och väntetiden är simulerad. |
| Bara svenska | Antagande för MVP. |
| Bara ljust läge | Antagande för MVP. |
| Handläggarvyn är byggd för dator | Fungerar på mobil, men är inte optimerad för det. |
| Tillgänglighet testad manuellt | Skärmläsare och tangentbordsnavigering är testade för hand, inte med automatiska verktyg. |
| Nyckelrotation för kryptering och audit | Stöd för rotation av krypterings- och signeringsnycklar ingår inte i MVP. Vi använder konfigurerade nycklar utan hantering av flera nyckelversioner. Fortsatt utveckling behöver säkerställa att äldre data kan avkrypteras och äldre auditsignaturer verifieras även efter nyckelbyte. Se [beslutsloggen](beslutslogg.md#gemensamma-beslut). |
| Automatiska native-tester enbart på Ubuntu 22.04 och 24.04 | Automatiska Windows-byggen och tester avgränsades bort på grund av tidsbrist. När övrig implementation var färdig återstod få kodändringar, och manuell Windows-testning bedömdes räcka för MVP. Linux-tester validerar inte Windows-DLL:erna; separat Windows-verifiering behövs vid relevanta ändringar. Se [beslutsloggen](beslutslogg.md#native-cc). |
| Extern lagring av checkpoints för hashkedjan | Native ger grunden (hashkedja och signatur), men skydd mot att någon med databasåtkomst skriver om hela kedjan kräver extern lagring. Avgränsat för nu. |
