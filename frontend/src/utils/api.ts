import axios from "axios"
import { getToken } from "./auth"

// dev: vite proxy strips "/api". 
// prod: (backend has no json api as of writing this) -- sending json but not getting json back
// no proxy, so backend must answer on /api too "server.servlet.context-path=/api"
const api = axios.create({
  baseURL: "/api",
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default api