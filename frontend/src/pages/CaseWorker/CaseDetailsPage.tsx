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
import NotFoundPage from "../NotFound/NotFoundPage"

// TODO: swap out EXTRA_INFO once backend sends the data they use for the calculations, they are convinced yearly company statements are gdpr and delete it =,)
const EXTRA_INFO = {
  currentAssets: 4200000, industry: "Bygg & Anläggning" 
}

type viewOptions = "overview" | "manageCase"

const options: SwitchOption<viewOptions>[] = [
  { label: "Översikt", value: "overview" },
  { label: "Hantera ärende", value: "manageCase" },
]

type ActionType = "approve" | "requestDocs" | "reject" | null

const CaseDetailsPage = () => {
  const [view, setView] = useState<viewOptions>("overview")
  const [ activeAction, setActiveAction ] = useState<ActionType>(null)
  const { id } = useParams()
  const applicationId = Number(id)

  const { data, isPending, isError, error } = useBackofficeApplication(applicationId)

  const handleActionToggle = (action: ActionType) => {
    if(action === activeAction) {
      setActiveAction(null)
      return
    }
    setActiveAction(action)
  }

  if (isPending) return <Loading size="lg" label="Hämtar ärende..." centerOnPage delay />

  if (isError) {
    if(error.status === 404) {
      return (
        <NotFoundPage
          description = "Ärendet du söker finns inte, kontrollera att du har skrivit in rätt ärendenummer eller länk. Fungerar det fortfarande inte? Då kan ärendet ha blivit borttaget eller arkiverat. Försök hitta ärendet du söker via ärendelistan."
          showRedirectButton = {false}
        />
      )
    }
    return <div className={s.centerWrapper}><p>{error.message}</p></div>
  }

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
          <div className={s.actionsWrapper}>
            <Button
              variant="secondary" 
              className={s.actionButton}
              active={activeAction === "approve"}
              onClick={() => handleActionToggle("approve")}
            >
              Godkänn
            </Button>
            <Button 
              variant="secondary"
              color="var(--color-warning-strong)"
              className={s.actionButton}
              active={activeAction === "requestDocs"}
              onClick={() => handleActionToggle("requestDocs")}
            >
              Komplettera
            </Button>
            <Button
              variant="secondary" 
              color="var(--color-error)"
              className={s.actionButton}
              active={activeAction === "reject"}
              onClick={() => handleActionToggle("reject")}
            >
              Avvisa
            </Button>
          </div>

          {/* TODO: Connect submit buttons with backend */}
          {activeAction === "approve" && (
            <Card as="section">
              <div className={s.textContainer}>
                <h3 className={s.addingTitle}>Vill du godkänna ärendet?</h3>
                <p>Kontrollera att uppgifterna och eventuella kompletteringar är granskade. Ingen kommentar krävs.</p>
              </div>
              <CardFooter className={s.addingFooter}>
                <Button>
                  Godkänn ärendet
                </Button>
                <Button variant="secondary" onClick={() => setActiveAction(null)}>
                  Avbryt
                </Button>
              </CardFooter>
            </Card>
          )}

          {activeAction === "requestDocs" && (
            <Card as="section">
              <h3 className={s.addingTitle}>Beskriv vilket dokument du behöver från kunden</h3>
              <TextArea id="message" label="message" placeholder="T.ex. årsredovisning, kontoutdrag, offert..." />
              <CardFooter className={s.addingFooter}>
                <Button>
                  Skicka förfrågan
                </Button>
                <Button variant="secondary" onClick={() => setActiveAction(null)}>
                  Avbryt
                </Button>
              </CardFooter>
            </Card>
          )}

          {activeAction === "reject" && (
            <Card as="section">
              <h3 className={s.addingTitle}>Skriv en kommentar till varför ärendet avvisas</h3>
              <TextArea id="message" label="message" placeholder="Beskriv anledningen till avslaget..." />
              <CardFooter className={s.addingFooter}>
                <Button>
                  Avvisa ärendet
                </Button>
                <Button variant="secondary" onClick={() => setActiveAction(null)}>
                  Avbryt
                </Button>
              </CardFooter>
            </Card>
          )}

          {/* TODO: Add view for requests and corresponding attachments */}
        </section>
      }
    </>
  )
}

export default CaseDetailsPage