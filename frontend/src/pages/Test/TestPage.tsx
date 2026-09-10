import { useState } from "react"
import Button from "../../components/Button/Button"
import Loading from "../../components/Loading/Loading"
import StatusTag from "../../components/StatusTag/StatusTag"
import ToggleSwitch from "../../components/ToggleSwitch/ToggleSwitch"
import type { SwitchOption } from "../../components/ToggleSwitch/ToggleSwitch"
import { Card } from "../../components/Card/Card"
import Dropdown from "../../components/Dropdown/Dropdown"
import type { DropdownOption } from "../../components/Dropdown/Dropdown"
import ButtonGroup from "../../components/ButtonGroup/ButtonGroup"
import Slider from "../../components/Slider/Slider"
import type { UserRole } from "../../types/user"

const SWITCH_OPTIONS: SwitchOption<UserRole>[] = [
{ label: "Företag", value: "company" },
{ label: "Handläggare", value: "caseWorker" },
]

const reasonOptions: DropdownOption[] = [
  { value: "renovering", label: "Renovering" },
  { value: "fruktkop", label: "Fruktköp" },
  { value: "ovrigt", label: "Övrigt" },
]
const TENURE_OPTIONS = [
  { label: "12 mån", value: 12 },
  { label: "24 mån", value: 24 },
  { label: "36 mån", value: 36 },
  { label: "48 mån", value: 48 },
  { label: "60 mån", value: 60 },
] as const

type TenureValue = typeof TENURE_OPTIONS[number]["value"];

const TestPage = () => {
  const [role, setRole] = useState<UserRole>("company");
  const [tenure, setTenure] = useState<TenureValue>()
  const [reason, setReason] = useState("");
  const [amount, setAmount] = useState<number>(3000000);

  return (
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
      <ToggleSwitch name="userRole" options={SWITCH_OPTIONS} selectedValue={role} onChange={(newRole) => setRole(newRole)} />
      <ButtonGroup<TenureValue>
        name="tenure"
        label="Önskad återbetalningstid (månad)"
        options={TENURE_OPTIONS}
        selectedValue={tenure}
        onChange={setTenure}
      />
      <Slider
          name="requestedAmount"
          label="Önskat belopp"
          min={50000}
          max={5000000}
          step={50000}
          value={amount}
          onChange={setAmount}
        />
    </Card>
  )
}

export default TestPage
