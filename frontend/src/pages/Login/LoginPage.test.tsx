import { describe, it, expect, vi } from "vitest"
import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { loginCompany } from "../../api/authApi"
import LoginPage from "./LoginPage"

const navigate = vi.fn()

vi.mock("react-router-dom", () => ({
  useNavigate: () => navigate,
}))

vi.mock("../../api/authApi", () => ({
  loginCompany: vi.fn(),
  loginCaseWorker: vi.fn(),
}))

// took from README.md
const ORG_NUMBER = "556000-1234"

const renderPage = () => {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <LoginPage />
    </QueryClientProvider>
  )

  return {
    field: screen.getByLabelText(/Organisationsnummer/),
    button: screen.getByRole("button", { name: /Logga in med BankID/ }) as HTMLButtonElement,
  }
}

describe("LoginPage", () => {
  it("disables the button while the field is empty", () => {
    const { field, button } = renderPage()

    expect(button.disabled).toBe(true)

    fireEvent.change(field, { target: { value: ORG_NUMBER } })

    expect(button.disabled).toBe(false)
  })

  it("shows an error message when the login fails", async () => {
    vi.mocked(loginCompany).mockRejectedValue(new Error("500"))
    const { field, button } = renderPage()

    fireEvent.change(field, { target: { value: "556000-0000" } })
    fireEvent.click(button)

    expect(await screen.findAllByRole("alert")).not.toHaveLength(0)
  })

  it("hides the error message when the user types again", async () => {
    vi.mocked(loginCompany).mockRejectedValue(new Error("500"))
    const { field, button } = renderPage()

    fireEvent.change(field, { target: { value: "556000-0000" } })
    fireEvent.click(button)
    await screen.findAllByRole("alert")

    fireEvent.change(field, { target: { value: ORG_NUMBER } })

    await waitFor(() => expect(screen.queryAllByRole("alert")).toHaveLength(0))
  })

  it("redirects when the org number is accepted", async () => {
    vi.mocked(loginCompany).mockResolvedValue({
      userId: 1,
      role: "company",
      orgNumber: ORG_NUMBER,
      companyName: "Malmö Fastigheter AB",
    })
    const { field, button } = renderPage()

    fireEvent.change(field, { target: { value: ORG_NUMBER } })
    fireEvent.click(button)

    await waitFor(() => expect(navigate).toHaveBeenCalledWith("/oversikt"))
  })
})
