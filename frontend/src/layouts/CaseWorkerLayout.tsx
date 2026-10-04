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
import s from "./CaseWorkerLayout.module.css"

const CaseWorkerLayout = () => {
  const [isCollapsed, setIsCollapsed] = useState(false)
  const outlet = useOutlet()
  const { data: cases, isPending, isError, error } = useBackofficeApplications()

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
          />
        }
      />
      <div className={s.content}>
        <aside className={`${s.sideBar} ${isCollapsed ? s.collapsed : ""}`}>
          <div className={s.sideBarHeader}>
            {!isCollapsed && cases && (
              <p className={s.casesLength}>{cases.length} ärenden</p>
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
              {cases?.map((application) => (
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