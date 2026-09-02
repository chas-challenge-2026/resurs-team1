import Button from "./components/Button/Button"
import Header from "./components/Header/Header"
import Loading from "./components/Loading/Loading"
import StatusTag from "./components/StatusTag/StatusTag"

function App() {
  return (
    <>
    <Header company="Coconut AB" onLogout={() => {}}>
      <input type="search" />
    </Header>
    <main>
      <h1 className="title">Titel</h1>
      <p className="subtitle">Undertitel</p>
      <Loading size="lg"/>
      <Button variant="ghost">
        test knapp
      </Button>
      <StatusTag status="approved"/>
    </main>
    </>
  )
}

export default App
