import { ICONS, STATUS_LABELS } from "../../constants/constants";
import type { ApplicationStatus } from "../../api/applicationApi";
import s from "./StatusTag.module.css"

interface StatusTagProps {
  status: ApplicationStatus;
  size?: "sm" | "md" | "lg";
  uppercase?: boolean;
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
      {STATUS_LABELS[status]}
    </span>
  )
}

export default StatusTag
