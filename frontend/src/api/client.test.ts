import { beforeEach, describe, expect, it, vi } from "vitest";
import { handleResponseError, type ApiErrorPayload } from "./client";
import type { AxiosError } from "axios";

describe("client API", () => {
  beforeEach(() => vi.clearAllMocks())
  
  it("returns connection error message when response is missing", async () => {
    const mockError = {} as AxiosError<ApiErrorPayload>

    await expect(handleResponseError(mockError)).rejects.toEqual({
      status: undefined,
      message: "Kunde inte ansluta till servern. Kontrollera din internetanslutning.",
      fieldErrors: undefined,
      code: undefined,
      originalError: mockError,
    })
  })

  it("clears auth storage and redirects to '/' on 401 for normal requests", async () => {
    Object.defineProperty(window, "location", {
      writable: true,
      value: { href: "" },
    })

    const mockError = {
      config: { 
        url: "/backoffice" 
      },
      response: {
        status: 401,
        data: {},
      },
    } as AxiosError<ApiErrorPayload>

    try {
      await handleResponseError(mockError)
    } catch {
      // Expected
    }
    
    expect(window.location.href).toBe("/")
  })
  
  it("formats 403 errors with custom fallback message", async () => {
    const mockError = {
      response: {
        status: 403,
        data: {},
      },
    } as AxiosError<ApiErrorPayload>

    await expect(handleResponseError(mockError)).rejects.toEqual({
      status: 403,
      message: "Du saknar behörighet att utföra denna åtgärd.",
      fieldErrors: undefined,
      code: undefined,
      originalError: mockError,
    })
  })

  it("formats 404 errors with custom fallback message", async () => {
    const mockError = {
      response: {
        status: 404,
        data: {}
      }
    } as AxiosError<ApiErrorPayload>

    await expect(handleResponseError(mockError)).rejects.toEqual({
      status: 404,
      message: "Den begärda resursen kunde inte hittas.",
      fieldErrors: undefined,
      code: undefined,
      originalError: mockError,
    })
  })

  it("formats 500 server errors with custom fallback message", async () => {
    const mockError = {
      response: {
        status: 500,
        data: {}
      }
    } as AxiosError<ApiErrorPayload>

    await expect(handleResponseError(mockError)).rejects.toEqual({
      status: 500,
      message: "Ett serverfel uppstod. Vänligen försök igen om en stund.",
      fieldErrors: undefined,
      code: undefined,
      originalError: mockError,
    })
  })
})