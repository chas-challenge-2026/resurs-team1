import { useState } from "react"
import { useNavigate } from "react-router-dom"
import ApplicationWizard, { type ApplicationFormData} from "../../../components/ApplicationWizard/ApplicationWizard"
import { EMAIL_PATTERN, PHONE_PATTERN } from "../../../constants/constants"
import ProgressBar from "../../../components/ProgressBar/ProgressBar"
import Button from "../../../components/Button/Button"
import { getUser } from "../../../utils/auth"
import { useSubmitApplication } from "../../../hooks/useApplication"
import s from "./ApplicationPage.module.css"
import Loading from "../../../components/Loading/Loading"

const TOTAL_STEPS = 3
const STEP_TITLES = ["Lånebehov", "Kontaktuppgifter", "Granska och skicka"]

const ApplicationFormPage = () => {
  const navigate = useNavigate()
  const user = getUser()
  const submitApplication = useSubmitApplication()

  const [step, setStep] = useState(1)
  const [values, setValues] = useState<ApplicationFormData>({
    orgNumber: user?.role === "company" ? user.orgNumber : "",
    companyName: user?.role === "company" ? user.companyName : "",
    contactName: "",
    email: "",
    phoneNumber: "",
    purpose: "",
    requestedAmount: 3000000,
    durationMonths: undefined,
  })

  // old answers first, then the patch overwrites only what changed
  const handleChange = (patch: Partial<ApplicationFormData>) =>
    setValues((prev) => ({ ...prev, ...patch }))

  // same rules the wizard shows errors for, so Fortsätt can't walk past a bad field
  const stepIsComplete =
    step === 1
      ? values.purpose !== "" && values.durationMonths !== undefined
      : step === 2
        ? values.contactName !== "" &&
          EMAIL_PATTERN.test(values.email) &&
          PHONE_PATTERN.test(values.phoneNumber)
        : true

  const handleSubmit = () => {
    //data being sent differs from the form data + back-end wants contact info nested :)
    submitApplication.mutate(
      { 
        contactDetails: {
          phoneNumber: values.phoneNumber,
          email: values.email,
          name: values.contactName
        },
        orgNumber: values.orgNumber,
        durationMonths: values.durationMonths!,
        purpose: values.purpose,
        requestedAmount: values.requestedAmount
      }, 
      { onSuccess: (response) => {
        navigate(`/mina-ansokningar/${response.id}`, {replace: true})
      }}
    )
  }

  return (
    <div className={s.wizard}>
      {submitApplication.isPending && <Loading fullscreen label="Skickar din ansökan..." delay />}

      <div className={s.header}>
        <div className={s.textWrapper}>
          <p className="subtitle">Steg {step} av {TOTAL_STEPS}</p>
          <h2 className="title">{STEP_TITLES[step - 1]}</h2>
        </div>
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
          // pending stops a double click from sending two applications
          disabled={!stepIsComplete || submitApplication.isPending}
          onClick={step === TOTAL_STEPS ? handleSubmit : () => setStep(step + 1)}
        >
          {step !== TOTAL_STEPS ? "Fortsätt" : submitApplication.isPending ? "Skickar..." : "Skicka ansökan"}
        </Button>
      </div>
        {submitApplication.isError &&
          <p role="alert" className={s.submitError}>Ansökan kunde inte skickas just nu. Försök igen</p>
        }
    </div>
  )
}

export default ApplicationFormPage
