# Resurs Kreditportal – Frontend

React-app för företagskunder (ansöka om kredit, följa ansökningar) och handläggare (granska ärenden).

## Kom igång

**Krav:** Node 22 och backend igång via Docker (se [README](../README.md)).

```bash
cd frontend
cp .env.example .env   # första gången
npm install            # första gången
npm run dev
```

Öppna [http://localhost:5173](http://localhost:5173). Anrop till `/api` skickas vidare till backend på port 8083.

## Skript

| Kommando          | Gör                                  |
|-------------------|--------------------------------------|
| `npm run dev`     | Startar dev-servern                  |
| `npm test`        | Kör testerna (Vitest)                |
| `npm run lint`    | Kollar koden med ESLint              |
| `npm run build`   | Typkollar och bygger för produktion  |
| `npm run preview` | Visar produktionsbygget lokalt       |

## Miljövariabler

| Variabel       | Värde  | Beskrivning              |
|----------------|--------|--------------------------|
| `VITE_API_URL` | `/api` | Bas-URL för API-anrop    |

## Teknik

| Bibliotek                | Används till                  |
|--------------------------|-------------------------------|
| React 19 + TypeScript    | UI                            |
| Vite                     | Dev-server och bygge          |
| React Router             | Routing                       |
| TanStack Query + Axios   | Hämta och cacha data från API |
| Sonner                   | Toast-notiser                 |
| React Icons              | Ikoner                        |
| Vitest + Testing Library | Tester                        |

## Mappstruktur

```
src/
  api/          ← API-anrop (ett filnamn per område)
  assets/       ← bilder och logotyper
  components/   ← återanvändbara komponenter
  constants/    ← konstanter
  hooks/        ← egna hooks (useUser, useApplication …)
  layouts/      ← sidlayouter
  pages/        ← en mapp per sida
  types/        ← delade TypeScript-typer
  utils/        ← hjälpfunktioner (formatering, validering)
```

Varje komponent har en egen mapp med `.tsx`, `.module.css` och eventuell `.test.tsx`.

## Roller och sidor

| Sida                     | Vem                     |
|--------------------------|-------------------------|
| `/`                      | Inloggning, företag     |
| `/handlaggare`           | Inloggning, handläggare |
| `/oversikt`              | `COMPANY`               |
| `/kreditansokan`         | `COMPANY`               |
| `/mina-ansokningar/:id?` | `COMPANY`               |
| `/arenden/:id?`          | `CASEWORKER`            |

`ProtectedRoute` skickar utloggade till `/` och visar 404 vid fel roll. `CERTIFIED` och `ADMIN` läggs till ovanpå `CASEWORKER` och styr främst behörighet i backend.

Testinloggningar finns i [README](../README.md#testinloggningar).
