import { Navigate, Outlet } from "react-router-dom"
import { getDefaultRedirectPath, getUser } from "../../utils/auth"

const PublicOnlyRoute = () => {
  const user = getUser()

  if(user) {
    return <Navigate to={getDefaultRedirectPath(user)} replace />
  }

  return <Outlet />
}

export default PublicOnlyRoute