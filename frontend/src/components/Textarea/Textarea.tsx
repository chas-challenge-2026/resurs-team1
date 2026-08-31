import type { TextareaHTMLAttributes } from "react"
import s from "./Textarea.module.css"

const TextArea = ({className, ...props}: TextareaHTMLAttributes<HTMLTextAreaElement>) => {

  const combinedClassName = [s.textarea, "input-base", className].filter(Boolean).join(" ")

  return(
    <textarea
      className={combinedClassName}
      {...props}
    />
  )
}

export default TextArea