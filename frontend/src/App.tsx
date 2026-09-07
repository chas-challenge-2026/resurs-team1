import { useState } from "react"
import Header from "./components/Header/Header"
import Button from "./components/Button/Button"
import Loading from "./components/Loading/Loading"
import StatusTag from "./components/StatusTag/StatusTag"
import ToggleSwitch from "./components/ToggleSwitch/ToggleSwitch"
import type { Option } from "./components/ToggleSwitch/ToggleSwitch"
import { Card } from "./components/Card/Card"
import Dropdown from "./components/Dropdown/Dropdown"
import type { DropdownOption } from "./components/Dropdown/Dropdown"

type UserRole = "COMPANY" | "AGENT";

const options: Option<UserRole>[] = [
{ label: "Företag", value: "COMPANY" },
{ label: "Handläggare", value: "AGENT" },
]

const reasonOptions: DropdownOption[] = [
  { value: "renovering", label: "Renovering" },
  { value: "fruktkop", label: "Fruktköp" },
  { value: "ovrigt", label: "Övrigt" },
]

function App() {
  const [role, setRole] = useState<UserRole>("COMPANY");
  const [reason, setReason] = useState("");

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
          test knapp
        <Button variant="ghost">
        <StatusTag status="approved"/>
        </Button>
      <Dropdown
        id="selectReason"
        label="Ange orsak för lån"
        placeholder="Välj orsak..."
        options={reasonOptions}
        value={reason}
        onChange={setReason}
      />
        <ToggleSwitch name="userRole" options={options} selectedValue={role} onChange={(newRole) => setRole(newRole)} />
      </Card>
    </main>
    </>
  )
}

export default App
