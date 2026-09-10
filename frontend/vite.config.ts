import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    // testing-library only registers its auto-cleanup if a global afterEach exists
    globals: true,
  },
  server: {
    // /api is a dev-only prefix so requests stay same-origin and skip CORS.
    // backend routes have no prefix, so strip it before forwarding.
    proxy: {
      "/api": {
        target: "http://localhost:8083",
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
