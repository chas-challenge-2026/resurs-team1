import type { InputHTMLAttributes } from "react";
import { RiErrorWarningLine } from "react-icons/ri";
import s from "./Input.module.css"

interface BaseInputProps extends InputHTMLAttributes<HTMLInputElement> {
  error?: string
  information?: string
}

interface StandardInputProps extends BaseInputProps {
  id: string;
  label: string;
}

interface SearchInputProps extends BaseInputProps {
  type: "search";
  "aria-label": string;
  id?: never;
  label?: never;
}

type InputProps = StandardInputProps | SearchInputProps

const Input = (props: InputProps) => {
  const { type="text", label, error, information, className, id, ...rest } = props

  const combinedClassName = [s.input, "input-base", className, error && s.errorBorder].filter(Boolean).join(" ")

  //Id for screen reader
  const errorId = id && error ? `${id}-error` : undefined
  const infoId = id && information ? `${id}-info` : undefined
  const describedBy = errorId || infoId

  return(
    <div className={s.wrapper}>
      {label && (
        <label htmlFor={id}>
          {label}
        </label>
      )}

      <input
        id={id}
        type={type}
        className={combinedClassName}
        aria-invalid={!!error}
        aria-describedby={describedBy}
        {...rest}
      />

      {information && !error && 
        <span id={infoId} className={s.information}>{information}</span>
      }

      {error && 
        <span id={errorId} className={s.error}>
          <span>{<RiErrorWarningLine aria-hidden={true}/>} </span>
          <span>{error}</span>
        </span>
      }

    </div>
  )
}

export default Input