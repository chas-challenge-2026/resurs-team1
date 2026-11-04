import { useQuery } from "@tanstack/react-query"
import { getAuditLog, type AuditEvent } from "../api/auditApi"
import type { ApiErrorPayload } from "../api/client"

export const useAuditLog = (id: number | undefined) => {
  return useQuery<AuditEvent[], ApiErrorPayload>({
    queryKey: ["backofficeApplications", id, "audit"], // same prefix so decisions and requests refetch it too
    queryFn: () => getAuditLog(id!),
    enabled: typeof id === "number" && !isNaN(id),
  })
}
