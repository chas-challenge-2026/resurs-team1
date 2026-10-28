import { useNavigate } from "react-router-dom";
import { logoutUser } from "../api/authApi";
import { useQueryClient } from "@tanstack/react-query";
import type { UserRole } from "../types/user";

const useLogout = (userRole: UserRole | undefined) => {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const logout = async () => {
    try {
      await logoutUser()
    } catch (error) {
      console.error("Logout failed on server:", error)
    } finally {
      queryClient.clear()
      if(userRole === "caseWorker") {
        navigate("/handlaggare")
      } else {
        navigate("/")
      }
    }
  }

  return { logout }
}

export { useLogout }