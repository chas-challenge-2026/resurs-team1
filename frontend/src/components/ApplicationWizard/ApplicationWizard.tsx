import { Card, CardBody } from "../Card/Card";

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
  loanAmount: number;
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
const ApplicationWizard = ({ step }: ApplicationWizardProps) => {
  switch (step) {
    case 1:
      return (
        <Card>
          <CardBody>
            hej
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
