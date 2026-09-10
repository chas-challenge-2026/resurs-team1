import type { User } from "../types/user"

const USER_KEY = "user"

export const setUser = (user: User) => localStorage.setItem(USER_KEY, JSON.stringify(user))

export const getUser = (): User | null => {
  const user = localStorage.getItem(USER_KEY)
  if(!user) return null
  try {
    return JSON.parse(user) as User
  } catch {
    return null
  }
}

export const removeUser = () => localStorage.removeItem(USER_KEY)

//Remove everything when logging out
export const clearAuthStorage = () => {
  removeUser()
}

//Get displayname
export const getUserDisplayName = (user: User): string => {
  return user.role === "company" ? user.companyName : user.name;
}