import s from "./ToggleSwitch.module.css"

/**
 * Represents a single choice option in the toggle switch.
 */
export type Option<T extends string> = {
  /** The text displayed to the user */
  label: string;
  /** The value stored in state */
  value: T
}

interface ToggleSwitchProps<T extends string> {
  /** Group name for the radio inputs (required for accessibility/WCAG) */
  name: string;
  /** List of selectable options */
  options: Option<T>[];
  /** Currently active value */
  selectedValue: T;
  /** Callback fired when a new option is selected */
  onChange: (value: T) => void
}

/**
 * Accessible, modular Segmented Control (Toggle Switch) built using semantic HTML radio buttons.
 *
 * @example
 * ```tsx
 * import ToggleSwitch, { Option } from "./ToggleSwitch";
 * 
 * // 1. Define your backend/domain union type
 * type UserRole = "COMPANY" | "AGENT";
 * 
 * // 2. Type your options array using the union type
 * const options: Option<UserRole>[] = [
 *   { label: "Företag", value: "COMPANY" },
 *   { label: "Handläggare", value: "AGENT" },
 * ];
 * 
 * // 3. Use the component passing the generic type parameter
 * <ToggleSwitch<UserRole>
 *   name="userRole"
 *   options={options}
 *   selectedValue={role}
 *   onChange={(newRole) => setRole(newRole)}
 * />
 * ```
 */
const ToggleSwitch = <T extends string>({name, options, selectedValue, onChange}: ToggleSwitchProps<T>) => {
  return(
    <div role="radiogroup" className={s.wrapper}>
      {options.map(({ label, value }) => {
        const isSelected = value === selectedValue;

        return (
          <label 
            key={value} 
            className={`${s.segment} ${isSelected ? s.active : ""}`}
          >
            <input
              type="radio"
              name={name}
              value={value}
              checked={isSelected}
              onChange={() => onChange(value)}
              className={s.hiddenInput}
            />
            <span>{label}</span>
          </label>
        )
      })}
    </div>
  )
}

export default ToggleSwitch