import { describe, it, expect } from "vitest"
import { render } from "@testing-library/react"
import { MemoryRouter } from "react-router-dom"
import SidebarCaseCard from "./SidebarCaseCard"
import type { CaseListItem } from "../../api/applicationApi"

const baseCase: CaseListItem = {
  id: 1042,
  status: "UNDER_REVIEW",
  companyName: "Frukt och grönt Göteborg",
  orgNumber: "556000-9999",
  requestedAmount: 20000,
  createdAt: "2026-10-30T10:00:00Z",
}

const renderCard = (application: CaseListItem) =>
  render(<MemoryRouter><SidebarCaseCard application={application} /></MemoryRouter>)

// a normal card has 1 icon (status), a protected card also has the lock in the corner
const countIcons = (container: HTMLElement) => container.querySelectorAll("svg").length

describe("SidebarCaseCard", () => {
  it("shows the protected corner when protectedResource is true", () => {
    const { container } = renderCard({ ...baseCase, protectedResource: true })

    expect(countIcons(container)).toBe(2)
  })

  it("shows no protected corner when protectedResource is false or missing", () => {
    const { container, unmount } = renderCard({ ...baseCase, protectedResource: false })
    expect(countIcons(container)).toBe(1)
    unmount()

    const { container: withoutField } = renderCard(baseCase)
    expect(countIcons(withoutField)).toBe(1)
  })
})
