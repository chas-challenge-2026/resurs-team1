import { FiShield } from "react-icons/fi"
import s from "./ProtectedNotice.module.css"

/**
 * Reminder shown on a case where the signatory has a protected identity.
 * Only certified caseworkers can open these cases, so this is a reminder, not a gate.
 *
 * @example
 * {data.protectedResource && <ProtectedNotice />}
 */
const ProtectedNotice = () => {
  return(
    <section className={s.notice} role="note" aria-labelledby="protected-notice-title">
      <FiShield className={s.icon} aria-hidden />
      <div>
        <h3 id="protected-notice-title" className={s.title}>Firmatecknaren har skyddad identitet</h3>
        <p>Lämna inte ut namn, e-post eller telefonnummer till någon.</p>
      </div>
    </section>
  )
}

export default ProtectedNotice
