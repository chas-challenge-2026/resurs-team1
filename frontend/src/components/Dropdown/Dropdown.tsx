import { useEffect, useRef, useState } from "react";
import type { KeyboardEvent } from "react";
import { RiArrowDownSLine, RiCheckLine, RiErrorWarningLine } from "react-icons/ri";
import Input from "../Input/Input";
import s from "./Dropdown.module.css";

/**
 * A selectable entry in a `Dropdown`.
 */
export interface DropdownOption {
  /** Value handed to `onChange`. */
  value: string;
  /** Text shown in the list and in the trigger once selected. */
  label: string;
  /** Selecting this option reveals a text field so the user can word it themselves. */
  freeText?: boolean;
}

interface DropdownBaseProps {
  /** Unique identifier linking the label, listbox and messages to the trigger for accessibility (WCAG) */
  id: string;
  /** Text label for the field. Required for screen readers and accessibility */
  label: string;
  /** Selectable options, in the order they should appear. ex: 
* options={[
 *     { value: "renovering", label: "Renovering" },
 *     { value: "bilkop", label: "Bilköp" },
 *     { value: "ovrigt", label: "Övrigt", freeText: true },
 *   ]}
   */
  options: DropdownOption[];
  /** Shown in the trigger while nothing is selected */
  placeholder?: string;
  /** Visually hides the label while keeping it accessible to screen readers */
  hideLabel?: boolean;
  /** Error message from the form. Overrides the built-in empty-field error */
  error?: string;
  /** Helper text rendered beneath the field (only displayed when no error is present) */
  information?: string;
  /** Marks the field as missing while nothing is selected. Set to `false` to stay quiet until the form validates */
  requiredError?: boolean;
  /** Message for the built-in empty-field error */
  requiredMessage?: string;
  /** Text the user wrote for the option marked `freeText` */
  freeTextValue?: string;
  /** Omit to leave the text field out entirely, even when a `freeText` option is selected */
  onFreeTextChange?: (value: string) => void;
  disabled?: boolean;
}

interface MultipleProps {
  /** Several options at a time. This is the default */
  multiple?: true;
  value: string[];
  onChange: (value: string[]) => void;
}

interface SingleProps {
  /** One option at a time. The list closes on pick */
  multiple: false;
  value: string | null;
  onChange: (value: string) => void;
}

// combinging them 
type DropdownProps = DropdownBaseProps & (MultipleProps | SingleProps);

/**
 * Accessible dropdown built on the combobox/listbox pattern.
 * Multi-select by default; pass `multiple={false}` for a single pick.
 *
 * Focus stays on the trigger and the active row is announced through
 * `aria-activedescendant`, which keeps every key in one handler.
 *
 * @example
 * // Several purposes, with a text field behind the option marked freeText
 * const [purposes, setPurposes] = useState<string[]>([]);
 * const [ownWording, setOwnWording] = useState("");
 *
 * <Dropdown
 *   id="purpose"
 *   label="Ändamål - Vad ska lånet användas till?"
 *   placeholder="Välj ändamål..."
 *   options={[
 *     { value: "renovering", label: "Renovering" },
 *     { value: "bilkop", label: "Bilköp" },
 *     { value: "ovrigt", label: "Övrigt", freeText: true },
 *   ]}
 *   value={purposes}
 *   onChange={setPurposes}
 *   freeTextValue={ownWording}
 *   onFreeTextChange={setOwnWording}
 * />
 *
 * @example
 * // One pick, no red field before the form has been submitted
 * const [term, setTerm] = useState<string | null>(null);
 *
 * <Dropdown
 *   multiple={false}
 *   id="term"
 *   label="Återbetalningstid"
 *   options={[
 *     { value: "24", label: "2 år" },
 *     { value: "60", label: "5 år" },
 *   ]}
 *   value={term}
 *   onChange={setTerm}
 *   requiredError={false}
 * />
 */
