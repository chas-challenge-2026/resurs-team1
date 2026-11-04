# Så bearbetade vi kundens feedforward

Efter livedemon för Resurs (22 oktober) sammanfattade vi kundens feedforward i en omtagsplan
(26 oktober), som vi gick igenom med vår PL dagen efter och sedan arbetade efter fram till slutleveransen.

## Det viktigaste från kunden, och vad vi gjorde

| Feedforward | Vad vi gjorde | Status |
|---|---|---|
| **Hantera skyddad identitet** | Mockad SPAR-klient kontrollerar om någon i bolaget har skyddad identitet (#393). Ärendet markeras som känsligt, filtreras bort på databasnivå för obehöriga (#398, #402, #414) och syns med en badge för behöriga handläggare (#405). Finansiella uppgifter döljs (#416). | ✅ Klart |
| **Egen inloggning för handläggare, tydligt skild från kundvyn** | Handläggarna loggar in på `/handlaggare` och har ett eget utseende, så det syns direkt vilken vy man är i (#397, #403). | ✅ Klart |
| **Handläggarroller** | Olika roller, där bara behöriga handläggare ser skyddade ärenden (#392, #399). | ✅ Klart |
| **Skalbarhet vid hög trafik** | Caching av scoring (#389), circuit breaker mot externa register (#361) och rate limiting. | ✅ Klart |

## Vad varje del av teamet prioriterade

- **Backend (Java):** handläggarroller och skyddad identitet, caching och hantering av hög trafik.
- **Native (C/C++):** testning, automatisering och kvalitet. Modulerna byggs och testas automatiskt i CI (#394), med utökad testtäckning (#396) och dokumentation (#395).
- **Frontend:** separata kund- och handläggarvyer, eget färgtema för handläggare och UI för ärenden med skyddad identitet.

## Det vi medvetet inte gjorde

| Avgränsning | Varför |
|---|---|
| Ändra typen av feedback till kunden | Vi bedömde att kunden redan får rätt feedback vid rätt tillfälle. |
| Endpoint för att ändra scoring-gränsvärden | Gränsvärdena ändras sällan och kan ändras i koden. Inte prioriterat. |

## Läget vid omtagsplanen, och nu

| Då (26 okt) | Nu |
|---|---|
| Många tester, men gamla kommentarer och dokumentation behövde ses över | Kommentarer rensade (#406) och dokumentationen uppdaterad (#407, #413, #415). |
| Databasen sparades inte mellan omstarter | Databasen sparas i en Docker-volym (`postgres_data`). |
| Uppladdade filer sparas inte permanent | Kvarstår, se [kända brister](kanda-brister.md). |
| Hemsidan var inte publicerad | Publicerad på stage och prod, se [README](../README.md#miljöer). |
