import type { InputHTMLAttributes, ReactNode } from "react";
import InputError from "../InputError/InputError";
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
  /** Decorative icon rendered inside the field, before the text. Hidden from screen readers, the label still carries the meaning */
  icon?: ReactNode;
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
 *
 * @example
 * // Input with a leading icon
 * <Input id="search" label="Search" icon={<FiSearch />} />
 */
const Input = ({id, label, hideLabel, error, information, size = "md", icon, className, ...props}: InputProps) => {

  const combinedClassName = [s.input, `input-${size}`, "input-base", className, error && "error-border"].filter(Boolean).join(" ")

  //Id for screen reader
  const errorId = `${id}-error`
  const infoId = `${id}-info`
  const describedBy = error ? errorId : information ? infoId : undefined

  const inputElement = (
    <input
      id={id}
      className={combinedClassName}
      aria-invalid={!!error}
      aria-describedby={describedBy}
      {...props}
    />
  )

  return(
    <div className={s.wrapper}>
      <label htmlFor={id} className={hideLabel ? s.hidden : undefined}>
        {label}
      </label>

      {/* the wrapper only exists to anchor the icon, so plain fields keep their markup */}
      {icon ? (
        <div className={s.field}>
          <span className={s.icon} aria-hidden={true}>{icon}</span>
          {inputElement}
        </div>
      ) : (
        inputElement
      )}

      {information && !error && 
        <span id={infoId} className="information-text">{information}</span>
      }

      {error && <InputError errorId={errorId} errorMsg={error} />}

    </div>
  )
}

export default Input