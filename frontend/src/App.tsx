import { Routes, Route } from "react-router-dom"
import PublicOnlyRoute from "./components/PublicOnlyRoute/PublicOnlyRoute"
import ProtectedRoute from "./components/ProtectedRoute/ProtectedRoute"
import { Toaster } from "sonner"
import Layout from "./layout/Layout"
import TestPage from "./pages/Test/TestPage"
import NotFoundPage from "./pages/NotFound/NotFoundPage"
import LoginPage from "./pages/Login/LoginPage"
import CompanyHomePage from "./pages/Company/Home/CompanyHomePage"
import ApplicationFormPage from "./pages/Company/Application/ApplicationPage"
import CasesOverviewPage from "./pages/CaseWorker/Cases/CasesOverviewPage"

function App() {
  return (
    <>
      <Toaster position="top-right" richColors closeButton />
      <Routes>
        {/* Public only pages */}
        <Route element={<PublicOnlyRoute />}>
          <Route element={<Layout fullWidth />}>
            <Route index element={<LoginPage />} />
          </Route>
        </Route>

        {/* Company pages */}
        <Route element={<ProtectedRoute allowedRoles={["company"]} />}>
          <Route element={<Layout fullWidth />}>
            <Route path="oversikt" element={<CompanyHomePage />} />
          </Route>
          <Route element={<Layout />}>
            <Route path="kreditansokan" element={<ApplicationFormPage />} />
          </Route>
        </Route>

        {/* Caseworker pages */}
        <Route element={<ProtectedRoute allowedRoles={["caseWorker"]} />}>
          <Route element={<Layout fullWidth />}>
            <Route path="arenden" element={<CasesOverviewPage />} />
          </Route>
        </Route>

        {/* Other */}
        <Route element={<Layout />}>
          <Route path='test' element={<TestPage />} />
          <Route path='*' element={<NotFoundPage />} />
        </Route>
      </Routes>
    </>
  )
}

export default App
