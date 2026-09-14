import { useState } from "react";
import { formatCurrency } from "../../utils/formatters";
import InputError from "../InputError/InputError";
import s from "./Slider.module.css";

/**
 * Props for the Slider component.
 */
export interface SliderProps {
  /** Unique name attribute for the input element. */
  name: string;
  /** Accessible label displayed above the slider. */
  label: string;
  /** Current selected numerical value. */
  value: number;
  /** Callback triggered when the slider or text input value changes. */
  onChange: (value: number) => void;
  /** Minimum allowed value. */
  min: number;
  /** Maximum allowed value. */
  max: number;
  /** Incremental step for the slider range. Defaults to 10000. */
  step?: number;
  /** Unit suffix appended to the displayed value (e.g., "kr"). Defaults to "kr". */
  unit?: string;
  /** Error message to display below the slider. */
  error?: string;
}


/**
 * Slider – An accessible, dual-input numerical range control.
 *
 * Allows users to select a value either by dragging a slider or by clicking
 * the displayed amount to enter a custom value manually via a text field.
 *
 * @example
 * ```tsx
 * <Slider label="Önskat belopp" max="{5000000}" min="{50000}" name="requestedAmount" onChange="{setAmount}" step="{50000}" unit="kr" value="{amount}"/>
 * ```
 */
const Slider = ({name, label, value, onChange, min, max, step = 10000, unit = "kr", error}: SliderProps) => {
  const [isEditing, setIsEditing] = useState(false)
  const [inputValue, setInputValue] = useState(String(value))

  // Calculate percentage filled for the dynamic CSS gradient track (clamped between 0 and 100%)
  const percentage = Math.min(
    100,
    Math.max(0, ((value - min) / (max - min)) * 100)
  )

  const inputId = `${name}-slider`
  const textInputId = `${name}-input`
  const errorId = `${name}-error`

  // Calculate percentage filled for the dynamic CSS gradient track (clamped between 0 and 100%)
  const trackStyle = {
    background: `linear-gradient(to right, var(--color-primary) 0%, var(--color-primary) ${percentage}%, var(--border) ${percentage}%, var(--border) 100%)`
  }


  // Handles direct text input changes, removing any non-digit characters.
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawVal = e.target.value.replace(/\D/g, "")
    setInputValue(rawVal)
  }


  // Clamps the input value to the min/max range when the text input loses focus.
  const handleBlur = () => {
    setIsEditing(false)
    let clampedVal = Number(inputValue)
    if (isNaN(clampedVal) || clampedVal < min) clampedVal = min
    if (clampedVal > max) clampedVal = max
    
    onChange(clampedVal);
    setInputValue(String(clampedVal))
  }

  return (
    <div className={s.container}>
      {label && (
        <label htmlFor={inputId}>
          {label}
        </label>
      )}

      {/* Interactive value display: toggles between a button and a text input */}
      {isEditing ? (
        <div className={s.inputWrapper}>
          <input
            id={textInputId}
            type="text"
            inputMode="numeric" /* Opens numeric keypad on mobile devices */
            value={inputValue}
            onChange={handleInputChange}
            onBlur={handleBlur}
            autoFocus
            className={s.numberInput}
          />
          <span className={s.unitSuffix}>{unit}</span>
        </div>
      ) : (
        <button
          type="button"
          className={s.valueDisplayButton}
          onClick={() => {
            setInputValue(String(value));
            setIsEditing(true);
          }}
          title="Klicka för att skriva belopp"
        >
          {formatCurrency(value, unit)}
        </button>
      )}

      <input
        id={inputId}
        name={name}
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className={s.rangeInput}
        style={trackStyle}
        aria-valuetext={formatCurrency(value, unit)}
        aria-invalid={!!error}
        aria-describedby={error ? errorId : undefined}
      />

      <div className={s.footerLabels} aria-hidden="true">
        <span>{formatCurrency(min, unit)}</span>
        <span>{formatCurrency(max, unit)}</span>
      </div>

      {error && <InputError errorId={errorId} errorMsg={error} />}
    </div>
  )
}

export default Slider