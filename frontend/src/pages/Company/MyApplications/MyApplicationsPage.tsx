import { useState } from "react"
import { useApplications } from "../../../hooks/useApplication"
import Pagination from "../../../components/Pagination/Pagination"
import NotFoundPage from "../../NotFound/NotFoundPage"
import ApplicationCard from "../../../components/ApplicationCard/ApplicationCard"
import Loading from "../../../components/Loading/Loading"
import s from "./MyApplicationsPage.module.css"

const MyApplicationsPage = () => {
  // backend sends open and closed in one list with no status filter, so we can't split
  // into pågående/historik without fetching everything. we follow their order (newest first)
  // and assume new cases matter most. an old open case means the customer has to gou through many pages.
  // 0-based like spring, Pagination is 1-based
  const [page, setPage] = useState(0)
  const { data: applications = [], isLoading, isError, error } = useApplications(page)
  // fetches 2 pages at a time so clicking ">" is instant
  const { data: nextApplications = [] } = useApplications(page + 1)

  // backend sends no total, so count the pages we know exist. always add +1 if there is next page since we fetch 2
  const totalPages = nextApplications.length > 0 ? page + 2 : page + 1

  // spring counts from 0, so p-1 to make spring understand
  const handlePageChange = (p: number) => {
    setPage(p - 1)
    window.scrollTo({ top: 0 })
  }

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

      <div className={s.applicationsContainer}>
        {applications.map((a) => (
          <ApplicationCard key={a.id} application={a} />
        ))}
      </div>

      <Pagination
        currentPage={page + 1}
        totalPages={totalPages}
        onPageChange={handlePageChange}
      />
    </div>
  )
}

export default MyApplicationsPage