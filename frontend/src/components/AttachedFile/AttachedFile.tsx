import { RiCloseLine, RiDownloadLine, RiFileLine } from "react-icons/ri"
import type { ApplicationDocument } from "../../api/applicationApi"
import s from "./AttachedFile.module.css"
import Button from "../Button/Button"
import { formatDate, formatFilename } from "../../utils/formatters"
import { useDownloadDocument } from "../../hooks/useDocument"

interface AttachedFileProps {
  document: ApplicationDocument |File
  isUploading?: boolean
  removeFile?: () => void
}

const AttachedFile = ({document, isUploading = false, removeFile}: AttachedFileProps) => {
  const { mutate: download, isPending } = useDownloadDocument()

  const isLocalFile = document instanceof File

  const filename = isLocalFile ? document.name : formatFilename(document.filename)

  const handleDownload = () => {
    if(!isLocalFile) {
      download({ id: document.id, filename: filename })
    }
  }

  return(
    <div className={s.wrapper}>
      <div className={s.iconWrapper}>
        <RiFileLine />
      </div>
      <div className={s.fileInformation}>
        <p className={s.filename}>{filename}</p>
        {isLocalFile ? (
          <p className={s.fileMeta}>Vald fil för uppladdning</p>
        ) : (
          <p className={s.fileMeta}>{document.docType} · Mottagen {formatDate(document.uploadedAt)}</p>
        )}
      </div>
      {isUploading ? (
        <Button 
          type="button"
          variant="ghost"
          className={s.removeFileBtn} 
          onClick={removeFile}
          aria-label="Ta bort vald fil"
        >
          <RiCloseLine />
        </Button>
      ) : (
        <Button variant="secondary" className={s.downloadButton} disabled={isPending} onClick={handleDownload}>
          <RiDownloadLine className={s.downloadIcon} aria-hidden="true" />
          <span className={s.downloadLabel}>{isPending ? "Laddar ned..." : "Ladda ned"}</span>
        </Button>
      )}
    </div>
  )
}

export default AttachedFile