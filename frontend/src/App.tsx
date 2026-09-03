import { useState } from "react"
import Button from "./components/Button/Button"
import Header from "./components/Header/Header"
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

const dropdownOptions: DropdownOption[] = [
{ label: "Renovering", value: "renovering" },
{ label: "Fruktköp", value: "fruktkop" },
{ label: "Övrigt", value: "ovrigt", freeText: true },
]

function App() {
  const [role, setRole] = useState<UserRole>("COMPANY");

  const [purposes, setPurposes] = useState<string[]>([]);
  const [ownWording, setOwnWording] = useState("");

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
      label="ange orsak för lån"
      placeholder="Välj orsak..."
      options={dropdownOptions}
      value={purposes}
      onChange={setPurposes}
      freeTextValue={ownWording}
      onFreeTextChange={setOwnWording}
      />
    </main>
    </>
  )
}

export default App
