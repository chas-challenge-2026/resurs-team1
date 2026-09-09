import { createContext, useContext } from "react"
import type { User } from "../types/user"

export interface AuthContextValue {
  user: User | null
  isLoggedIn: boolean
  isLoading: boolean
  handleAuthSuccess: (token: string, user: User) => void
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)


export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error("useAuth used outside AuthProvider")
  return context
}
