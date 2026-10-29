import { Outlet } from "react-router-dom"
import { FiSearch } from "react-icons/fi"
import Header from "../components/Header/Header"
import Input from "../components/Input/Input"
import s from "./CardLayout.module.css"
import { useUser } from "../hooks/useUser"
import { isCaseWorkerUser } from "../utils/auth"

const CardLayout = () => {

  const { data: user } = useUser()

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
      <main className={s.main}>
        <Outlet />
      </main>
    </div>
  )
}

export default CardLayout