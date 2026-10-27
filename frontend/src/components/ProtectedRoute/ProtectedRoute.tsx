import { Navigate, Outlet } from "react-router-dom"
import { useUser } from "../../hooks/useUser"
import type { UserRole } from "../../types/user"
import Loading from "../Loading/Loading"

interface ProtectedRouteProps {
  allowedRoles?: UserRole[]
}

const ProtectedRoute = ({allowedRoles}: ProtectedRouteProps) => {
  const { data: user, isPending } = useUser()

  if(isPending) {
    return <Loading />
  }

  if(!user) {
    return <Navigate to="/" replace />
  }

  if(allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/404" replace />
  }

  return <Outlet />
}

export default ProtectedRoute