import { describe, it, expect, vi, beforeEach } from "vitest"
import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { postApplication } from "../../../api/applicationApi"
import ApplicationPage from "./ApplicationPage"

const navigate = vi.fn()

vi.mock("react-router-dom", () => ({
  useNavigate: () => navigate,
}))

vi.mock("../../../api/applicationApi", () => ({
  postApplication: vi.fn(),
}))

// the page prefills org number and company name from the logged in user
vi.mock("../../../utils/auth", () => ({
  getUser: () => ({
    userId: 1,
    role: "company",
    orgNumber: "556000-1234",
    companyName: "Malmö Fastigheter AB",
  }),
}))

const renderPage = () =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { mutations: { retry: false } } })}>
      <ApplicationPage />
    </QueryClientProvider>
  )

const nextButton = () => screen.getByRole("button", { name: /Fortsätt|Skicka ansökan|Skickar/ }) as HTMLButtonElement

const fillStep1 = () => {
  fireEvent.change(screen.getByLabelText(/Ändamål/), { target: { value: "Expansion" } })
  fireEvent.click(screen.getByLabelText("36 mån"))
}

const fillStep2 = () => {
  fireEvent.change(screen.getByLabelText("Kontaktperson"), { target: { value: "Anna Andersson" } })
  fireEvent.change(screen.getByLabelText("E-postadress"), { target: { value: "anna@foretag.se" } })
  fireEvent.change(screen.getByLabelText("Telefonnummer"), { target: { value: "070-123 45 67" } })
}

// walks a valid application all the way to the review step
const goToReview = () => {
  renderPage()
  fillStep1()
  fireEvent.click(nextButton())
  fillStep2()
  fireEvent.click(nextButton())
}

describe("ApplicationPage", () => {
  beforeEach(() => vi.clearAllMocks())

  it("keeps Fortsätt disabled until step 1 is filled in", () => {
    renderPage()
    expect(nextButton().disabled).toBe(true)

    fireEvent.change(screen.getByLabelText(/Ändamål/), { target: { value: "Expansion" } })
    expect(nextButton().disabled).toBe(true)

    fireEvent.click(screen.getByLabelText("36 mån"))
    expect(nextButton().disabled).toBe(false)
  })

  it("keeps Fortsätt disabled on step 2 until email and phone look valid", () => {
    renderPage()
    fillStep1()
    fireEvent.click(nextButton())

    expect(nextButton().disabled).toBe(true)

    fireEvent.change(screen.getByLabelText("Kontaktperson"), { target: { value: "Anna Andersson" } })
    expect(nextButton().disabled).toBe(true)

    fireEvent.change(screen.getByLabelText("Telefonnummer"), { target: { value: "070-123 45 67" } })
    expect(nextButton().disabled).toBe(true)

    fireEvent.change(screen.getByLabelText("E-postadress"), { target: { value: "anna@foretag.s" } }) // intentionally wrong format (2 letters after dot needed)
    expect(nextButton().disabled).toBe(true)

    fireEvent.change(screen.getByLabelText("E-postadress"), { target: { value: "anna@foretag.se" } })
    expect(nextButton().disabled).toBe(false)
  })

  it("keeps the answers when going back a step", () => {
    renderPage()
    fillStep1()
    fireEvent.click(nextButton())

    fireEvent.click(screen.getByRole("button", { name: "Tillbaka" }))

    expect((screen.getByLabelText(/Ändamål/) as HTMLSelectElement).value).toBe("Expansion")
    expect((screen.getByLabelText("36 mån") as HTMLInputElement).checked).toBe(true)
  })

  it("sends the form as the nested payload the backend wants", async () => {
    vi.mocked(postApplication).mockResolvedValue({} as never)
    goToReview()

    fireEvent.click(nextButton())

    await waitFor(() =>
      expect(postApplication).toHaveBeenCalledWith({
        orgNumber: "556000-1234",
        contactDetails: {
          name: "Anna Andersson",
          email: "anna@foretag.se",
          phoneNumber: "070-123 45 67",
        },
        purpose: "Expansion",
        requestedAmount: 3000000,
        durationMonths: 36,
      })
    )
  })

  it("disables the submit button while sending", async () => {
    // never resolves, so the request stays pending
    vi.mocked(postApplication).mockReturnValue(new Promise(() => {}))
    goToReview()

    fireEvent.click(nextButton())

    await waitFor(() => expect(nextButton().textContent).toBe("Skickar..."))
    expect(nextButton().disabled).toBe(true)
  })

  it("shows the receipt when the application is sent", async () => {
    vi.mocked(postApplication).mockResolvedValue({} as never)
    goToReview()

    fireEvent.click(nextButton())

    expect(await screen.findByText(/Tack, vi har tagit emot/)).toBeTruthy()
    fireEvent.click(screen.getByRole("button", { name: /Mina ansökningar/ }))
    expect(navigate).toHaveBeenCalledWith("/mina-ansokningar")
  })

  it("shows an error when the application fails", async () => {
    vi.mocked(postApplication).mockRejectedValue(new Error("500"))
    goToReview()

    fireEvent.click(nextButton())

    expect(await screen.findByRole("alert")).toBeTruthy()
  })
})
