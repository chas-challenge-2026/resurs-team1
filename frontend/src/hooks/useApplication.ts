import { useQuery } from "@tanstack/react-query"
import { getApplications, type Application } from "../api/applicationApi"

export const useApplications = () => {
  return useQuery<Application[], Error>({
    queryKey: ["applications"],
    queryFn: getApplications,
  })
}