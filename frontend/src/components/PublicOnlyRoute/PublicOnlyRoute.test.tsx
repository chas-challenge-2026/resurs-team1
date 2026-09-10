import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import * as authUtils from "../../utils/auth";
import PublicOnlyRoute from "./PublicOnlyRoute";

describe("PublicOnlyRoute", () => {
  it("allows unauthorized user to access /", () => {
    vi.spyOn(authUtils, "getUser").mockReturnValue(null)

    render(
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/" element={<p>Login Page</p>} />
          </Route>
          <Route path="oversikt" element={<p>Company Overview Page</p>} />
          <Route path="arenden" element={<p>Cases Overview Page</p>} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText("Login Page")).toBeDefined()
    expect(screen.queryByText("Company Overview Page")).toBeNull()
    expect(screen.queryByText("Cases Overview Page")).toBeNull()
  })

  it("redirect away company user from / to /oversikt", () => {
    vi.spyOn(authUtils, "getUser").mockReturnValue({ userId: 1, role: "company", orgNumber: "556600-1234", companyName: "Malmö Fastigheter AB"})

    render(
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/" element={<p>Login Page</p>} />
          </Route>
          <Route path="oversikt" element={<p>Company Overview Page</p>} />
          <Route path="arenden" element={<p>Cases Overview Page</p>} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.queryByText("Login Page")).toBeNull()
    expect(screen.getByText("Company Overview Page")).toBeDefined()
    expect(screen.queryByText("Cases Overview Page")).toBeNull()
  })

  it("redirect away caseWorker user from / to /arenden", () => {
    vi.spyOn(authUtils, "getUser").mockReturnValue({ userId: 1, role: "caseWorker", name: "Karin Handläggare", email: "karin@resurs.se" })

    render(
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/" element={<p>Login Page</p>} />
          </Route>
          <Route path="oversikt" element={<p>Company Overview Page</p>} />
          <Route path="arenden" element={<p>Cases Overview Page</p>} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.queryByText("Login Page")).toBeNull()
    expect(screen.queryByText("Company Overview Page")).toBeNull()
    expect(screen.getByText("Cases Overview Page")).toBeDefined()
  })
})