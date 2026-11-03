import { useState } from "react"
import { FiEye } from "react-icons/fi"
import s from "./ProtectedValue.module.css"

interface ProtectedValueProps {
  /** the sensitive value, e.g. a name or phone number */
  children: React.ReactNode
}

/**
 * Hides a sensitive value until the caseworker hovers it (desktop)
 * or taps/clicks it (mobile + keyboard). Tapping again hides it.
 * Visual only: protects against someone looking over the shoulder,
 * the value is still in the page and in the API response.
 * Screen readers read the value as normal, the "Visa" mask is hidden from them.
 *
 * @example
 * <ProtectedValue>{data.contactDetails.name}</ProtectedValue>
 */
const ProtectedValue = ({ children }: ProtectedValueProps) => {
  const [isRevealed, setIsRevealed] = useState(false)

  return(
    // a button so tap, click, Enter and Space all work without extra code
    <button
      type="button"
      className={isRevealed ? `${s.wrapper} ${s.revealed}` : s.wrapper}
      aria-pressed={isRevealed}
      onClick={() => setIsRevealed(!isRevealed)}
    >
      <span className={s.value}>{children}</span>
      <span className={s.mask} aria-hidden>
        <FiEye className={s.icon} />
        Visa
      </span>
    </button>
  )
}

export default ProtectedValue
