import type { IconType } from "react-icons"
import { FiAlertCircle, FiArchive, FiCheckCircle, FiClock } from "react-icons/fi"
import type { ApplicationStatus } from "../../api/applicationApi";
import s from "./StatusTag.module.css"

interface StatusTagProps {
  status: ApplicationStatus;
  size?: "sm" | "md" | "lg";
  uppercase?: boolean;
}

const LABELS: Record<ApplicationStatus, string> = {
  PENDING_DOCS: "Komplettering krävs",
  UNDER_REVIEW: "Under behandling",
  APPROVED: "Godkänd",
  REJECTED: "Avvisad"
}

const ICONS: Record<ApplicationStatus, IconType> = {
  PENDING_DOCS: FiAlertCircle,
  UNDER_REVIEW: FiClock,
  APPROVED: FiCheckCircle,
  REJECTED: FiArchive,
}

const StatusTag = ({status, size="md", uppercase=true}: StatusTagProps) => {
  const Icon = ICONS[status]

  const combinedClassName = [
    s.base,
    s[size],
    s[status],
    uppercase && s.uppercase
  ].filter(Boolean).join(" ")

  return(
    <span className={combinedClassName}>
      <Icon className={s.icon} aria-hidden />
      {LABELS[status]}
    </span>
  )
}

export default StatusTag
