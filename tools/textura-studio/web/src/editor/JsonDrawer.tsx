import CodeMirror from '@uiw/react-codemirror'
import { json } from '@codemirror/lang-json'
import { X } from 'lucide-react'

const EXTENSIONS = [json()]

interface JsonDrawerProps {
  value: string
  error: string | null
  onChange: (value: string) => void
  onClose: () => void
}

export function JsonDrawer({ value, error, onChange, onClose }: JsonDrawerProps) {
  return (
    <aside className="json-drawer">
      <div className="panel-head">
        <span className="eyebrow">JSON da tela</span>
        <span className="spacer" style={{ flex: 1 }} />
        <button className="icon-btn" onClick={onClose} title="Fechar JSON">
          <X />
        </button>
      </div>
      <CodeMirror
        value={value}
        theme="dark"
        extensions={EXTENSIONS}
        onChange={onChange}
        basicSetup={{ foldGutter: true, highlightActiveLine: true, tabSize: 2 }}
        height="100%"
        style={{ flex: 1, minHeight: 0, overflow: 'hidden' }}
      />
      {error && <div className="json-error">{error}</div>}
    </aside>
  )
}
