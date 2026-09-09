import { useState } from "react";
import ToggleSwitch from "../../components/ToggleSwitch/ToggleSwitch"
import type { SwitchOption } from "../../components/ToggleSwitch/ToggleSwitch";
import s from "./LoginPage.module.css"
import Input from "../../components/Input/Input";
import Button from "../../components/Button/Button";

type UserRole = "COMPANY" | "AGENT";

const SWITCH_OPTIONS: SwitchOption<UserRole>[] = [
{ label: "Företag", value: "COMPANY" },
{ label: "Handläggare", value: "AGENT" },
]

const LoginPage = () => {
  const [role, setRole] = useState<UserRole>("COMPANY")

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

          {role === "COMPANY" ? (
            <>
              <Input placeholder="XXXXXX-XXXX" label="Organisationsnummer *" id="orgnr" information="Ange 10 siffror" />
              <Button className={s.button}>Logga in med BankID</Button>
              <div className={s.divider} />
              <p className={s.info}>Behörig firmateckare i organisationen signerar med sitt personliga BankID. Företagets uppgifter hämtas automatiskt från officiella register.</p>
            </>
          ) : (
            <>
              <Input placeholder="namn.exempel@foretag.se" label="E-postadress *" id="email" type="email" />
              <Input placeholder="••••••••••" label="Lösenord *" id="password" type="password" information="Lösenordet måste vara minst 8 tecken" />
              <div className={s.divider} />
              <Button className={s.button}>Logga in</Button>
            </>
          )}
        </section>
      </div>
    </div>
  )
}

export default LoginPage