import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { getApplicationById, getApplications, postApplication, getBackofficeApplicationById, getBackofficeApplications, type Application, type ApplicationStatus, type BackofficeLists, type CaseListItem, type NewApplicationPayload } from "../api/applicationApi"

export const useApplications = () => {
  return useQuery<Application[], Error>({
    queryKey: ["applications"],
    queryFn: getApplications,
  })
}

export const useApplication = (id: number | undefined) => {
  return useQuery<Application, Error>({
    queryKey: ["applications", id],
    queryFn: () => getApplicationById(id!),
    enabled: typeof id === "number" && !isNaN(id),
  })
}

export const useSubmitApplication = () => {
  const queryClient = useQueryClient()

  return useMutation<Application, Error, NewApplicationPayload>({
    mutationFn: (payload) => postApplication(payload),
    meta: { preventGlobalToast: true}, // the page shows its own error under the button
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["applications"]}) // remove old memory to force refresh of new applications
    },
  })
}

export const useBackofficeApplication = (id: number | undefined) => {
  return useQuery<Application, Error>({
    queryKey: ["backofficeApplications", id],
    queryFn: () => getBackofficeApplicationById(id!),
    enabled: typeof id === "number" && !isNaN(id),
  })
}

export const useBackofficeApplications = () => {
  return useQuery<BackofficeLists, Error, CaseListItem[]>({
    queryKey: ["backofficeApplications"],
    queryFn: getBackofficeApplications,
    // backend sends two lists without status, the sidebar wants one list with it
    select: (data) => [
      ...data.reviewApplications.map((application) => ({
        id: application.id,
        status: "UNDER_REVIEW" as const,
        companyName: application.companyName,
        requestedAmount: application.requested_amount,
        createdAt: application.createdAt,
      })),
      // big problemo TODO: -----> backend only sends the 20 oldest decided cases, so newer decisions go missing after 20 <---------------------
      ...data.decidedApplications.map((application) => ({
        id: application.id,
        status: application.decision as ApplicationStatus, 
        companyName: application.companyName,
        requestedAmount: application.requestedAmount,
        createdAt: application.createdAt,
      })),
    ],
  })
}