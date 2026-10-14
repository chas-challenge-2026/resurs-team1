import { Navigate, Outlet } from "react-router-dom"
import { getDefaultRedirectPath } from "../../utils/auth"
import { useUser } from "../../hooks/useUser"

const PublicOnlyRoute = () => {
  const { data: user } = useUser()
  console.log(user)

  if(user) {
    return <Navigate to={getDefaultRedirectPath(user)} replace />
  }

  return <Outlet />
}

export default PublicOnlyRoute