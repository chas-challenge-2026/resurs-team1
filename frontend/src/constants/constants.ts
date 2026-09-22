import { FiAlertCircle, FiClock, FiCheckCircle, FiXCircle } from "react-icons/fi"
import type { ApplicationStatus } from "../api/applicationApi"
import type { IconType } from "react-icons"
import type { DropdownOption } from "../components/Dropdown/Dropdown"
import type { ButtonGroupOption } from "../components/ButtonGroup/ButtonGroup"

export const PURPOSE_OPTIONS: DropdownOption[] = [
  { value: "waiting", label: "Väntar" },
  { value: "for", label: "På" },
  { value: "backend", label: "Back-end" },
]

export const ICONS: Record<ApplicationStatus, IconType> = {
  PENDING_DOCS: FiAlertCircle,
  UNDER_REVIEW: FiClock,
  APPROVED: FiCheckCircle,
  REJECTED: FiXCircle,
}

export const STATUS_LABELS: Record<ApplicationStatus, string> = {
  PENDING_DOCS: "Komplettering krävs",
  UNDER_REVIEW: "Under behandling",
  APPROVED: "Godkänd",
  REJECTED: "Avvisad"
}

export const REPAYMENT_OPTIONS: ButtonGroupOption<number>[] = [
  { value: 12, label: "12 mån" },
  { value: 24, label: "24 mån" },
  { value: 36, label: "36 mån" },
  { value: 48, label: "48 mån" },
  { value: 60, label: "60 mån" },
]

// stricter than the browser's own rule, which accepts a domain without a dot
export const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/

// loose on purpose: swedish numbers are written with spaces, dashes and +46
export const PHONE_PATTERN = /^[\d\s+()-]{6,20}$/

export const LOAN_MIN = 50000
export const LOAN_MAX = 10000000