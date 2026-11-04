import { useQuery } from "@tanstack/react-query"
import { getMe } from "../api/authApi"
import type { User } from "../types/user"

export const useUser = () => {
  return useQuery<User>({
    queryKey: ["me"],
    queryFn: getMe,
    retry: false,
    // without this it refetches in a loop
    retryOnMount: false,
    staleTime: 1000 * 60 * 5,
  })
}