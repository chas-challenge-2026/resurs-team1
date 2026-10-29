import { QueryClient, QueryCache, MutationCache } from "@tanstack/react-query"
import type { ApiErrorPayload } from "./client"
import { toast } from 'sonner'

export const queryClient = new QueryClient({
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
      // via custom mutation metadata: 
      // useMutation({ meta: { preventGlobalToast: true } })
      if (!mutation.meta?.preventGlobalToast && (!error.status || error.status >= 500)) {
        toast.error(error.message)
      }
    },
  })
})