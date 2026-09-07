import { useEffect, useState } from "react"
import type { ReactNode } from "react"
import { AuthContext } from "./AuthContext"
import type { User } from "./AuthContext"
import { getToken, removeToken, setToken } from "../utils/auth"

// uses thymleaf now -> no REST profile route -> confirm path before wiring login
const PROFILE_URL = "/api/profile"

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  // only loading when there is a token to trade for a user
  const [isLoading, setIsLoading] = useState(() => !!getToken())

  // a reload keeps the token but loses the user object, so fetch it back
  useEffect(() => {
    const token = getToken()
    if (!token) return

    let cancelled = false

    fetch(PROFILE_URL, { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
      .then((data: User) => {
        if (!cancelled) setUser(data)
      })
      // rejected token is not an error to report, it just means the session is gone
      .catch(() => {
        if (!cancelled) removeToken()
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const handleAuthSuccess = (token: string, userData: User) => {
    setToken(token)
    setUser(userData)
  }

  const logout = () => {
    removeToken()
    setUser(null)
  }

  return (
    <AuthContext.Provider
      value={{ user, isLoggedIn: !!user, isLoading, handleAuthSuccess, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}
