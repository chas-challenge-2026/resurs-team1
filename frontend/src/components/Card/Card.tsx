import type { HTMLAttributes, CSSProperties } from "react";
import s from "./Card.module.css"

/**
 * Props for Card component.
 */
interface CardProps extends HTMLAttributes<HTMLElement> {
  /** The content to display inside the card (e.g. CardHeader, CardBody, CardFooter). */
  children: React.ReactNode;
  /** Visual style variant of the card. */
  variant?: "default" | "accent" | "warning";
  /** Custom accent color for top line when variant="accent" (e.g. "var(--color-primary)"). */
  accentColor?: string;
  /** Semantic HTML element to render. Use 'article' or 'section' for standalone content. */
  as?: "div" | "article" | "section"
}

/**
 * Flexible container component for grouping related content using compound components.
 *
 * @example
 * <Card as="article" variant="accent" accentColor="#1d2a44">
 *   <CardHeader>
 *     <h2>Card Title</h2>
 *   </CardHeader>
 *   <CardBody>
 *     <p>Card body content...</p>
 *   </CardBody>
 *   <CardFooter>
 *     <a href="/details" className={s.stretchedLink}>Card Action</a>
 *   </CardFooter>
 * </Card>
 */
const Card = ({children, variant = "default", accentColor, as: Component = "div", className, style, ...props}: CardProps) => {

  const combinedClassName = [ s.card, s[variant], className].filter(Boolean).join(" ");

  //Add variable for accent color
  const combinedStyle: CSSProperties = {
    ...style,
    ...(accentColor ? { ["--card-accent-color" as string]: accentColor } : {}),
  };

  return(
    <Component className={combinedClassName} style={combinedStyle} {...props}>
      {children}
    </Component>
  )
}

/**
 * Header section of the Card. Renders a semantic <header> tag.
 */
const CardHeader = ({className, children, ...props}: HTMLAttributes<HTMLElement>) => (
  <header className={[s.header, className].filter(Boolean).join(" ")} {...props}>{children}</header>
)

/**
 * Main content container for the Card.
 */
const CardBody = ({className, children, ...props}: HTMLAttributes<HTMLDivElement>) => (
  <div className={[s.body, className].filter(Boolean).join(" ")} {...props}>{children}</div>
)

/**
 * Footer section of the Card. Renders a semantic <footer> tag.
 * 
 * @example
 * // To make the entire card clickable using the Stretched Link pattern:
 * import s from "./Card.module.css";
 * 
 * <CardFooter>
 *   <a href="/ansokan/ny" className={s.stretchedLink}>
 *     Card Action
 *   </a>
 * </CardFooter>
 */
const CardFooter = ({className, children, ...props}: HTMLAttributes<HTMLElement>) => (
  <footer className={[s.footer, className].filter(Boolean).join(" ")} {...props}>{children}</footer>
)

export {Card, CardHeader, CardBody, CardFooter}