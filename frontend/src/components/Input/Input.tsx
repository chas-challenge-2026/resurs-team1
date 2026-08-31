import type { InputHTMLAttributes } from "react";
import s from "./Input.module.css"

interface BaseInputProps extends InputHTMLAttributes<HTMLInputElement> {
  error?: string
}

interface StandardInputProps extends BaseInputProps {
  id: string;
  label: string
}

interface SearchInputProps extends BaseInputProps {
  type: "search";
  "aria-label": string;
  id?: never;
  label?: never
}

type InputProps = StandardInputProps | SearchInputProps

const Input = (props: InputProps) => {
  const { type="text", label, error, className, id, ...rest } = props

  const combinedClassName = [s.input, className].filter(Boolean).join(" ")

  return(
    <div className={s.wrapper}>
      {label && (
        <label htmlFor={id} className={s.label}>
          {label}
        </label>
      )}

      <input
        id={id}
        type={type}
        className={combinedClassName}
        {...rest}
      />

      {error && <span className={s.errorMessage}>{error}</span>}

    </div>
  )
}

export default Input