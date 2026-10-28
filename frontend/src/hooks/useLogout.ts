import { useNavigate } from "react-router-dom";
import { logoutUser } from "../api/authApi";
import { useQueryClient } from "@tanstack/react-query";

const useLogout = () => {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const logout = async () => {
    try {
      await logoutUser()
    } catch (error) {
      console.error("Logout failed on server:", error)
    } finally {
      queryClient.clear()
      navigate("/")
    }
  }

  return { logout }
}

export { useLogout }