import axios, { AxiosError } from "axios"
import { clearAuthStorage } from "../utils/auth"

export interface ApiErrorPayload {
  status?: number;
  message: string;
  fieldErrors?: Record<string, string>;
  code?: string;
  originalError: AxiosError;
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
  timeout: 10000,
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
      clearAuthStorage()
      window.location.href = "/"
      return Promise.reject(error)
    } else if (status === 401 && isLoginRequest) {
      fallbackMessage = "Felaktiga inloggningsuppgifter."
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