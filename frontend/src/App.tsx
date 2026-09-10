import { Routes, Route } from "react-router-dom"
import { Toaster } from "sonner"
import Layout from "./layout/layout"
import LoginPage from "./pages/Login/LoginPage"
import NotFoundPage from "./pages/NotFound/NotFoundPage"
import TestPage from "./pages/Test/TestPage"

function App() {
  return (
    <>
      <Toaster position="top-right" richColors closeButton />
      <Routes>
        <Route element={<Layout fullWidth />}>
          <Route index element={<LoginPage />} />
        </Route>

        <Route element={<Layout />}>
          <Route path='test' element={<TestPage />} />
          <Route path='*' element={<NotFoundPage />} />
        </Route>
      </Routes>
    </>
  )
}

export default App
