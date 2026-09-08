import { Routes, Route } from "react-router-dom"
import Layout from "./layout/Layout"
import LoginPage from "./pages/LoginPage"
import NotFoundPage from "./pages/NotFoundPage"
import TestPage from "./pages/TestPage"

function App() {
  return (
    <Routes>
      <Route element={<Layout fullWidth />}>
        <Route index element={<LoginPage />} />
      </Route>

      <Route element={<Layout />}>
        <Route path='test' element={<TestPage />} />
        <Route path='*' element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
