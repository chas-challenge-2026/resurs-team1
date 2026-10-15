import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import type { User } from "../../types/user";
import type { UseQueryResult } from "@tanstack/react-query";
import PublicOnlyRoute from "./PublicOnlyRoute";
import * as useUser from "../../hooks/useUser"

vi.mock("../../hooks/useUser")

describe("PublicOnlyRoute", () => {
  it("allows unauthorized user to access /", () => {
    vi.spyOn(useUser, "useUser").mockReturnValue({data: undefined} as unknown as UseQueryResult<User, Error> )

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
    vi.spyOn(useUser, "useUser").mockReturnValue({
      data: {
        userId: 1, 
        role: "company", 
        orgNumber: "556600-1234", 
        displayName: "Malmö Fastigheter AB"
      }
    } as unknown as UseQueryResult<User, Error>)

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
    vi.spyOn(useUser, "useUser").mockReturnValue({ 
      data: {
        userId: 1,
        role: "caseWorker",
        name: "Karin Handläggare",
        email: "karin@resurs.se"
      }
    } as unknown as UseQueryResult<User, Error>)

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