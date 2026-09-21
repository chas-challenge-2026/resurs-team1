import { useState } from "react"
import { useNavigate } from "react-router-dom"
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
import ApplicationWizard from "../../components/ApplicationWizard/ApplicationWizard"
import type { ApplicationFormData } from "../../components/ApplicationWizard/ApplicationWizard"
import ApplicationSummary from "../../components/ApplicationSummary/ApplicationSummary"
import type { Application } from "../../api/applicationApi"
import SidebarCaseCard from "../../components/SidebarCaseCard/SidebarCaseCard"
import AttachedFile from "../../components/AttachedFile/AttachedFile"
import { EMAIL_PATTERN, PHONE_PATTERN } from "../../constants/constants"
        
const summaryApplication: Application = {
  id: 1,
  requestedAmount: 850000,
  purpose: "Investering i maskiner/utrustning",
  status: "UNDER_REVIEW",
  createdAt: "2026-08-20T10:00:00Z",
  updatedAt: "2026-08-20T10:00:00Z",
  companyName: "Andersson Bygg AB",
  orgNumber: "556123-4567",
  authorizedSignatory: "Lars Andersson",
  durationMonths: 36,
}

const SWITCH_OPTIONS: SwitchOption<UserRole>[] = [
{ label: "Företag", value: "company" },
{ label: "Handläggare", value: "caseWorker" },
]

const DOCUMENT_MOCK = {
  id: 1,
  applicationId: 1,
  filename: "årsredovisning_2025.pdf",
  docType: "PDF",
  uploadedAt: "2026-08-27T10:30:00Z",
}

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

// taking start values from already signed in user. need to assign all data at start because of TS
const applicationData: ApplicationFormData = {
  orgNumber: "556677-8899",
  companyName: "Mangobolaget AB",
  contactName: "",
  email: "",
  phoneNumber: "",
  purpose: "",
  requestedAmount: 50000,
}

// copy pasted mock cases to try it out
const SAMPLE_CASES: Application[] = [
  { id: 387139, companyName: "Nordvik Bygg AB", orgNumber: "556600-0000", authorizedSignatory: "Anna Nordvik", purpose: "Rörelsekapital", requestedAmount: 3000000, status: "PENDING_DOCS", createdAt: "2026-08-27T09:00:00Z", updatedAt: "2026-08-27T09:00:00Z" },
  { id: 387142, companyName: "Lindqvist Logistik AB", orgNumber: "559012-3456", authorizedSignatory: "Erik Lindqvist", purpose: "Maskininvestering", requestedAmount: 850000, status: "UNDER_REVIEW", createdAt: "2026-08-29T09:00:00Z", updatedAt: "2026-08-29T09:00:00Z" },
  { id: 387150, companyName: "Solberga Café & Bageri AB", orgNumber: "556788-1122", authorizedSignatory: "Sara Solberg", purpose: "Expansion", requestedAmount: 1200000, status: "APPROVED", createdAt: "2026-09-02T09:00:00Z", updatedAt: "2026-09-02T09:00:00Z" },
  { id: 387155, companyName: "Västra Götalands Maskin- och Fastighetsservice AB", orgNumber: "559334-7788", authorizedSignatory: "Johan Hallberg", purpose: "Rörelsekapital", requestedAmount: 400000, status: "REJECTED", createdAt: "2026-09-08T09:00:00Z", updatedAt: "2026-09-08T09:00:00Z" },
]

// to make button appear and dissapear
const REVIEW_STEP = 3
const RECEIPT_STEP = 4

// the page owns the answers, so the page decides when a step may be left
const isStepComplete = (step: number, values: ApplicationFormData) => {
  switch (step) {
    case 1:
      return values.purpose !== "" && values.repaymentPeriod !== undefined
    case 2:
      return (
        values.contactName !== "" &&
        EMAIL_PATTERN.test(values.email) &&
        PHONE_PATTERN.test(values.phoneNumber)
      )
    default:
      return true
  }
}

type TenureValue = typeof TENURE_OPTIONS[number]["value"];

const TestPage = () => {
  const [role, setRole] = useState<UserRole>("company");
  const [tenure, setTenure] = useState<TenureValue>()
  const [reason, setReason] = useState("");
  const [amount, setAmount] = useState<number>(3000000);

  const [step, setStep] = useState(1)
  const [values, setValues] = useState<ApplicationFormData>(applicationData)

  const handleChange = (patch: Partial<ApplicationFormData>) =>
    setValues((prev) => ({ ...prev, ...patch }));

  const navigate = useNavigate()

  const handleCancel = () => {
    if (window.confirm("Vill du avbryta ansökan? Uppgifterna sparas inte.")) {
      navigate("/oversikt")
    }
  }

  return (
    <>
      <ApplicationSummary application={summaryApplication} />

      <div style={{width: "20rem"}}>
        {SAMPLE_CASES.map((application) => (
          <SidebarCaseCard key={application.id} application={application} />
        ))}
      </div>

      <Card>
        <h1 className="title">Titel</h1>
        <p className="subtitle">Undertitel</p>
        <Loading size="lg"/>
          test knapp
        <Button variant="ghost">
        <StatusTag status="APPROVED"/>
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
          <AttachedFile document={DOCUMENT_MOCK} />
      </Card>

      <ApplicationWizard step={step} onChange={handleChange} values={values}></ApplicationWizard>
      {/* step 4 is the receipt, so it has no navigation of its own */}
      {step < RECEIPT_STEP && (
        <>
          {step > 1 && (
            <Button variant="secondary" onClick={() => setStep((prev) => prev - 1)}>
              Tillbaka
            </Button>
          )}
          <Button variant="ghost" onClick={handleCancel}>
            Avbryt ansökan
          </Button>
          <Button
            onClick={() => setStep((prev) => prev + 1)}
            disabled={!isStepComplete(step, values)}
          >
            {step === REVIEW_STEP ? "Skicka ansökan" : "Fortsätt"}
          </Button>
        </>
      )}

      {step === RECEIPT_STEP && (
        <Button onClick={() => navigate("/oversikt")}>Till översikten</Button>
      )}
      <h3>{step}</h3>
    </>
  )
}

export default TestPage
