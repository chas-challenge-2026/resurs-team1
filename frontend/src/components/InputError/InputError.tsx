import { RiErrorWarningLine } from "react-icons/ri"
import s from "./InputError.module.css"

interface InputErrorProps {
  errorId: string,
  errorMsg: string,
}

const InputError = ({errorId, errorMsg}: InputErrorProps) => {
  return(
    <span id={errorId} className={s.error}>
      <RiErrorWarningLine aria-hidden={true}/>
      <span>{errorMsg}</span>
    </span>
  )
}

export default InputError