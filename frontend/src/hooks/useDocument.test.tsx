import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import * as documentApi from "../api/documentApi"
import { renderHook, waitFor } from "@testing-library/react"
import { useDownloadDocument } from "./useDocument"

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {retry: false},
      mutations: {retry: false}
    }
  })
  return ({children}: {children: React.ReactNode}) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}

describe("useDownloadDocument", () => {
  beforeEach(() => {
    window.URL.createObjectURL = vi.fn().mockReturnValue("blob:http://localhost/mock-url")
    window.URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("fetch file, create downloadable link and clean up", async () => {
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
    result.current.mutate({id: 1, fileName: "arsredovisning_2025.pdf"})

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