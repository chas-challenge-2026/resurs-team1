import { useState } from "react"
import { Outlet, useOutlet } from "react-router-dom"
import { FiFile, FiSearch } from "react-icons/fi"
import { IoIosArrowBack, IoIosArrowForward } from "react-icons/io"
import { useBackofficeApplications } from "../hooks/useApplication"
import Header from "../components/Header/Header"
import Input from "../components/Input/Input"
import SidebarCaseCard from "../components/SidebarCaseCard/SidebarCaseCard"
import Button from "../components/Button/Button"
import Loading from "../components/Loading/Loading"
import { formatReferenceNumber } from "../utils/formatters"
import s from "./CaseWorkerLayout.module.css"

const CaseWorkerLayout = () => {
  const [isCollapsed, setIsCollapsed] = useState(false)
  const outlet = useOutlet()
  const [search, setSearch] = useState("")
  const { data: cases, isPending, isError, error } = useBackofficeApplications()

  // ignore dashes so "5566778899" finds "556677-8899"
  const query = search.trim().toLowerCase().replaceAll("-", "")
  const filteredCases = cases?.filter((application) =>
    formatReferenceNumber(application.id).toLowerCase().replaceAll("-", "").includes(query) ||
    application.orgNumber.replaceAll("-", "").includes(query)
  )

  const toggleCollapsed = () => {
    setIsCollapsed(!isCollapsed)
  }

  return(
    <div>
      <Header
        search={
          <Input
            id="caseSearch"
            type="search"
            label="Sök ärende"
            hideLabel
            size="sm"
            icon={<FiSearch />}
            placeholder="Sök på ärendenummer eller org.nr..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        }
      />
      <div className={s.content}>
        <aside className={`${s.sideBar} ${isCollapsed ? s.collapsed : ""}`}>
          <div className={s.sideBarHeader}>
            {!isCollapsed && filteredCases && (
              <p className={s.casesLength}>{filteredCases.length} ärenden</p>
            )}

            <Button
              variant="ghost"
              onClick={toggleCollapsed}
              aria-label={isCollapsed ? "Expandera sidofält" : "Minimera sidofält"}
              className={s.toggleButton}
            >
              {isCollapsed ? <IoIosArrowForward /> : <IoIosArrowBack />}
            </Button>
          </div>

          {!isCollapsed && (
            <div className={s.casesWrapper}>
              {isPending && <Loading size="sm" label="Hämtar ärenden..." />}
              {isError && <p>{error.message}</p>}
              {filteredCases?.length === 0 && <p>Inga ärenden matchar sökningen</p>}
              {filteredCases?.map((application) => (
                <SidebarCaseCard key={application.id} application={application} />
              ))}
            </div>
          )}
        </aside>
        <main className={`${s.main} ${isCollapsed ? "" : s.hidden}`}>
          {outlet ? (
            <Outlet />
          ) : (
            <div className={s.emptyState}>
              <FiFile />
              <p>Välj ett ärende i listan</p>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}

export default CaseWorkerLayout