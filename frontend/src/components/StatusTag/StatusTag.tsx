import type { IconType } from "react-icons"
import { FiAlertCircle, FiArchive, FiCheckCircle, FiClock, FiXCircle } from "react-icons/fi"
import s from "./StatusTag.module.css"

export type Status = "needs_info" | "processing" | "approved" | "closed" | "rejected"

interface StatusTagProps {
  status: Status;
  size?: "sm" | "md" | "lg";
  uppercase?: boolean;
}

const LABELS: Record<Status, string> = {
  needs_info: "Komplettering krävs",
  processing: "Under behandling",
  approved: "Godkänd",
  closed: "Avslutad",
  rejected: "Avvisad"
}

const ICONS: Record<Status, IconType> = {
  needs_info: FiAlertCircle,
  processing: FiClock,
  approved: FiCheckCircle,
  closed: FiArchive,
  rejected: FiXCircle
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
