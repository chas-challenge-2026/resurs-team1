import { Outlet } from "react-router-dom"
import Header from "../components/Header/Header"
import Input from "../components/Input/Input"
import { FiSearch } from "react-icons/fi"
import s from "./Layout.module.css"
import { getUser } from "../utils/auth"

interface LayoutProps {
  fullWidth?: boolean
}

const Layout = ({fullWidth = false}: LayoutProps) => {

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
      <main className={fullWidth ? s.mainFullWidth : s.main}>
        <Outlet />
      </main>
    </div>
  )
}

export default Layout