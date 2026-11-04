# Beslutslogg

Våra designbeslut och varför vi tog dem. Den här filen utgår från en kopia från 3 november 2026 och har kompletterats lokalt den 4 november 2026 med beslut om nyckelrotation och native-testning.
Levande version: [Design Beslut vi tagit (Google Docs)](https://docs.google.com/document/d/1mBkqaqmbPnAC48q2r7RIx__4Ojy2nNBGFFncHj-Lgas/edit)

- [Gemensamma beslut](#gemensamma-beslut)
- [Frontend (JavaScript)](#frontend-javascript)
- [Native (C/C++)](#native-cc)
- [Backend (Java)](#backend-java)

## Gemensamma beslut

| Beslut | Anledning |
|---|---|
| **Docker secrets och nyckelhantering: Java** (inte native) | Native C/C++ levererar ett fristående bibliotek utan beroenden och konfiguration, som bara krypterar och avkrypterar efter input. Därför har Java ansvaret för nyckelhantering. |
| **Nyckelrotation för kryptering och audit avgränsas för MVP** | Kräver hantering av flera nyckelversioner så att äldre data kan avkrypteras och auditsignaturer verifieras efter nyckelbyte. Leveransen använder fasta nycklar, och rotationsstöd skjuts upp till fortsatt utveckling. |
| **Fritextsvar på komplettering är inte MVP** | Dokumentuppladdning täcker i de flesta fall det kunden ska skicka in, så den valdes som MVP. Fritext läggs till om tid finns. |
| **Sök på org.nr i handläggarvyn är inte MVP** | MVP är en lista över ärenden som behöver manuell hantering. Sökning läggs till om tid finns. |
| **Ränta är inte MVP** | Ränteberäkning är ett stretch goal. Det kräver ombyggnad av scoring-motorn, och vi behöver först bestämma räntesatser och hur löpande ränteförändringar hanteras. |

## Frontend (JavaScript)

| Beslut | Anledning |
|---|---|
| **Flödesbaserad utvecklingsplan** (features byggs i den ordning användaren/handläggaren möter dem) | Lättare att veta vad nästa steg är, sparar mycket tid. |
| **Sprint 4 lämnas utan utvecklings-issues** | Utrymme för planeringsbrister om något tar längre tid än väntat. |
| **Frontend-koden i en egen mapp**, separat från backend | Separation of concerns och tydligare mappstruktur. |
| **React 19 i stället för 18** | Inbyggda Actions och Hooks för formulär, passar bättre för wizarden. |
| **Relativa API-anrop** (`/api/...`) i stället för full URL i en miljövariabel | Full URL skiljer sig mellan dev och drift. Vår compose startar en webbserver, så React och API:t kommer från samma adress och samma kod fungerar i båda miljöerna. |
| **All API-uppsättning samlad i `src/utils/api.ts`** | Ett ställe att ändra när backend ändras. Komponenter importerar bara klienten. |
| **TanStack Query** | Inbyggd caching och ingen onödig nätverkstrafik. |
| **UI-styling** | Vi prioriterar inte styling på mobil och vissa delar av desktop så länge det fungerar, så att alla funktioner hinner bli klara. |
| **Vitest som testbibliotek** | Samma syntax som Jest men modernare och anpassad för Vite. Mindre konfiguration för samma resultat. |
| **Testfiler ligger bredvid komponenten** (`components/Button/Button.test.tsx`) | Testet raderas samtidigt som komponenten, så inga spöktester lever kvar. Lättare att se vilka tester som finns. |
| **Wizardens ordning: personuppgifter sist** | Statistiskt är mejl och telefonnummer där flest slutar fylla i en wizard. De interaktiva delarna (slider, knappar) kommer först. När kunden redan lagt ner tid ökar chansen att de fyller i resten. |
| **Avvaktar med att kunna avbryta pågående företagsinloggning** | För MVP räcker det att användaren kan återställa formuläret. Väntetiden (4 s) är en simulerad timer och ger inga prestandaproblem. Avbrytning är ett stretch goal. |

## Native (C/C++)

| Beslut | Anledning |
|---|---|
| **Techstack: C++17 med g++** (i stället för C) | Vi använder RAII i C++. C++17 är vanligt i industrin. g++ är vanligt, fungerar bra och är det vi kan. |
| **Automatiska native-tester enbart på Linux; Windows testas manuellt i MVP** | Ubuntu 22.04 och 24.04 täcker automatiska bygg- och funktionstester. När övrig implementation var klar återstod få kodändringar, och manuell Windows-testning bedömdes räcka inom tidsramen. Automatiska Windows-tester avgränsas för MVP. |
| **AES-256-GCM** | Mycket säker variant av AES-256 och industristandard för banksystem. ChaCha20-Poly1305 var ett alternativ, men AES-256-GCM är vanligare, följer ett verifierat säkerhetskoncept och uppfyller regelverk inom EU/Norden. |
| **Digital signatur: Ed25519** (i stället för RSA eller ECDSA) | RSA (2048/3072) har universellt stöd och är relevant mot äldre bankinfrastruktur, men har större nycklar och signaturer. ECDSA ger RSA-3072-nivå med mindre nycklar men kräver val av kurva, hash och nonce-hantering. Ed25519 har samma säkerhetsnivå, 32 byte publik nyckel och 64 byte signatur, är deterministisk och låst (inga val att göra). Utan legacy-krav föredrar vi Ed25519. |
| **Hash: SHA-256** | Väletablerad i säkerhetssystem, brett stöd, smidig. |
| **En hashkedja per kreditansökan** (inte för hela databasen) | Om något strular behöver bara ett ärende låsas, inte hela databasen. Kedjan verifieras i slutet av varje ärende, med möjlighet att köra med intervall (dagligen, veckovis). Bättre skalbarhet och färre problem med låsning/samtidighet. |
| **Första `previousHash` är 32 nollbytes** | 32 byte är storleken på SHA-256. En programspecifik ID-sträng var ett alternativ men kändes överdrivet. |
| **AuditEntry kanonikaliseras enligt RFC 8785 före hashning** | Hashning sker på bytes. `{a: 1, b: 2}` och `{b: 2, a: 1}` är samma data men olika bytes, och ger olika hash. Java formaterar AuditEntry enligt RFC 8785 (UTF-8 som sista steg). |
| **Tidsstämplar: RFC 3339** (i stället för RFC 3161) | RFC 3339 är vanligt (`2026-09-01T11:11:11Z`). RFC 3161 är ett kryptografiskt protokoll som kräver integration mot en extern Time-Stamp Authority. |
| **Skydd mot trunkering av kedjan** | Om en angripare skriver om de sista stegen i ett ärende märks det inte. Därför loggas `CASE_CLOSED` och `CASE_REOPENED` (inte `FINAL_DECISION`, eftersom ärenden kan överklagas och öppnas igen). Native ger grunden. Fullt skydd kräver att Java sparar checkpoints externt (t.ex. `applicationId`, `entryCount`, `lastHash`, tidsstämpel), så att en omskriven kedja inte matchar. Extern förankring avgränsas för nu. |
| **Vad som hashas och signeras med Ed25519** | Alla AuditEntry-fält hashas utom `currentHash` och `signature` (de är resultaten). Signeringen görs på `currentHash` med privata nyckeln. Eftersom `currentHash` bygger på alla fält, inklusive `previousHash` och `sequenceNumber`, kan inget fält ändras utan att verifieringen fallerar. |

## Backend (Java)

| Beslut | Anledning |
|---|---|
| **Lagerarkitektur:** controllers, services, repositories, `SecurityFilterChain` | Separation of concerns, tydlig mappstruktur och tydligt ansvar. |
| **Fler services:** `ApplicationService`, `DocumentService`, `BankIdService`, `StatusUpdateService`, `AuthService`, `CreditReportService`, `AuditService`, `NotificationService`, `ScoringService`, `CompanyValidationService` | Varje service har ett tydligt ansvar, blir lättare att testa isolerat, och flera i teamet kan jobba parallellt utan att krocka i samma fil. |
| **Nytt repository: `AuditEventRepository`** | Vi behöver logga skapad ansökan, automatisk scoring, manuellt beslut och uppladdade dokument. |
| **Spring Boot 4.x i stället för 3.x** | 3.x nådde end-of-life 30 juni 2026 och får inga säkerhetsuppdateringar. Projektet handlar om att åtgärda säkerhetsbrister, så att bygga på en opatchad version är inte logiskt. |
| **`CreditReportService` som interface** (mock och skarp implementation) | Följer SOLID. Tjänsten sköter HTTP-anrop mot UC, mappar svaret till vår modell, autentiserar med API-nyckel och hanterar fel med tydliga undantag. Det är inte `ApplicationService` ansvar. |
| **`BankIdService` som interface** (mock och skarp implementation) | `AuthService` behöver inte veta hur BankID fungerar, bara fråga via ett gränssnitt (`isApproved(orgNumber)`). Mocken kan bytas mot ett riktigt anrop utan att röra `AuthService`. Mocken följer formatet på BankID:s `/collect`. |
| **Enkelriktade relationer från Application** | Application har 1–M till både Document och AuditEvent. De är separata ansvarsområden utan stark koppling, så relationerna är enkelriktade i JPA. |
| **Företagsvalidering och finansdata hämtas externt** | Mycket av datan som frontend skickade i v1 behövs inte. Tack vare offentlighetsprincipen finns den via Bolagsverket (Bolags-API har öppen dokumentation och de flesta fält vi behöver). Resten mockas i MVP. Vi sparar aldrig datan, på grund av GDPR och kreditupplysningslagen. |
| **Paginering av backoffice-listorna** med Spring Data JPA `Pageable`/`Page` | `known-bugs.md` #10: `BackofficeController` hämtade alla ärenden i ett anrop, vilket gav minnesproblem vid hög belastning. Både ärenden under granskning och avgjorda ärenden pagineras, med ett tak på `MAX_PAGE_SIZE = 100`. En relaterad bugg i seed-datan (saknat `version`-värde som bröt optimistic locking) fixades i `seed.sql`. |
| **`TIMESTAMP` byts mot `TIMESTAMPTZ`** | `LocalDateTime.now()` saknar tidszon. `Instant.now()` är samma för alla, och tiden visas sedan i användarens lokala tid. |
| **Skyddad identitet** (kundens feedforward) | Hanteras via en mockad extern klient som efterliknar SPAR/Navet. Ansökningar markeras som känsliga och filtreras på databasnivå innan de visas. Har användaren behörighet finns fältet `protectedResource`, annars tas det bort innan svaret skickas till frontend. |
| **Skalbarhet** (kundens feedforward) | Vi skalar enligt CIA (konfidentialitet, integritet, tillgänglighet) efter kundens feedforward, med caching, rate limiting och circuit breaker för färre anrop och stabil trafik. |
