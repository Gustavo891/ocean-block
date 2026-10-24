import { useEffect, useMemo, useState } from 'react'
import { RefreshCw, Search } from 'lucide-react'
import { filesApi, type BlockEntry, type TextureFace } from '../files'
import { SectionShell } from '../SectionShell'
import { PixelEditor } from './PixelEditor'

const FACE_LABELS: Record<string, string> = {
  all: 'Todas as faces',
  top: 'Topo',
  bottom: 'Base',
  side: 'Laterais',
  item: 'Item',
}

function faceLabel(face: string): string {
  return FACE_LABELS[face] ?? face
}

function faceFor(entry: BlockEntry, wanted: string[]): TextureFace | undefined {
  return wanted.map((name) => entry.faces.find((face) => face.face === name)).find(Boolean) ?? entry.faces[0]
}

function message(cause: unknown): string {
  return cause instanceof Error ? cause.message : 'Falha inesperada'
}

function CubePreview({ entry, version }: { entry: BlockEntry; version: number }) {
  if (entry.kind === 'item') {
    const face = entry.faces[0]
    return <img className="item-preview" src={filesApi.rawUrl(face.path, version)} alt={entry.name} />
  }
  const top = faceFor(entry, ['top', 'all'])
  const side = faceFor(entry, ['side', 'all'])
  const url = (face?: TextureFace) => (face ? `url(${filesApi.rawUrl(face.path, version)})` : 'none')
  return (
    <div className="cube-scene">
      <div className="cube">
        <span className="cube-face top" style={{ backgroundImage: url(top) }} />
        <span className="cube-face front" style={{ backgroundImage: url(side) }} />
        <span className="cube-face right" style={{ backgroundImage: url(side) }} />
      </div>
    </div>
  )
}

export function BlocksPage() {
  const [entries, setEntries] = useState<BlockEntry[]>([])
  const [selected, setSelected] = useState<string | null>(null)
  const [facePath, setFacePath] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [version, setVersion] = useState(0)
  const [notice, setNotice] = useState<{ text: string; bad: boolean } | null>(null)

  const load = async () => {
    try {
      setEntries(await filesApi.blocks())
    } catch (cause: unknown) {
      setNotice({ text: message(cause), bad: true })
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const groups = useMemo(() => {
    const filter = query.trim().toLowerCase()
    const visible = entries.filter((entry) => !filter || `${entry.id} ${entry.name}`.toLowerCase().includes(filter))
    return [...new Set(visible.map((entry) => `${entry.kind}:${entry.file}`))].map((key) => ({
      key,
      items: visible.filter((entry) => `${entry.kind}:${entry.file}` === key),
    }))
  }, [entries, query])

  const current = entries.find((entry) => `${entry.kind}:${entry.id}` === selected) ?? null
  const face = current?.faces.find((item) => item.path === facePath) ?? current?.faces[0] ?? null

  const saved = (path: string) => {
    setVersion((value) => value + 1)
    setNotice({ text: `Salvo em ${path}. Rode /textura gerar no servidor (ou reinicie) para o pack novo.`, bad: false })
  }

  const save = async (blob: Blob) => {
    if (!face) return
    try {
      await filesApi.saveBlockTexture(face.path, blob)
      saved(face.path)
    } catch (cause: unknown) {
      setNotice({ text: message(cause), bad: true })
    }
  }

  return (
    <SectionShell
      section="blocos"
      subtitle="configs/blocks e configs/items"
      actions={
        <button className="btn ghost" onClick={() => void load()} title="Relê os ymls de blocos e itens">
          <RefreshCw /> Recarregar
        </button>
      }
    >
      <aside className="components">
        <div className="panel-head">
          <span className="eyebrow">Blocos e itens</span>
        </div>
        <div className="search" style={{ margin: '0 10px 8px' }}>
          <Search />
          <input value={query} placeholder="Buscar" onChange={(event) => setQuery(event.target.value)} />
        </div>
        <div className="tree">
          {groups.map((group) => (
            <div key={group.key}>
              <div className="tree-group">{group.key.replace(':', ' · ')}</div>
              {group.items.map((entry) => {
                const key = `${entry.kind}:${entry.id}`
                return (
                  <div
                    key={key}
                    className={`tree-row${key === selected ? ' selected' : ''}`}
                    style={{ paddingLeft: 10 }}
                    onClick={() => {
                      setSelected(key)
                      setFacePath(null)
                    }}
                  >
                    <img className="tree-thumb" src={filesApi.rawUrl(entry.faces[0].path, version)} alt="" />
                    <span className="name">{entry.name}</span>
                  </div>
                )
              })}
            </div>
          ))}
        </div>
      </aside>

      <section className="stage">
        {current && face ? (
          <div className="blocks-editor">
            <div className="blocks-editor-head">
              <span className="eyebrow">{current.kind === 'item' ? 'Item' : 'Bloco'}</span>
              <h3>
                {current.name} · {faceLabel(face.face)}
              </h3>
              <code>{face.path}</code>
            </div>
            <PixelEditor key={face.path} src={filesApi.rawUrl(face.path, version)} onSave={save} onUpload={save} />
            <p className="hint-text">
              PNG de 16×16 ou do tamanho atual da textura (até 256). Depois de salvar, rode <code>/textura gerar</code> ou reinicie o servidor.
            </p>
            {notice && <div className={`notice${notice.bad ? ' bad' : ''}`}>{notice.text}</div>}
          </div>
        ) : (
          <div className="stage-empty">Escolha um bloco ou item na lista.</div>
        )}
      </section>

      <aside className="inspector">
        {current ? (
          <div className="section">
            <span className="eyebrow">Preview</span>
            <CubePreview entry={current} version={version} />
            <span className="eyebrow">Faces</span>
            <div className="glyph-grid blocks-faces">
              {current.faces.map((item) => (
                <button key={item.path} className={`glyph-card${item.path === face?.path ? ' on' : ''}`} onClick={() => setFacePath(item.path)}>
                  <span className="glyph-thumb">
                    <img src={filesApi.rawUrl(item.path, version)} alt="" />
                  </span>
                  <b>{faceLabel(item.face)}</b>
                  <small>{item.path.split('/').at(-1)}</small>
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="section">
            <span className="eyebrow">Blocos e itens</span>
            <h3>Edite as texturas</h3>
            <p className="hint-text">Escolha uma máquina, bloco ou item e depois a face. Dá para pintar pixel a pixel, usar uma imagem de referência ou enviar um PNG.</p>
            {notice && <div className={`notice${notice.bad ? ' bad' : ''}`}>{notice.text}</div>}
          </div>
        )}
      </aside>
    </SectionShell>
  )
}
