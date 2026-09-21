import { NavLink } from "react-router-dom"
import type { Application } from "../../api/applicationApi"
import { ICONS, STATUS_LABELS } from "../../constants/constants"
import { formatCurrency, formatDate, formatReferenceNumber } from "../../utils/formatters"
import s from "./SidebarCaseCard.module.css"

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
            aria-label={STATUS_LABELS[status]}
            title={STATUS_LABELS[status]}
          />
          {formatReferenceNumber(id)}
        </span>
        <span className={s.date}>{formatDate(createdAt)}</span>
      </div>
    </NavLink>
  )
}

export default SidebarCaseCard
