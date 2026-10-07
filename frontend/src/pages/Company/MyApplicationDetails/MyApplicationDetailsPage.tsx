import { useRef, useState } from "react"
import { useParams } from "react-router-dom"
import { RiChat3Line, RiTimeLine, RiUploadCloud2Line } from "react-icons/ri"
import { formatCurrency, formatDate, formatReferenceNumber, getPurposeLabel } from "../../../utils/formatters"
import { useDocuments, useUploadDocument } from "../../../hooks/useDocument"
import { useApplication, useDetailedApplication } from "../../../hooks/useApplication"
import { Card, CardBody, CardFooter, CardHeader } from "../../../components/Card/Card"
import { DataList, DataListItem } from "../../../components/DataList/DataList"
import InputError from "../../../components/InputError/InputError"
import Loading from "../../../components/Loading/Loading"
import StatusTag from "../../../components/StatusTag/StatusTag"
import AttachedFile from "../../../components/AttachedFile/AttachedFile"
import Button from "../../../components/Button/Button"
import s from "./MyApplicationDetailsPage.module.css"
import NotFoundPage from "../../NotFound/NotFoundPage"
import { getCustomerRejectionsSummary } from "../../../utils/scoringConverter"

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB

const MyApplicationDetailsPage = () => {
  const { id } = useParams()
  const applicationId = Number(id)

  const { data: application, isPending, isError, error } = useApplication(applicationId)
  const { data: documents } = useDocuments(applicationId)
  const { mutate: uploadDocument, isPending: isUploading } = useUploadDocument()
  const detailedApplication = useDetailedApplication(applicationId)

  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string | null>(null)

  const fileInputRef = useRef<HTMLInputElement>(null)

  let reasons: string[] = []
  if(detailedApplication.data?.app.decisionReason) {
    reasons = getCustomerRejectionsSummary(detailedApplication.data?.app.decisionReason)
  }

  if(isPending) return <Loading size="lg" label="Hämtar ansökan..." centerOnPage delay />
  
  if (isError) {
    if(error.status === 404) {
      return (
        <NotFoundPage
          description = "Ansökan du söker finns inte, kontrollera att du har rätt ärendenummer eller länk. Fungerar det fortfarande inte? Då kan ansökan ha blivit borttagen. Försök hitta informationen du söker via dina övriga ansökningar."
          backLinkText="Gå till ansökningar"
          backLinkUrl="/mina-ansokningar"
        />
      )
    }
    return <p>{error.message}</p>
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]

    if (!file) return
    setFileError(null)

    if(file.size > MAX_FILE_SIZE_BYTES) {
      setFileError("Filen får inte vara större än 10 MB.")
      setSelectedFile(null)
      return
    }

    if (file.type !== "application/pdf" && !file.name.endsWith(".pdf")) {
      setFileError("Endast PDF-filer är tillåtna.")
      if (fileInputRef.current) {
        fileInputRef.current.value = ""
      }
      return
    }

    setFileError(null)
    setSelectedFile(file)
  }

  const handleRemoveFile = () => {
    setSelectedFile(null)
    setFileError(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ""
    }
  }

  const handleSubmit = () => {
    if (selectedFile) {
      uploadDocument({
        applicationId,
        docType: "pdf",
        file: selectedFile,
      }, {
        onSuccess: () => {
          setSelectedFile(null)
        }
      })
    }
  }

  return(
    <div className={s.wrapper}>
      <div>
        <div className={s.titleGroup}>
          <h2 className="title">{formatReferenceNumber(application.id)}</h2>
          <StatusTag status={application.status} />
        </div>
        <p className={s.description}>{getPurposeLabel(application.purpose)} · {formatCurrency(application.requestedAmount)} {application.durationMonths && `· ${application.durationMonths} månader`}</p>
      </div>

      {/* TODO: Add text from caseworker if caseworker rejected the case */}
      {application.status === "REJECTED" &&
        <Card as="section" variant="info">
          <CardHeader className={s.infoHeader}>
            <div className={s.headerText}>
              <h3 className={s.infoTitle}>Din ansökan har blivit avslagen</h3>
              <p className={s.infoSubtitle}>
                Din ansökan kunde tyvärr inte beviljas automatiskt på grund av följande faktorer:
              </p>
              <ul className={s.reasonList}>
                {reasons.map((reason, index) => (
                  <li key={index} className={s.reasonItem}>
                    <span className={s.bulletIcon}>-</span>
                    <span>{reason}</span>
                  </li>
                ))}
              </ul>
            </div>
          </CardHeader>
        </Card>
      }

      {application.status === "UNDER_REVIEW" &&
        <Card as="section" variant="info">
          <CardHeader className={s.infoHeader}>
            <div className={s.iconWrapper}>
              <RiTimeLine />
            </div>
            <div className={s.headerText}>
              <h3 className={s.infoTitle}>Din ansökan behandlas</h3>
              <p className={s.infoSubtitle}>
                En handläggare granskar just nu dina uppgifter. Normal handläggningstid är 1-2 bankdagar. Vi hör av oss om vi behöver kompletterande information.
              </p>
            </div>
          </CardHeader>
        </Card>
      }

      {application.status === "PENDING_DOCS" &&
        <Card as="section" variant="info">
          <CardHeader className={s.infoHeader}>
            <div className={s.iconWrapper}>
              <RiChat3Line />
            </div>
            <div className={s.headerText}>
              <h3 className={s.infoTitle}>Vi behöver mer information</h3>
              <p className={s.infoSubtitle}>Din handläggare behöver kompletterande information innan ansökan kan behandlas vidare.</p>
            </div>
          </CardHeader>
          <CardBody className={s.infoBody}>
            <p>{application.decision}</p>

            <Button
              variant="ghost"
              className={s.dropzone}
              onClick={() => fileInputRef.current?.click()}
            >
              <RiUploadCloud2Line className={s.dropzoneIcon} aria-hidden="true" />
              <span className={s.dropzoneTitle}>Bifoga fil</span>
              <span className={s.dropzoneHint}>Klicka för att bifoga en PDF (max 10 MB)</span>
            </Button>

            {selectedFile && (
              <AttachedFile document={selectedFile} isUploading removeFile={handleRemoveFile} />
            )}

            {fileError && <InputError errorId="fileError" errorMsg={fileError} />}

            <input
              type="file" 
              ref={fileInputRef} 
              onChange={handleFileChange}
              accept=".pdf,application/pdf"
              style={{ display: "none" }} 
            />
          </CardBody>
          <CardFooter className={s.warningFooter}>
            <div className={s.actionGroup}>
              <Button 
                onClick={handleSubmit} 
                disabled={isUploading || Boolean(!selectedFile)}
              >
                {isUploading ? "Skickar..." : "Skicka"}
              </Button>
            </div>
          </CardFooter>
        </Card>
      }

      {documents && documents.length > 0 &&
        <Card as="section">
          <h3 className="subtitle">Uppladdade filer</h3>
          {documents?.map((document) => (
            <AttachedFile key={document.id} document={document} removeFile={handleRemoveFile} />
          ))}
        </Card>
      }

      <Card as="section">
        <CardHeader>
          <h3 className="subtitle">Ansökningsuppgifter</h3>
          <DataList>
            <DataListItem label="Ärendenummer" value={formatReferenceNumber(application.id)} />
            <DataListItem label="Ändamål" value={getPurposeLabel(application.purpose)} />
            <DataListItem label="Belopp" value={formatCurrency(application.requestedAmount)} />
            <DataListItem label="Återbetalningstid" value={`${application.durationMonths} månader`} />
            <DataListItem label="Inskickad" value={formatDate(application.createdAt)} />
          </DataList>
        </CardHeader>
      </Card>
    </div>
  )
}

export default MyApplicationDetailsPage