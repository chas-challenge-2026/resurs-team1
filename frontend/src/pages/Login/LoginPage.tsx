import { useState } from "react";
import { useCaseWorkerLogin, useCompanyLogin } from "../../hooks/useLogin";
import type { SwitchOption } from "../../components/ToggleSwitch/ToggleSwitch";
import type { UserRole } from "../../types/user";
import ToggleSwitch from "../../components/ToggleSwitch/ToggleSwitch"
import Button from "../../components/Button/Button";
import Input from "../../components/Input/Input";
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

const LoginPage = () => {
  const [role, setRole] = useState<UserRole>("company")
  const [formData, setFormData] = useState(INITIAL_FORM)

  const companyLogin = useCompanyLogin()
  const caseWorkerLogin = useCaseWorkerLogin()

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target
    setFormData((prev) => ({ ...prev, [id]: value }))
  }

  const handleCompanySubmit = (e: React.SubmitEvent) => {
    e.preventDefault()
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
              <Input 
                id="orgNumber" 
                label="Organisationsnummer *" 
                placeholder="XXXXXX-XXXX" 
                information="Ange 10 siffror" 
                value={formData.orgNumber} 
                onChange={handleChange} 
              />
              <Button type="submit" className={s.button}>Logga in med BankID</Button>
              <div className={s.divider} />
              <p className={s.info}>Behörig firmateckare i organisationen signerar med sitt personliga BankID. Företagets uppgifter hämtas automatiskt från officiella register.</p>
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
              <Button type="submit" className={s.button}>Logga in</Button>
            </form>
          )}
        </section>
      </div>
    </div>
  )
}

export default LoginPage