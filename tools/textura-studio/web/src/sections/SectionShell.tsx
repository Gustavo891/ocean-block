import type { ReactNode } from 'react'
import { Brand } from '../Brand'
import { Library, sectionLabel, type SectionId } from './Library'

interface SectionShellProps {
  section: SectionId
  subtitle?: ReactNode
  dirty?: boolean
  actions?: ReactNode
  children: ReactNode
}

/** Topo + biblioteca lateral comuns às seções que editam arquivos do pack. */
export function SectionShell({ section, subtitle, dirty, actions, children }: SectionShellProps) {
  return (
    <div className="shell">
      <header className="topbar">
        <Brand />
        <span className="top-sep" />
        <div className="doc-title">
          <b>{sectionLabel(section)}</b>
          <span>
            {subtitle}
            {dirty && <i className="dirty" title="Alterações não salvas" />}
          </span>
        </div>
        <div className="top-actions">{actions}</div>
      </header>
      <div className="workspace">
        <Library active={section} />
        {children}
      </div>
    </div>
  )
}
