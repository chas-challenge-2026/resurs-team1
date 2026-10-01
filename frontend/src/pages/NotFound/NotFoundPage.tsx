import { useNavigate } from "react-router-dom"
import Button from "../../components/Button/Button"
import s from "./NotFoundPage.module.css"

interface NotFoundPageProps {
  title?: string
  description?: string
  backLinkText?: string
  backLinkUrl?: string
}

const NotFoundPage = ({
  title = "Hoppsan, något gick visst fel!",
  description = "Sidan du söker finns inte, kontrollera att du skrivit in rätt webbadress. Fungerar det fortfarande inte? Då kan sidan vara borttagen. Försök hitta informationen du söker på någon av de andra sidorna.",
  backLinkText = "Gå till startsidan",
  backLinkUrl = "/",
}: NotFoundPageProps) => {

  const navigate = useNavigate()

  return(
    <div className={s.wrapper}>
      <h2 className="title">{title}</h2>
      <p>{description}</p>
      <Button onClick={() => navigate(backLinkUrl)}>
        {backLinkText}
      </Button>
    </div>
  )
}

export default NotFoundPage