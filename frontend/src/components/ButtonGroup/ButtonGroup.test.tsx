import {describe, it, expect} from "vitest"
import { render, screen } from "@testing-library/react"
import ButtonGroup from "./ButtonGroup"

const OPTS = [
  { label: "12 mån", value: 12 },
  { label: "36 mån", value: 36 },
]

describe("ButtonGroup", () => {
  it("checks the option matching selectedValue", () => {
    render(
      <ButtonGroup
        name="tenure"
        options={OPTS}
        selectedValue={36}
        onChange={() => undefined}
      />
    )

    const selected = screen.getByRole("radio", { name: "36 mån" })
    const other = screen.getByRole("radio", { name: "12 mån" })

    expect((selected as HTMLInputElement).checked).toBe(true)
    expect((other as HTMLInputElement).checked).toBe(false)
  })
})