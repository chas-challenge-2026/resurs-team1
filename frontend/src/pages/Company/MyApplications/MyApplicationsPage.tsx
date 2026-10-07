import { useApplications } from "../../../hooks/useApplication"
import NotFoundPage from "../../NotFound/NotFoundPage"
import ApplicationCard from "../../../components/ApplicationCard/ApplicationCard"
import Loading from "../../../components/Loading/Loading"
import s from "./MyApplicationsPage.module.css"

const MyApplicationsPage = () => {
  const { data: applications = [], isLoading, isError, error } = useApplications()
  
  const ongoing = applications.filter((a) => 
    a.status === "PENDING_DOCS" || a.status === "UNDER_REVIEW"
  )

  const history = applications.filter((a) => 
    a.status === "APPROVED" || a.status === "REJECTED"
  )

  if (isLoading) {
    return <Loading label="Hämtar ansökningar..." size="lg" centerOnPage delay />
  }

  if(isError) {
    if(error.status === 404) return (
      <NotFoundPage 
        title="Mina Ansökningar"
        description="Du har inga registrerade kreditansökningar hos oss just nu. När du påbörjar och skickar in en ansökan kommer du att kunna följa hela din process, komplettera handlingar och se din historik här."
        backLinkText="Skapa ny ansökan"
        backLinkUrl="/kreditansokan"
      />
    )
    return <p>{error.message}</p>
  }

  return (
    <div className={s.wrapper}>
      <h2 className="title">Mina Ansökningar</h2>

      {ongoing.length > 0 &&
        <div className={s.applicationsContainer}>
          <h3 className="subtitle">Pågående</h3>
          {ongoing.map((a) => (
            <ApplicationCard key={a.id} application={a} />
          ))}
        </div>
      }

      {history.length > 0 &&
        <div className={s.applicationsContainer}>
          <h3 className="subtitle">Historik</h3>
          {history.map((a) => (
            <ApplicationCard key={a.id} application={a} />
          ))}
        </div>
      }
    </div>
  )
}

export default MyApplicationsPage