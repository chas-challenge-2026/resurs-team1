import { useState, type ReactNode } from "react"
import { FiMenu, FiX } from "react-icons/fi"
import Button from "../Button/Button"
import logo from "../../assets/branding/resurs-wordmark.png"
import { useAuth } from "../../context/AuthContext"
import s from "./Header.module.css"

interface HeaderProps {
  /** Stays in the bar on mobile instead of collapsing into the menu. */
  search?: ReactNode;
  children?: ReactNode;
}

// TODO: connect with router later so it highlights correct button automatically.
const Header = ({ search, children }: HeaderProps) => {
  const [open, setOpen] = useState(false)
  const { user, logout } = useAuth()

  // agents get the role badge, companies are identified by their own name
  const roleLabel = user?.role === "AGENT" ? "Handläggare" : null

  // the whole user cluster is meaningless before the profile resolves
  const userBlock = user && (
    <p className={s.user}>
      <span>Inloggad som</span>
      <strong>{user.name}</strong>
    </p>
  )

  return (
    <>
      <header className={s.header}>
        <div className={s.inner}>
          <a className={s.logo} href="/">
            <img src={logo} alt="Resurs" />
          </a>

          {roleLabel && (
            <>
              <span className={s.divider} />
              <span className={s.role}>{roleLabel}</span>
            </>
          )}

          {/* not inside .nav, it has to survive the mobile breakpoint */}
          {search && <div className={s.search}>{search}</div>}

          {children && <nav className={s.nav}>{children}</nav>}

          {/* using .right styling to cluster togeather */}
          <div className={s.right}>
            {userBlock}
            <span className={s.divider} />
            <Button variant="ghost" onClick={logout}>Logga ut</Button>
          </div>

          <button
            type="button"
            className={s.burger}
            aria-label={open ? "Stäng meny" : "Öppna meny"}
            aria-expanded={open}
            aria-controls="header-menu"
            onClick={() => setOpen(v => !v)}
          >
            {open ? <FiX /> : <FiMenu />}
          </button>
        </div>

        {/* always mounted so it can animate both in and out */}
        <div
          id="header-menu"
          className={`${s.menuWrap} ${open ? s.menuWrapOpen : ""}`}
        >
          {/* click anywhere in the panel closes it, links navigate away anyway */}
          <div className={s.menu}>
            {children && <nav className={s.menuNav}>{children}</nav>}
            {roleLabel && <span className={s.menuRole}>{roleLabel}</span>}
            {userBlock}
            <Button variant="ghost" onClick={logout}>Logga ut</Button>
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
