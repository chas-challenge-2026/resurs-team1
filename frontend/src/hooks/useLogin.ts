import { useMutation } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"
import type { CaseworkerLoginPayload, CompanyLoginPayload } from "../api/authApi"
import { loginCaseWorker, loginCompany } from "../api/authApi"
import { setUser } from "../utils/auth"
import type { CaseWorkerUser, CompanyUser } from "../types/user"

const useCompanyLogin = () => {
  const navigate = useNavigate()

  return useMutation<CompanyUser, Error, CompanyLoginPayload>({
    mutationFn: (payload) => loginCompany(payload),
    onSuccess: (data) => {
      setUser(data)
      navigate("/oversikt")
    }
  })
}

const useCaseWorkerLogin = () => {
  const navigate = useNavigate()

  return useMutation<CaseWorkerUser, Error, CaseworkerLoginPayload>({
    mutationFn: (payload) => loginCaseWorker(payload),
    onSuccess: (data) => {
      setUser(data)
      navigate("/arenden")
    }
  })
}

export { useCompanyLogin, useCaseWorkerLogin }