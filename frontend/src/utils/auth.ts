import type { AuthResponse } from "../api/authApi"

const TOKEN_KEY = "token"
const USER_KEY = "user"

//Token
export const setToken = (token: string) => localStorage.setItem(TOKEN_KEY, token)
export const getToken = (): string | null => localStorage.getItem(TOKEN_KEY)
export const removeToken = () => localStorage.removeItem(TOKEN_KEY)

//User
export const setUser = (user: AuthResponse) => localStorage.setItem(USER_KEY, JSON.stringify(user))

export const getUser = (): AuthResponse | null => {
  const user = localStorage.getItem(USER_KEY)
  if(!user) return null
  try {
    return JSON.parse(user) as AuthResponse
  } catch {
    return null
  }
}

export const removeUser = () => localStorage.removeItem(USER_KEY)

//Remove everything when logging out
export const clearAuthStorage = () => {
  removeToken()
  removeUser()
}