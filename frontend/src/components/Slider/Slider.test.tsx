import { describe, it, expect, vi } from "vitest"
import { fireEvent, render, screen } from "@testing-library/react"
import Slider from "./Slider"

describe("Slider", () => {
  
  it("clamps value to maximum allowed value on blur if typed input exceeds max", () => {
    const handleChange = vi.fn()

    render(
      <Slider
        name="amount"
        label="Belopp"
        value={3000000}
        min={0}
        max={5000000}
        onChange={handleChange}
      />
    )

    const inputField = screen.getByRole("textbox") as HTMLInputElement

    fireEvent.change(inputField, { target: { value: "6000000" } })
    fireEvent.blur(inputField)

    expect(inputField.value).toMatch(/5\s000\s000(\s?kr)?/)
    expect(handleChange).toHaveBeenCalledWith(5000000)
  })

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
