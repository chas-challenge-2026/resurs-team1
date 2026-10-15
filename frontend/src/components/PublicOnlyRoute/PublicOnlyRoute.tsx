import { Navigate, Outlet } from "react-router-dom"
import { getDefaultRedirectPath } from "../../utils/auth"
import { useUser } from "../../hooks/useUser"
import Loading from "../Loading/Loading"

const PublicOnlyRoute = () => {
  const { data: user, isPending } = useUser()

  if(isPending) {
    return <Loading />
  }

  if(user) {
    return <Navigate to={getDefaultRedirectPath(user)} replace />
  }

  return <Outlet />
}

export default PublicOnlyRoute