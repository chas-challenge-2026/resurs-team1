import { describe, it, expect } from "vitest"
import { render, screen } from "@testing-library/react"
import Slider from "./Slider"

describe("Slider", () => {
  it("displays the same value in input field as it holds in the range input", () => {
    render(
      <Slider
        name="amount"
        label="Belopp"
        value={3000000}
        min={0}
        max={5000000}
        onChange={() => undefined}
      />
    )

    const range = screen.getByRole("slider") as HTMLInputElement
    const inputField = screen.getByRole("textbox") as HTMLInputElement

    expect(range.value).toBe("3000000")
    expect(inputField.value).toMatch(/3\s000\s000(\s?kr)?/)
  })
})
