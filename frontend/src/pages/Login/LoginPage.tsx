import { useState } from "react";
import { useCaseWorkerLogin, useCompanyLogin } from "../../hooks/useLogin";
import type { SwitchOption } from "../../components/ToggleSwitch/ToggleSwitch";
import type { UserRole } from "../../types/user";
import ToggleSwitch from "../../components/ToggleSwitch/ToggleSwitch"
import Button from "../../components/Button/Button";
import Input from "../../components/Input/Input";
import Loading from "../../components/Loading/Loading";
import s from "./LoginPage.module.css"

const SWITCH_OPTIONS: SwitchOption<UserRole>[] = [
{ label: "Företag", value: "company" },
{ label: "Handläggare", value: "caseWorker" },
]

const INITIAL_FORM = {
  orgNumber: "",
  email: "",
  password: "",
}

const ORG_NUMBER_DIGITS = 10

// force the shape
// the "-" waits for a digit to follow it, otherwise backspace can never delete it
const formatOrgNumber = (value: string) => {
  const digits = value.replace(/\D/g, "").slice(0, ORG_NUMBER_DIGITS)
  return digits.length > 6 ? `${digits.slice(0, 6)}-${digits.slice(6)}` : digits // add "-"
}

const LoginPage = () => {
  const [role, setRole] = useState<UserRole>("company")
  const [formData, setFormData] = useState(INITIAL_FORM)
  const [orgNumberError, setOrgNumberError] = useState("")

  const companyLogin = useCompanyLogin()
  const caseWorkerLogin = useCaseWorkerLogin()

  const companyFieldsEmpty = !formData.orgNumber.trim()
  const caseWorkerFieldsEmpty = !formData.email.trim() || !formData.password.trim()

  const activeLogin = role === "company" ? companyLogin : caseWorkerLogin

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target
    // reset error as soon as user ttypes again
    if (activeLogin.isError) activeLogin.reset()
    setOrgNumberError("")
    setFormData((prev) => ({
      ...prev,
      [id]: id === "orgNumber" ? formatOrgNumber(value) : value,
    }))
  }

  const handleCompanySubmit = (e: React.SubmitEvent) => {
    e.preventDefault()

    // insta reject wrong format, wait for BankID on correct nyumbers
    if (formData.orgNumber.replace(/\D/g, "").length !== ORG_NUMBER_DIGITS) {
      setOrgNumberError("Organisationsnumret ska innehålla 10 siffror")
      return
    }

    companyLogin.mutate({ orgNumber: formData.orgNumber })
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
          <ToggleSwitch name="userRole" options={SWITCH_OPTIONS} selectedValue={role} onChange={(newRole) => setRole(newRole)} />

          {role === "company" ? (
            <form className={s.form} onSubmit={handleCompanySubmit}>
              {companyLogin.isPending &&
                <Loading fullscreen size="lg" label="Väntar på BankID..." />
              }

              <Input 
                id="orgNumber"
                inputMode="numeric"
                label="Organisationsnummer *"
                placeholder="XXXXXX-XXXX"
                information="Ange 10 siffror"
                error={orgNumberError}
                value={formData.orgNumber}
                onChange={handleChange}
              />
              {companyLogin.isError &&
                <div>

                  <p role="alert" className={s.formError}>Inloggningen misslyckades. Kontrollera organisationsnumret och försök igen.</p>
                  <p role="alert" className={s.formError}>Format: XXXXXX-XXXX</p>
                </div>
                }

              <Button
                type="submit"
                className={s.button}
                disabled={companyFieldsEmpty || companyLogin.isPending}
              >
                {companyLogin.isPending ? "Loggar in..." : "Logga in med BankID"}
              </Button>
              <div className={s.divider} />
              <p className={s.info}>Behörig firmateckare i organisationen signerar med personligt BankID.</p>
            </form>
          ) : (
            <form className={s.form} onSubmit={handleCaseWorkerSubmit}>
              <Input 
                type="email" 
                id="email"
                label="E-postadress *" 
                placeholder="namn.exempel@foretag.se" 
                value={formData.email} 
                onChange={handleChange} 
              />
              <Input 
                type="password" 
                id="password" 
                label="Lösenord *" 
                placeholder="••••••••••" 
                information="Lösenordet måste vara minst 8 tecken" 
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