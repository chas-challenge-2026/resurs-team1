import { Navigate, Outlet } from "react-router-dom"
import { getUser } from "../../utils/auth"
import type { UserRole } from "../../types/user"

interface ProtectedRouteProps {
  allowedRoles?: UserRole[]
}

const ProtectedRoute = ({allowedRoles}: ProtectedRouteProps) => {
  const user = getUser()

  if(!user) {
    return <Navigate to="/" replace />
  }

  if(allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/404" replace />
  }

  return <Outlet />
}

export default ProtectedRoute