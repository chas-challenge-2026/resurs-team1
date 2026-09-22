import { useState } from "react"
import { Outlet, useOutlet } from "react-router-dom"
import { FiFile, FiSearch } from "react-icons/fi"
import { IoIosArrowBack, IoIosArrowForward } from "react-icons/io"
import type { Application } from "../api/applicationApi"
import Header from "../components/Header/Header"
import Input from "../components/Input/Input"
import SidebarCaseCard from "../components/SidebarCaseCard/SidebarCaseCard"
import Button from "../components/Button/Button"
import s from "./CaseWorkerLayout.module.css"

// TODO: Switch out MOCK_DATA to real data
const SAMPLE_CASES: Application[] = [
  { id: 387139, companyName: "Nordvik Bygg AB", orgNumber: "556600-0000", authorizedSignatory: "Anna Nordvik", purpose: "Rörelsekapital", requestedAmount: 3000000, status: "PENDING_DOCS", createdAt: "2026-08-27T09:00:00Z", updatedAt: "2026-08-27T09:00:00Z" },
  { id: 387142, companyName: "Lindqvist Logistik AB", orgNumber: "559012-3456", authorizedSignatory: "Erik Lindqvist", purpose: "Maskininvestering", requestedAmount: 850000, status: "UNDER_REVIEW", createdAt: "2026-08-29T09:00:00Z", updatedAt: "2026-08-29T09:00:00Z" },
  { id: 387150, companyName: "Solberga Café & Bageri AB", orgNumber: "556788-1122", authorizedSignatory: "Sara Solberg", purpose: "Expansion", requestedAmount: 1200000, status: "APPROVED", createdAt: "2026-09-02T09:00:00Z", updatedAt: "2026-09-02T09:00:00Z" },
  { id: 387155, companyName: "Västra Götalands Maskin- och Fastighetsservice AB", orgNumber: "559334-7788", authorizedSignatory: "Johan Hallberg", purpose: "Rörelsekapital", requestedAmount: 400000, status: "REJECTED", createdAt: "2026-09-08T09:00:00Z", updatedAt: "2026-09-08T09:00:00Z" },
]

const CaseWorkerLayout = () => {
  const [isCollapsed, setIsCollapsed] = useState(false)
  const outlet = useOutlet()

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
            {!isCollapsed && (
              <p className={s.casesLength}>{SAMPLE_CASES.length} ärenden</p>
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
              {SAMPLE_CASES.map((application) => (
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