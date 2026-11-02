import { useEffect } from "react"
import { Outlet, useLocation } from "react-router-dom"
import { useUser } from "../hooks/useUser"
import { isCaseWorkerUser } from "../utils/auth"
import { FiSearch } from "react-icons/fi"
import Header from "../components/Header/Header"
import Input from "../components/Input/Input"

const FullWidthLayout = () => {

  const { data: user } = useUser()
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
        search={isCaseWorkerUser(user) ? (
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