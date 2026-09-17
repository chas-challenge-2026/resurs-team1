import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { FiArrowRight } from "react-icons/fi"
import ApplicationWizard, {
  EMAIL_PATTERN,
  PHONE_PATTERN,
  type ApplicationFormData,
} from "../../../components/ApplicationWizard/ApplicationWizard"
import ProgressBar from "../../../components/ProgressBar/ProgressBar"
import Button from "../../../components/Button/Button"
import { getUser } from "../../../utils/auth"
import s from "./ApplicationPage.module.css"

const TOTAL_STEPS = 3
// the receipt is not a step, it has no progress bar and no way back
const RECEIPT_STEP = TOTAL_STEPS + 1

const STEP_TITLES = ["Lånebehov", "Kontaktuppgifter", "Granska och skicka"]

const ApplicationFormPage = () => {
  const navigate = useNavigate()
  const user = getUser()

  const [step, setStep] = useState(1)
  const [values, setValues] = useState<ApplicationFormData>({
    orgNumber: user?.role === "company" ? user.orgNumber : "",
    companyName: user?.role === "company" ? user.companyName : "",
    contactName: "",
    email: "",
    phoneNumber: "",
    purpose: "",
    requestedAmount: 3000000,
    repaymentPeriod: undefined,
  })

  // old answers first, then the patch overwrites only what changed
  const handleChange = (patch: Partial<ApplicationFormData>) =>
    setValues((prev) => ({ ...prev, ...patch }))

  // same rules the wizard shows errors for, so Fortsätt can't walk past a bad field
  const stepIsComplete =
    step === 1
      ? values.purpose !== "" && values.repaymentPeriod !== undefined
      : step === 2
        ? values.contactName !== "" &&
          EMAIL_PATTERN.test(values.email) &&
          PHONE_PATTERN.test(values.phoneNumber)
        : true

  // TODO: post the application here once the backend endpoint exists
  const handleSubmit = () => setStep(RECEIPT_STEP)

  if (step === RECEIPT_STEP) {
    return (
      <div className={s.wizard}>
        <ApplicationWizard step={step} values={values} onChange={handleChange} />

        <div className={s.actions}>
          <Button className={s.submit} onClick={() => navigate("/mina-ansokningar")}>
            Mina ansökningar
            <FiArrowRight aria-hidden />
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className={s.wizard}>
      <div className={s.header}>
        <p className="subtitle">Steg {step} av {TOTAL_STEPS}</p>
        <h2 className="title">{STEP_TITLES[step - 1]}</h2>
        <ProgressBar currentStep={step} totalSteps={TOTAL_STEPS} />
      </div>

      <ApplicationWizard step={step} values={values} onChange={handleChange} />

      <div className={s.actions}>
        {step > 1 && (
          <Button variant="secondary" onClick={() => setStep(step - 1)}>
            Tillbaka
          </Button>
        )}
        <Button variant="ghost" onClick={() => navigate("/oversikt")}>
          Avbryt ansökan
        </Button>
        <Button
          className={s.submit}
          disabled={!stepIsComplete}
          onClick={step === TOTAL_STEPS ? handleSubmit : () => setStep(step + 1)}
        >
          {step === TOTAL_STEPS ? "Skicka ansökan" : "Fortsätt"}
        </Button>
      </div>
    </div>
  )
}

export default ApplicationFormPage
