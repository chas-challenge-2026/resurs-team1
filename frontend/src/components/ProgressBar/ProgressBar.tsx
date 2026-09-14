import s from "./ProgressBar.module.css"

interface ProgressBarProps {
  currentStep: number;
  totalSteps: number
}

/**
 * Visual progress bar for multi-step forms.
 */
const ProgressBar = ({currentStep, totalSteps}: ProgressBarProps) => {
  const percentage = (currentStep / totalSteps) * 100

  return(
    <div className={s.track}>
      <div className={s.fill} style={{width: `${percentage}%`}}></div>
    </div>
  )
}

export default ProgressBar