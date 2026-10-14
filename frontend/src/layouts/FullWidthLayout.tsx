import { Outlet } from "react-router-dom"
import { FiSearch } from "react-icons/fi"
import { getUser } from "../utils/auth"
import Header from "../components/Header/Header"
import Input from "../components/Input/Input"

const FullWidthLayout = () => {

  const user = getUser()
  // case search is a caseworker tool, companies never see it
  const isCaseWorker = user?.role === "caseWorker";

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