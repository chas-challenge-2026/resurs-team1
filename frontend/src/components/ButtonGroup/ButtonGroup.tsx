import s from "./ButtonGroup.module.css"
import { RiErrorWarningLine } from "react-icons/ri";

/**
 * Represents an individual selectable option within a ButtonGroup.
 * @template T - The type of the option's value (string or number)
 */
export interface ButtonGroupOption<T extends string | number> {
  /** The human-readable label displayed inside the button (e.g., "36 mån") */
  label: string;
  /** The underlying value passed to forms or the backend (e.g., 36) */
  value: T
}

/**
 * Props for the ButtonGroup component.
 * @template T - The type of the option values (inferred automatically from options)
 */
interface ButtonGroupProps<T extends string | number> {
  /** Unique group name for the underlying radio inputs. Required for accessibility and form association. */
  name: string;
  /** Read-only array containing all selectable options. */
  options: ReadonlyArray<ButtonGroupOption<T>>;
  /** The currently selected value. Can be undefined if no initial selection is made. */
  selectedValue?: T;
  /** Callback fired when a new option is selected */
  onChange: (value: T) => void
  /** Optional visual label/heading displayed above the button group. */
  label?: string;
  /** Error message displayed below the component when form validation fails. */
  error?: string
}

/**
 * ButtonGroup – A semantic and accessible radio group styled as interactive buttons.
 * Used in wizard steps for single-choice selections where users need to make quick decisions.
 *
 * @example
 * ```tsx
 * <ButtonGroup label="Select tenure" name="tenure" onChange="{setTenure}" options="{TENURE_OPTIONS}" selectedValue="{tenure}"/>
 * ```
 */
const ButtonGroup = <T extends string | number>({name, options, selectedValue, onChange, label, error}: ButtonGroupProps<T>) => {
  return(
    <fieldset className={s.fieldset}>
      {label && <legend className={s.label}>{label}</legend>}

      <div className={s.group}>
        {options.map((opt) => {
          const isChecked = opt.value === selectedValue
          const inputId = `${name}-${opt.value}`

          return(
            <label
              key={String(opt.value)}
              htmlFor={inputId}
              className={[s.button, isChecked && s.isSelected, error && s.errorBorder].filter(Boolean).join(" ")}
            >
              <input 
                type="radio"
                id={inputId}
                name={name}
                value={opt.value}
                checked={isChecked}
                onChange={() => onChange(opt.value)}
                className={s.hiddenInput}
              />
              {opt.label}
            </label>
          )
        })}
      </div>
      
      {error && 
        <span className={s.error}>
          <RiErrorWarningLine aria-hidden={true}/>
          <span>{error}</span>
        </span>
      }
    </fieldset>
  )
}

export default ButtonGroup