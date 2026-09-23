import { useState } from "react"
import { formatCurrency, formatDate, formatReferenceNumber } from "../../utils/formatters"
import type { Application } from "../../api/applicationApi"
import ToggleSwitch, { type SwitchOption } from "../../components/ToggleSwitch/ToggleSwitch"
import { Card, CardFooter } from "../../components/Card/Card"
import { DataList, DataListItem } from "../../components/DataList/DataList"
import ApplicationSummary from "../../components/ApplicationSummary/ApplicationSummary"
import StatusTag from "../../components/StatusTag/StatusTag"
import Button from "../../components/Button/Button"
import TextArea from "../../components/Textarea/Textarea"
import s from "./CaseDetailsPage.module.css"

// TODO: Switch out MOCK_DATA to real data, have to wait for backend to send correct information/fields
const SAMPLE_CASE: Application =
  { id: 387139, companyName: "Nordvik Bygg AB", orgNumber: "556600-0000", authorizedSignatory: "Anna Nordvik", purpose: "Rörelsekapital", requestedAmount: 3000000, status: "PENDING_DOCS", createdAt: "2026-08-27T09:00:00Z", updatedAt: "2026-08-27T09:00:00Z"}
const EXTRA_INFO = {
  contactName: "Anna Nordvik", email: "anna@nordvik.se", phoneNumber: "070-000 00 00", currentAssets: 4200000, industry: "Bygg & Anläggning" 
}

type viewOptions = "overview" | "manageCase"

const options: SwitchOption<viewOptions>[] = [
  { label: "Översikt", value: "overview" },
  { label: "Hantera ärende", value: "manageCase" },
]

const CaseDetailsPage = () => {
  const [view, setView] = useState<viewOptions>("overview")
  const [ isAdding, setIsAdding ] = useState(false)

  return(
    <>
      <section className={s.topSection}>
        <div className={s.topWrapper}>
          <div>
            <div className={s.applicationRefWrapper}>
              <h2 className={s.applicationRef}>{formatReferenceNumber(SAMPLE_CASE.id)}</h2>
              <StatusTag status={SAMPLE_CASE.status} />
            </div>

            <div>
              <p className="title">{SAMPLE_CASE.companyName}</p>
              <p className={s.infoText}>Org.nr {SAMPLE_CASE.orgNumber} · Inkommet {formatDate(SAMPLE_CASE.createdAt)}</p>
            </div>
          </div>
          <ApplicationSummary application={SAMPLE_CASE} />
        </div>

        <ToggleSwitch variant="accent" name="view" options={options} selectedValue={view} onChange={(newView) => setView(newView)} />
      </section>

      {view === "overview" &&
        <section className={s.contentWrapper}>
          <Card>
            <h3>Kontakt</h3>
            <DataList>
              <DataListItem label="Namn" value={EXTRA_INFO.contactName} />
              <DataListItem label="E-postadress" value={EXTRA_INFO.email} />
              <DataListItem label="Telefonnummer" value={EXTRA_INFO.phoneNumber} />
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
          {/* TODO: Connect status-change buttons with backend */}
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