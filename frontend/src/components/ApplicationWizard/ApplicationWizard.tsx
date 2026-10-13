import { formatCurrency } from "../../utils/formatters";
import { LOAN_MIN, LOAN_MAX, EMAIL_PATTERN, PHONE_PATTERN } from "../../constants/constants";
import { PURPOSE_OPTIONS } from "../../constants/constants";
import { REPAYMENT_OPTIONS } from "../../constants/constants";
import Dropdown from "../Dropdown/Dropdown";
import ButtonGroup from "../ButtonGroup/ButtonGroup";
import { Card, CardBody } from "../Card/Card";
import { DataList, DataListItem } from "../DataList/DataList";
import Input from "../Input/Input";
import Slider from "../Slider/Slider";
import s from "./ApplicationWizard.module.css"
import { getUser } from "../../utils/auth";
import type { CompanyUser } from "../../types/user";
import { useGetFinancial } from "../../hooks/useFinancial";
import Loading from "../Loading/Loading";
import Button from "../Button/Button";

/** Everything the customer fills in + autofilled*/
export interface ApplicationFormData {
  // auto filled:
  orgNumber: string;
  companyName: string;

  // filled out during wizard:
  contactName: string;
  email: string;
  phoneNumber: string;


  purpose: string; // becomes a union once the backend hands over the enum values -- A | B | C
  requestedAmount: number;
  // undefined until the customer picks one
  durationMonths?: number;
}

interface ApplicationWizardProps {
  /** Which step to render. Owned by the page. */
  step: number;
  /** Current answers. Owned by the page. */
  values: ApplicationFormData;
  /** Report a change upwards. Partial, so a field can be sent on its own. */
  onChange: (patch: Partial<ApplicationFormData>) => void;
  /** if form submit has an error or not, use to show error message */
  isSubmitError: boolean;
  /** If user has accepted terms or not */
  isTermsAccepted: boolean;
  /** Function for toggling accepting terms */
  onTermsChange: (accepted: boolean) => void;
}

/**
 * ApplicationWizard – renders the fields for one step of the credit application.
 *
 * Holds no state. The page owns the answers and therefore also the buttons —
 * navigation, "Avbryt ansökan" and submit all live on the page.
 *
 * @example
 * ```tsx
 * const [step, setStep] = useState(1)
 * const [values, setValues] = useState<ApplicationFormData>(emptyApplication)
 *
 * // old answers first, then the patch overwrites only what changed
 * const handleChange = (patch: Partial<ApplicationFormData>) =>
 *   setValues((prev) => ({ ...prev, ...patch }))
 *
 * <ApplicationWizard step={step} values={values} onChange={handleChange} />
 * <Button onClick={() => setStep(step + 1)}>Fortsätt</Button>
 *
 * // dragging the slider calls onChange({ loanAmount: 2000000 })
 * // → values becomes { ...everything else, loanAmount: 2000000 }
 * ```
 */
