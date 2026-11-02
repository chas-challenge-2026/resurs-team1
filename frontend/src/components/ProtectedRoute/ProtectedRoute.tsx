import { Navigate, Outlet } from "react-router-dom"
import { useUser } from "../../hooks/useUser"
import type { UserRole } from "../../types/user"
import Loading from "../Loading/Loading"
import NotFoundPage from "../../pages/NotFound/NotFoundPage"
import CardLayout from "../../layouts/CardLayout"

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

  if(allowedRoles && !user.roles.some((role) => allowedRoles.includes(role))) {
    return (
      <CardLayout>
        <NotFoundPage />
      </CardLayout>
    )
  }

  return <Outlet />
}

export default ProtectedRoute