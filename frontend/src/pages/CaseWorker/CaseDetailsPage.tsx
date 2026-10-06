import { useState } from "react"
import { useParams } from "react-router-dom"
import { formatCurrency, formatDate, formatReferenceNumber } from "../../utils/formatters"
import { getMetric } from "../../utils/scoringConverter"
import { useBackofficeApplication, useDecideApplication } from "../../hooks/useApplication"
import type { Decision } from "../../api/applicationApi"
import type { ScoringStatus, BadgeConfig } from "../../types/scoring"
import ToggleSwitch, { type SwitchOption } from "../../components/ToggleSwitch/ToggleSwitch"
import { Card, CardFooter } from "../../components/Card/Card"
import { DataList, DataListItem } from "../../components/DataList/DataList"
import ApplicationSummary from "../../components/ApplicationSummary/ApplicationSummary"
import StatusTag from "../../components/StatusTag/StatusTag"
import Button from "../../components/Button/Button"
import TextArea from "../../components/Textarea/Textarea"
import Loading from "../../components/Loading/Loading"
import NotFoundPage from "../NotFound/NotFoundPage"
import s from "./CaseDetailsPage.module.css"

type viewOptions = "overview" | "manageCase"

const options: SwitchOption<viewOptions>[] = [
  { label: "Översikt", value: "overview" },
  { label: "Hantera ärende", value: "manageCase" },
]

const STATUS_BADGE: Record<ScoringStatus, BadgeConfig> = {
  REJECT: { label: "Avvisad", className: s.badgeReject},
  FLAGGED: { label: "Flaggad", className: s.badgeFlagged},
  OK: { label: "Normal", className: s.badgeOk},
  GOOD: { label: "God", className: s.badgeOk}
}

// keys must match the backend scoring log exactly
const SCORING_METRICS = [
  { key: "kreditPoäng", label: "Kreditpoäng" },
  { key: "soliditet", label: "Soliditet" },
  { key: "likviditetsgrad", label: "Likviditetsgrad" },
  { key: "skuldsättningsgrad", label: "Skuldsättningsgrad" },
  { key: "ränteTäckning", label: "Räntetäckningsgrad" },
]

type ActionType = "approve" | "requestDocs" | "reject" | null

const CaseDetailsPage = () => {
  const [view, setView] = useState<viewOptions>("overview")
  const [ activeAction, setActiveAction ] = useState<ActionType>(null)
  const { id } = useParams()
  const applicationId = Number(id)

  const [rejectComment, setRejectComment] = useState("")
  const { data, isPending, isError, error } = useBackofficeApplication(applicationId)
  console.log(data)
  const decide = useDecideApplication()

  const handleDecision = (decision: Decision, comment = "") => {
    decide.mutate({ id: applicationId, decision, comment }, {
      onSuccess: () => {
        setActiveAction(null)
        setRejectComment("")
      },
    })
  }

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
          <ApplicationSummary application={data} amountBackground="neutral" durationColor="neutral" />
        </div>

        <ToggleSwitch variant="accent" name="view" options={options} selectedValue={view} onChange={(newView) => setView(newView)} />
      </section>

      {view === "overview" &&
        <section className={s.contentWrapper}>
          <Card>
            <h3 className="subtitle">Kontakt</h3>
            <DataList>
              <DataListItem label="Namn" value={data.contactDetails.name} />
              <DataListItem label="E-postadress" value={data.contactDetails.email} />
              <DataListItem label="Telefonnummer" value={data.contactDetails.phoneNumber} />
            </DataList>
          </Card>
          
          {data.scoringResult && (
            <Card>
              <h3 className="subtitle">Scoringresultat</h3>
              <DataList>
                {SCORING_METRICS.map(({key, label}) => {
                  const metric = getMetric(data.scoringResult ?? "", key)
                  return(
                    <DataListItem
                      key={key}
                      label={label}
                      value={
                        <div className={s.metricRow}>
                          <span>{metric.value}</span>
                          {metric.scoringStatus && (
                            <span className={`${s.badge} ${STATUS_BADGE[metric.scoringStatus].className}`}>{STATUS_BADGE[metric.scoringStatus].label}</span>
                          )}
                        </div>
                      }
                    />
                  )
                })}
              </DataList>
            </Card>
          )}

          <Card>
            <h3 className="subtitle">Ekonomi</h3>
            {data.companyFinances ? (
              <DataList>
                <DataListItem label="Omsättning" value={formatCurrency(data.companyFinances.incomeStatement.revenue)} />
                <DataListItem label="Rörelseresultat" value={formatCurrency(data.companyFinances.incomeStatement.operatingResult)} />
                <DataListItem label="Eget kapital" value={formatCurrency(data.companyFinances.balanceSheet.equity)} />
                <DataListItem label="Totalt kassaflöde" value={formatCurrency(data.companyFinances.cashFlowStatement.operatingCashFlow)} />
                <DataListItem label="Kortfristiga skulder" value={formatCurrency(data.companyFinances.balanceSheet.shortTermLiabilities)} />
              </DataList>
            ) : (
              <p>Ingen årsredovisning hittades</p>
            )}
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
              color="var(--color-warning)"
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

          {activeAction === "approve" && (
            <Card as="section">
              <div className={s.textContainer}>
                <h3 className={s.addingTitle}>Vill du godkänna ärendet?</h3>
                <p>Kontrollera att uppgifterna och eventuella kompletteringar är granskade. Ingen kommentar krävs.</p>
              </div>
              <CardFooter className={s.addingFooter}>
                <Button onClick={() => handleDecision("APPROVED")} disabled={decide.isPending}>
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
              <TextArea
                id="message"
                label="message"
                placeholder="Beskriv anledningen till avslaget..."
                value={rejectComment}
                onChange={(e) => setRejectComment(e.target.value)}
              />
              <CardFooter className={s.addingFooter}>
                <Button onClick={() => handleDecision("REJECTED", rejectComment)} disabled={decide.isPending || !rejectComment.trim()}>
                  Avvisa ärendet
                </Button>
                <Button variant="secondary" onClick={() => setActiveAction(null)}>
                  Avbryt
                </Button>
              </CardFooter>
            </Card>
          )}

          {decide.isError && <p>{decide.error.message}</p>}

          {/* TODO: Add view for requests and corresponding attachments */}
        </section>
      }
    </>
  )
}

export default CaseDetailsPage