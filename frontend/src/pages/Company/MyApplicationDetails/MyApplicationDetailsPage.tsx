import { useRef, useState } from "react"
import { useParams } from "react-router-dom"
import { RiChat3Line, RiUploadCloud2Line } from "react-icons/ri"
import { formatCurrency, formatDate, formatReferenceNumber } from "../../../utils/formatters"
import { useDocuments, useUploadDocument } from "../../../hooks/useDocument"
import { useApplication } from "../../../hooks/useApplication"
import { Card, CardBody, CardFooter, CardHeader } from "../../../components/Card/Card"
import { DataList, DataListItem } from "../../../components/DataList/DataList"
import InputError from "../../../components/InputError/InputError"
import Loading from "../../../components/Loading/Loading"
import StatusTag from "../../../components/StatusTag/StatusTag"
import AttachedFile from "../../../components/AttachedFile/AttachedFile"
import Button from "../../../components/Button/Button"
import s from "./MyApplicationDetailsPage.module.css"

const MyApplicationDetailsPage = () => {
  const { id } = useParams()
  const applicationId = Number(id)

  const { data: application, isPending, isError, error } = useApplication(applicationId)
  const { data: documents } = useDocuments(applicationId)
  const { mutate: uploadDocument, isPending: isUploading } = useUploadDocument()

  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string | null>(null)

  const fileInputRef = useRef<HTMLInputElement>(null)

  if(isPending) return <Loading label="Hämtar ansökan..." delay />
  if (isError) return <p>{error.message}</p>

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

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
        fileName: selectedFile.name,
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
        <p className={s.description}>{application.purpose} · {formatCurrency(application.requestedAmount)} {application.durationMonths && `· ${application.durationMonths} månader`}</p>
      </div>

      {application.status === "PENDING_DOCS" &&
        <Card as="section" variant="warning">
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

            <Button
              variant="ghost"
              className={s.dropzone}
              onClick={() => fileInputRef.current?.click()}
            >
              <RiUploadCloud2Line className={s.dropzoneIcon} aria-hidden="true" />
              <span className={s.dropzoneTitle}>Bifoga fil</span>
              <span className={s.dropzoneHint}>Klicka för att bifoga en PDF-fil</span>
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
            <DataListItem label="Ändamål" value={application.purpose} />
            <DataListItem label="Belopp" value={formatCurrency(application.requestedAmount)} />
            {application.durationMonths && <DataListItem label="Återbetalningstid" value={`${application.durationMonths} månader`} />}
            <DataListItem label="Inskickad" value={formatDate(application.createdAt)} />
          </DataList>
        </CardHeader>
      </Card>
    </div>
  )
}

export default MyApplicationDetailsPage