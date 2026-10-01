import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { getApplicationById, getApplications, postApplication, getBackofficeApplicationById, type Application, type NewApplicationPayload } from "../api/applicationApi"
import type { ApiErrorPayload } from "../api/client"

export const useApplications = () => {
  return useQuery<Application[], ApiErrorPayload>({
    queryKey: ["applications"],
    queryFn: getApplications,
  })
}

export const useApplication = (id: number | undefined) => {
  return useQuery<Application, ApiErrorPayload>({
    queryKey: ["applications", id],
    queryFn: () => getApplicationById(id!),
    enabled: typeof id === "number" && !isNaN(id),
  })
}

export const useSubmitApplication = () => {
  const queryClient = useQueryClient()

  return useMutation<Application, ApiErrorPayload, NewApplicationPayload>({
    mutationFn: (payload) => postApplication(payload),
    meta: { preventGlobalToast: true}, // the page shows its own error under the button
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["applications"]}) // remove old memory to force refresh of new applications
    },
  })
}

export const useBackofficeApplication = (id: number | undefined) => {
  return useQuery<Application, ApiErrorPayload>({
    queryKey: ["backofficeApplications", id],
    queryFn: () => getBackofficeApplicationById(id!),
    enabled: typeof id === "number" && !isNaN(id),
  })
}