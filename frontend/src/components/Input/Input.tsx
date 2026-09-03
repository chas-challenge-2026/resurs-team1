import type { InputHTMLAttributes } from "react";
import { RiErrorWarningLine } from "react-icons/ri";
import s from "./Input.module.css"

/**
 * Props for Input component.
 */
interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "size"> {
  /** Unique identifier linking the label, error message, and helper text to the input for accessibility (WCAG) */
  id: string;
  /** Text label for the input. Required for screen readers and accessibility */
  label: string;
  /** Visually hides the label while keeping it accessible to screen readers */
  hideLabel?: boolean;
  /** Error message string. Triggers error state (`aria-invalid="true"`) on the input */
  error?: string;
  /** Helper text rendered beneath the field (only displayed when no error is present) */
  information?: string;
  /** Visual scale of the input element. Defaults to 'md' */
  size?: "sm" | "md";
}

/**
 * Accessible and reusable Input component.
 * Handles labels, error states, helper texts, and WCAG accessibility bindings automatically.
 *
 * @example
 * // Standard input with label
 * <Input id="email" label="Email address" error={errors.email} />
 *
 * @example
 * // Search input with visually hidden label
 * <Input id="search" type="search" label="Search archive" hideLabel placeholder="Search..." />
 */
const Input = ({id, label, hideLabel, error, information, size = "md", className, ...props}: InputProps) => {

  const combinedClassName = [s.input, s[size], "input-base", className, error && s.errorBorder].filter(Boolean).join(" ")

  //Id for screen reader
  const errorId = id && error ? `${id}-error` : undefined
  const infoId = id && information ? `${id}-info` : undefined
  const describedBy = errorId || infoId

  return(
    <div className={s.wrapper}>
      <label htmlFor={id} className={hideLabel ? s.hidden : undefined}>
        {label}
      </label>

      <input
        id={id}
        className={combinedClassName}
        aria-invalid={!!error}
        aria-describedby={describedBy}
        {...props}
      />

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

export default Input