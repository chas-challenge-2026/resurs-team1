import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import * as documentApi from "../api/documentApi"
import { renderHook, waitFor } from "@testing-library/react"
import { useDocuments, useDownloadDocument, useUploadDocument } from "./useDocument"
import type { ApplicationDocument } from "../api/applicationApi"

const createTestQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: {retry: false},
    mutations: {retry: false}
  }
})

const createWrapper = (queryClient = createTestQueryClient()) => {
  return ({children}: {children: React.ReactNode}) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}

const mockDocument: ApplicationDocument = {
  id: 1,
  applicationId: 3,
  filename: "arsredovisning.pdf",
  docType: "pdf",
  uploadedAt: "2026-09-29T10:00:00Z",
}

const mockDocuments: ApplicationDocument[] = [mockDocument]

const dummyFile = new File(["content"], "test.pdf", { type: "application/pdf" })

describe("useDocument hooks", () => {

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe("useDownloadDocument", () => {
    beforeEach(() => {
      window.URL.createObjectURL = vi.fn().mockReturnValue("blob:http://localhost/mock-url")
      window.URL.revokeObjectURL = vi.fn()
    })
  
    it("downloads a file successfully and triggers a browser download", async () => {
      //Mock API call
      const mockBlob = new Blob(["dummy content"], {type: "application/pdf"})
      const getDocumentSpy = vi.spyOn(documentApi, "getDocument").mockResolvedValue(mockBlob)
  
      //Spy on that a-element creates and clicks on
      const linkClickSpy = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {})
  
      //Run hook with tanstack wrapper
      const {result} = renderHook(() => useDownloadDocument(), {
        wrapper: createWrapper()
      })
  
      //Trigger download
      result.current.mutate({id: 1, filename: "arsredovisning_2025.pdf"})
  
      //Verify
      await waitFor(() => {
        //Check API call with correct ID
        expect(getDocumentSpy).toHaveBeenCalledWith(1)
  
        //Check that blob was created for the file
        expect(window.URL.createObjectURL).toHaveBeenCalledWith(mockBlob)
  
        //Check that the click was registered
        expect(linkClickSpy).toHaveBeenCalled()
  
        //Check that the memory was cleaned up
        expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:http://localhost/mock-url")
      })
    })
  })

  describe("useDocuments", () => {
    it("fetches documents for a given applicationId", async () => {
      const getDocumentsSpy = vi.spyOn(documentApi, "getDocuments").mockResolvedValue(mockDocuments)

      const { result } = renderHook(() => useDocuments(3), {
        wrapper: createWrapper(),
      })

      await waitFor(() => expect(result.current.isSuccess).toBe(true))

      expect(getDocumentsSpy).toHaveBeenCalledWith(3)
      expect(result.current.data).toEqual(mockDocuments)
    })

    it("does not fetch when applicationId is missing", () => {
      const getDocumentsSpy = vi.spyOn(documentApi, "getDocuments")
      
      // Run hook with missing/invalid ID
      const { result } = renderHook(() => useDocuments(0), {
        wrapper: createWrapper(),
      })
  
      // Verify that no API call was triggered and hook stays pending
      expect(getDocumentsSpy).not.toHaveBeenCalled()
      expect(result.current.isPending).toBe(true)
    })
  })
  
  describe("useUploadDocument", () => {
    it("uploads a document and invalidates the document query cache", async () => {
      // Create query client spy to check cache invalidation
      const queryClient = createTestQueryClient()
      const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries")

      const postDocumentSpy = vi.spyOn(documentApi, "postDocument").mockResolvedValue(mockDocument)
  
      const payload: documentApi.DocumentUploadPayload = {
        applicationId: 3,
        docType: "pdf",
        file: dummyFile,
      }
  
      const { result } = renderHook(() => useUploadDocument(), {
        wrapper: createWrapper(queryClient)
      })
  
      // Trigger upload
      result.current.mutate(payload)
  
      // Verify API call and cache invalidation
      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true)
        expect(postDocumentSpy).toHaveBeenCalledWith(payload)
        expect(invalidateSpy).toHaveBeenCalledWith({
          queryKey: ["documents", 3],
        })
      })
    })
  })
})