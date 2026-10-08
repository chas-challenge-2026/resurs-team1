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

  const handlePreview = () => {
    if (isLocalFile) {
      const objectUrl = URL.createObjectURL(document)
      window.open(objectUrl, "_blank", "noopener,noreferrer")

      setTimeout(() => {
        URL.revokeObjectURL(objectUrl)
      }, 1000)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (isLocalFile && (e.key === "Enter" || e.key === " ")) {
      e.preventDefault();
      handlePreview();
    }
  }

  return(
    <div className={s.wrapper}>
      <div className={s.iconWrapper}>
        <RiFileLine />
      </div>

      <div 
        className={`${s.fileInformation} ${isLocalFile ? s.clickable : ""}`}
        onClick={isLocalFile ? handlePreview : undefined}
        onKeyDown={handleKeyDown}
        role={isLocalFile ? "button" : undefined}
        tabIndex={isLocalFile ? 0 : undefined}
        aria-label={isLocalFile ? `Förhandsgranska ${filename}` : undefined}
      >
        <p className={s.filename}>{filename}</p>
        {isLocalFile ? (
          <p className={s.fileMeta}>Klicka för att förhandsgranska</p>
        ) : (
          <p className={s.fileMeta}>{document.docType} · Mottagen {formatDate(document.uploadedAt)}</p>
        )}
      </div>


      {!isLocalFile &&
        <Button variant="secondary" className={s.downloadButton} disabled={isPending} onClick={handleDownload}>
          <RiDownloadLine className={s.downloadIcon} aria-hidden="true" />
          <span className={s.downloadLabel}>{isPending ? "Laddar ned..." : "Ladda ned"}</span>
        </Button>
      }

      {isUploading &&
        <Button 
          type="button"
          variant="ghost"
          className={s.removeFileBtn} 
          onClick={removeFile}
          aria-label="Ta bort vald fil"
        >
          <RiCloseLine />
        </Button>
      }
    </div>
  )
}

export default AttachedFile