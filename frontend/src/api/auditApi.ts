import api from "./client"
import type { ApplicationStatus } from "./applicationApi"

//taken from AuditAction.java
export type AuditActions =
  | "APPLICATION_CREATED"
  | "SCORING_RUN"
  | "DOCUMENT_UPLOADED"
  | "MANUAL_DECISION"
  | "COMMENT_ADDED"
  | "DOCUMENTS_REQUESTED"

// plain strings in AuditService.java, not an enum
export type ActorType = "COMPANY" | "SYSTEM" | "CASE_WORKER"

export interface AuditEvent {
  sequenceNumber: number,
  occurredAt: string,
  action: AuditActions,
  actor: string,
  data: {
    actorType: ActorType
    workerName?: string
    newStatus?: ApplicationStatus
    comment?: string
    filename?: string
  }
}

export const getAuditLog = async (id: number): Promise<AuditEvent[]> => {
  const response = await api.get<AuditEvent[]>(`/backoffice/application/${id}/audit`)
  return response.data
}
