import { Link } from "react-router-dom"
import { Card, CardBody, CardFooter, CardHeader } from "../../../components/Card/Card"
import s from "./CompanyHomePage.module.css"
import c from "../../../components/Card/Card.module.css"

const CompanyHomePage = () => {
  return(
    <div className={s.wrapper}>
      <div className={s.titleContainer}>
        <h2 className={s.title}>Vad vill du göra?</h2>
        <p className={s.subTitle}>Välj ett alternativ nedan för att komma igång.</p>
      </div>

      <div className={s.actionsContainer}>
        <Card as="article" variant="accent" accentColor="var(--color-primary)" className={`${s.action} ${s.actionPrimary}`}>
          <CardHeader>
            <span className={s.label} style={{color: "var(--color-primary)"}}>Ansökan</span>
            <h3 className={s.cardTitle}>Ny ansökan</h3>
          </CardHeader>
          <CardBody>
            <p>Starta en kreditansökan för er verksamhet. Fyll i uppgifter och få svar inom några minuter.</p>
          </CardBody>
          <CardFooter>
            <Link to="/kreditansokan" className={`${c.stretchedLink} ${s.link}`}>Påbörja ansökan</Link>
          </CardFooter>
        </Card>

        <Card as="article" variant="accent" accentColor="var(--color-secondary)" className={`${s.action} ${s.actionSecondary}`}>
          <CardHeader>
            <span className={s.label}>Historik</span>
            <h3 className={s.cardTitle}>Mina ansökningar</h3>
          </CardHeader>
          <CardBody>
            <p>Följ status på pågående ärenden och se er ansökningshistorik</p>
          </CardBody>
          <CardFooter>
            <Link to="/mina-ansokningar" className={`${c.stretchedLink} ${s.link}`}>Visa ansökningar</Link>
          </CardFooter>
        </Card>
      </div>

      <div className={s.divider}></div>

      <p className={s.supportText}>
        Behöver du hjälp? Kontakta oss på{" "}
        <a href="tel:042-382138" className={s.supportLink}>
          042-38 21 38
        </a>{" "}
        eller via mejl på{" "}
        <a href="mailto:foretagsbanken@resurs.se" className={s.supportLink}>
          foretagsbanken@resurs.se
        </a>
      </p>
    </div>
  )
}

export default CompanyHomePage