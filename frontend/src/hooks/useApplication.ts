import { type QueryKey, useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { getApplicationById, getApplications, postApplication, getBackofficeApplicationById, postDecision, type Application, type ApplicationStatus, type CaseListItem, type DecidedApplication, type Decision, type NewApplicationPayload, type PagedResult, type ReviewApplication, getReviewPage, getDecidedPage, getApplicationDetails, type DetailedApplication } from "../api/applicationApi"
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

export const useReviewApplications = () => {
  return useInfiniteQuery<PagedResult<ReviewApplication>, ApiErrorPayload, CaseListItem[], QueryKey, number>({
    queryKey: ["backofficeApplications", "review"],
    queryFn: ({ pageParam }) => getReviewPage(pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.page + 1 < lastPage.totalPages ? lastPage.page + 1 : undefined,
    select: (data) => data.pages.flatMap((page) => page.content.map((application) => ({
      ...application, status: "UNDER_REVIEW" as const
    })))
  })
}

export const useDecidedApplications = () => {
  return useInfiniteQuery<PagedResult<DecidedApplication>, ApiErrorPayload, CaseListItem[], QueryKey, number>({
    queryKey: ["backofficeApplications", "decided"],
    queryFn: ({ pageParam }) => getDecidedPage(pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.page + 1 < lastPage.totalPages ? lastPage.page + 1 : undefined,
    select: (data) => data.pages.flatMap((page) => page.content.map((application) => ({
      ...application, status: application.decision as ApplicationStatus
    })))
  })
}

export const useDetailedApplication = (id: number) => {
  return useQuery<DetailedApplication, ApiErrorPayload>({
    queryKey: ["detailedapplication", id],
    queryFn: () => getApplicationDetails(id),
    enabled: typeof id === "number" && !isNaN(id),
  })
}