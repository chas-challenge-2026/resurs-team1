import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import * as authUtils from "../../utils/auth";
import ProtectedRoute from "./ProtectedRoute";

describe("ProtectedRoute", () => {
  it("allows access to caseWorker user to caseWorker paths", () => {
    vi.spyOn(authUtils, "getUser").mockReturnValue({ userId: 1, role: "caseWorker", name: "Karin Handläggare", email: "karin@resurs.se" })

    render(
      <MemoryRouter initialEntries={["/oversikt"]}>
        <Routes>
          <Route element={<ProtectedRoute allowedRoles={["caseWorker"]} />}>
            <Route path="oversikt" element={<p>Protected Page</p>} />
          </Route>
          <Route path="/" element={<p>Login Page</p>} />
          <Route path="*" element={<p>404 Page</p>} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText("Protected Page")).toBeDefined()
    expect(screen.queryByText("Login Page")).toBeNull()
    expect(screen.queryByText("404 Page")).toBeNull()
  })

  it("send logged in user with wrong role to Not Found", () => {
    vi.spyOn(authUtils, "getUser").mockReturnValue({ userId: 1, role: "caseWorker", name: "Karin Handläggare", email: "karin@resurs.se" })

    render(
      <MemoryRouter initialEntries={["/oversikt"]}>
        <Routes>
          <Route element={<ProtectedRoute allowedRoles={["company"]} />}>
            <Route path="oversikt" element={<p>Protected Page</p>} />
          </Route>
          <Route path="/" element={<p>Login Page</p>} />
          <Route path="*" element={<p>404 Page</p>} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.queryByText("Protected Page")).toBeNull()
    expect(screen.queryByText("Login Page")).toBeNull()
    expect(screen.getByText("404 Page")).toBeDefined()
  })

  it("redirect unauthorized user to /", () => {
    vi.spyOn(authUtils, "getUser").mockReturnValue(null)

    render(
      <MemoryRouter initialEntries={["/oversikt"]}>
        <Routes>
          <Route element={<ProtectedRoute allowedRoles={["company"]} />}>
            <Route path="oversikt" element={<p>Protected Page</p>} />
          </Route>
          <Route path="/" element={<p>Login Page</p>} />
          <Route path="*" element={<p>404 Page</p>} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.queryByText("Protected Page")).toBeNull()
    expect(screen.getByText("Login Page")).toBeDefined()
    expect(screen.queryByText("404 page")).toBeNull()
  })
})