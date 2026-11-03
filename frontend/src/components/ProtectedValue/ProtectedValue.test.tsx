import { describe, it, expect } from "vitest"
import { render, screen, fireEvent } from "@testing-library/react"
import ProtectedValue from "./ProtectedValue"
import ProtectedNotice from "../ProtectedNotice/ProtectedNotice"

describe("ProtectedValue", () => {
  it("keeps the real value in the page and hides the mask from screen readers", () => {
    render(<ProtectedValue>070-123 45 67</ProtectedValue>)

    expect(screen.getByText("070-123 45 67")).toBeTruthy()
    expect(screen.getByText("Visa").getAttribute("aria-hidden")).toBe("true")
  })

  it("toggles between hidden and shown on tap/click (mobile + keyboard)", () => {
    render(<ProtectedValue>Erik Lindqvist</ProtectedValue>)
    const button = screen.getByRole("button")

    expect(button.getAttribute("aria-pressed")).toBe("false")

    fireEvent.click(button)
    expect(button.getAttribute("aria-pressed")).toBe("true")

    fireEvent.click(button)
    expect(button.getAttribute("aria-pressed")).toBe("false")
  })
})

describe("ProtectedNotice", () => {
  it("tells the caseworker what must not be shared", () => {
    render(<ProtectedNotice />)

    expect(screen.getByRole("note", { name: "Firmatecknaren har skyddad identitet" })).toBeTruthy()
    expect(screen.getByText("Lämna inte ut namn, e-post eller telefonnummer till någon.")).toBeTruthy()
  })
})
