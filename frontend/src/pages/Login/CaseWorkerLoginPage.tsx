import { useState } from "react";
import { Link } from "react-router-dom";
import { useCaseWorkerLogin } from "../../hooks/useLogin";
import { EMAIL_PATTERN } from "../../constants/constants";
import { Card } from "../../components/Card/Card";
import Button from "../../components/Button/Button";
import Input from "../../components/Input/Input";
import owl from "../../assets/mascot-owl-autumn.webp"
import s from "./LoginPage.module.css"
import c from "./CaseWorkerLoginPage.module.css"

const INITIAL_FORM = {
  email: "",
  password: "",
}

const CaseWorkerLoginPage = () => {
  const [formData, setFormData] = useState(INITIAL_FORM)
  const [errors, setErrors] = useState(INITIAL_FORM)

  const caseWorkerLogin = useCaseWorkerLogin()
  const caseWorkerFieldsTooShort = !formData.email.trim() || formData.password.trim().length < 8

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target
    // reset error as soon as user types again
    if (caseWorkerLogin.isError) caseWorkerLogin.reset()
    setErrors((prev) => ({ ...prev, [id]: "" }))

    setFormData((prev) => ({
      ...prev,
      [id]: value
    }))
  }

  // show errors after leaving input field
  const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    const { id, value } = e.target

    let errorMsg = ""

    if (id === "email" && value.trim() && !EMAIL_PATTERN.test(value)) {
      errorMsg = "Ange en giltig e-postadress"
    }

    if (id === "password" && value.trim() && value.trim().length < 8) {
      errorMsg = "Lösenordet måste vara minst 8 tecken"
    }

    setErrors((prev) => ({ ...prev, [id]: errorMsg }))
  }

  const handleCaseWorkerSubmit = (e: React.SubmitEvent) => {
    e.preventDefault()
    caseWorkerLogin.mutate({ 
      email: formData.email, 
      password: formData.password 
    })
  }

  return(
    <div className={`${s.wrapper} ${c.wrapper}`}>
      <div className={s.container}>
        <section className={s.hero}>
          <h1>Välkommen till Resurs <br/>Kreditportal för handläggare</h1>
          <p className={s.subTitle}>Handläggning och hantering av kreditärenden</p>
        </section>

         <Card as="section" className={s.loginSection}>
          <div className={s.loginHeader}>
            <div className={s.titleRow}>
              <span className={s.badge}>Handläggare</span>
              <h2 className="title">Logga in</h2>
            </div>
            <img src={owl} alt="" className={s.loginMascot} />
          </div>

          <div className={s.divider} />

          <form className={s.form} onSubmit={handleCaseWorkerSubmit} noValidate>
            <Input
              type="email"
              id="email"
              label="E-postadress *"
              placeholder="namn@resurs.se"
              error={errors.email}
              value={formData.email}
              onChange={handleChange}
              onBlur={handleBlur}
            />
            <Input 
              type="password" 
              id="password" 
              label="Lösenord *" 
              placeholder="••••••••••"
              information="Lösenordet måste vara minst 8 tecken."
              error={errors.password}
              value={formData.password} 
              onChange={handleChange}
              onBlur={handleBlur}
            />
            <div className={s.divider} />

            {caseWorkerLogin.isError &&
              <p role="alert" className={s.formError}>Fel e-postadress eller lösenord.</p>
            }

            <Button
              type="submit"
              className={s.button}
              disabled={caseWorkerFieldsTooShort || caseWorkerLogin.isPending}
            >
              {caseWorkerLogin.isPending ? "Loggar in..." : "Logga in"}
            </Button>
          </form>
          <span className="information-text">Är du kund? <Link to={"/"} className={s.link}>Gå till kundinloggningen</Link></span>
        </Card>
      </div>
    </div>
  )
}

export default CaseWorkerLoginPage