const ApplicationWizard = ({ step, values, onChange, isSubmitError, isTermsAccepted, onTermsChange }: ApplicationWizardProps) => {
  const user = getUser() as CompanyUser
  const financials = useGetFinancial()
  
  switch (step) {
    case 1:
      return (
        <Card>
          <CardBody>
            <Dropdown
              id="purpose"
              label="Ändamål - Vad ska lånet användas till?"
              placeholder="Välj ändamål..."
              options={PURPOSE_OPTIONS}
              value={values.purpose}
              onChange={(purpose) => onChange({ purpose })}
            />
            <Slider
              name="loanAmount"
              label="Önskat belopp"
              min={LOAN_MIN}
              max={LOAN_MAX}
              step={50000}
              value={values.requestedAmount}
              onChange={(loanAmount) => onChange({ requestedAmount: loanAmount })}
            />
            <ButtonGroup
              name="durationMonths"
              label="Önskad återbetalningstid (månad)"
              options={REPAYMENT_OPTIONS}
              selectedValue={values.durationMonths}
              onChange={(durationMonths) => onChange({ durationMonths })}
            />
          </CardBody>
        </Card>
      )

    case 2: {
      // an untouched field is not wrong yet, so only complain once something is typed
      const emailError =
        values.email !== "" && !EMAIL_PATTERN.test(values.email)
          ? "Kontrollera e-postadressen"
          : undefined
      const phoneError =
        values.phoneNumber !== "" && !PHONE_PATTERN.test(values.phoneNumber)
          ? "Kontrollera telefonnumret"
          : undefined

      return (
        <Card>
          <CardBody>
            <div className={`input-base ${s.preFilled}`}>
              <span>Org.nr</span>
              <p>{user.orgNumber}</p>
            </div>

            <Input
              id="contactName"
              label="Kontaktperson"
              placeholder="Anna Andersson"
              value={values.contactName}
              onChange={(e) => onChange({ contactName: e.target.value })}
            />
            <Input
              id="email"
              label="E-postadress"
              type="email"
              placeholder="anna@foretag.se"
              value={values.email}
              error={emailError}
              onChange={(e) => onChange({ email: e.target.value })}
            />
            <Input
              id="phoneNumber"
              label="Telefonnummer"
              type="tel"
              placeholder="070-123 45 67"
              information="Vi ringer er om vi behöver kompletterande uppgifter"
              value={values.phoneNumber}
              error={phoneError}
              onChange={(e) => onChange({ phoneNumber: e.target.value })}
            />
          </CardBody>
        </Card>
      )
    }

    case 3: {
      
      if (financials.isLoading) {
        return (
          <Card>
            <CardBody className={s.loadingCard}>
              <Loading />
              <span>Hämtar ekonomiska uppgifter från Bolagsverket...</span>
            </CardBody>
          </Card>
        )
      }
      
      if (financials.isError || !financials.data) {
        return (
          <Card>
            <CardBody className={s.errorCard}>
              <p>Det gick inte att hämta de ekonomiska uppgifterna automatiskt.</p>
              <Button variant="secondary" onClick={() => financials.refetch()}>
                Försök igen
              </Button>
            </CardBody>
          </Card>
        )
      }

      const { incomeStatement, balanceSheet } = financials.data

      return (
        <Card>
          <CardBody>
            <div className={s.infoBox}>
              <span className={s.infoBoxTitle}>Automatisk datahämtning</span>
              <p>
                Ekonomiska uppgifter hämtas automatiskt från Bolagsverket. Du behöver inte fylla i något manuellt.
              </p>
            </div>

            <DataList>
              <DataListItem label="Företagsnamn" value={user.companyName} />
              <DataListItem label="Omsättning (senaste räkenskapsåret)" value={formatCurrency(incomeStatement.revenue)} />
              <DataListItem label="Rörelseresultat / EBIT" value={formatCurrency(incomeStatement.operatingResult)} />
              <DataListItem label="Eget kapital" value={formatCurrency(balanceSheet.equity)} />
              <DataListItem label="Summa tillgångar" value={formatCurrency(balanceSheet.totalAssets)} />
              <DataListItem label="Kortfristiga skulder" value={formatCurrency(balanceSheet.shortTermLiabilities)} />
            </DataList>
          </CardBody>
        </Card>
      )
    }

    case 4: {
      const purposeLabel = PURPOSE_OPTIONS.find((option) => option.value === values.purpose)?.label

      return (
        <Card>
          <CardBody className={s.checkContainer}>
            <p className={`information-text ${s.informationText}`}>
              Kontrollera att uppgifterna stämmer innan du skickar in.
            </p>

            <div>
              <h3 className="subtitle">Kontakt</h3>
              <DataList>
                <DataListItem label="Organisationsnummer" value={values.orgNumber} />
                <DataListItem label="Kontaktperson" value={values.contactName} />
                <DataListItem label="E-post" value={values.email} />
                <DataListItem label="Telefonnummer" value={values.phoneNumber} />
              </DataList>
            </div>

            <div>
              <h3 className="subtitle">Ansökan</h3>
              <DataList>
                <DataListItem label="Ändamål" value={purposeLabel ?? "Ej valt"} />
                <DataListItem label="Belopp" value={formatCurrency(values.requestedAmount)} />
                <DataListItem
                  label="Återbetalningstid"
                  value={values.durationMonths ? `${values.durationMonths} mån` : "Ej valt"}
                />
              </DataList>
            </div>

            {financials.data && 
              <div>
                <h3 className="subtitle">Ekonomi</h3>
                <DataList>
                  <DataListItem label="Företagsnamn" value={user.companyName} />
                  <DataListItem label="Omsättning" value={formatCurrency(financials.data.incomeStatement.revenue)} />
                  <DataListItem label="Rörelseresultat" value={formatCurrency(financials.data.incomeStatement.operatingResult)} />
                </DataList>
              </div>
            }

            <div>
              <label className={s.checkboxLabel}>
                <input
                  type="checkbox"
                  checked={isTermsAccepted}
                  onChange={(e) => onTermsChange(e.target.checked)}
                />
                <span className="information-text">
                  Jag intygar att uppgifterna är korrekta och godkänner Resurs{" "}
                  <a href="https://documenthandler.resurs.com/Dokument.pdf?customerType=legal&docType=commonTerms&land=SE&language=sv" target="_blank" rel="noopener noreferrer" className={s.link}>
                    allmänna villkor
                  </a>{" "}
                  samt att kreditupplysning får inhämtas.
                </span>
              </label>
            </div>

            {isSubmitError &&
              <p role="alert" className={s.submitError}>Ansökan kunde inte skickas just nu. Försök igen senare.</p>
            }

          </CardBody>
        </Card>
      )
    }

    default:
      return null
  }
}

export default ApplicationWizard
