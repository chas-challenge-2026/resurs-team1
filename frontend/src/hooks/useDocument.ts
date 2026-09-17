import { useMutation } from "@tanstack/react-query"
import { getDocument } from "../api/documentApi"

export const useDownloadDocument = () => {
  return useMutation({
    mutationFn: async({id, fileName}: {id: number, fileName: string}) => {
      const blob = await getDocument(id)

      //Create temporary link and trigger download in browser
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement("a")
      a.href = url
      a.download = fileName
      a.click()

      //Clean up
      a.remove()
      window.URL.revokeObjectURL(url)
    }
  })
}