const Dropdown = (props: DropdownProps) => {
  const {
    id,
    label,
    options,
    placeholder = "Välj...",
    hideLabel,
    error,
    information,
    requiredError = true,
    requiredMessage = "Välj ett alternativ",
    freeTextValue = "",
    onFreeTextChange,
    disabled,
  } = props;

  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  // the keyboard position is only painted once a key has been used, otherwise
  // the first row looks hovered the moment the list opens
  const [keyboardNav, setKeyboardNav] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  const selectedValues =
    props.multiple === false ? (props.value ? [props.value] : []) : props.value;

  const shownError =
    error ?? (requiredError && selectedValues.length === 0 ? requiredMessage : undefined);

  const listId = `${id}-listbox`;
  const errorId = shownError ? `${id}-error` : undefined;
  const infoId = information ? `${id}-info` : undefined;

  const freeTextOption = options.find(
    (option) => option.freeText && selectedValues.includes(option.value)
  );

  // a click anywhere else closes the list
  useEffect(() => {
    if (!open) return;
    const close = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("pointerdown", close);
    return () => document.removeEventListener("pointerdown", close);
  }, [open]);

  // keep the active row inside the scroll area
  useEffect(() => {
    if (!open) return;
    const active = listRef.current?.children[activeIndex] as HTMLElement | undefined;
    active?.scrollIntoView({ block: "nearest" });
  }, [open, activeIndex]);

  const openList = () => {
    const first = options.findIndex((option) => selectedValues.includes(option.value));
    setActiveIndex(first === -1 ? 0 : first);
    setOpen(true);
  };

  const commit = (index: number) => {
    const picked = options[index].value;

    if (props.multiple === false) {
      props.onChange(picked);
      setOpen(false);
      return;
    }

    props.onChange(
      props.value.includes(picked)
        ? props.value.filter((value) => value !== picked)
        : [...props.value, picked]
    );
  };

  const onKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
    const last = options.length - 1;
    setKeyboardNav(true);

    switch (event.key) {
      case "ArrowDown":
        event.preventDefault();
        if (open) setActiveIndex(activeIndex === last ? 0 : activeIndex + 1);
        else openList();
        break;
      case "ArrowUp":
        event.preventDefault();
        if (open) setActiveIndex(activeIndex === 0 ? last : activeIndex - 1);
        else openList();
        break;
      case "Home":
        if (open) {
          event.preventDefault();
          setActiveIndex(0);
        }
        break;
      case "End":
        if (open) {
          event.preventDefault();
          setActiveIndex(last);
        }
        break;
      case "Enter":
      case " ":
        event.preventDefault();
        if (open) commit(activeIndex);
        else openList();
        break;
      case "Escape":
      case "Tab":
        setOpen(false);
        break;
    }
  };

  const selectedOptions = options.filter((option) => selectedValues.includes(option.value));

  return (
    <div className={s.dropdown} ref={rootRef} data-open={open}>
      <label htmlFor={id} className={hideLabel ? s.hidden : undefined}>
        {label}
      </label>

      <button
        type="button"
        id={id}
        role="combobox"
        className={[s.trigger, shownError && s.errorBorder].filter(Boolean).join(" ")}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        aria-activedescendant={open ? `${id}-option-${activeIndex}` : undefined}
        aria-invalid={!!shownError}
        aria-describedby={errorId ?? infoId}
        disabled={disabled}
        // pointerdown, not click: Enter and Space fire a click too
        onPointerDown={() => setKeyboardNav(false)}
        onClick={() => (open ? setOpen(false) : openList())}
        onKeyDown={onKeyDown}
      >
        <span className={selectedOptions.length ? s.value : s.placeholder}>
          {selectedOptions[0]?.label ?? placeholder}
        </span>

        {selectedOptions.length > 1 && (
          <span className={s.count}>+{selectedOptions.length - 1}</span>
        )}

        <RiArrowDownSLine className={s.chevron} aria-hidden={true} />
      </button>

      <ul
        id={listId}
        ref={listRef}
        role="listbox"
        aria-label={label}
        aria-multiselectable={props.multiple !== false}
        className={s.menu}
        hidden={!open}
      >
        {options.map((option, index) => {
          const isSelected = selectedValues.includes(option.value);

          return (
            <li
              key={option.value}
              id={`${id}-option-${index}`}
              role="option"
              aria-selected={isSelected}
              className={s.option}
              data-active={keyboardNav && index === activeIndex}
              onMouseEnter={() => {
                setKeyboardNav(false);
                setActiveIndex(index);
              }}
              onClick={() => commit(index)}
            >
              {props.multiple !== false && (
                <span className={s.box} aria-hidden={true}>{isSelected && <RiCheckLine />}</span>
              )}

              <span>{option.label}</span>

              {props.multiple === false && isSelected && (
                <RiCheckLine className={s.check} aria-hidden={true} />
              )}
            </li>
          );
        })}
      </ul>

      {information && !shownError && (
        <span id={infoId} className={s.information}>
          {information}
        </span>
      )}

      {shownError && (
        <span id={errorId} className={s.error}>
          <RiErrorWarningLine aria-hidden={true} />
          <span>{shownError}</span>
        </span>
      )}

      {freeTextOption && onFreeTextChange && (
        <Input
          id={`${id}-free-text`}
          label={`${freeTextOption.label} - beskriv med egna ord`}
          hideLabel
          placeholder="Beskriv med egna ord"
          value={freeTextValue}
          onChange={(event) => onFreeTextChange(event.target.value)}
        />
      )}
    </div>
  );
};

export default Dropdown;
