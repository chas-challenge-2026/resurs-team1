import React, { useState } from "react"
import { Outlet, useOutlet } from "react-router-dom"
import { FiFile, FiSearch, FiX } from "react-icons/fi"
import { IoIosArrowBack, IoIosArrowForward } from "react-icons/io"
import ToggleSwitch, { type SwitchOption } from "../components/ToggleSwitch/ToggleSwitch"
import { useReviewApplications, useDecidedApplications, useCompanyApplications } from "../hooks/useApplication"
import Header from "../components/Header/Header"
import Input from "../components/Input/Input"
import SidebarCaseCard from "../components/SidebarCaseCard/SidebarCaseCard"
import Button from "../components/Button/Button"
import Loading from "../components/Loading/Loading"
import s from "./CaseWorkerLayout.module.css"
import { format10DigitNumber } from "../utils/formatters"

type ApplicationView = "reviewApplicationsView" | "decidedApplicationsView"

const options: SwitchOption<ApplicationView>[] = [
  {label: "Öppnade", value: "reviewApplicationsView"},
  {label: "Avslutade", value: "decidedApplicationsView"}
]

const CaseWorkerLayout = () => {
  const outlet = useOutlet()
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [search, setSearch] = useState("")
  const [activeOrgNumber, setActiveOrgNumber] = useState("")
  const [view, setView] = useState<ApplicationView>("reviewApplicationsView")

  const reviewApplication = useReviewApplications()
  const decidedApplication = useDecidedApplications()
  const companySearchResults = useCompanyApplications(activeOrgNumber)
  
  const isSearching = Boolean(activeOrgNumber)
  const activeList = isSearching 
    ? companySearchResults
    : view === "reviewApplicationsView" 
      ? reviewApplication 
      : decidedApplication

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

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = format10DigitNumber(e.target.value)
    setSearch(formatted)
    
    if (!formatted) {
      setActiveOrgNumber("")
    }
  }

  const handleSearch = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if(e.key === "Enter" && e.currentTarget.value.length === 11) {
      setActiveOrgNumber(search)
    }
  }

  const handleClearSearch = () => {
    setSearch("")
    setActiveOrgNumber("")
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
            placeholder="Sök på org.nr..."
            value={search}
            onChange={handleInputChange}
            onKeyDown={handleSearch}
          />
        }
      />
      <div className={s.content}>
        <aside className={`${s.sideBar} ${isCollapsed ? s.collapsed : ""}`}>
          <div className={s.sticky}>
            {!isCollapsed && (
              isSearching ? (
              <div className={s.searchHeaderInfo}>
                <span>Sökning: <strong>{activeOrgNumber}</strong></span>
                <Button variant="ghost" onClick={handleClearSearch} className={s.clearSearchButton}>
                  <FiX /> Rensa
                </Button>
              </div>
            ) : (
              <ToggleSwitch
                name="applicationView"
                options={options}
                selectedValue={view}
                onChange={setView}
                variant="accent"
              />
            ))}
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
            ) : activeList.data && activeList.data.length > 20 ? (
              <p className={s.endOfCases}>Slut på ärenden...</p>
            ) : null
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