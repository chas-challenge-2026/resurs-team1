import { useState } from "react"
import Button from "./components/Button/Button"
import Header from "./components/Header/Header"
import Loading from "./components/Loading/Loading"
import StatusTag from "./components/StatusTag/StatusTag"
import ToggleSwitch from "./components/ToggleSwitch/ToggleSwitch"
import type { Option } from "./components/ToggleSwitch/ToggleSwitch"
import { Card } from "./components/Card/Card"
import Dropdown from "./components/Dropdown/Dropdown"

type UserRole = "COMPANY" | "AGENT";

const options: Option<UserRole>[] = [
{ label: "Företag", value: "COMPANY" },
{ label: "Handläggare", value: "AGENT" },
]

function App() {
  const [role, setRole] = useState<UserRole>("COMPANY");

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
        <ToggleSwitch name="userRole" options={options} selectedValue={role} onChange={(newRole) => setRole(newRole)} />
      </Card>
      <Dropdown
  id="selectReason"
  label="Ange orsak för lån"
  options={[
    { value: "renovering", label: "Renovering" },
    { value: "fruktkop", label: "Fruktköp" },
    { value: "ovrigt", label: "Övrigt" },
  ]}
/>
    </main>
    </>
  )
}

export default App
