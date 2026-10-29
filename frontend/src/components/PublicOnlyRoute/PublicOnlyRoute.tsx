import { Navigate, Outlet, useLocation } from "react-router-dom"
import { getDefaultRedirectPath } from "../../utils/auth"
import { useUser } from "../../hooks/useUser"
import Loading from "../Loading/Loading"

const PublicOnlyRoute = () => {
  const { data: user, isPending } = useUser()
  const location = useLocation()

  if(isPending) {
    return <Loading />
  }

  if(user) {
    return <Navigate to={getDefaultRedirectPath(user, location.pathname)} replace />
  }

  return <Outlet />
}

export default PublicOnlyRoute