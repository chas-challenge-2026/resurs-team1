const TOKEN_KEY = "token"

export const setToken = (token: string) => localStorage.setItem(TOKEN_KEY, token)
export const getToken = (): string | null => localStorage.getItem(TOKEN_KEY)
export const removeToken = () => localStorage.removeItem(TOKEN_KEY)