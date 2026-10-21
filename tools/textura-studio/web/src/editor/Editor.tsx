import { Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react'
import { ArrowLeft, Braces, Check, Download, Maximize, MousePointer2, Play, Redo2, RefreshCw, Save, ScanSearch, Undo2, ZoomIn, ZoomOut } from 'lucide-react'
import { api, assetApi, type AssetCatalog } from '../api'
import { Brand } from '../Brand'
import { navigate } from '../nav'
import { ComponentsPanel } from './ComponentsPanel'
import { ContextMenu, type MenuEntry } from './ContextMenu'
import { EditLayer } from './EditLayer'
import { Inspector, type NodeAction, type SaveState } from './Inspector'
import { format } from './ops'
import { PAGE_ID, addPage, deletePage, renamePage } from './pages'
import { EMPTY_SAMPLES, type Samples } from './SamplesPanel'
import { MONITORS, Stage, type MonitorId, type Mode } from './Stage'
import { findNode, isObject, pageIds, parentOf, screenTree, setAt, trail, type Json, type JsonPath } from './tree'
import { useHistory } from './useHistory'
import { useNodeActions } from './useNodeActions'
import { usePreview } from './usePreview'

// O CodeMirror só carrega quando o JSON é aberto.
const JsonDrawer = lazy(() => import('./JsonDrawer').then((module) => ({ default: module.JsonDrawer })))

const ZOOMS = [0.5, 0.75, 1, 1.25, 1.5, 2, 3]

function template(id: string): string {
  return format({
    title: id,
    theme: 'stone',
    column: [
      { text: 'Nova tela', style: 'heading-bare', w: 'fit' },
      { text: 'Edite esta tela no Textura Studio.', style: 'body-bare', w: 'fit' },
    ],
    w: 'fit',
    h: 'fit',
    gap: 6,
    pad: 0,
  })
}

function parseDoc(text: string): { doc: Json | null; error: string | null } {
  try {
    const value: unknown = JSON.parse(text)
    return isObject(value) ? { doc: value, error: null } : { doc: null, error: 'A tela precisa ser um objeto JSON' }
  } catch (cause: unknown) {
    return { doc: null, error: cause instanceof Error ? cause.message : 'JSON inválido' }
  }
}

function loadSamples(id: string): Samples {
  try {
    const raw = window.localStorage.getItem(`textura-studio:samples:${id}`)
    return raw ? { ...EMPTY_SAMPLES, ...(JSON.parse(raw) as Samples) } : EMPTY_SAMPLES
  } catch {
    return EMPTY_SAMPLES
  }
}

function isTyping(target: EventTarget | null): boolean {
  return target instanceof HTMLElement && Boolean(target.closest('input, textarea, select, .cm-editor'))
}

export function Editor({ id, fresh, from }: { id: string; fresh: boolean; from: string | null }) {
  const history = useHistory('')
  const [saved, setSaved] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [catalog, setCatalog] = useState<AssetCatalog | null>(null)
  const [monitor, setMonitor] = useState<MonitorId>('1080p')
  const [scale, setScale] = useState(0)
  const [zoom, setZoom] = useState(1)
  const [mode, setMode] = useState<Mode>('edit')
  const [page, setPage] = useState<string | null>(null)
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const [jsonOpen, setJsonOpen] = useState(false)
  const [showHits, setShowHits] = useState(false)
  const [save, setSave] = useState<SaveState>({ kind: 'idle' })
  const [refresh, setRefresh] = useState(0)
  const [samples, setSamples] = useState<Samples>(() => loadSamples(id))
  const [menu, setMenu] = useState<{ x: number; y: number } | null>(null)

  useEffect(() => {
    assetApi.catalog().then(setCatalog).catch(() => setCatalog(null))
    api
      .dialog(id)
      .then((text) => {
        history.reset(text)
        setSaved(text)
      })
      .catch(async (cause: unknown) => {
        if (!fresh) return setLoadError(cause instanceof Error ? cause.message : 'Falha ao abrir')
        const base = from ? await api.dialog(from).catch(() => null) : null
        history.reset(base ?? template(id))
        setSaved('')
      })
  }, [id, fresh, from, history.reset])

  useEffect(() => {
    try {
      window.localStorage.setItem(`textura-studio:samples:${id}`, JSON.stringify(samples))
    } catch {
      // sem armazenamento: os exemplos valem só nesta aba
    }
  }, [id, samples])

  const text = history.text
  const { doc, error: jsonError } = useMemo(() => parseDoc(text), [text])
  const pages = useMemo(() => (doc ? pageIds(doc) : []), [doc])
  const activePage = page && pages.includes(page) ? page : (pages[0] ?? null)
  const tree = useMemo(() => (doc ? screenTree(doc, activePage) : null), [doc, activePage])
  const selected = tree && selectedKey ? findNode(tree, selectedKey) : null
  const dirty = saved !== null && text !== saved
  const theme = doc ? String(doc.theme ?? (isObject(doc.document) ? doc.document.theme : '') ?? '') : null

  const params = useMemo(
    () =>
      saved === null
        ? null
        : {
            id,
            json: text,
            page: activePage,
            monitorWidth: MONITORS[monitor].width,
            monitorHeight: MONITORS[monitor].height,
            scale,
            lists: samples.lists,
            values: samples.values,
          },
    [id, text, activePage, monitor, scale, saved, samples],
  )
  const { preview, busy } = usePreview(params, refresh)

  const commit = useCallback(
    (next: Json, select: JsonPath | null, source: string) => {
      history.set(format(next), source)
      if (select) setSelectedKey(select.length ? select.join('.') : null)
    },
    [history],
  )

  const change = useCallback(
    (path: JsonPath, value: unknown, source = path.join('.')) => {
      if (doc) history.set(format(setAt(doc, path, value) as Json), source)
    },
    [doc, history],
  )

  const actions = useNodeActions({ doc, page: activePage, selected, commit })

  const persist = useCallback(async () => {
    if (jsonError) return setSave({ kind: 'error', message: `JSON inválido: ${jsonError}` })
    setSave({ kind: 'saving' })
    try {
      const result = await api.save(id, text)
      setSaved(text)
      setSave({ kind: 'saved', file: result.saved })
      if (fresh) navigate(`#/dialog/${id}`)
    } catch (cause: unknown) {
      setSave({ kind: 'error', message: cause instanceof Error ? cause.message : 'Falha ao salvar' })
    }
  }, [id, text, jsonError, fresh])

  const selectParent = useCallback(() => {
    if (!tree || !selectedKey) return
    const parent = parentOf(tree, selectedKey)
    setSelectedKey(parent && parent.kind !== 'screen' ? parent.key : null)
  }, [tree, selectedKey])

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      const mod = event.ctrlKey || event.metaKey
      const key = event.key.toLowerCase()
      if (mod && key === 's') {
        event.preventDefault()
        void persist()
        return
      }
      if (isTyping(event.target)) return
      const shortcuts: Record<string, () => void> = {
        z: () => (event.shiftKey ? history.redo() : history.undo()),
        y: history.redo,
        d: actions.duplicateSelected,
        c: actions.copy,
        v: actions.paste,
      }
      if (mod && shortcuts[key]) {
        event.preventDefault()
        shortcuts[key]()
      } else if (key === 'delete' || key === 'backspace') {
        event.preventDefault()
        actions.removeSelected()
      } else if (event.altKey && (key === 'arrowup' || key === 'arrowdown')) {
        event.preventDefault()
        actions.step(key === 'arrowup' ? -1 : 1)
      } else if (key === 'escape') selectParent()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [persist, history, actions, selectParent])

  useEffect(() => {
    const onLeave = (event: BeforeUnloadEvent) => {
      if (dirty) event.preventDefault()
    }
    window.addEventListener('beforeunload', onLeave)
    return () => window.removeEventListener('beforeunload', onLeave)
  }, [dirty])

  const back = () => {
    if (dirty && !window.confirm('Sair sem salvar as alterações?')) return
    navigate('#/')
  }

  const reloadAssets = async () => {
    await api.reload().catch(() => undefined)
    assetApi.catalog().then(setCatalog).catch(() => undefined)
    setRefresh((value) => value + 1)
  }

  const exportPng = () => {
    if (!preview?.png) return
    const link = document.createElement('a')
    link.href = `data:image/png;base64,${preview.png}`
    link.download = `${id}-${monitor}-gui${preview.scale}.png`
    link.click()
  }

  const stepZoom = (direction: 1 | -1) => {
    const index = ZOOMS.findIndex((value) => value >= zoom)
    setZoom(ZOOMS[Math.min(ZOOMS.length - 1, Math.max(0, (index < 0 ? ZOOMS.length - 1 : index) + direction))])
  }

  const onAction = (action: NodeAction) =>
    ({ duplicate: actions.duplicateSelected, delete: actions.removeSelected, up: () => actions.step(-1), down: () => actions.step(1) })[action]()

  const pageActions = {
    add: () => {
      if (!doc) return
      const result = addPage(doc, activePage)
      commit(result.doc, null, 'page')
      setPage(result.id)
    },
    rename: (from: string) => {
      const to = window.prompt('Novo id da página (letras, números, - e _):', from)?.trim()
      if (!doc || !to || to === from) return
      if (!PAGE_ID.test(to) || pages.includes(to)) return window.alert('Id inválido ou já usado.')
      commit(renamePage(doc, from, to), null, 'page')
      setPage(to)
    },
    remove: (target: string) => {
      if (!doc || !window.confirm(`Apagar a página '${target}'?`)) return
      commit(deletePage(doc, target), null, 'page')
      setPage(null)
    },
  }

  const menuItems: MenuEntry[] = [
    { label: 'Duplicar', shortcut: 'Ctrl+D', disabled: !actions.movable, run: actions.duplicateSelected },
    { label: 'Copiar', shortcut: 'Ctrl+C', run: actions.copy },
    { label: 'Colar', shortcut: 'Ctrl+V', disabled: !actions.canPaste(), run: actions.paste },
    'divider',
    { label: 'Envolver em Row', disabled: !actions.movable, run: () => actions.wrapSelected('row') },
    { label: 'Envolver em Column', disabled: !actions.movable, run: () => actions.wrapSelected('column') },
    { label: 'Envolver em Card', disabled: !actions.movable, run: () => actions.wrapSelected('card') },
    'divider',
    { label: 'Subir', shortcut: 'Alt+↑', disabled: !actions.movable, run: () => actions.step(-1) },
    { label: 'Descer', shortcut: 'Alt+↓', disabled: !actions.movable, run: () => actions.step(1) },
    { label: 'Selecionar pai', shortcut: 'Esc', run: selectParent },
    'divider',
    { label: 'Apagar', shortcut: 'Del', danger: true, disabled: !actions.movable, run: actions.removeSelected },
  ]

  if (loadError) {
    return (
      <div className="shell">
        <header className="topbar">
          <Brand />
        </header>
        <div className="empty">
          {loadError} — <a href="#/">voltar</a>
        </div>
      </div>
    )
  }

  const isSaved = !dirty && saved !== null && saved !== ''
  const crumbs = tree && selectedKey ? trail(tree, selectedKey) : tree ? [tree] : []

  return (
    <div className="shell">
      <header className="topbar">
        <Brand />
        <span className="top-sep" />
        <button className="icon-btn" onClick={back} title="Voltar à galeria">
          <ArrowLeft />
        </button>
        <div className="doc-title">
          <b>{preview?.title || id}</b>
          <span>
            {id}.json{dirty && <i className="dirty" title="Alterações não salvas" />}
          </span>
        </div>
        <div className="mode-switch" role="tablist">
          <button className={mode === 'edit' ? 'on' : ''} onClick={() => setMode('edit')}>
            <MousePointer2 /> Editar
          </button>
          <button className={mode === 'play' ? 'on' : ''} onClick={() => setMode('play')}>
            <Play /> Testar
          </button>
        </div>
        <div className="top-actions">
          <button className="btn ghost" onClick={reloadAssets} title="Relê temas, artes, fontes e glyphs do servidor">
            <RefreshCw /> Assets
          </button>
          <button className="btn ghost" onClick={exportPng} disabled={!preview?.png} title="Baixa o preview como PNG">
            <Download /> PNG
          </button>
          <button className={`btn ghost${jsonOpen ? ' active' : ''}`} onClick={() => setJsonOpen((open) => !open)}>
            <Braces /> JSON
          </button>
          <button className={`btn primary${isSaved ? ' done' : ''}`} onClick={() => void persist()} disabled={save.kind === 'saving'} title="Ctrl+S">
            {isSaved ? <Check /> : <Save />} {save.kind === 'saving' ? 'Salvando…' : isSaved ? 'Salvo' : 'Salvar'}
          </button>
        </div>
      </header>

      <div className="workspace">
        <ComponentsPanel
          tree={tree}
          selected={selectedKey}
          onSelect={(node) => setSelectedKey(node.kind === 'screen' ? null : node.key)}
          onContext={(_, x, y) => setMenu({ x, y })}
          onMove={(key, to) => {
            const node = tree ? findNode(tree, key) : null
            if (node) actions.moveTo(node.path, to)
          }}
          onAdd={actions.add}
        />

        <Stage
          preview={preview}
          busy={busy}
          mode={mode}
          monitor={monitor}
          scale={scale}
          zoom={zoom}
          showHits={showHits}
          tools={
            <div className="canvas-tools" onMouseDown={(event) => event.stopPropagation()}>
              <button title="Desfazer (Ctrl+Z)" disabled={!history.canUndo} onClick={history.undo}>
                <Undo2 />
              </button>
              <button title="Refazer (Ctrl+Shift+Z)" disabled={!history.canRedo} onClick={history.redo}>
                <Redo2 />
              </button>
              <span className="tool-sep" />
              <button title="Afastar" onClick={() => stepZoom(-1)}>
                <ZoomOut />
              </button>
              <button className="zoom-value" title="Caber na tela" onClick={() => setZoom(1)}>
                {Math.round(zoom * 100)}%
              </button>
              <button title="Aproximar" onClick={() => stepZoom(1)}>
                <ZoomIn />
              </button>
              <button title="Caber na tela" className={zoom === 1 ? 'on' : ''} onClick={() => setZoom(1)}>
                <Maximize />
              </button>
              <span className="tool-sep" />
              <button title="Mostrar áreas clicáveis" className={showHits ? 'on' : ''} onClick={() => setShowHits((on) => !on)}>
                <ScanSearch />
              </button>
            </div>
          }
          pages={pages}
          page={activePage}
          breadcrumb={crumbs.map((node, index) =>
            index === crumbs.length - 1 ? (
              <b key={node.key}>{node.label}</b>
            ) : (
              <button key={node.key} className="crumb" onClick={() => setSelectedKey(node.kind === 'screen' ? null : node.key)}>
                {node.label} ›
              </button>
            ),
          )}
          overlay={(guiToPx) =>
            doc &&
            tree &&
            preview && (
              <EditLayer
                doc={doc}
                tree={tree}
                boxes={preview.boxes}
                guiToPx={guiToPx}
                selected={selectedKey}
                onSelect={setSelectedKey}
                onMove={actions.moveTo}
                onDropNew={actions.add}
                onChange={change}
                onContext={(_, x, y) => setMenu({ x, y })}
              />
            )
          }
          onMonitor={setMonitor}
          onScale={setScale}
          onPage={setPage}
          onAddPage={pageActions.add}
          onRenamePage={pageActions.rename}
          onDeletePage={pageActions.remove}
        />

        {jsonOpen ? (
          <Suspense fallback={<aside className="json-drawer" />}>
            <JsonDrawer value={text} error={jsonError} onChange={(value) => history.set(value, 'json')} onClose={() => setJsonOpen(false)} />
          </Suspense>
        ) : (
          <Inspector
            doc={doc}
            selected={selected}
            context={{ catalog, theme, pages }}
            warnings={preview?.warnings ?? []}
            dirty={dirty}
            save={save}
            lists={preview?.lists ?? {}}
            samples={samples}
            onSamples={setSamples}
            onChange={change}
            onAction={onAction}
          />
        )}
      </div>

      {menu && <ContextMenu x={menu.x} y={menu.y} items={menuItems} onClose={() => setMenu(null)} />}
    </div>
  )
}
