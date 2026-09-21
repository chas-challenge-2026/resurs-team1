import type { IconType } from "react-icons"
import { FiAlertCircle, FiCheckCircle, FiClock, FiXCircle } from "react-icons/fi"
import { NavLink } from "react-router-dom"
import type { Application, ApplicationStatus } from "../../api/applicationApi"
import { formatCurrency, formatDate, formatReferenceNumber } from "../../utils/formatters"
import s from "./SidebarCaseCard.module.css"

// TODO: will be moved to a shared file soon, because StatusTag.tsx is using it too
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
  REJECTED: FiXCircle,
}

interface SidebarCaseCardProps {
  application: Application
}

const SidebarCaseCard = ({ application }: SidebarCaseCardProps) => {
  const { id, status, companyName, requestedAmount, createdAt } = application
  const StatusIcon = ICONS[status]

  return(
    <NavLink
      to={`/arenden/${id}`}
      className={({ isActive }) => isActive ? `${s.card} ${s.active}` : s.card}
    >
      <div className={s.row}>
        <span className={s.company} title={companyName}>{companyName}</span>
        <span className={s.amount}>{formatCurrency(requestedAmount)}</span>
      </div>

      <div className={s.row}>
        <span className={s.reference}>
          <StatusIcon
            className={`${s.icon} ${s[status]}`}
            role="img"
            aria-label={LABELS[status]}
            title={LABELS[status]}
          />
          {formatReferenceNumber(id)}
        </span>
        <span className={s.date}>{formatDate(createdAt)}</span>
      </div>
    </NavLink>
  )
}

export default SidebarCaseCard
