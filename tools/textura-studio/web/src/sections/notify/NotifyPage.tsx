import { useEffect, useMemo, useRef, useState } from 'react'
import { Check, Plus, Redo2, Save, Trash2, Undo2 } from 'lucide-react'
import { assetApi, type AssetCatalog, type Box } from '../../api'
import { AssetField, BoolField, NumberField, SelectField, TextField } from '../../editor/fields'
import { isObject } from '../../editor/tree'
import { filesApi } from '../files'
import { SectionShell } from '../SectionShell'
import { useFileShortcuts, useYamlFile } from '../useYamlFile'
import { Viewport, useView } from '../Viewport'

const FILE = 'configs/hud/notificacoes.yml'
const ANCHORS = ['TOPO_ESQUERDA', 'TOPO_DIREITA', 'MEIO_ESQUERDA', 'MEIO_DIREITA', 'BASE_ESQUERDA', 'BASE_DIREITA']
const LINES_KEY = 'textura-studio:notify-lines'
const HUD_KEY = 'textura-studio:hud-sample'

interface Line {
  text: string
  icon: string
}

interface HudSample {
  enabled: boolean
  main: string
  secondary: string
  side: 'right' | 'left'
  gap: number
}

interface RegionBox extends Box {
  id: string
  anchor: string
  pointX: number
  pointY: number
}

interface HudPreview {
  png: string
  guiWidth: number
  guiHeight: number
  scale: number
  regions: RegionBox[]
  hud: Box | null
  warnings: string[]
}

const DEFAULT_LINES: Record<string, Line[]> = {
  coleta: [
    { text: '<green>+3</green> <lang:block.minecraft.oak_planks>', icon: 'item:oak_planks' },
    { text: '<green>+1</green> <lang:item.minecraft.string>', icon: 'item:string' },
    { text: '<green>+2</green> <lang:item.minecraft.kelp>', icon: 'item:kelp' },
  ],
  avisos: [{ text: '<yellow>Sua Rede de Detritos ficou cheia!</yellow>', icon: 'item:loom' }],
}

const DEFAULT_HUD: HudSample = { enabled: true, main: '<yellow>⛏</yellow> Ilha nível <green>3</green>', secondary: '', side: 'right', gap: 6 }

function stored<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

function store(key: string, value: unknown) {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // sem armazenamento: vale só nesta aba
  }
}

