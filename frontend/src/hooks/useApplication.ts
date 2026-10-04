import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { getApplicationById, getApplications, postApplication, getBackofficeApplicationById, getBackofficeApplications, postDecision, type Application, type ApplicationStatus, type BackofficeLists, type CaseListItem, type Decision, type NewApplicationPayload } from "../api/applicationApi"
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

export const useDecideApplication = () => {
  const queryClient = useQueryClient()

  return useMutation<void, ApiErrorPayload, { id: number, decision: Decision, comment: string }>({
    mutationFn: ({ id, decision, comment }) => postDecision(id, decision, comment),
    meta: { preventGlobalToast: true },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["backofficeApplications"] }) // refreshes both the sidebar list and the open case
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

export const useBackofficeApplications = () => {
  return useQuery<BackofficeLists, ApiErrorPayload, CaseListItem[]>({
    queryKey: ["backofficeApplications"],
    queryFn: getBackofficeApplications,
    // backend sends two lists without status, the sidebar wants one list with it
    select: (data) => [
      ...data.reviewApplications.content.map((application) => ({
        id: application.id,
        status: "UNDER_REVIEW" as const,
        companyName: application.companyName,
        orgNumber: application.orgNumber,
        requestedAmount: application.requestedAmount,
        createdAt: application.createdAt,
      })),
      // big problemo TODO: -----> backend only sends the 20 oldest decided cases, so newer decisions go missing after 20 <---------------------
      ...data.decidedApplications.content.map((application) => ({
        id: application.id,
        status: application.decision as ApplicationStatus,
        companyName: application.companyName,
        orgNumber: application.orgNumber,
        requestedAmount: application.requestedAmount,
        createdAt: application.createdAt,
      })),
    ],
  })
}