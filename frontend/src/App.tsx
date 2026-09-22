import { Routes, Route } from "react-router-dom"
import PublicOnlyRoute from "./components/PublicOnlyRoute/PublicOnlyRoute"
import ProtectedRoute from "./components/ProtectedRoute/ProtectedRoute"
import { Toaster } from "sonner"
import CardLayout from "./layouts/CardLayout"
import TestPage from "./pages/Test/TestPage"
import NotFoundPage from "./pages/NotFound/NotFoundPage"
import LoginPage from "./pages/Login/LoginPage"
import CompanyHomePage from "./pages/Company/Home/CompanyHomePage"
import ApplicationFormPage from "./pages/Company/Application/ApplicationPage"
import MyApplicationsPage from "./pages/Company/MyApplications/MyApplicationsPage"
import MyApplicationDetailsPage from "./pages/Company/MyApplicationDetails/MyApplicationDetailsPage"
import FullWidthLayout from "./layouts/FullWidthLayout"
import CaseWorkerLayout from "./layouts/CaseWorkerLayout"
import CaseDetailsPage from "./pages/CaseWorker/CaseDetailsPage"

function App() {
  return (
    <>
      <Toaster position="top-right" richColors closeButton />
      <Routes>
        {/* Public only pages */}
        <Route element={<PublicOnlyRoute />}>
          <Route element={<FullWidthLayout />}>
            <Route index element={<LoginPage />} />
          </Route>
        </Route>

        {/* Company pages */}
        <Route element={<ProtectedRoute allowedRoles={["company"]} />}>
          <Route element={<FullWidthLayout />}>
            <Route path="oversikt" element={<CompanyHomePage />} />
          </Route>
          <Route element={<CardLayout />}>
            <Route path="kreditansokan" element={<ApplicationFormPage />} />
            <Route path="/mina-ansokningar" element={<MyApplicationsPage />} />
            <Route path="/mina-ansokningar/:id" element={<MyApplicationDetailsPage />} />
          </Route>
        </Route>

        {/* Caseworker pages */}
        <Route element={<ProtectedRoute allowedRoles={["caseWorker"]} />}>
          <Route path="arenden" element={< CaseWorkerLayout />}>
            <Route path=":id" element={ <CaseDetailsPage />} />
          </Route>
        </Route>

        {/* Other */}
        <Route element={<CardLayout />}>
          <Route path='test' element={<TestPage />} />
          <Route path='*' element={<NotFoundPage />} />
        </Route>
      </Routes>
    </>
  )
}

export default App
