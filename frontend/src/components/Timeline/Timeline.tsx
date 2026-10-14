import type { AuditEvent } from "../../api/applicationApi"
import { ACTOR_LABELS, AUDIT_ACTION_LABELS, STATUS_LABELS } from "../../constants/constants"
import s from "./Timeline.module.css"

interface TimelineProps {
  events: AuditEvent[]
}

const Timeline = ({ events }: TimelineProps) => {
  return (
    <ol className={s.list}>
      {events.toReversed().map((event) => {
        const { comment, filename, newStatus, workerName, actorType } = event.data

        const action = AUDIT_ACTION_LABELS[event.action]
        const label = newStatus ? `${action}: ${STATUS_LABELS[newStatus]}` : action
        const actor = workerName ?? ACTOR_LABELS[actorType]

        return (
          <li key={event.sequenceNumber} className={s.item}>
            <p className={s.timeAndActor}>
              <time dateTime={event.occurredAt}>
                {new Date(event.occurredAt).toLocaleString("sv-SE", { dateStyle: "short", timeStyle: "short" })}
              </time>
              {" · "}{actor}
            </p>
            <p className={s.action}>{label}</p>
            {comment && <p className={`${s.detail} ${s.comment}`}>{comment}</p>}
            {filename && <p className={s.detail}>{filename}</p>}
          </li>
        )
      })}
    </ol>
  )
}

export default Timeline
