import { useState } from "react"
import { Outlet, useOutlet } from "react-router-dom"
import { FiFile, FiSearch } from "react-icons/fi"
import { IoIosArrowBack, IoIosArrowForward } from "react-icons/io"
import ToggleSwitch, { type SwitchOption } from "../components/ToggleSwitch/ToggleSwitch"
import { useReviewApplications, useDecidedApplications } from "../hooks/useApplication"
import Header from "../components/Header/Header"
import Input from "../components/Input/Input"
import SidebarCaseCard from "../components/SidebarCaseCard/SidebarCaseCard"
import Button from "../components/Button/Button"
import Loading from "../components/Loading/Loading"
import s from "./CaseWorkerLayout.module.css"

type ApplicationView = "reviewApplicationsView" | "decidedApplicationsView"

const options: SwitchOption<ApplicationView>[] = [
  {label: "Öppnade", value: "reviewApplicationsView"},
  {label: "Avslutade", value: "decidedApplicationsView"}
]

const CaseWorkerLayout = () => {
  const outlet = useOutlet()
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [search, setSearch] = useState("")
  const [view, setView] = useState<ApplicationView>("reviewApplicationsView")
  const reviewApplication = useReviewApplications()
  const decidedApplication = useDecidedApplications()
  
  const activeList = view === "reviewApplicationsView" ? reviewApplication : decidedApplication

  // TODO: Add endpoint for searching org.nr.
  // const query = search.trim().toLowerCase().replaceAll("-", "")

  const toggleCollapsed = () => {
    setIsCollapsed(!isCollapsed)
  }

  
  const handleShowMore = () => {
    if(activeList.hasNextPage) {
      activeList.fetchNextPage()
    }
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
          <div className={s.sticky}>
            {!isCollapsed && (
              <ToggleSwitch
                name="applicationView"
                options={options}
                selectedValue={view}
                onChange={setView}
                variant="accent"
              />
            )}
            <div className={s.sideBarHeader}>
              {!isCollapsed && (
                <p className={s.casesLength}>{activeList.data?.length} ärenden</p>
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
          </div>

          {!isCollapsed && (
            <>
              {activeList && (
                <div className={s.casesWrapper}>
                  {activeList.isPending && <Loading size="sm" label="Hämtar ärenden..." />}
                  {activeList.isError && <p>{activeList.error.message}</p>}
                  {activeList.data?.map((application) => (
                    <SidebarCaseCard key={application.id} application={application} />
                  ))}
                </div>
              )}

            </>
          )}

          {!isCollapsed && (
            activeList.hasNextPage ? (
              <Button
                variant="primary"
                color="var(--color-text-main)"
                className={s.showMoreButton}
                onClick={handleShowMore}
              >
                Visa fler
              </Button>
            ) : (
              <p className={s.endOfCases}>Slut på ärenden...</p>
            )
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