import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ApiErrorPayload } from './api/client.ts'
import { toast } from 'sonner'
import App from './App.tsx'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },

  // Global error handler for all GET requests (useQuery)
  queryCache: new QueryCache({
    onError: (err) => {
      const error = err as unknown as ApiErrorPayload
      if (!error.status || error.status >= 500) {
        toast.error(error.message)
      }
    },
  }),

  // Global error handler for all POST/PUT/DELETE requests (useMutation)
  mutationCache: new MutationCache({
    onError: (err, _variables, _context, mutation) => {
      const error = err as unknown as ApiErrorPayload
      
      // Display toast on 500/network errors UNLESS the component explicitly opts out
      // via custom mutation metadata: useMutation({ meta: { preventGlobalToast: true } })
      if (!mutation.meta?.preventGlobalToast && (!error.status || error.status >= 500)) {
        toast.error(error.message)
      }
    },
  })
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
