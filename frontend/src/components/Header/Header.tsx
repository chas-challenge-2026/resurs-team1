import { useState, type ReactNode } from "react"
import { getUser, getUserDisplayName } from "../../utils/auth"
import { FiMenu, FiX } from "react-icons/fi"
import Button from "../Button/Button"
import logo from "../../assets/branding/resurs-wordmark.png"
import s from "./Header.module.css"
import { useLogout } from "../../hooks/useLogout"

interface HeaderProps {
  /** Stays in the bar on mobile instead of collapsing into the menu. */
  search?: ReactNode;
  children?: ReactNode;
}

const Header = ({ search, children }: HeaderProps) => {
  const [open, setOpen] = useState(false)

  const user = getUser()
  const { logout } = useLogout()

  // agents get the role badge, companies are identified by their own name
  const roleLabel = user?.role === "caseWorker" ? "Handläggare" : null

  // the whole user cluster is meaningless before the profile resolves
  // no wrapper of its own, the bar and the dropdown lay it out differently
  const userBlock = user && (
    <>
      <p className={s.user}>
        <span>Inloggad som</span>
        <strong>{getUserDisplayName(user)}</strong>
      </p>
      <span className={s.divider} />
      <Button variant="ghost" onClick={logout}>Logga ut</Button>
    </>
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
              {/* own class so it can leave with the badge at the breakpoint */}
              <span className={`${s.divider} ${s.roleDivider}`} />
              <span className={s.role}>{roleLabel}</span>
            </>
          )}

          {/* not inside .nav, it has to survive the mobile breakpoint */}
          {search && <div className={s.search}>{search}</div>}

          {children && <nav className={s.nav}>{children}</nav>}

          {/* displayed on desktop */}
          {userBlock && <div className={s.right}>{userBlock}</div>}

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
          {/* hidden in css if desktop size */}
          <div className={s.menu}>
            {children && <nav className={s.menuNav}>{children}</nav>}
            {roleLabel && <span className={s.menuRole}>{roleLabel}</span>}
            {userBlock}
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
