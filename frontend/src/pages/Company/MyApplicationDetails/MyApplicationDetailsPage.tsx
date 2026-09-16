import { useParams } from "react-router-dom"
import { RiChat3Line, RiLink } from "react-icons/ri"
import { useApplication } from "../../../hooks/useApplication"
import { formatCurrency, formatDate, formatReferenceNumber } from "../../../utils/formatters"
import { Card, CardBody, CardFooter, CardHeader } from "../../../components/Card/Card"
import { DataList, DataListItem } from "../../../components/DataList/DataList"
import Loading from "../../../components/Loading/Loading"
import StatusTag from "../../../components/StatusTag/StatusTag"
import TextArea from "../../../components/Textarea/Textarea"
import Button from "../../../components/Button/Button"
import s from "./MyApplicationDetailsPage.module.css"

const MyApplicationDetailsPage = () => {
  const { id } = useParams()
  const applicationId = Number(id)
  const { data: application, isPending, isError, error } = useApplication(applicationId)

  if(isPending) return <Loading label="Hämtar ansökan..." delay />
  if (isError) return <p>{error.message}</p>

  return(
    <div className={s.wrapper}>
      <div>
        <div className={s.titleGroup}>
          <h2 className="title">{formatReferenceNumber(application.id)}</h2>
          <StatusTag status={application.status} />
        </div>
        <p className={s.description}>{application.purpose} · {formatCurrency(application.requested_amount)} {application.duration_months && `· ${application.duration_months} månader`}</p>
      </div>

      {application.status === "PENDING_DOCS" &&
        <Card as="article" variant="warning">
          <CardHeader className={s.warningHeader}>
            <div className={s.iconWrapper}>
              <RiChat3Line />
            </div>
            <div className={s.headerText}>
              <h3 className={s.warningTitle}>Vi behöver mer information</h3>
              <p className={s.warningSubtitle}>Din handläggare behöver kompletterande information innan ansökan kan behandlas vidare.</p>
            </div>
          </CardHeader>
          <CardBody className={s.warningBody}>
              <p>{application.decision}</p>
              <TextArea id="message" label="Meddelande" />
          </CardBody>
          <CardFooter className={s.warningFooter}>
            <div className={s.actionGroup}>
              <Button>Skicka svar</Button>
              <Button variant="secondary" className={s.attachButton}>
                <RiLink />
                <span>Bifoga fil</span>
              </Button>
            </div>
          </CardFooter>
        </Card>
      }

      <Card>
        <CardHeader>
          <h3 className="subtitle">Ansökningsuppgifter</h3>
          <DataList>
            <DataListItem label="Ärendenummer" value={formatReferenceNumber(application.id)} />
            <DataListItem label="Ändamål" value={application.purpose} />
            <DataListItem label="Belopp" value={formatCurrency(application.requested_amount)} />
            {application.duration_months && <DataListItem label="Återbetalningstid" value={`${application.duration_months} månader`} />}
            <DataListItem label="Inskickad" value={formatDate(application.created_at)} />
          </DataList>
        </CardHeader>
      </Card>
    </div>
  )
}

export default MyApplicationDetailsPage