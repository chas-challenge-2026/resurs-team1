import axios from "axios"
import { clearAuthStorage } from "./auth"

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
  timeout: 10000,
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      clearAuthStorage()
      window.location.href = "/"
    }
    return Promise.reject(error)
  }
)

export default api