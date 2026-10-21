import { useEffect, useMemo, useState } from 'react'
import { Check, FilePlus2, Plus, Redo2, Save, Search, Trash2, Undo2, Upload } from 'lucide-react'
import { BoolField, ListField, NumberField, TextField } from '../../editor/fields'
import { isObject } from '../../editor/tree'
import { filesApi } from '../files'
import { SectionShell } from '../SectionShell'
import { useFileShortcuts, useYamlFile, type YamlFile } from '../useYamlFile'

const FOLDER = 'configs/glyphs'
const ICONS = 'assets/oneblock/textures/icons'
const KEY = /^[a-z0-9_-]{1,48}$/

type Entry = Record<string, unknown>

/** Caminho do PNG para o `/api/raw`: layout novo em resource/, antigo dentro de configs/glyphs. */
export function imagePath(caminho: unknown): string | null {
  if (typeof caminho !== 'string' || !caminho.endsWith('.png')) return null
  return caminho.startsWith('assets/') ? `resource/${caminho}` : `${FOLDER}/${caminho}`
}

function pickPng(): Promise<File | null> {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = 'image/png'
    input.onchange = () => resolve(input.files?.[0] ?? null)
    input.click()
  })
}

function ChatPreview({ entry, version }: { entry: Entry; version: number }) {
  const [png, setPng] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const caminho = typeof entry.caminho === 'string' ? entry.caminho : ''

  useEffect(() => {
    if (!caminho.startsWith('assets/')) return setError('Preview só para imagens em resource/assets')
    const timer = window.setTimeout(() => {
      filesApi
        .post<{ png: string }>('/api/glyph/preview', {
          path: caminho,
          ascent: Number(entry.ascent ?? 8),
          height: Number(entry.height ?? 8),
          size: typeof entry.size === 'number' ? entry.size : undefined,
          text: 'Olá {glyph} mundo',
        })
        .then((result) => {
          setPng(result.png)
          setError(null)
        })
        .catch((cause: unknown) => setError(cause instanceof Error ? cause.message : 'Falha no preview'))
    }, 150)
    return () => window.clearTimeout(timer)
  }, [caminho, entry.ascent, entry.height, entry.size, version])

  if (error) return <p className="hint-text">{error}</p>
  return png ? <img className="chat-preview" src={`data:image/png;base64,${png}`} alt="Glyph no chat" /> : null
}

function FontsField({ file, entryKey, fonts }: { file: YamlFile; entryKey: string; fonts: unknown }) {
  const entries = isObject(fonts) ? Object.entries(fonts) : []
  return (
    <div className="field">
      <label>Fontes nomeadas</label>
      <div className="list-field">
        {entries.map(([name, value]) => {
          const font = isObject(value) ? value : {}
          const at = (field: string) => [entryKey, 'fonts', name, field]
          return (
            <div key={name} className="slot-item">
              <div className="list-row">
                <span className="slot-at">{name}</span>
                <button className="icon-btn" title="Remover fonte" onClick={() => file.set([entryKey, 'fonts', name], undefined)}>
                  <Trash2 />
                </button>
              </div>
              <div className="grid2">
                <NumberField label="Ascent" value={font.ascent} onChange={(next) => file.set(at('ascent'), next)} />
                <NumberField label="Height" value={font.height} onChange={(next) => file.set(at('height'), next)} />
              </div>
              <TextField label="Namespace" value={font.namespace} placeholder="minecraft" onChange={(next) => file.set(at('namespace'), next)} />
              <TextField label="Unicode próprio" value={font['unicode-specify']} placeholder="herda o global" onChange={(next) => file.set(at('unicode-specify'), next)} />
            </div>
          )
        })}
        <button
          className="add-row"
          onClick={() => {
            const name = window.prompt('Nome da fonte (ex.: bossbar):')?.trim()
            if (name && KEY.test(name)) file.set([entryKey, 'fonts', name], { ascent: 7, height: 8 })
          }}
        >
          <Plus /> Fonte
        </button>
      </div>
    </div>
  )
}

