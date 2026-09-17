import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { getDocument, getDocuments, postDocument, type DocumentUploadPayload } from "../api/documentApi"
import type { ApplicationDocument } from "../api/applicationApi"

export const useDownloadDocument = () => {
  return useMutation({
    mutationFn: async({id, fileName}: {id: number, fileName: string}) => {
      const blob = await getDocument(id)

      //Create temporary link and trigger download in browser
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement("a")
      a.href = url
      a.download = fileName
      document.body.appendChild(a)
      a.click()

      //Clean up
      a.remove()
      window.URL.revokeObjectURL(url)
    }
  })
}

export const useDocuments = (applicationId: number) => {
  return useQuery<ApplicationDocument[], Error>({
    queryKey: ["documents", applicationId],
    queryFn: () => getDocuments(applicationId),
    enabled: Boolean(applicationId),
  })
}

export const useUploadDocument = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (params: DocumentUploadPayload) => postDocument(params),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: ["documents", variables.applicationId]
      })
    }
  })
}