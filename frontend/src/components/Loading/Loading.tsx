import { Card } from "../Card/Card";
import s from "./Loading.module.css";

type LoadingSize = "sm" | "md" | "lg";

type LoadingProps = {
  /** sm = 20px, md = 40px, lg = 64px. */
  size?: LoadingSize;
  /** Screen reader text. Shown visually only when reduced motion is on. */
  label?: string;
  /** Centered overlay covering the viewport. */
  fullscreen?: boolean;
  /** Center loading icon on page but don't cover viewport */
  centerOnPage?: boolean
  /** Stay invisible for 300ms so a fast response never flashes a spinner. */
  delay?: boolean;
};

const SIZES: Record<LoadingSize, string> = {
  sm: "20px",
  md: "40px",
  lg: "64px",
};

function Loading({
  size = "md",
  label = "Laddar...",
  fullscreen,
  centerOnPage,
  delay = true,
}: LoadingProps) {
  const content = (
    <span
    className={`${s.wrapper} ${delay && s.delayed} ${centerOnPage && s.centerWrapper}`}
    role="status"
    >
      <svg
        className={s.arc}
        style={{ width: SIZES[size], height: SIZES[size] }}
        viewBox="0 0 50 50"
        aria-hidden="true"
        >
        <circle className={s.arcTrack} cx="25" cy="25" r="20" /> 
        <circle className={s.arcPath} cx="25" cy="25" r="20" />
      </svg>
      {!fullscreen && <span className={s.label}>{label}</span>}
    </span>
  );
  
  if (!fullscreen) return content;
  
  // the fullscreen styling
  return (
    <div className={s.overlay}>
      <Card className={s.fullscreen}>
        {content}
        <div>
          <p className={s.title}>{label}</p>
          <p className={s.subtitle}>Det tar bara en liten stund. Stäng inte fönstret.</p>
        </div>
      </Card>
    </div>
  )
}

export default Loading;
