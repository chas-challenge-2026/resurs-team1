import { useState } from "react";
import { IoCheckmarkCircle } from "react-icons/io5";
import { useCompanyLogin } from "../../hooks/useLogin";
import { ID_NUMBER_DIGITS } from "../../constants/constants";
import { format10DigitNumber } from "../../utils/formatters";
import { Card } from "../../components/Card/Card";
import Button from "../../components/Button/Button";
import Input from "../../components/Input/Input";
import Loading from "../../components/Loading/Loading";
import owl from "../../assets/mascot-owl.webp"
import s from "./LoginPage.module.css"

const INITIAL_FORM = {
  orgNumber: "",
  personalNumber: "",
}

const CustomerLoginPage = () => {
  const [formData, setFormData] = useState(INITIAL_FORM)
  const [errors, setErrors] = useState(INITIAL_FORM)

  const companyLogin = useCompanyLogin()
  const companyFieldsTooShort = formData.orgNumber.trim().length < 11 || formData.personalNumber.trim().length < 11

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target
    // reset error as soon as user types again
    if (companyLogin.isError) companyLogin.reset()
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

    companyLogin.mutate({ 
      orgNumber: formData.orgNumber, 
      personalNumber: formData.personalNumber
    })
  }

  // show errors after leaving input field
  const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    const { id, value } = e.target
    const rawValue = value.replace(/\D/g, "").trim()

    let errorMsg = ""

    if (id === "orgNumber" && value.trim() && rawValue.length < ID_NUMBER_DIGITS) {
      errorMsg = "Organisationsnumret måste innehålla 10 siffror"
    }

    if (id === "personalNumber" && value.trim() && rawValue.length < ID_NUMBER_DIGITS) {
      errorMsg = "Personnumret måste innehålla 10 siffror"
    }

    setErrors((prev) => ({ ...prev, [id]: errorMsg }))
  }

  return(
    <div className={s.wrapper}>
      <div className={s.container}>
        <section className={s.hero}>
          <h1>Välkommen till Resurs <br/>Kreditportal</h1>
          <p className={s.subTitle}>Ansök om krediter och se pågående ärenden</p>
        </section>

         <Card as="section" className={s.loginSection}>
          <div className={s.loginHeader}>
            <div className={s.titleRow}>
              <h2 className="title">Logga in</h2>
              <p className={`${s.subTitle} ${s.hiddenDesktop}`}>Resurs Kreditportal</p>
            </div>
            <img src={owl} alt="" className={s.loginMascot} />
          </div>

          <div className={s.divider} />

          <form className={s.form} onSubmit={handleCompanySubmit}>
            <Input 
              id="orgNumber"
              inputMode="numeric"
              label="Organisationsnummer *"
              placeholder="556XXX-XXXX"
              error={errors.orgNumber}
              value={formData.orgNumber}
              onChange={handleChange}
              onBlur={handleBlur}
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
              onBlur={handleBlur}
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
                  disabled={companyFieldsTooShort || companyLogin.isPending}
                >
                  Logga in med BankID
                </Button>
                <div className={s.divider} />
                <p className="information-text">Behörig firmatecknare i organisationen signerar med personligt BankID.</p>
              </>
            )}
          </form>
        </Card>
      </div>
    </div>
  )
}

export default CustomerLoginPage