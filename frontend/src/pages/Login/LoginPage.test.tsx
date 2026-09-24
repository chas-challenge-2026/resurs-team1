import { describe, it, expect, vi, beforeEach } from "vitest"
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
const PERSONAL_NUMBER = "750312-1234"

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  render(
    <QueryClientProvider client={queryClient}>
      <LoginPage />
    </QueryClientProvider>
  )

  return {
    orgNumberField: screen.getByLabelText(/Organisationsnummer/) as HTMLInputElement,
    personalNumberField: screen.getByLabelText(/Personnummer/) as HTMLInputElement,
    button: screen.getByRole("button", { name: /Logga in med BankID/ }) as HTMLButtonElement,
  }
}

describe("LoginPage", () => {
  beforeEach(() => vi.clearAllMocks())

  it("formats what the user types into one org number shape", () => {
    const { orgNumberField } = renderPage()

    fireEvent.change(orgNumberField, { target: { value: "556000" } })
    expect(orgNumberField.value).toBe("556000")

    fireEvent.change(orgNumberField, { target: { value: "5560001234" } })
    expect(orgNumberField.value).toBe(ORG_NUMBER)

    fireEvent.change(orgNumberField, { target: { value: "556000-1234-999" } })
    expect(orgNumberField.value).toBe(ORG_NUMBER)

    fireEvent.change(orgNumberField, { target: { value: "55x60y00" } })
    expect(orgNumberField.value).toBe("556000")
  })

  it("formats what the user types into one personal number shape", () => {
    const { personalNumberField } = renderPage()

    fireEvent.change(personalNumberField, { target: { value: "750312" } })
    expect(personalNumberField.value).toBe("750312")

    fireEvent.change(personalNumberField, { target: { value: "7503121234" } })
    expect(personalNumberField.value).toBe(PERSONAL_NUMBER)

    fireEvent.change(personalNumberField, { target: { value: "750312-1234-999" } })
    expect(personalNumberField.value).toBe(PERSONAL_NUMBER)

    fireEvent.change(personalNumberField, { target: { value: "75x03y12" } })
    expect(personalNumberField.value).toBe("750312")
  })

  it("rejects a short org number without calling the api", () => {
    const { orgNumberField, personalNumberField, button } = renderPage()

    fireEvent.change(orgNumberField, { target: { value: "556000" } })
    fireEvent.change(personalNumberField, {target: {value: "750312"}})
    fireEvent.click(button)

    expect(screen.getByText(/Organisationsnumret måste innehålla 10 siffror/)).toBeTruthy()
    expect(screen.getByText(/Personnumret måste innehålla 10 siffror/)).toBeTruthy()
    expect(loginCompany).not.toHaveBeenCalled()
  })

  it("disables the button while the fields are empty", () => {
    const { orgNumberField, personalNumberField, button } = renderPage()

    expect(button.disabled).toBe(true)

    fireEvent.change(orgNumberField, { target: { value: ORG_NUMBER } })
    fireEvent.change(personalNumberField, { target: { value: PERSONAL_NUMBER } })
    
    expect(button.disabled).toBe(false)
  })

  it("shows an error message when the login fails", async () => {
    vi.mocked(loginCompany).mockRejectedValue(new Error("500"))
    const { orgNumberField, personalNumberField, button } = renderPage()

    fireEvent.change(orgNumberField, { target: { value: "556000-0000" } })
    fireEvent.change(personalNumberField, { target: {value: PERSONAL_NUMBER} })
    fireEvent.click(button)

    expect(await screen.findAllByRole("alert")).not.toHaveLength(0)
  })

  it("hides the error message when the user types again", async () => {
    vi.mocked(loginCompany).mockRejectedValue(new Error("500"))
    const { orgNumberField, personalNumberField, button } = renderPage()

    fireEvent.change(orgNumberField, { target: { value: "556000-0000" } })
    fireEvent.change(personalNumberField, { target: {value: PERSONAL_NUMBER} })
    fireEvent.click(button)
    await screen.findAllByRole("alert")

    fireEvent.change(orgNumberField, { target: { value: ORG_NUMBER } })

    await waitFor(() => expect(screen.queryAllByRole("alert")).toHaveLength(0))
  })

  it("sends formatted numbers with hyphens in the payload to the api", async () => {
    vi.mocked(loginCompany).mockResolvedValue({
      userId: 1,
      role: "company",
      orgNumber: ORG_NUMBER,
      companyName: "Fasen Elteknik AB",
    })

    const { orgNumberField, personalNumberField, button } = renderPage()

    fireEvent.change(orgNumberField, { target: { value: ORG_NUMBER } })
    fireEvent.change(personalNumberField, { target: {value: PERSONAL_NUMBER} })
    fireEvent.click(button)

    await waitFor(() =>
      expect(loginCompany).toHaveBeenCalledWith({
        orgNumber: ORG_NUMBER,
        personalNumber: PERSONAL_NUMBER,
      })
    )
  })

  it("redirects when the login is accepted", async () => {
    vi.mocked(loginCompany).mockResolvedValue({
      userId: 1,
      role: "company",
      orgNumber: ORG_NUMBER,
      companyName: "Fasen Elteknik AB",
    })

    const { orgNumberField, personalNumberField, button } = renderPage()

    fireEvent.change(orgNumberField, { target: { value: ORG_NUMBER } })
    fireEvent.change(personalNumberField, { target: {value: PERSONAL_NUMBER} })
    fireEvent.click(button)

    // Extended timeout needed due to the 1500ms delay in onSuccess before navigation
    await waitFor(() => expect(navigate).toHaveBeenCalledWith("/oversikt"), {timeout: 2000})
  })
})
