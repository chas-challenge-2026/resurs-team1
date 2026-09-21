import type { ApplicationDocument } from "./applicationApi"
import api from "./client"

export interface DocumentUploadPayload {
  applicationId: number
  docType: string
  fileName: string
}

export const getDocument = async (id: number): Promise<Blob> => {
  const response = await api.get<Blob>(`/documents/${id}`, {responseType: "blob"})
  return response.data
}

export const getDocuments = async (applicationId: number): Promise<ApplicationDocument[]> => {
  const response = await api.get<ApplicationDocument[]>(`/documents/application/${applicationId}`)
  return response.data
}

//TODO: Fix this so it works
export const postDocument = async ({applicationId, docType, fileName}: DocumentUploadPayload) => {
  const response = await api.post<ApplicationDocument>("/documents/upload",
    { file: fileName },
    {
      params: {
        applicationId,
        docType,
      },
    }
  )
  return response.data
}