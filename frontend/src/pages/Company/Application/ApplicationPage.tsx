import { useState } from "react"
import { useNavigate } from "react-router-dom"
import ApplicationWizard, { type ApplicationFormData} from "../../../components/ApplicationWizard/ApplicationWizard"
import { EMAIL_PATTERN } from "../../../constants/constants"
import { isValidPhone } from "../../../utils/validators"
import ProgressBar from "../../../components/ProgressBar/ProgressBar"
import Button from "../../../components/Button/Button"
import { useApplications, useSubmitApplication } from "../../../hooks/useApplication"
import s from "./ApplicationPage.module.css"
import Loading from "../../../components/Loading/Loading"
import { useUser } from "../../../hooks/useUser"
import { isCompanyUser } from "../../../utils/auth"

const TOTAL_STEPS = 4
const STEP_TITLES = ["Lånebehov", "Kontaktuppgifter", "Ekonomi", "Granska och skicka"]

const ApplicationFormPage = () => {
  const navigate = useNavigate()
  const { data: user } = useUser()
  const submitApplication = useSubmitApplication()

  //grab recent application to get a name for autofill
  const recentApplications = useApplications(0)
  const recentContactDetails = recentApplications.data?.[0]?.contactDetails

  const [step, setStep] = useState(1)
  const [values, setValues] = useState<ApplicationFormData>({
    orgNumber: isCompanyUser(user) ? user.orgNumber : "",
    companyName: isCompanyUser(user) ? user.displayName : "",
    contactName: "",
    email: "",
    phoneNumber: "",
    purpose: "",
    requestedAmount: 3000000,
    durationMonths: undefined,
  })
  const [isTermsAccepted, setIsTermsAccepted] = useState(false)

  // old answers first, then the patch overwrites only what changed
  const handleChange = (patch: Partial<ApplicationFormData>) =>
    setValues((prev) => ({ ...prev, ...patch }))

  // the fetch comes after the first render, so fill once when it comes
  const [hasAutofilled, setHasAutofilled] = useState(false)
  if (!hasAutofilled && recentApplications.isSuccess) {
    setHasAutofilled(true)
    handleChange({
      contactName: recentContactDetails?.name || "",
      email: recentContactDetails?.email || "",
      phoneNumber: recentContactDetails?.phoneNumber || "",
    })
  }

  // same rules the wizard shows errors for, so Fortsätt can't walk past a bad field
  const stepIsComplete =
    step === 1
      ? values.purpose !== "" && values.durationMonths !== undefined
      : step === 2
        ? values.contactName !== "" &&
          EMAIL_PATTERN.test(values.email) &&
          isValidPhone(values.phoneNumber)
        : true

  const handleSubmit = () => {
    //data being sent differs from the form data
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

      <ApplicationWizard step={step} values={values} onChange={handleChange} isSubmitError={submitApplication.isError} isTermsAccepted={isTermsAccepted} onTermsChange={setIsTermsAccepted} />

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
          disabled={!stepIsComplete || step === TOTAL_STEPS && !isTermsAccepted || submitApplication.isPending}
          onClick={step === TOTAL_STEPS ? handleSubmit : () => setStep(step + 1)}
        >
          {step !== TOTAL_STEPS ? "Fortsätt" : submitApplication.isPending ? "Skickar..." : "Skicka ansökan"}
        </Button>
      </div>
    </div>
  )
}

export default ApplicationFormPage
