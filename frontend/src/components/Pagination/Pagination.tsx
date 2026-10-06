import { useMemo } from "react";
import { RiArrowLeftSLine, RiArrowRightSLine } from "react-icons/ri";
import Button from "../Button/Button";
import s from "./Pagination.module.css"

// TODO: Confirm with backend how the data type is supposed to be and move out type to seperate folder
interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

function getPageNumbers(current: number, total: number): (number | "...")[] {
  // Gather first, last and pages around the active page
  const set = new Set(
    [1, total, current - 1, current, current + 1].filter(
      (p) => p >= 1 && p <= total
    )
  )

  const sorted = Array.from(set).sort((a, b) => a - b)

  const result: (number | "...")[] = []

  for (let i = 0; i < sorted.length; i++) {
    if (i > 0 && sorted[i] - sorted[i - 1] > 1) {
      result.push("...");
    }
    result.push(sorted[i])
  }
  
  return result
}

const Pagination = ({ currentPage, totalPages, onPageChange, disabled = false }: PaginationProps) => {
  const pages = useMemo(
    () => getPageNumbers(currentPage, totalPages),
    [currentPage, totalPages]
  )

  if (totalPages <= 1) return null
  
  return (
    <nav aria-label="Paginering" className={s.wrapper}>
      {/* Back arrow */}
      <Button
        variant="ghost"
        className={s.arrowButton}
        onClick={() => onPageChange(currentPage - 1)}
        disabled={disabled || currentPage === 1}
        aria-label="Gå till föregående sida"
      >
        <RiArrowLeftSLine />
      </Button>

      {/* Page numbers and ellipses */}
      <ul className={s.pageList}>
        {pages.map((p, index) =>
          p === "..." ? (
            <li key={`ellipsis-${index}`} className={s.ellipsisItem}>
              <span className={s.ellipsis} aria-hidden="true">
                ...
              </span>
            </li>
          ) : (
            <li key={p}>
              <Button
                variant="ghost"
                className={`${s.pageButton} ${p === currentPage ? s.active : ""}`}
                onClick={() => onPageChange(p)}
                disabled={disabled}
                aria-current={p === currentPage ? "page" : undefined}
                aria-label={`Sida ${p}`}
              >
                {p}
              </Button>
            </li>
          )
        )}
      </ul>

      {/* Next arrow */}
      <Button
        variant="ghost"
        className={s.arrowButton}
        onClick={() => onPageChange(currentPage + 1)}
        disabled={disabled || currentPage === totalPages}
        aria-label="Gå till nästa sida"
      >
        <RiArrowRightSLine />
      </Button>
    </nav>
  )
}

export default Pagination