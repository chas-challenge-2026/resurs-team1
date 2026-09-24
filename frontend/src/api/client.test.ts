import { beforeEach, describe, expect, it, vi } from "vitest";
import { clearAuthStorage } from "../utils/auth";
import { handleResponseError, type ApiErrorPayload } from "./client";
import type { AxiosError } from "axios";

vi.mock("../utils/auth.ts", () => ({
  clearAuthStorage: vi.fn(),
}))

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

  it("uses backend error message when provided", async () => {
    const mockError = {
      response: {
        status: 400,
        data: { message: "Ogiltigt personnummer" }
      }
    } as unknown as AxiosError<ApiErrorPayload>

    await expect(handleResponseError(mockError)).rejects.toEqual({
      status: 400,
      message: "Ogiltigt personnummer",
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
    
    expect(clearAuthStorage).toHaveBeenCalled()
    expect(window.location.href).toBe("/")
  })

  it("does not clear storage or redirect on 401 when request URL includes /login", async () => {
    const mockError = {
      config: { 
        url: "/auth/login/company" 
      },
      response: {
        status: 401,
        data: {},
      },
    } as AxiosError<ApiErrorPayload>

    await expect(handleResponseError(mockError)).rejects.toEqual({
      status: 401,
      message: "Felaktiga inloggningsuppgifter.",
      fieldErrors: undefined,
      code: undefined,
      originalError: mockError,
    })

    expect(clearAuthStorage).not.toHaveBeenCalled()
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