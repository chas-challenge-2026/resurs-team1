import { useState, type ReactNode } from "react"
import Button from "../Button/Button"
import wordmark from "../../assets/branding/resurs-wordmark.png"
import mark from "../../assets/branding/resurs-logo.png"
import s from "./Header.module.css"

interface HeaderProps {
  company: string;
  onLogout: () => void;
  children?: ReactNode;
}

// TODO: connect with router later so it highlights correct button automatically.
const Header = ({ company, onLogout, children }: HeaderProps) => {
  const [open, setOpen] = useState(false)

  return (
    <>
      <header className={s.header}>
        <div className={s.inner}>
          <a className={s.logo} href="/">
            {/* css picks one, only ever one is visible */}
            <img className={s.logoWordmark} src={wordmark} alt="Resurs" />
            <img className={s.logoMark} src={mark} alt="Resurs" />
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

          <button
            type="button"
            className={s.burger}
            aria-label={open ? "Stäng meny" : "Öppna meny"}
            aria-expanded={open}
            aria-controls="header-menu"
            onClick={() => setOpen(v => !v)}
          >
            <span className={s.burgerBar} />
            <span className={s.burgerBar} />
            <span className={s.burgerBar} />
          </button>
        </div>

        {/* always mounted so it can animate both in and out */}
        <div
          id="header-menu"
          className={`${s.menuWrap} ${open ? s.menuWrapOpen : ""}`}
        >
          {/* click anywhere in the panel closes it, links navigate away anyway */}
          <div className={s.menu} onClick={() => setOpen(false)}>
            {children && <nav className={s.menuNav}>{children}</nav>}
            <Button variant="ghost" onClick={onLogout}>Logga ut</Button>
          </div>
        </div>
      </header>

      {/* sibling of header so it dims the page but not the header itself */}
      <div
        className={`${s.backdrop} ${open ? s.backdropOpen : ""}`}
        onClick={() => setOpen(false)}
      />
    </>
  )
}

export default Header