export function GlyphsPage() {
  const [paths, setPaths] = useState<string[]>([])
  const [path, setPath] = useState<string | null>(null)
  const [selected, setSelected] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [version, setVersion] = useState(0)
  const [message, setMessage] = useState<string | null>(null)
  const file = useYamlFile(path)
  useFileShortcuts(file)

  const refreshList = () =>
    filesApi
      .list(FOLDER)
      .then((list) => {
        setPaths(list)
        setPath((current) => current ?? list[0] ?? null)
      })
      .catch((cause: unknown) => setMessage(cause instanceof Error ? cause.message : 'Falha ao listar'))

  useEffect(() => {
    void refreshList()
  }, [])

  const entries = useMemo(() => {
    const json = file.doc?.toJSON() as unknown
    if (!isObject(json)) return []
    return Object.entries(json).flatMap(([key, value]) => (isObject(value) ? [{ key, value: value as Entry }] : []))
  }, [file.doc])
  const visible = entries.filter(({ key, value }) => `${key} ${String(value.id ?? '')}`.toLowerCase().includes(query.toLowerCase()))
  const current = entries.find((entry) => entry.key === selected) ?? null
  const set = (field: string, value: unknown) => current && file.set([current.key, field], value)

  const switchFile = (next: string) => {
    if (file.dirty && !window.confirm('Trocar de arquivo sem salvar?')) return
    setPath(next)
    setSelected(null)
  }

  const uploadFor = async (id: string): Promise<string | null> => {
    const png = await pickPng()
    if (!png) return null
    const target = `${ICONS}/${id}.png`
    try {
      await filesApi.upload(`resource/${target}`, png)
      setVersion((value) => value + 1)
      setMessage(`Imagem salva em resource/${target}. Rode /textura gerar para o pack.`)
      return target
    } catch (cause: unknown) {
      setMessage(cause instanceof Error ? cause.message : 'Falha no upload')
      return null
    }
  }

  const addEntry = async () => {
    const key = window.prompt('Id do glyph (letras minúsculas, números, - e _):')?.trim()
    if (!key) return
    if (!KEY.test(key) || entries.some((entry) => entry.key === key)) return window.alert('Id inválido ou já existe neste arquivo.')
    const caminho = await uploadFor(key)
    file.set([key], { id: key, caminho: caminho ?? `${ICONS}/${key}.png`, ascent: 8, height: 8, default: true, show_in_gui: true }, 'add')
    setSelected(key)
  }

  const addFile = async () => {
    const name = window.prompt('Nome do novo arquivo (ex.: icons-ilha):')?.trim()
    if (!name || !KEY.test(name)) return
    const target = `${FOLDER}/${name}.yml`
    try {
      await filesApi.write(target, `# Glyphs de ${name}\n`)
      await refreshList()
      setPath(target)
    } catch (cause: unknown) {
      setMessage(cause instanceof Error ? cause.message : 'Falha ao criar')
    }
  }

  const sheet = current && Array.isArray(current.value.ids)

  return (
    <SectionShell
      section="glyphs"
      subtitle={path ?? FOLDER}
      dirty={file.dirty}
      actions={
        <>
          <button className="icon-btn" title="Desfazer (Ctrl+Z)" disabled={!file.canUndo} onClick={file.undo}>
            <Undo2 />
          </button>
          <button className="icon-btn" title="Refazer (Ctrl+Y)" disabled={!file.canRedo} onClick={file.redo}>
            <Redo2 />
          </button>
          <button className={`btn primary${file.dirty ? '' : ' done'}`} onClick={() => void file.save()} disabled={file.saving || !path} title="Ctrl+S">
            {file.dirty ? <Save /> : <Check />} {file.saving ? 'Salvando…' : file.dirty ? 'Salvar' : 'Salvo'}
          </button>
        </>
      }
    >
      <aside className="components">
        <div className="panel-head">
          <span className="eyebrow">Arquivos</span>
          <span style={{ flex: 1 }} />
          <button className="icon-btn" title="Novo arquivo" onClick={() => void addFile()}>
            <FilePlus2 />
          </button>
        </div>
        <div className="tree">
          {paths.map((item) => (
            <div key={item} className={`tree-row${item === path ? ' selected' : ''}`} style={{ paddingLeft: 10 }} onClick={() => switchFile(item)}>
              <span className="name">{item.replace(`${FOLDER}/`, '')}</span>
            </div>
          ))}
        </div>
      </aside>

      <section className="stage">
        <div className="view-bar">
          <div className="search">
            <Search />
            <input value={query} placeholder="Buscar glyph" onChange={(event) => setQuery(event.target.value)} />
          </div>
          <span className="meta">{entries.length} entradas</span>
          <button className="btn ghost" style={{ marginLeft: 'auto' }} onClick={() => void addEntry()} disabled={!path}>
            <Plus /> Glyph
          </button>
        </div>
        <div className="glyph-grid">
          {file.error && <div className="stage-error">YAML inválido: {file.error}</div>}
          {visible.map(({ key, value }) => {
            const image = imagePath(value.caminho)
            return (
              <button key={key} className={`glyph-card${key === selected ? ' on' : ''}`} onClick={() => setSelected(key)}>
                <span className="glyph-thumb">{image && <img src={filesApi.rawUrl(image, version)} alt="" />}</span>
                <b>{String(value.id ?? key)}</b>
                <small>{Array.isArray(value.ids) ? `folha · ${value.ids.length}` : String(value.unicode ?? 'sem unicode')}</small>
              </button>
            )
          })}
        </div>
      </section>

      <aside className="inspector">
        {current ? (
          <>
            <div className="section">
              <span className="eyebrow">{sheet ? 'Folha de sprites' : 'Glyph'}</span>
              <div className="inspector-head">
                <h3>{current.key}</h3>
                <button
                  className="icon-btn danger"
                  title="Apagar entrada"
                  onClick={() => {
                    if (!window.confirm(`Apagar '${current.key}' deste arquivo? (a imagem fica em resource/)`)) return
                    file.set([current.key], undefined, 'delete')
                    setSelected(null)
                  }}
                >
                  <Trash2 />
                </button>
              </div>
              <div className="glyph-big">{imagePath(current.value.caminho) && <img src={filesApi.rawUrl(imagePath(current.value.caminho) ?? '', version)} alt="" />}</div>
              {!sheet && <ChatPreview entry={current.value} version={version} />}
              {!sheet && <TextField label="Id" value={current.value.id} placeholder={current.key} onChange={(value) => set('id', value)} />}
              <div className="field">
                <label>Imagem (caminho)</label>
                <div className="asset-field">
                  <input type="text" value={String(current.value.caminho ?? '')} onChange={(event) => set('caminho', event.target.value || undefined)} />
                  <button
                    className="icon-btn"
                    title="Enviar PNG"
                    onClick={async () => {
                      const caminho = await uploadFor(String(current.value.id ?? current.key))
                      if (caminho) set('caminho', caminho)
                    }}
                  >
                    <Upload />
                  </button>
                </div>
              </div>
              {sheet ? (
                <>
                  <NumberField label="Quantidade de fatias" value={current.value.amount} onChange={(value) => set('amount', value)} />
                  <ListField label="Ids" value={current.value.ids} placeholder="glyph" onChange={(value) => set('ids', value)} />
                </>
              ) : (
                <TextField label="Unicode" value={current.value.unicode} placeholder="gerado ao recarregar" onChange={(value) => set('unicode', value)} />
              )}
              <div className="grid2">
                <NumberField label="Ascent" value={current.value.ascent} placeholder="8" onChange={(value) => set('ascent', value)} />
                <NumberField label="Height" value={current.value.height} placeholder="8" onChange={(value) => set('height', value)} />
              </div>
              {!sheet && <NumberField label="Size (largura p/ medir texto)" value={current.value.size} placeholder="medido do PNG" onChange={(value) => set('size', value)} />}
              <BoolField label="Também na fonte padrão (chat)" value={current.value.default} onChange={(value) => set('default', value === true)} />
              <BoolField label="Aparece em /textura glyphs" value={current.value.show_in_gui} onChange={(value) => set('show_in_gui', value === true)} />
            </div>
            {!sheet && (
              <div className="section">
                <FontsField file={file} entryKey={current.key} fonts={current.value.fonts} />
              </div>
            )}
          </>
        ) : (
          <div className="section">
            <span className="eyebrow">Glyphs</span>
            <h3>Selecione um glyph</h3>
            <p className="hint-text">
              Unicode só na Private Use Area (U+E000+). Depois de salvar, <code>/textura reload</code> relê os ymls e <code>/textura gerar</code> leva as
              imagens para o pack.
            </p>
          </div>
        )}
        {(message || file.saveError) && (
          <div className="section">
            {file.saveError && <div className="notice bad">{file.saveError}</div>}
            {message && <div className="notice">{message}</div>}
          </div>
        )}
      </aside>
    </SectionShell>
  )
}
