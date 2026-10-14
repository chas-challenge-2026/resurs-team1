import type { ApplicationDocument } from "./applicationApi"
import api from "./client"

export interface DocumentUploadPayload {
  applicationId: number
  docType: string
  file: File
}

export const getDocument = async (id: number): Promise<Blob> => {
  const response = await api.get<Blob>(`/documents/${id}`, {responseType: "blob"})
  return response.data
}

export const getDocuments = async (applicationId: number): Promise<ApplicationDocument[]> => {
  const response = await api.get<ApplicationDocument[]>(`/documents/application/${applicationId}`)
  return response.data
}

export const postDocument = async ({applicationId, docType, file}: DocumentUploadPayload) => {
  const formData = new FormData()
  formData.append("file", file)

  const response = await api.post<ApplicationDocument>(`/documents/upload?applicationId=${applicationId}&docType=${docType}`, formData)
  return response.data
}