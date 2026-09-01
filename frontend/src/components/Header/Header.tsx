import type { ReactNode } from "react"
import Button from "../Button/Button"
import wordmark from "../../assets/branding/resurs-wordmark.png"
import s from "./Header.module.css"

interface HeaderProps {
  company: string;
  onLogout: () => void;
  children?: ReactNode;
}

// TODO: connect with router later so it highlights correct button automatically.
const Header = ({ company, onLogout, children }: HeaderProps) => (
  <header className={s.header}>
    <div className={s.inner}>
      <a className={s.logo} href="/">
        <img src={wordmark} alt="Resurs" />
      </a>

      {children && <nav className={s.nav}>{children}</nav>}

      {/* using .right styling to cluster togeather */}
      <div className={s.right}> 
        <p className={s.user}>
          <span>Inloggad som</span>
          <strong>{company}</strong>
        </p>
        <span className={s.divider} />
        <Button variant="ghost" onClick={onLogout}>Logga ut</Button>
      </div>
    </div>
  </header>
)

export default Header
