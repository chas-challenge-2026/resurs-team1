import styles from "./Loading.module.css";

type LoadingSize = "sm" | "md" | "lg";

type LoadingProps = {
  /** sm = 20px, md = 40px, lg = 64px. */
  size?: LoadingSize;
  /** Screen reader text. Shown visually only when reduced motion is on. */
  label?: string;
  /** Centered overlay covering the viewport. */
  fullscreen?: boolean;
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
  delay = true,
}: LoadingProps) {
  const content = (
    <span
    className={delay ? `${styles.wrapper} ${styles.delayed}` : styles.wrapper}
    role="status"
    >
      <svg
        className={styles.arc}
        style={{ width: SIZES[size], height: SIZES[size] }}
        viewBox="0 0 50 50"
        aria-hidden="true"
        >
        <circle className={styles.arcTrack} cx="25" cy="25" r="20" /> 
        <circle className={styles.arcPath} cx="25" cy="25" r="20" />
      </svg>
      <span className={styles.label}>{label}</span>
    </span>
  );
  
  if (!fullscreen) return content;
  
  // the fullscreen stylinjg
  return <div className={styles.overlay}>{content}</div>;
}

export default Loading;
