import type { ButtonHTMLAttributes, CSSProperties } from "react"
import s from "./Button.module.css"

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "ghost";
  color?: string
}

const Button = ({variant="primary", color, className, children, style, ...props}: ButtonProps) => {
  const combinedClassName = [
    s.base,
    s[variant],
    className
  ].filter(Boolean).join(" ")

  const combinedStyle: CSSProperties = {
    ...style,
    ...(color ? { ["--accent-color" as string]: color } : {}),
  }

  return(
    <button
      className={combinedClassName}
      style={combinedStyle}
      {...props}
    >
      {children}
    </button>
  )
}

export default Button