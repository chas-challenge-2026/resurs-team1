import { Outlet, useLocation } from "react-router-dom"
import { FiSearch } from "react-icons/fi"
import Header from "../components/Header/Header"
import Input from "../components/Input/Input"
import { useUser } from "../hooks/useUser"
import { useEffect } from "react"

const FullWidthLayout = () => {

  const { data: user } = useUser()
  // case search is a caseworker tool, companies never see it
  const isCaseWorker = user?.role === "caseWorker";
  const location = useLocation()

  useEffect(() => {
    if(location.pathname === "/handlaggare") {
      document.documentElement.classList.add('theme-caseworker');
    }

    // 2. Cleanup
    return () => {
      document.documentElement.classList.remove('theme-caseworker');
    };
  }, [location.pathname])

  return(
    <div>
      <Header
        search={isCaseWorker ? (
          <Input
            id="caseSearch"
            type="search"
            label="Sök ärende"
            hideLabel
            size="sm"
            icon={<FiSearch />}
            placeholder="Sök på ärendenummer eller org.nr..."
          />
        ) : undefined}
      />
      <main>
        <Outlet />
      </main>
    </div>
  )
}

export default FullWidthLayout