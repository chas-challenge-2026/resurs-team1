import { useApplications } from "../../../hooks/useApplication"
import Loading from "../../../components/Loading/Loading"
import s from "./MyApplicationsPage.module.css"
import ApplicationCard from "../../../components/ApplicationCard/ApplicationCard"

const MyApplicationsPage = () => {
  const { data: applications = [], isLoading, isError, error } = useApplications()

  console.log(applications)

  if (isLoading) {
    return <Loading label="Hämtar ansökningar..." delay />
  }

  if (isError) {
    return (
      <p>{error.message}</p>
    )
  }
  
  const ongoing = applications.filter((a) => 
    a.status === "PENDING_DOCS" || a.status === "UNDER_REVIEW"
  )

  const history = applications.filter((a) => 
    a.status === "APPROVED" || a.status === "REJECTED"
  )

  return (
    <div className={s.wrapper}>
      <h2 className="title">Mina Ansökningar</h2>

      {ongoing.length === 0 && history.length === 0 &&
        <p>Du har inga registrerade ansökningar ännu.</p>
      }

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