import { Outlet } from "react-router-dom"
import Header from "../components/Header/Header"

const Layout = () => {
  return(
    <div>
    <Header company="Coconut AB" onLogout={() => {}}>
      <input type="search" />
    </Header>
      <main>
        <Outlet />
      </main>
    </div>
  )
}

export default Layout