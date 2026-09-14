import { useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { clearAuthStorage } from "../utils/auth";

const useLogout = () => {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const logout = () => {
    clearAuthStorage()
    queryClient.clear()
    navigate("/")
  }

  return { logout }
}

export { useLogout }