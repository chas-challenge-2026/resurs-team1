import { useState } from "react"
import { formatCurrency, formatDate, formatReferenceNumber } from "../../utils/formatters"
import ToggleSwitch, { type SwitchOption } from "../../components/ToggleSwitch/ToggleSwitch"
import { Card, CardFooter } from "../../components/Card/Card"
import { DataList, DataListItem } from "../../components/DataList/DataList"
import ApplicationSummary from "../../components/ApplicationSummary/ApplicationSummary"
import StatusTag from "../../components/StatusTag/StatusTag"
import Button from "../../components/Button/Button"
import TextArea from "../../components/Textarea/Textarea"
import Loading from "../../components/Loading/Loading"
import s from "./CaseDetailsPage.module.css"
import { useBackofficeApplication } from "../../hooks/useApplication"
import { useParams } from "react-router-dom"

// TODO: swap out EXTRA_INFO once backend sends the data they use for the calculations, they are convinced yearly company statements are gdpr and delete it =,)
const EXTRA_INFO = {
  currentAssets: 4200000, industry: "Bygg & Anläggning" 
}

type viewOptions = "overview" | "manageCase"

const options: SwitchOption<viewOptions>[] = [
  { label: "Översikt", value: "overview" },
  { label: "Hantera ärende", value: "manageCase" },
]

const CaseDetailsPage = () => {
  const [view, setView] = useState<viewOptions>("overview")
  const [ isAdding, setIsAdding ] = useState(false)
  const { id } = useParams()
  const applicationId = Number(id)

  const { data, isPending, isError, error } = useBackofficeApplication(applicationId)

  if (isPending) return <Loading size="lg" label="Hämtar ärende..." delay />
  if (isError) return <p>{error.message}</p>

  return(
    <>
      <section className={s.topSection}>
        <div className={s.topWrapper}>
          <div>
            <div className={s.applicationRefWrapper}>
              <h2 className={s.applicationRef}>{formatReferenceNumber(data.id)}</h2>
              <StatusTag status={data.status} />
            </div>

            <div>
              <p className="title">{data.companyName}</p>
              <p className={s.infoText}>Org.nr {data.orgNumber} · Inkommet {formatDate(data.createdAt)}</p>
            </div>
          </div>
          <ApplicationSummary application={data} />
        </div>

        <ToggleSwitch variant="accent" name="view" options={options} selectedValue={view} onChange={(newView) => setView(newView)} />
      </section>

      {view === "overview" &&
        <section className={s.contentWrapper}>
          <Card>
            <h3>Kontakt</h3>
            <DataList>
              <DataListItem label="Namn" value={data.contactDetails.name} />
              <DataListItem label="E-postadress" value={data.contactDetails.email} />
              <DataListItem label="Telefonnummer" value={data.contactDetails.phoneNumber} />
            </DataList>
          </Card>

          <Card>
            <h3>Ekonomi</h3>
            <DataList>
              <DataListItem label="Omsättning" value={formatCurrency(EXTRA_INFO.currentAssets)} />
              <DataListItem label="Bransch" value={EXTRA_INFO.industry} />
            </DataList>
          </Card>
        </section>
      }

      {view === "manageCase" &&
        <section className={s.contentWrapper}>
          {/* TODO: Connect status-change buttons with backend + refresh the querykeydata upon selecting a button */}
          <div className={s.actionsWrapper}>
            <Button variant="secondary" className={`${s.actionButton} ${s.accept}`}>
              Godkänn
            </Button>
            <Button variant="secondary" className={`${s.actionButton} ${s.request}`} onClick={() => setIsAdding(!isAdding)}>
              Komplettera
            </Button>
            <Button variant="secondary" className={`${s.actionButton} ${s.reject}`}>
              Avvisa
            </Button>
          </div>

          {isAdding && (
            <section>
              <Card>
                <h3 className={s.addingTitle}>Beskriv vilket dokument du behöver från kunden</h3>
                <TextArea id="message" label="message" placeholder="T.ex. årsredovisning, kontoutdrag, offert..." />
                <CardFooter className={s.addingFooter}>
                  <Button disabled>
                    Skicka förfrågan
                  </Button>
                  <Button variant="secondary" onClick={() => setIsAdding(false)}>
                    Avbryt
                  </Button>
                </CardFooter>
              </Card>

              {/* TODO: Add view for requests and corresponding attachments */}
            </section>
          )}
        </section>
      }
    </>
  )
}

export default CaseDetailsPage