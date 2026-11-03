import { FiLock } from "react-icons/fi"
import s from "./ProtectedTag.module.css"

/**
 * Small "Skyddat" tag, placed next to a heading whose content is protected.
 *
 * @example
 * <h3>Kontakt {data.protectedResource && <ProtectedTag />}</h3>
 */
const ProtectedTag = () => {
  return(
    <span className={s.tag}>
      <FiLock className={s.icon} aria-hidden />
      Skyddat
    </span>
  )
}

export default ProtectedTag
