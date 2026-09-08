import { RiErrorWarningLine } from "react-icons/ri";
import s from "./Dropdown.module.css";

export interface DropdownOption {
  value: string;
  label: string;
}

export interface DropdownProps {
  id: string;
  label: string;
  options: DropdownOption[];
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  hideLabel?: boolean;
  error?: string;
  information?: string;
  disabled?: boolean;
  size?: "sm" | "md";
}

const Dropdown = ({ id, label, options, value, onChange, placeholder = "Välj...", hideLabel, error, information, disabled, size = "md" }: DropdownProps) => {

  const combinedClassName = [s.select, s[size], "input-base", error && s.errorBorder].filter(Boolean).join(" ")

  const errorId = error ? `${id}-error` : undefined
  const infoId = information ? `${id}-info` : undefined
  const describedBy = errorId || infoId

  return (
    <div className={s.wrapper}>
      <label htmlFor={id} className={hideLabel ? s.hidden : undefined}>
        {label}
      </label>

      <select
        id={id}
        className={combinedClassName}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={!!error}
        aria-describedby={describedBy}
        disabled={disabled}
      >
        <option value="" disabled>
          {placeholder}
        </option>

        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      {information && !error &&
        <span id={infoId} className={s.information}>{information}</span>
      }

      {error &&
        <span id={errorId} className={s.error}>
          <RiErrorWarningLine aria-hidden={true}/>
          <span>{error}</span>
        </span>
      }

    </div>
  )
}

export default Dropdown
