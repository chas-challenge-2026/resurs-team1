import { RiDownloadLine, RiFileLine } from "react-icons/ri"
import type { ApplicationDocument } from "../../api/applicationApi"
import s from "./AttachedFile.module.css"
import Button from "../Button/Button"
import { formatDate } from "../../utils/formatters"
import { useDownloadDocument } from "../../hooks/useDocument"

interface AttachedFileProps {
  document: ApplicationDocument
}

const AttachedFile = ({document}: AttachedFileProps) => {
  const { mutate: download, isPending } = useDownloadDocument()

  const handleDownload = () => {
    download({ id: document.id, fileName: document.filename })
  }

  return(
    <div className={s.wrapper}>
      <div className={s.iconWrapper}>
        <RiFileLine />
      </div>
      <div className={s.fileInformation}>
        <p className={s.fileName}>{document.filename}</p>
        <p className={s.fileMeta}>{document.docType} · Mottagen {formatDate(document.uploadedAt)}</p>
      </div>
      <Button variant="secondary" className={s.downloadButton} disabled={isPending} onClick={handleDownload}>
        <RiDownloadLine className={s.downloadIcon} aria-hidden="true" />
        <span className={s.downloadLabel}>{isPending ? "Laddar ned..." : "Ladda ned"}</span>
      </Button>
    </div>
  )
}

export default AttachedFile