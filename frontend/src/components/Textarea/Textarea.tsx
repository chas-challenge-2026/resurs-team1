import type { TextareaHTMLAttributes } from "react"
import InputError from "../InputError/InputError";
import s from "./Textarea.module.css"

/**
 * Props for the Textarea component.
 */
export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  /** Unique ID linking the label, error, and helper text to the textarea for accessibility (WCAG). */
  id: string;
  /** Text label for the textarea. Always visually hidden, but required for screen readers and accessibility. */
  label: string;
  /** Error message string. Triggers error state (`aria-invalid="true"`) on the textarea. */
  error?: string;
  /** Helper text rendered beneath the field (only displayed when no error is present). */
  information?: string;
}

/**
 * Accessible multi-line text input component with a visually hidden label.
 * Handles WCAG accessibility bindings, error states, and helper texts automatically.
 *
 * @example
 * <Textarea id="description" label="Description" placeholder="Enter description..." />
 */
const TextArea = ({id, label, error, information, className, ...props}: TextareaProps) => {

  const combinedClassName = [s.textarea, "input-base", className, error && "error-border"].filter(Boolean).join(" ")

  //Id for screen reader
  const errorId = `${id}-error`
  const infoId = `${id}-info`
  const describedBy = error ? errorId : information ? infoId : undefined

  return(
    <div className={s.wrapper}>
      <label htmlFor={id} className={s.label}>
        {label}
      </label>

      <textarea
        id={id}
        className={combinedClassName}
        aria-invalid={!!error}
        aria-describedby={describedBy}
        {...props}
      />
  
      {information && !error && 
        <span id={infoId} className="information-text">{information}</span>
      }
  
      {error && <InputError errorId={errorId} errorMsg={error} />}
    </div>
  )
}

export default TextArea