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
    onSuccess: (data) => {
      queryClient.setQueryData(["me"], {
        ...data,
        displayName: data.companyName
      })
      setTimeout(() => {
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
    onSuccess: (data) => {
      queryClient.setQueryData(["me"], {
        ...data,
        displayName: data.name
      })
      navigate("/arenden")
    },
    meta: {
      preventGlobalToast: true
    },
  })
}

export { useCompanyLogin, useCaseWorkerLogin }