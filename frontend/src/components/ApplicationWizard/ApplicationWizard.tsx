import ButtonGroup from "../ButtonGroup/ButtonGroup";
import type { ButtonGroupOption } from "../ButtonGroup/ButtonGroup";
import { Card, CardBody } from "../Card/Card";
import Dropdown from "../Dropdown/Dropdown";
import type { DropdownOption } from "../Dropdown/Dropdown";
import Slider from "../Slider/Slider";

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
        <Card>
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
    case 2:
      return (
        <Card>
          <CardBody>
            hej!
          </CardBody>
        </Card>
      )

    default:
      return null
  }
}

export default ApplicationWizard
