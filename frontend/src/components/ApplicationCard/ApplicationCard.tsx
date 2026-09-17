import { formatCurrency, formatDate, formatReferenceNumber } from "../../utils/formatters"
import type { Application } from "../../api/applicationApi"
import { Card, CardBody, CardFooter } from "../Card/Card"
import StatusTag from "../StatusTag/StatusTag"
import s from "./ApplicationCard.module.css"
import c from "../Card/Card.module.css"
import { Link } from "react-router-dom"
import { RiArrowRightSLine } from "react-icons/ri"

interface ApplicationCardProps {
  application: Application
}

const ApplicationCard = ({application}: ApplicationCardProps) => {
  return(
    <Card as="article" className={s.card}>
      <CardBody className={s.cardContent}>
        <div>
          <div className={s.applicationInfo}>
            <span className={s.reference}>{formatReferenceNumber(application.id)}</span>
            <StatusTag status={application.status} />
          </div>

          <p className={s.purpose}>{application.purpose}</p>
          <p className={s.updatedAt}>Uppdaterad {formatDate(application.updated_at)}</p>
        </div>
      </CardBody>

      <CardFooter className={s.cardFooter}>
        <div className={s.amountGroup}>
          <span className={s.amountLabel}>Sökt belopp</span>
          <span className={s.amountValue}>{formatCurrency(application.requested_amount)}</span>
        </div>

        <Link
          to={`/mina-ansokningar/${application.id}`}
          aria-label={`Visa ansökan ${application.id}`}
          className={`${s.link} ${c.stretchedLink}`}
        >
          <span>Visa detaljer</span>
          <span><RiArrowRightSLine /></span>
        </Link>
      </CardFooter>
    </Card>
  )
}

export default ApplicationCard