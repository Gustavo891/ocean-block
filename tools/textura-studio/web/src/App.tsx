import { useEffect, useState } from 'react'
import { Home } from './home/Home'
import { Editor } from './editor/Editor'
import { GlyphsPage } from './sections/glyphs/GlyphsPage'
import { NotifyPage } from './sections/notify/NotifyPage'
import { ThemesPage } from './sections/themes/ThemesPage'

type Route = { name: 'home' } | { name: 'notificacoes' } | { name: 'hud' } | { name: 'glyphs' } | { name: 'temas' } | { name: 'dialog'; id: string; fresh: boolean; from: string | null }

function parse(hash: string): Route {
  const section = /^#\/(notificacoes|hud|glyphs|temas)$/.exec(hash)?.[1]
  if (section) return { name: section as 'notificacoes' | 'hud' | 'glyphs' | 'temas' }
  const match = /^#\/dialog\/([a-z0-9_-]+)(\?novo(?:&de=([a-z0-9_-]+))?)?$/.exec(hash)
  return match ? { name: 'dialog', id: match[1], fresh: Boolean(match[2]), from: match[3] ?? null } : { name: 'home' }
}


export function App() {
  const [route, setRoute] = useState<Route>(() => parse(window.location.hash))

  useEffect(() => {
    const onHash = () => setRoute(parse(window.location.hash))
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  }, [])

  if (route.name === 'dialog') return <Editor key={route.id} id={route.id} fresh={route.fresh} from={route.from} />
  if (route.name === 'notificacoes') return <NotifyPage key="notify" mode="notify" />
  if (route.name === 'hud') return <NotifyPage key="hud" mode="hud" />
  if (route.name === 'glyphs') return <GlyphsPage />
  if (route.name === 'temas') return <ThemesPage />
  return <Home />
}
