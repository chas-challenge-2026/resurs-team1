import s from "./Datalist.module.css"

interface DatalistItemProps {
  label: string;
  value: string;
}

const DatalistItem = ({label, value}: DatalistItemProps) => {


  return(
    <div className={s.itemRow}>
      <dt className={s.label}>{label}</dt>
      <dd className={s.value}>{value}</dd>
    </div>
  )
}

interface DatalistProps {
  children: React.ReactNode
}

const Datalist = ({children}: DatalistProps) => {
  return(
    <dl>
      {children}
    </dl>
  )
}

export {Datalist, DatalistItem}