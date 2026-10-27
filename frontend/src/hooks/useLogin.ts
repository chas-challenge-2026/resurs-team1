import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"
import type { CaseworkerLoginPayload, CompanyLoginPayload } from "../api/authApi"
import { loginCaseWorker, loginCompany } from "../api/authApi"
import type { CaseWorkerUser, CompanyUser } from "../types/user"
import type { ApiErrorPayload } from "../api/client"

const useCompanyLogin = () => {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation<CompanyUser, ApiErrorPayload, CompanyLoginPayload>({
    mutationFn: (payload) => loginCompany(payload),
    onSuccess: () => {
      setTimeout(async () => {
        // route guards and header read the user from /auth/me, the login response has another shape
        await queryClient.invalidateQueries({ queryKey: ["me"] })
        navigate("/oversikt")
      }, 1500)
    },
    meta: {
      preventGlobalToast: true
    },
  })
}

const useCaseWorkerLogin = () => {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation<CaseWorkerUser, ApiErrorPayload, CaseworkerLoginPayload>({
    mutationFn: (payload) => loginCaseWorker(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["me"] })
      navigate("/arenden")
    },
    meta: {
      preventGlobalToast: true
    },
  })
}

export { useCompanyLogin, useCaseWorkerLogin }