
interface DropdownOption {
  value: string;
  label: string;
}

interface DropdownProps {
  id: string;
  label: string;
  options: DropdownOption[];
}

function Dropdown({ id, label, options }: DropdownProps) {
  return (
    <div>
      <label htmlFor={id}>{label}</label>

      <select id={id}>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
}

export default Dropdown;