import type { ButtonHTMLAttributes } from "react"
import s from "./Button.module.css"

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "ghost";
  className?: string
}

const Button = ({variant="primary", className, children, ...props}: ButtonProps) => {
  const combinedClassName = [
    s.base,
    s[variant],
    className
  ].filter(Boolean).join(" ")

  return(
    <button
      className={combinedClassName}
      {...props}
    >
      {children}
    </button>
  )
}

export default Button