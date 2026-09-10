import { describe, it, expect } from "vitest"
import { render, screen } from "@testing-library/react"
import Loading from "./Loading"

describe("Loading", () => {
  // the reduced-motion rule only reveals text that is already there,
  // so the text has to be in the DOM in every case
  it("always renders the label, announced as a status region", () => {
    render(<Loading />)

    expect(screen.getByRole("status").textContent).toBe("Laddar...")
  })

  it("hides the spinner from assistive tech so the label is the only content", () => {
    render(<Loading label="Hämtar ansökan" />)

    const status = screen.getByRole("status")

    expect(status.textContent).toBe("Hämtar ansökan")
    expect(status.querySelector("svg")?.getAttribute("aria-hidden")).toBe("true")
  })
})
