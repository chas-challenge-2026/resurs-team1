import ButtonGroup from "../ButtonGroup/ButtonGroup";
import type { ButtonGroupOption } from "../ButtonGroup/ButtonGroup";
import { Card, CardBody } from "../Card/Card";
import { DataList, DataListItem } from "../DataList/DataList";
import Dropdown from "../Dropdown/Dropdown";
import type { DropdownOption } from "../Dropdown/Dropdown";
import Input from "../Input/Input";
import Slider from "../Slider/Slider";
import { formatCurrency } from "../../utils/formatters";
import s from "./ApplicationWizard.module.css";

// placeholder options -- pratat med back-end "ej enum, det är  vanlig text sträng"
const PURPOSE_OPTIONS: DropdownOption[] = [
  { value: "waiting", label: "Väntar" },
  { value: "for", label: "På" },
  { value: "backend", label: "Back-end" },
]

const REPAYMENT_OPTIONS: ButtonGroupOption<number>[] = [
  { value: 12, label: "12 mån" },
  { value: 24, label: "24 mån" },
  { value: 36, label: "36 mån" },
  { value: 48, label: "48 mån" },
  { value: 60, label: "60 mån" },
]

// exported so the page can gate the Fortsätt button on the same rules
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
// loose on purpose: swedish numbers are written with spaces, dashes and +46
export const PHONE_PATTERN = /^[\d\s+()-]{6,20}$/

const LOAN_MIN = 50000
const LOAN_MAX = 10000000

/** Everything the customer fills in + autofilled*/
export interface ApplicationFormData {
  // auto filled:
  orgNumber: string;
  companyName: string;

  // filled out during wizard:
  contactName: string;
  email: string;
  phoneNumber: string;

  // becomes a union once the backend hands over the enum values -- A | B | C
  purpose: string;
  // TODO: "requestedAmount" in api rn, until they change according to request in slack
  requestedAmount: number;
  // undefined until the customer picks one
  repaymentPeriod?: number;
}

interface ApplicationWizardProps {
  /** Which step to render. Owned by the page. */
  step: number;
  /** Current answers. Owned by the page. */
  values: ApplicationFormData;
  /** Report a change upwards. Partial, so a field can be sent on its own. */
  onChange: (patch: Partial<ApplicationFormData>) => void;
}

/**
 * ApplicationWizard – renders the fields for one step of the credit application.
 *
 * Holds no state. The page owns the answers and therefore also the buttons —
 * navigation, "Avbryt ansökan" and submit all live on the page.
 *
 * @example
 * ```tsx
 * const [step, setStep] = useState(1)
 * const [values, setValues] = useState<ApplicationFormData>(emptyApplication)
 *
 * // old answers first, then the patch overwrites only what changed
 * const handleChange = (patch: Partial<ApplicationFormData>) =>
 *   setValues((prev) => ({ ...prev, ...patch }))
 *
 * <ApplicationWizard step={step} values={values} onChange={handleChange} />
 * <Button onClick={() => setStep(step + 1)}>Fortsätt</Button>
 *
 * // dragging the slider calls onChange({ loanAmount: 2000000 })
 * // → values becomes { ...everything else, loanAmount: 2000000 }
 * ```
 */
const ApplicationWizard = ({ step, values, onChange }: ApplicationWizardProps) => {
  switch (step) {
    case 1:
      return (
        <Card className={s.card}>
          <CardBody>
            <Dropdown
              id="purpose"
              label="Ändamål - Vad ska lånet användas till?"
              placeholder="Välj ändamål..."
              options={PURPOSE_OPTIONS}
              value={values.purpose}
              onChange={(purpose) => onChange({ purpose })}
            />
            <Slider
              name="loanAmount"
              label="Önskat belopp"
              min={LOAN_MIN}
              max={LOAN_MAX}
              step={50000}
              value={values.requestedAmount}
              onChange={(loanAmount) => onChange({ requestedAmount: loanAmount })}
            />
            <ButtonGroup
              name="repaymentPeriod"
              label="Önskad återbetalningstid (månad)"
              options={REPAYMENT_OPTIONS}
              selectedValue={values.repaymentPeriod}
              onChange={(repaymentPeriod) => onChange({ repaymentPeriod })} //is set to whatever is sent in as prop
            />
          </CardBody>
        </Card>
      )
    case 2: {
      // an untouched field is not wrong yet, so only complain once something is typed
      const emailError =
        values.email !== "" && !EMAIL_PATTERN.test(values.email)
          ? "Kontrollera e-postadressen"
          : undefined
      const phoneError =
        values.phoneNumber !== "" && !PHONE_PATTERN.test(values.phoneNumber)
          ? "Kontrollera telefonnumret"
          : undefined

      return (
        <Card className={s.card}>
          <CardBody>
            <DataList>
              <DataListItem label="Organisationsnummer" value={values.orgNumber} />
              <DataListItem label="Företagsnamn" value={values.companyName} />
            </DataList>
            <p className="information-text">
              Vi hämtar företagets bokslutsuppgifter via organisationsnumret.
            </p>

            {/* native input, so onChange hands over an event instead of a value */}
            <Input
              id="contactName"
              label="Kontaktperson"
              placeholder="Anna Andersson"
              value={values.contactName}
              onChange={(e) => onChange({ contactName: e.target.value })}
            />
            <Input
              id="email"
              label="E-post"
              type="email"
              placeholder="anna@foretag.se"
              value={values.email}
              error={emailError}
              onChange={(e) => onChange({ email: e.target.value })}
            />
            <Input
              id="phoneNumber"
              label="Telefonnummer"
              type="tel"
              placeholder="070-123 45 67"
              value={values.phoneNumber}
              error={phoneError}
              onChange={(e) => onChange({ phoneNumber: e.target.value })}
            />
          </CardBody>
        </Card>
      )
    }

    case 3: {
      // the form stores the value, the customer should read the label
      const purposeLabel = PURPOSE_OPTIONS.find((option) => option.value === values.purpose)?.label

      return (
        <Card className={s.card}>
          <CardBody>
            <DataList>
              <DataListItem label="Organisationsnummer" value={values.orgNumber} />
              <DataListItem label="Företagsnamn" value={values.companyName} />
              <DataListItem label="Kontaktperson" value={values.contactName} />
              <DataListItem label="E-post" value={values.email} />
              <DataListItem label="Telefonnummer" value={values.phoneNumber} />
              <DataListItem label="Ändamål" value={purposeLabel ?? "Ej valt"} />
              <DataListItem label="Önskat belopp" value={formatCurrency(values.requestedAmount)} />
              <DataListItem
                label="Önskad återbetalningstid"
                value={values.repaymentPeriod ? `${values.repaymentPeriod} mån` : "Ej valt"}
              />
            </DataList>
            <p className="information-text">
              Kontrollera uppgifterna innan du skickar in ansökan.
            </p>
          </CardBody>
        </Card>
      )
    }

    case 4:
      return (
        <Card className={s.card}>
          <CardBody>
            <h2>Tack, vi har tagit emot din ansökan</h2>
            <p>
              Vi återkommer med besked till {values.email}. Handläggningen tar
              normalt några arbetsdagar.
            </p>
          </CardBody>
        </Card>
      )

    default:
      return null
  }
}

export default ApplicationWizard