export function NotifyPage({ mode }: { mode: 'notify' | 'hud' }) {
  const file = useYamlFile(FILE)
  useFileShortcuts(file)
  const [view, setView] = useView()
  const [selected, setSelected] = useState<string | null>(null)
  const [lines, setLines] = useState<Record<string, Line[]>>(() => stored(LINES_KEY, DEFAULT_LINES))
  const [hud, setHud] = useState<HudSample>(() => stored(HUD_KEY, DEFAULT_HUD))
  const [preview, setPreview] = useState<HudPreview | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [catalog, setCatalog] = useState<AssetCatalog | null>(null)
  const drag = useRef<{ key: string; x: number; y: number; clientX: number; clientY: number } | null>(null)

  useEffect(() => store(LINES_KEY, lines), [lines])
  useEffect(() => store(HUD_KEY, hud), [hud])
  useEffect(() => {
    assetApi.catalog().then(setCatalog).catch(() => setCatalog(null))
  }, [])

  const regions = useMemo(() => {
    const json = file.doc?.toJSON() as unknown
    return isObject(json) ? Object.entries(json).filter(([, value]) => isObject(value)).map(([key, value]) => ({ key, value: value as Record<string, unknown> })) : []
  }, [file.doc])
  const idOf = (key: string) => {
    const value = regions.find((region) => region.key === key)?.value
    return typeof value?.id === 'string' && value.id ? value.id : key
  }
  const current = regions.find((region) => region.key === selected) ?? null

  useEffect(() => {
    if (!file.loaded) return
    const controller = new AbortController()
    setBusy(true)
    const timer = window.setTimeout(() => {
      const sampleLines = Object.fromEntries(regions.map((region) => [idOf(region.key), lines[idOf(region.key)] ?? lines[region.key] ?? []]))
      filesApi
        .post<HudPreview>('/api/hud/preview', {
          regions: file.text,
          lines: mode === 'hud' && !hud.enabled ? {} : sampleLines,
          hud: mode === 'hud' ? { ...hud, enabled: true } : { enabled: false },
          monitorWidth: { '1080p': 1920, '1440p': 2560, '4k': 3840 }[view.monitor],
          monitorHeight: { '1080p': 1080, '1440p': 1440, '4k': 2160 }[view.monitor],
          scale: view.scale,
        })
        .then((result) => {
          if (controller.signal.aborted) return
          setPreview(result)
          setError(null)
        })
        .catch((cause: unknown) => !controller.signal.aborted && setError(cause instanceof Error ? cause.message : 'Falha no preview'))
        .finally(() => !controller.signal.aborted && setBusy(false))
    }, 150)
    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [file.text, file.loaded, lines, hud, view, mode])

  const addRegion = () => {
    let index = regions.length + 1
    while (regions.some((region) => region.key === `regiao-${index}`)) index++
    const key = `regiao-${index}`
    file.set([key], { id: key, ancora: 'TOPO_DIREITA', x: 0, y: 4, linhas: 3, espacamento: 1, fundo: true, direcao: 'baixo' }, 'add')
    setSelected(key)
  }

  const removeRegion = (key: string) => {
    if (!window.confirm(`Apagar a região '${key}'?`)) return
    file.set([key], undefined, 'delete')
    setSelected(null)
  }

  const set = (field: string, value: unknown) => current && file.set([current.key, field], value)
  const regionLines = current ? (lines[idOf(current.key)] ?? []) : []
  const setRegionLines = (next: Line[]) => current && setLines({ ...lines, [idOf(current.key)]: next })

  const overlay = (guiToPx: number) => (
    <div
      className="region-layer"
      onMouseMove={(event) => {
        const state = drag.current
        if (!state || guiToPx === 0) return
        const dx = Math.round((event.clientX - state.clientX) / guiToPx)
        const dy = Math.round((event.clientY - state.clientY) / guiToPx)
        file.edit((doc) => {
          doc.setIn([state.key, 'x'], state.x + dx)
          doc.setIn([state.key, 'y'], state.y + dy)
        }, `drag:${state.key}`)
      }}
      onMouseUp={() => (drag.current = null)}
      onMouseLeave={() => (drag.current = null)}
    >
      {mode === 'notify' &&
        preview?.regions.map((box) => {
          const key = regions.find((region) => idOf(region.key) === box.id)?.key ?? box.id
          return (
            <div
              key={box.id}
              className={`region-box${key === selected ? ' on' : ''}`}
              style={{ left: box.x * guiToPx, top: box.y * guiToPx, width: box.w * guiToPx, height: box.h * guiToPx }}
              onMouseDown={(event) => {
                event.preventDefault()
                setSelected(key)
                const value = regions.find((region) => region.key === key)?.value
                drag.current = { key, x: Number(value?.x ?? 0), y: Number(value?.y ?? 0), clientX: event.clientX, clientY: event.clientY }
              }}
            >
              <span className="region-tag">
                {box.id} · {box.anchor.toLowerCase().replace('_', ' ')}
              </span>
              <i className="region-point" style={{ left: (box.pointX - box.x) * guiToPx, top: (box.pointY - box.y) * guiToPx }} />
            </div>
          )
        })}
      {preview?.hud && (
        <div className={`region-box hud${mode === 'hud' ? ' on' : ''}`} style={{ left: preview.hud.x * guiToPx, top: preview.hud.y * guiToPx, width: preview.hud.w * guiToPx, height: preview.hud.h * guiToPx }}>
          <span className="region-tag">HUD bossbar</span>
        </div>
      )}
    </div>
  )

  const context = { catalog, theme: null, pages: [] }

  return (
    <SectionShell
      section={mode === 'hud' ? 'hud' : 'notificacoes'}
      subtitle={mode === 'hud' ? 'teste de texto · HudApi' : FILE}
      dirty={file.dirty}
      actions={
        mode === 'notify' && (
          <>
            <button className="icon-btn" title="Desfazer (Ctrl+Z)" disabled={!file.canUndo} onClick={file.undo}>
              <Undo2 />
            </button>
            <button className="icon-btn" title="Refazer (Ctrl+Y)" disabled={!file.canRedo} onClick={file.redo}>
              <Redo2 />
            </button>
            <button className={`btn primary${file.dirty ? '' : ' done'}`} onClick={() => void file.save()} disabled={file.saving} title="Ctrl+S">
              {file.dirty ? <Save /> : <Check />} {file.saving ? 'Salvando…' : file.dirty ? 'Salvar' : 'Salvo'}
            </button>
          </>
        )
      }
    >
      {mode === 'notify' && (
        <aside className="components">
          <div className="panel-head">
            <span className="eyebrow">Regiões</span>
            <span style={{ flex: 1 }} />
            <button className="icon-btn" title="Nova região" onClick={addRegion}>
              <Plus />
            </button>
          </div>
          <div className="tree">
            {regions.map((region) => (
              <div key={region.key} className={`tree-row${region.key === selected ? ' selected' : ''}`} style={{ paddingLeft: 10 }} onClick={() => setSelected(region.key)}>
                <span className="name">{idOf(region.key)}</span>
                <span className="detail">{String(region.value.ancora ?? '').toLowerCase().replace('_', ' ')}</span>
              </div>
            ))}
            {regions.length === 0 && <div className="empty">Nenhuma região</div>}
          </div>
        </aside>
      )}

      <Viewport
        view={view}
        onView={setView}
        png={preview?.png ?? null}
        appliedScale={preview?.scale ?? null}
        guiSize={preview ? `${preview.guiWidth}×${preview.guiHeight}` : null}
        busy={busy}
        error={error ?? (file.error ? `YAML inválido: ${file.error}` : null)}
        overlay={overlay}
        foot={<span>{mode === 'notify' ? 'Arraste uma região para mudar x/y · o ponto marca âncora + x/y' : 'A HUD é montada pelo código; aqui você testa texto e fundo (glyphs hud_left/body/right)'}</span>}
      />

      <aside className="inspector">
        {mode === 'hud' ? (
          <div className="section">
            <span className="eyebrow">HUD bossbar</span>
            <h3>Texto de teste</h3>
            <TextField label="Texto principal" value={hud.main} onChange={(value) => setHud({ ...hud, main: String(value ?? '') })} />
            <TextField label="Secundária (ao lado)" value={hud.secondary} placeholder="nenhuma" onChange={(value) => setHud({ ...hud, secondary: String(value ?? '') })} />
            <div className="grid2">
              <SelectField label="Lado" value={hud.side} options={['right', 'left']} empty="right" onChange={(value) => setHud({ ...hud, side: value === 'left' ? 'left' : 'right' })} />
              <NumberField label="Espaço" value={hud.gap} onChange={(value) => setHud({ ...hud, gap: Number(value ?? 6) })} />
            </div>
            <BoolField label="Mostrar notificações junto" value={hud.enabled} onChange={(value) => setHud({ ...hud, enabled: value === true })} />
            <p className="hint-text">
              MiniMessage com as tags do Textura. O fundo usa os glyphs <code>hud_left</code>, <code>hud_body</code> e <code>hud_right</code> — edite em{' '}
              <a href="#/glyphs">Glyphs</a>.
            </p>
          </div>
        ) : current ? (
          <>
            <div className="section">
              <span className="eyebrow">Região</span>
              <div className="inspector-head">
                <h3>{idOf(current.key)}</h3>
                <button className="icon-btn danger" title="Apagar região" onClick={() => removeRegion(current.key)}>
                  <Trash2 />
                </button>
              </div>
              <TextField label="Id (usado no código)" value={current.value.id} placeholder={current.key} onChange={(value) => set('id', value)} />
              <SelectField label="Âncora" value={current.value.ancora} options={ANCHORS} empty="—" onChange={(value) => set('ancora', value)} />
              <div className="grid2">
                <NumberField label="X" value={current.value.x ?? 0} onChange={(value) => set('x', value ?? 0)} />
                <NumberField label="Y" value={current.value.y ?? 0} onChange={(value) => set('y', value ?? 0)} />
                <NumberField label="Linhas (1–15)" value={current.value.linhas ?? 3} onChange={(value) => set('linhas', value)} />
                <NumberField label="Espaçamento" value={current.value.espacamento ?? 1} onChange={(value) => set('espacamento', value)} />
              </div>
              <SelectField label="Direção" value={current.value.direcao} options={['baixo', 'cima']} empty="padrão da âncora" onChange={(value) => set('direcao', value)} />
              <BoolField label="Placa de fundo" value={current.value.fundo !== false} onChange={(value) => set('fundo', value === true ? undefined : false)} />
            </div>
            <div className="section">
              <span className="eyebrow">Linhas de exemplo</span>
              <p className="hint-text">Só para o preview. O código manda as linhas reais com NotificationApi.push.</p>
              {regionLines.map((line, index) => (
                <div key={index} className="slot-item" style={{ marginTop: 10 }}>
                  <div className="list-row">
                    <span className="slot-at">#{index + 1}</span>
                    <button className="icon-btn" title="Remover" onClick={() => setRegionLines(regionLines.filter((_, at) => at !== index))}>
                      <Trash2 />
                    </button>
                  </div>
                  <TextField label="Texto (MiniMessage)" value={line.text} onChange={(value) => setRegionLines(regionLines.map((old, at) => (at === index ? { ...old, text: String(value ?? '') } : old)))} />
                  <AssetField label="Ícone" value={line.icon} context={context} kinds={['item', 'glyph']} onChange={(value) => setRegionLines(regionLines.map((old, at) => (at === index ? { ...old, icon: String(value ?? '') } : old)))} />
                </div>
              ))}
              <button className="add-row" style={{ marginTop: 10, width: '100%' }} onClick={() => setRegionLines([...regionLines, { text: `<white>Linha ${regionLines.length + 1}</white>`, icon: 'item:oak_planks' }])}>
                <Plus /> Linha
              </button>
            </div>
          </>
        ) : (
          <div className="section">
            <span className="eyebrow">Notificações</span>
            <h3>Selecione uma região</h3>
            <p className="hint-text">
              Clique numa região na tela ou na lista. Mudar y, linhas, espaçamento ou direção pede <code>/textura gerar</code> depois de salvar; o resto basta{' '}
              <code>/textura reload</code>.
            </p>
          </div>
        )}
        {(file.saveError || (preview?.warnings.length ?? 0) > 0) && (
          <div className="section">
            {file.saveError && <div className="notice bad">{file.saveError}</div>}
            {preview && preview.warnings.length > 0 && (
              <div className="notice warn">
                {preview.warnings.length} aviso(s)
                <ul>
                  {preview.warnings.map((warning) => (
                    <li key={warning}>{warning}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </aside>
    </SectionShell>
  )
}
