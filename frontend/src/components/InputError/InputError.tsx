import { RiErrorWarningLine } from "react-icons/ri"
import s from "./InputError.module.css"

interface InputErrorProps {
  errorId?: string,
  errorMsg: string,
  showIcon?: boolean,
}

const InputError = ({errorId, errorMsg, showIcon = true}: InputErrorProps) => {
  return(
    <span id={errorId} className={s.error}>
      {showIcon && <RiErrorWarningLine aria-hidden={true}/> }
      <span>{errorMsg}</span>
    </span>
  )
}

export default InputError