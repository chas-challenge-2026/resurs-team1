import { queryClient } from "../main";
import { useNavigate } from "react-router-dom";
import { logoutUser } from "../api/authApi";

const useLogout = () => {
  const navigate = useNavigate()

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