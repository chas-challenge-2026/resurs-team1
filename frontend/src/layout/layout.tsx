import { Outlet } from "react-router-dom"
import Header from "../components/Header/Header"
import s from "./Layout.module.css"

interface LayoutProps {
  fullWidth?: boolean
}

const Layout = ({fullWidth = false}: LayoutProps) => {
  return(
    <div>
    <Header company="Coconut AB" onLogout={() => {}}>
      <input type="search" />
    </Header>
      <main className={fullWidth ? s.mainFullWidth : s.main}>
        <Outlet />
      </main>
    </div>
  )
}

export default Layout