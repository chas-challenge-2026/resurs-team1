import { useState } from "react";
import { useCaseWorkerLogin, useCompanyLogin } from "../../hooks/useLogin";
import type { SwitchOption } from "../../components/ToggleSwitch/ToggleSwitch";
import type { UserRole } from "../../types/user";
import { EMAIL_PATTERN } from "../../constants/constants";
import ToggleSwitch from "../../components/ToggleSwitch/ToggleSwitch"
import Button from "../../components/Button/Button";
import Input from "../../components/Input/Input";
import Loading from "../../components/Loading/Loading";
import s from "./LoginPage.module.css"
import { IoCheckmarkCircle } from "react-icons/io5";

const SWITCH_OPTIONS: SwitchOption<UserRole>[] = [
  { label: "Företag", value: "company" },
  { label: "Handläggare", value: "caseWorker" },
]

const INITIAL_FORM = {
  orgNumber: "",
  personalNumber: "",
  email: "",
  password: "",
}

const ID_NUMBER_DIGITS = 10

// force the shape
// the "-" waits for a digit to follow it, otherwise backspace can never delete it
const format10DigitNumber = (value: string) => {
  const digits = value.replace(/\D/g, "").slice(0, ID_NUMBER_DIGITS)
  return digits.length > 6 ? `${digits.slice(0, 6)}-${digits.slice(6)}` : digits // add "-"
}

const LoginPage = () => {
  const [role, setRole] = useState<UserRole>("company")
  const [formData, setFormData] = useState(INITIAL_FORM)
  const [errors, setErrors] = useState(INITIAL_FORM)

  const companyLogin = useCompanyLogin()
  const caseWorkerLogin = useCaseWorkerLogin()

  const companyFieldsEmpty = !formData.orgNumber.trim() || !formData.personalNumber.trim()
  const caseWorkerFieldsEmpty = !formData.email.trim() || !formData.password.trim()

  const activeLogin = role === "company" ? companyLogin : caseWorkerLogin

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target
    // reset error as soon as user types again
    if (activeLogin.isError) activeLogin.reset()
    setErrors((prev) => ({ ...prev, [id]: "" }))

    let formattedValue = value
    if (id === "orgNumber") formattedValue = format10DigitNumber(value)
    if (id === "personalNumber") formattedValue = format10DigitNumber(value)

    setFormData((prev) => ({
      ...prev,
      [id]: formattedValue
    }))
  }

  const handleCompanySubmit = (e: React.SubmitEvent) => {
    e.preventDefault()

    const rawOrg = formData.orgNumber.replace(/\D/g, "")
    const rawPersonal = formData.personalNumber.replace(/\D/g, "")
    
    // insta reject wrong format, wait for BankID on correct numbers
    const newErrors = {
      orgNumber: rawOrg.length !== ID_NUMBER_DIGITS ? "Organisationsnumret måste innehålla 10 siffror" : "",
      personalNumber: rawPersonal.length !== ID_NUMBER_DIGITS ? "Personnumret måste innehålla 10 siffror" : "",
    }

    setErrors((prev) => ({ ...prev, ...newErrors }))

    if (newErrors.orgNumber || newErrors.personalNumber) return

    companyLogin.mutate({ 
      orgNumber: formData.orgNumber, 
      personalNumber: formData.personalNumber
    })
  }

  // show error after leaving input field
  const handleEmailBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    const email = e.target.value.trim()
    const errorMsg = !email || EMAIL_PATTERN.test(email) ? "" : "Ange en giltig e-postadress"

    setErrors((prev) => ({...prev, email: errorMsg}))
  }

  const handleCaseWorkerSubmit = (e: React.SubmitEvent) => {
    e.preventDefault()
    caseWorkerLogin.mutate({ 
      email: formData.email, 
      password: formData.password 
    })
  }

  return(
    <div className={s.wrapper}>
      <div className={s.container}>
        <section className={s.hero}>
          <h1>Välkommen till Resurs <br/>Kreditportal</h1>
          <p className={s.subTitle}>Ansök om krediter och se pågående ärenden</p>
        </section>

        <section className={s.loginSection}>
          <h2 className="title">Logga in</h2>
          <p className={s.subTitle}>Välj inloggningsmetod nedan</p>

          <div className={s.divider} />
          <ToggleSwitch 
            name="userRole" 
            options={SWITCH_OPTIONS} 
            selectedValue={role} 
            onChange={(newRole) => {
              setRole(newRole)
              setFormData(INITIAL_FORM)
              setErrors(INITIAL_FORM)
              companyLogin.reset()
              caseWorkerLogin.reset()
            }}
          />

          {role === "company" ? (
            <form className={s.form} onSubmit={handleCompanySubmit}>
              <Input 
                id="orgNumber"
                inputMode="numeric"
                label="Organisationsnummer *"
                placeholder="556XXX-XXXX"
                error={errors.orgNumber}
                value={formData.orgNumber}
                onChange={handleChange}
                disabled={companyLogin.isPending}
              />

              <Input 
                id="personalNumber"
                inputMode="numeric"
                label="Personnummer *"
                placeholder="ÅÅMMDD-XXXX"
                information="Ange 10 siffror"
                error={errors.personalNumber}
                value={formData.personalNumber}
                onChange={handleChange}
                disabled={companyLogin.isPending}
              />

              {companyLogin.isError &&
                <div>
                  <p role="alert" className={s.formError}>Inloggningen misslyckades. Kontrollera organisationsnumret och personnumret.</p>
                </div>
              }

              {companyLogin.isPending && (
                <div className={s.pendingWrapper} role="status" aria-live="polite">
                  <Loading delay={false} />
                  <p className={s.pendingTitle}>Väntar på Bank-ID signering</p>
                  <p className="information-text">Öppna Bank-ID appen och godkänn inloggningen.</p>
                </div>
              )}

              {companyLogin.isSuccess && (
                <div className={s.successWrapper} role="status" aria-live="polite">
                  <IoCheckmarkCircle aria-hidden="true" />
                  <div>
                    <p className={s.successTitle}>Signering godkänd</p>
                    <p className="information-text">Du loggas in...</p>
                  </div>
                </div>
              )}

              {!companyLogin.isPending && !companyLogin.isSuccess && (
                <>
                  <Button
                    type="submit"
                    className={s.button}
                    disabled={companyFieldsEmpty || companyLogin.isPending}
                  >
                    Logga in med BankID
                  </Button>
                  <div className={s.divider} />
                  <p className="information-text">Behörig firmatecknare i organisationen signerar med personligt BankID.</p>
                </>
              )}
            </form>
          ) : (
            <form className={s.form} onSubmit={handleCaseWorkerSubmit} noValidate>
              <Input
                type="email"
                id="email"
                label="E-postadress *"
                placeholder="namn.exempel@foretag.se"
                error={errors.email}
                value={formData.email}
                onChange={handleChange}
                onBlur={handleEmailBlur}
              />
              <Input 
                type="password" 
                id="password" 
                label="Lösenord *" 
                placeholder="••••••••••" 
                error={errors.password}
                value={formData.password} 
                onChange={handleChange} 
              />
              <div className={s.divider} />

              {caseWorkerLogin.isError &&
                <p role="alert" className={s.formError}>Fel e-postadress eller lösenord.</p>
              }

              <Button
                type="submit"
                className={s.button}
                disabled={caseWorkerFieldsEmpty || caseWorkerLogin.isPending}
              >
                {caseWorkerLogin.isPending ? "Loggar in..." : "Logga in"}
              </Button>
            </form>
          )}
        </section>
      </div>
    </div>
  )
}

export default LoginPage