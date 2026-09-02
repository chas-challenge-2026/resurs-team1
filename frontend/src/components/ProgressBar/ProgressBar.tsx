import s from "./ProgressBar.module.css"

interface ProgressBarProps {
  currentStep: number;
  totalSteps: number
}

/**
 * Visual progress bar for multi-step forms.
 * Hidden from screen readers to avoid redundant announcements 
 * when step info is already conveyed via surrounding headings/text.
 */
const ProgressBar = ({currentStep, totalSteps}: ProgressBarProps) => {
  const percentage = (currentStep / totalSteps) * 100

  return(
    <div className={s.track} aria-hidden={true}>
      <div className={s.fill} style={{width: `${percentage}%`}}></div>
    </div>
  )
}

export default ProgressBar