import s from "./Datalist.module.css"

/**
 * Props for individual rows in the DataList.
 */
interface DataListItemProps {
  /** The label/title describing the data (e.g., "Contact person" or "Organization number"). */
  label: string;
  /** The value associated with the label (e.g., "Anna Andersson" or "5566000-0000"). */
  value: string;
}

/**
 * Represents an individual key/value pair within a `DataList`.
 * Built using semantic `<dt>` (Data Term) and `<dd>` (Data Definition) HTML elements.
 *
 * @example
 * <DataListItem label="Email address" value="namn@foretag.se" />
 */
const DataListItem = ({label, value}: DataListItemProps) => {

  return(
    <div className={s.itemRow}>
      <dt className={s.label}>{label}</dt>
      <dd className={s.value}>{value}</dd>
    </div>
  )
}

/**
 * Props for the main DataList container.
 */
interface DataListProps {
  /** One or more `DataListItem` components. */
  children: React.ReactNode
}

/**
 * An accessible key/value list (Description List) used to display
 * structured data such as summaries, profile details, or receipts.
 * 
 * Uses the HTML5 `<dl>` element internally for proper WCAG and screen reader support.
 *
 * @example
 * ```tsx
 * <DataList>
 *   <DataListItem label="Organization number" value="5566000-0000"/>
 *   <DataListItem label="Contact person" value="Anna Andersson"/>
 * </DataList>
 * ```
 */
const DataList = ({children}: DataListProps) => {
  return(
    <dl>
      {children}
    </dl>
  )
}

export {DataList, DataListItem}