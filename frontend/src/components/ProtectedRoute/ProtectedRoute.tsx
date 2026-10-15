import { Navigate, Outlet } from "react-router-dom"
import { useUser } from "../../hooks/useUser"
import type { UserRole } from "../../types/user"

interface ProtectedRouteProps {
  allowedRoles?: UserRole[]
}

const ProtectedRoute = ({allowedRoles}: ProtectedRouteProps) => {
  const { data: user } = useUser()

  if(!user) {
    return <Navigate to="/" replace />
  }

  if(allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/404" replace />
  }

  return <Outlet />
}

export default ProtectedRoute