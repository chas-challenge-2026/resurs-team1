import { useState } from "react"
import Button from "./components/Button/Button"
import Header from "./components/Header/Header"
import Loading from "./components/Loading/Loading"
import StatusTag from "./components/StatusTag/StatusTag"
import ToggleSwitch from "./components/ToggleSwitch/ToggleSwitch"
import type { SwitchOption } from "./components/ToggleSwitch/ToggleSwitch"
import { Card } from "./components/Card/Card"
import ButtonGroup from "./components/ButtonGroup/ButtonGroup"

type UserRole = "COMPANY" | "AGENT";

const SWITCH_OPTIONS: SwitchOption<UserRole>[] = [
{ label: "Företag", value: "COMPANY" },
{ label: "Handläggare", value: "AGENT" },
]

const TENURE_OPTIONS = [
  { label: "12 mån", value: 12 },
  { label: "24 mån", value: 24 },
  { label: "36 mån", value: 36 },
  { label: "48 mån", value: 48 },
  { label: "60 mån", value: 60 },
] as const

type TenureValue = typeof TENURE_OPTIONS[number]["value"];

function App() {
  const [role, setRole] = useState<UserRole>("COMPANY");
  const [tenure, setTenure] = useState<TenureValue | undefined>(undefined)

  return (
    <>
    <Header company="Coconut AB" onLogout={() => {}}>
      <input type="search" />
    </Header>
    <main>
      <Card>
        <h1 className="title">Titel</h1>
        <p className="subtitle">Undertitel</p>
        <Loading size="lg"/>
        <Button variant="ghost">
          test knapp
        </Button>
        <StatusTag status="approved"/>
        <ToggleSwitch name="userRole" options={SWITCH_OPTIONS} selectedValue={role} onChange={(newRole) => setRole(newRole)} />
        <ButtonGroup<TenureValue>
          name="tenure"
          label="Önskad återbetalningstid (månad)"
          options={TENURE_OPTIONS}
          selectedValue={tenure}
          onChange={setTenure}
        />
      </Card>
    </main>
    </>
  )
}

export default App
