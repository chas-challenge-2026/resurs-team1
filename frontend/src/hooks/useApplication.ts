import { useQuery } from "@tanstack/react-query"
import { getApplicationById, getApplications, type Application } from "../api/applicationApi"

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