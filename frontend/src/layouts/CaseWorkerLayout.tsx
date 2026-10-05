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
import { formatReferenceNumber } from "../utils/formatters"
import s from "./CaseWorkerLayout.module.css"

type ApplicationView = "reviewApplicationsView" | "decidedApplicationsView"

const options: SwitchOption<ApplicationView>[] = [
  {label: "Öppnade", value: "reviewApplicationsView"},
  {label: "Avslutade", value: "decidedApplicationsView"}
]

const CaseWorkerLayout = () => {
  const [isCollapsed, setIsCollapsed] = useState(false)
  const outlet = useOutlet()
  const [search, setSearch] = useState("")
  const [view, setView] = useState<ApplicationView>("reviewApplicationsView")
  const { data: reviewApplications, isPending: reviewIsPending, isError: reviewIsError, error: reviewError } = useReviewApplications()
  const { data: decidedApplications, isPending: decidedIsPending, isError: decidedIsError, error: decidedError } = useDecidedApplications()

  // ignore dashes so "5566778899" finds "556677-8899"
  const query = search.trim().toLowerCase().replaceAll("-", "")
  const filteredReviewCases = reviewApplications?.filter((application) =>
    formatReferenceNumber(application.id).toLowerCase().replaceAll("-", "").includes(query) ||
    application.orgNumber.replaceAll("-", "").includes(query)
  )
  const filteredDecidedCases = decidedApplications?.filter((application) =>
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
              <p className={s.casesLength}>{view === "reviewApplicationsView" ? filteredReviewCases?.length : filteredDecidedCases?.length} ärenden</p>
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
            <>
              
              {view === "reviewApplicationsView" && (
                <div className={s.casesWrapper}>
                  {reviewIsPending && <Loading size="sm" label="Hämtar ärenden..." />}
                  {reviewIsError && <p>{reviewError.message}</p>}
                  {filteredReviewCases?.map((application) => (
                    <SidebarCaseCard key={application.id} application={application} />
                  ))}
                </div>
              )}

              {view === "decidedApplicationsView" && (
                <div className={s.casesWrapper}>
                  {decidedIsPending && <Loading size="sm" label="Hämtar ärenden..." />}
                  {decidedIsError && <p>{decidedError.message}</p>}
                  {filteredDecidedCases?.map((application) => (
                    <SidebarCaseCard key={application.id} application={application} />
                  ))}
                </div>
              )}
            </>
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