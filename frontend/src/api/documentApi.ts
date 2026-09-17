import api from "./client"

export const getDocument = async (id: number): Promise<Blob> => {
  const response = await api.get<Blob>(`/documents/${id}`, {responseType: "blob"})
  return response.data
}