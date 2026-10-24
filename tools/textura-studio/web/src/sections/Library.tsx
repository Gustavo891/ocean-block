import { Bell, Box, Palette, PanelTop, ScrollText, Sparkles, type LucideIcon } from 'lucide-react'

export type SectionId = 'dialogs' | 'notificacoes' | 'hud' | 'glyphs' | 'temas' | 'blocos'

const SECTIONS: { id: SectionId; icon: LucideIcon; label: string; hash: string }[] = [
  { id: 'dialogs', icon: ScrollText, label: 'Dialogs', hash: '#/' },
  { id: 'notificacoes', icon: Bell, label: 'Notificações', hash: '#/notificacoes' },
  { id: 'hud', icon: PanelTop, label: 'HUD bossbar', hash: '#/hud' },
  { id: 'glyphs', icon: Sparkles, label: 'Glyphs / ícones', hash: '#/glyphs' },
  { id: 'temas', icon: Palette, label: 'Temas', hash: '#/temas' },
  { id: 'blocos', icon: Box, label: 'Blocos e itens', hash: '#/blocos' },
]

export function sectionLabel(id: SectionId): string {
  return SECTIONS.find((section) => section.id === id)?.label ?? id
}

/** Navegação entre as seções do Studio. */
export function Library({ active }: { active: SectionId }) {
  return (
    <nav className="library">
      <span className="eyebrow">Biblioteca</span>
      {SECTIONS.map(({ id, icon: Icon, label, hash }) => (
        <a key={id} className={`lib-item${id === active ? ' on' : ''}`} href={hash}>
          <Icon /> {label}
        </a>
      ))}
    </nav>
  )
}
