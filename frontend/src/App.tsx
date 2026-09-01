import Button from "./components/Button/Button"
import Header from "./components/Header/Header"
import Loading from "./components/Loading/Loading"

function App() {
  return (
    <>
    <Header company="Coconut AB" onLogout={() => {}}>
      <a href="/" aria-current="page">Översikt</a>
      <a href="/ansokningar">Ansökningar</a>
      <a href="/dokument">Dokument</a>
    </Header>
    <main>
      <h1 className="title">Titel</h1>
      <p className="subtitle">Undertitel</p>
      <Loading size="lg"/>
      <Button variant="ghost">
        test knapp
      </Button>
    </main>
    </>
  )
}

export default App
