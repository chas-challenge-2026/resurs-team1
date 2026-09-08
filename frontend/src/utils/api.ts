import axios, { AxiosError } from "axios"
import { getToken, removeToken } from "./auth"

export interface ApiErrorPayload {
  status?: number;
  message: string;
  fieldErrors?: Record<string, string>;
  code?: string;
  originalError: AxiosError;
}

// dev: vite proxy strips "/api". 
// prod: (backend has no json api as of writing this) -- sending json but not getting json back
// no proxy, so backend must answer on /api too "server.servlet.context-path=/api"
const api = axios.create({
  baseURL: "/api",
  timeout: 10000,
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    return response
  },

  (error: AxiosError<ApiErrorPayload>) => {
    const status = error.response?.status
    const isLoginRequest = error.config?.url?.includes("/login")

    const backendMessage = error.response?.data?.message
    let fallbackMessage = "Ett oväntat fel uppstod. Försök igen senare."

    if (!error.response) {
      fallbackMessage = "Kunde inte ansluta till servern. Kontrollera din internetanslutning."
    } else if (status === 401 && !isLoginRequest) {
      fallbackMessage = "Sessionen har gått ut, loggar ut..."
      removeToken()
      window.location.href = "/logga-in"
    } else if (status === 403) {
      fallbackMessage = "Du saknar behörighet att utföra denna åtgärd."
    } else if (status === 404) {
      fallbackMessage = "Den begärda resursen kunde inte hittas."
    } else if (status && status >= 500) {
      fallbackMessage = "Ett serverfel uppstod. Vänligen försök igen om en stund."
    }

    const normalizedError = {
      status,
      message: backendMessage || fallbackMessage,
      fieldErrors: error.response?.data?.fieldErrors,
      code: error.response?.data?.code,
      originalError: error,
    };

    return Promise.reject(normalizedError)
  }
)

export default api