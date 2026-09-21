import type { Application } from "../../api/applicationApi"
import { formatCurrency } from "../../utils/formatters"
import { Card } from "../Card/Card"
import s from "./ApplicationSummary.module.css"

interface ApplicationSummaryProps {
  application: Application
  /** Color of the requested amount. "primary" is the brand green, "neutral" is black. */
  amountColor?: "primary" | "neutral"
  /** Color of the repayment time number. "primary" is the brand green, "neutral" is black. */
  durationColor?: "primary" | "neutral"
  /** Background shared by amount and repayment time. "primary" is the light brand green, "neutral" is grey. */
  amountBackground?: "primary" | "neutral"
}

/**
 * Key facts of an application, shown at the top of a case in the caseworker view.
 * Repayment time is only rendered when the application has one.
 *
 * @example
 * <ApplicationSummary application={application} amountColor="neutral" />
 */
const ApplicationSummary = ({ application, amountColor = "primary", durationColor = "primary", amountBackground = "primary" }: ApplicationSummaryProps) => {
  const panelClassName = amountBackground === "primary" ? s.panelPrimary : s.panel

  return(
    <Card className={s.card}>
      <dl className={s.summary}>
        <div className={`${s.item} ${s.amountItem} ${panelClassName}`}>
          <dt className={s.label}>Sökt belopp</dt>
          <dd className={`${s.amount} ${s[amountColor]}`}>{formatCurrency(application.requested_amount)}</dd>
        </div>

        {application.duration_months && (
          <div className={`${s.item} ${s.durationItem} ${panelClassName}`}>
            <dt className={s.label}><span className={s.hidden}>Återbetalningstid i </span>Månader</dt>
            <dd className={`${s.duration} ${s[durationColor]}`}>{application.duration_months}</dd>
          </div>
        )}

        <div className={`${s.item} ${s.purposeItem}`}>
          <dt className={s.label}>Ändamål</dt>
          <dd className={s.value}>{application.purpose}</dd>
        </div>
      </dl>
    </Card>
  )
}

export default ApplicationSummary
