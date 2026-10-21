import { useEffect, useMemo, useState } from 'react'
import { Check, Plus, Redo2, Save, Trash2, Undo2 } from 'lucide-react'
import { api, assetApi, type AssetCatalog, type Preview } from '../../api'
import { AssetField, BoolField, ColorField, NumberField, SelectField, TextField, type FieldContext } from '../../editor/fields'
import { isObject } from '../../editor/tree'
import { filesApi } from '../files'
import { SectionShell } from '../SectionShell'
import { useFileShortcuts, useYamlFile, type YamlFile } from '../useYamlFile'
import { Viewport, useView } from '../Viewport'
import { ArtsPanel } from './ArtsPanel'
import { themeSample } from './themeSample'

const FOLDER = 'configs/ui'
const TABS = ['Texto', 'Superfícies', 'Controles', 'Ícones', 'Artes'] as const
type Tab = (typeof TABS)[number]

type Json = Record<string, unknown>

interface Control {
  key: string
  label: string
  fields: { key: string; label: string; kind: 'art' | 'style' | 'number' | 'text' }[]
}

const CONTROLS: Control[] = [
  {
    key: 'button',
    label: 'Botão',
    fields: [
      { key: 'art', label: 'Arte', kind: 'art' },
      { key: 'text-style', label: 'Estilo do texto', kind: 'style' },
    ],
  },
  {
    key: 'nav',
    label: 'Sidebar button',
    fields: [
      { key: 'art', label: 'Arte', kind: 'art' },
      { key: 'active-art', label: 'Arte ativa', kind: 'art' },
      { key: 'text-style', label: 'Estilo', kind: 'style' },
      { key: 'active-text-style', label: 'Estilo ativo', kind: 'style' },
      { key: 'pad', label: 'Padding', kind: 'number' },
    ],
  },
  {
    key: 'switch',
    label: 'Switch',
    fields: [
      { key: 'on', label: 'Arte ligado', kind: 'art' },
      { key: 'off', label: 'Arte desligado', kind: 'art' },
      { key: 'labels.on', label: 'Texto ligado', kind: 'text' },
      { key: 'labels.off', label: 'Texto desligado', kind: 'text' },
    ],
  },
  {
    key: 'segmented',
    label: 'Segmented',
    fields: [
      { key: 'art', label: 'Arte', kind: 'art' },
      { key: 'selected-art', label: 'Arte selecionada', kind: 'art' },
      { key: 'frame', label: 'Moldura', kind: 'art' },
      { key: 'text-style', label: 'Estilo', kind: 'style' },
      { key: 'selected-text-style', label: 'Estilo selecionado', kind: 'style' },
      { key: 'pad', label: 'Padding', kind: 'number' },
    ],
  },
  {
    key: 'dropdown',
    label: 'Dropdown',
    fields: [
      { key: 'art', label: 'Arte', kind: 'art' },
      { key: 'arrow', label: 'Seta', kind: 'art' },
      { key: 'text-style', label: 'Estilo', kind: 'style' },
    ],
  },
  {
    key: 'slider',
    label: 'Slider',
    fields: [
      { key: 'track', label: 'Trilho', kind: 'art' },
      { key: 'fill', label: 'Preenchimento', kind: 'art' },
      { key: 'text-style', label: 'Estilo', kind: 'style' },
    ],
  },
  {
    key: 'slots',
    label: 'Slots',
    fields: [
      { key: 'art', label: 'Arte do slot', kind: 'art' },
      { key: 'text-style', label: 'Estilo da quantidade', kind: 'style' },
    ],
  },
  {
    key: 'chip',
    label: 'Chip',
    fields: [
      { key: 'art', label: 'Arte', kind: 'art' },
      { key: 'text-style', label: 'Estilo', kind: 'style' },
      { key: 'height', label: 'Altura', kind: 'number' },
      { key: 'pad', label: 'Padding', kind: 'number' },
    ],
  },
]

const LOOK_FIELDS = [
  { key: 'art', label: 'Arte', kind: 'art' },
  { key: 'text-style', label: 'Estilo', kind: 'style' },
  { key: 'selected-art', label: 'Arte selecionada', kind: 'art' },
  { key: 'selected-text-style', label: 'Estilo selecionado', kind: 'style' },
  { key: 'disabled-art', label: 'Arte desativada', kind: 'art' },
  { key: 'disabled-text-style', label: 'Estilo desativado', kind: 'style' },
] as const

const NAME = /^[a-z0-9_-]{1,32}$/

function askName(label: string, taken: string[]): string | null {
  const name = window.prompt(label)?.trim()
  if (!name) return null
  if (!NAME.test(name) || taken.includes(name)) {
    window.alert('Nome inválido ou já usado.')
    return null
  }
  return name
}

interface PanelProps {
  file: YamlFile
  name: string
  theme: Json
  context: FieldContext
}

function MapField({ file, at, value, label, kinds, context }: { file: YamlFile; at: string[]; value: unknown; label: string; kinds: ('theme' | 'ui' | 'glyph' | 'item')[]; context: FieldContext }) {
  const entries = isObject(value) ? Object.entries(value) : []
  return (
    <>
      {entries.map(([key, art]) => (
        <div key={key} className="map-row">
          <AssetField label={key} value={art} context={context} kinds={kinds} onChange={(next) => file.set([...at, key], next ?? '')} />
          <button className="icon-btn" title="Remover" onClick={() => file.set([...at, key], undefined)}>
            <Trash2 />
          </button>
        </div>
      ))}
      <button
        className="add-row"
        style={{ marginTop: 10, width: '100%' }}
        onClick={() => {
          const name = askName(`Nome (${label}):`, entries.map(([key]) => key))
          if (name) file.set([...at, name], 'ui:')
        }}
      >
        <Plus /> {label}
      </button>
    </>
  )
}

function TextPanel({ file, name, theme }: PanelProps) {
  const styles = isObject(theme.text) ? Object.entries(theme.text) : []
  return (
    <div className="section">
      <span className="eyebrow">Estilos de texto</span>
      {styles.map(([style, raw]) => {
        const value = isObject(raw) ? raw : {}
        const at = (field: string) => [name, 'text', style, field]
        return (
          <div key={style} className="slot-item" style={{ marginTop: 10 }}>
            <div className="list-row">
              <span className="slot-at">{style}</span>
              <button className="icon-btn" title="Remover estilo" onClick={() => file.set([name, 'text', style], undefined)}>
                <Trash2 />
              </button>
            </div>
            <ColorField label="Cor" value={value.color} onChange={(next) => file.set(at('color'), next)} />
            <div className="grid2">
              <BoolField label="Negrito" value={value.bold} onChange={(next) => file.set(at('bold'), next === true ? true : undefined)} />
              <BoolField label="Sombra" value={value.shadow !== false} onChange={(next) => file.set(at('shadow'), next === true ? undefined : false)} />
            </div>
          </div>
        )
      })}
      <button
        className="add-row"
        style={{ marginTop: 10, width: '100%' }}
        onClick={() => {
          const style = askName('Nome do estilo (ex.: titulo, alerta-bare):', styles.map(([key]) => key))
          if (style) file.set([name, 'text', style], { color: '#ffffff' })
        }}
      >
        <Plus /> Estilo
      </button>
    </div>
  )
}

function ControlsPanel({ file, name, theme, context, styles }: PanelProps & { styles: string[] }) {
  const field = (group: string, spec: { key: string; label: string; kind: string }) => {
    const at = [name, group, ...spec.key.split('.')]
    const value = file.get(at)
    if (spec.kind === 'art') return <AssetField key={spec.key} label={spec.label} value={value} context={context} kinds={['ui', 'glyph']} onChange={(next) => file.set(at, next)} />
    if (spec.kind === 'style') return <SelectField key={spec.key} label={spec.label} value={value} options={styles} onChange={(next) => file.set(at, next)} />
    if (spec.kind === 'number') return <NumberField key={spec.key} label={spec.label} value={value} onChange={(next) => file.set(at, next)} />
    return <TextField key={spec.key} label={spec.label} value={value} onChange={(next) => file.set(at, next)} />
  }
  const looks = isObject(theme.button) && isObject(theme.button.looks) ? Object.keys(theme.button.looks) : []
  return (
    <>
      {CONTROLS.map((control) => (
        <details key={control.key} className="section group" open={control.key === 'button'}>
          <summary className="eyebrow">{control.label}</summary>
          {control.fields.map((spec) => field(control.key, spec))}
          {control.key === 'button' && (
            <>
              {looks.map((look) => (
                <div key={look} className="slot-item" style={{ marginTop: 10 }}>
                  <div className="list-row">
                    <span className="slot-at">look · {look}</span>
                    <button className="icon-btn" title="Remover look" onClick={() => file.set([name, 'button', 'looks', look], undefined)}>
                      <Trash2 />
                    </button>
                  </div>
                  {LOOK_FIELDS.map((spec) => field('button', { ...spec, key: `looks.${look}.${spec.key}` }))}
                </div>
              ))}
              <button
                className="add-row"
                style={{ marginTop: 10, width: '100%' }}
                onClick={() => {
                  const look = askName('Nome do visual (ex.: light, danger):', looks)
                  if (look) file.set([name, 'button', 'looks', look], { art: `ui:${name}/button_${look}`, 'text-style': styles[0] ?? 'body' })
                }}
              >
                <Plus /> Visual (look)
              </button>
            </>
          )}
        </details>
      ))}
    </>
  )
}

export function ThemesPage() {
  const [paths, setPaths] = useState<string[]>([])
  const [path, setPath] = useState<string | null>(null)
  const [themeName, setThemeName] = useState<string | null>(null)
  const [tab, setTab] = useState<Tab>('Texto')
  const [view, setView] = useView()
  const [catalog, setCatalog] = useState<AssetCatalog | null>(null)
  const [preview, setPreview] = useState<Preview | null>(null)
  const [busy, setBusy] = useState(false)
  const [version, setVersion] = useState(0)
  const file = useYamlFile(path)
  useFileShortcuts(file)

  useEffect(() => {
    filesApi
      .list(FOLDER)
      .then((list) => {
        setPaths(list)
        setPath((current) => current ?? list[0] ?? null)
      })
      .catch(() => setPaths([]))
  }, [])
  useEffect(() => {
    assetApi.catalog().then(setCatalog).catch(() => setCatalog(null))
  }, [version])

  const themes = useMemo(() => {
    const json = file.doc?.toJSON() as unknown
    return isObject(json) ? Object.entries(json).filter(([, value]) => isObject(value)).map(([key, value]) => ({ key, value: value as Json })) : []
  }, [file.doc])
  const current = themes.find((theme) => theme.key === themeName) ?? themes[0] ?? null
  const styles = current && isObject(current.value.text) ? Object.keys(current.value.text) : []

  useEffect(() => {
    if (!current || !path || file.error) return
    const controller = new AbortController()
    setBusy(true)
    const timer = window.setTimeout(() => {
      api
        .preview(
          {
            id: `tema-${current.key}`,
            json: themeSample(current.key, current.value),
            page: null,
            monitorWidth: { '1080p': 1920, '1440p': 2560, '4k': 3840 }[view.monitor],
            monitorHeight: { '1080p': 1080, '1440p': 1440, '4k': 2160 }[view.monitor],
            scale: view.scale,
            themeFiles: { [path.split('/').pop() ?? 'tema.yml']: file.text },
          },
          controller.signal,
        )
        .then((result) => {
          setPreview(result)
          setBusy(false)
        })
        .catch(() => !controller.signal.aborted && setBusy(false))
    }, 200)
    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [file.text, current?.key, path, view, version])

  const context: FieldContext = { catalog, theme: current?.key ?? null, pages: [] }
  const panel: PanelProps | null = current ? { file, name: current.key, theme: current.value, context } : null

  return (
    <SectionShell
      section="temas"
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
          <button className={`btn primary${file.dirty ? '' : ' done'}`} onClick={() => void file.save().then((ok) => ok && setVersion((value) => value + 1))} disabled={file.saving || !path} title="Ctrl+S">
            {file.dirty ? <Save /> : <Check />} {file.saving ? 'Salvando…' : file.dirty ? 'Salvar' : 'Salvo'}
          </button>
        </>
      }
    >
      <aside className="components">
        <div className="panel-head">
          <span className="eyebrow">Temas</span>
        </div>
        <div className="tree">
          {paths.map((item) =>
            item === path ? (
              themes.map((theme) => (
                <div key={theme.key} className={`tree-row${theme.key === current?.key ? ' selected' : ''}`} style={{ paddingLeft: 10 }} onClick={() => setThemeName(theme.key)}>
                  <span className="name">{theme.key}</span>
                  <span className="detail">{item.replace(`${FOLDER}/`, '')}</span>
                </div>
              ))
            ) : (
              <div key={item} className="tree-row" style={{ paddingLeft: 10 }} onClick={() => (!file.dirty || window.confirm('Trocar sem salvar?')) && setPath(item)}>
                <span className="detail">{item.replace(`${FOLDER}/`, '')}</span>
              </div>
            ),
          )}
        </div>
      </aside>

      <Viewport
        view={view}
        onView={setView}
        png={preview?.png ?? null}
        appliedScale={preview?.scale ?? null}
        guiSize={preview ? `${preview.guiWidth}×${preview.guiHeight}` : null}
        busy={busy}
        error={file.error ? `YAML inválido: ${file.error}` : (preview?.error ?? null)}
        foot={<span>Amostra gerada com tudo que o tema declara · as mudanças aparecem antes de salvar</span>}
      />

      <aside className="inspector">
        <div className="tab-strip">
          {TABS.map((item) => (
            <button key={item} className={item === tab ? 'on' : ''} onClick={() => setTab(item)}>
              {item}
            </button>
          ))}
        </div>
        {panel && tab === 'Texto' && <TextPanel {...panel} />}
        {panel && tab === 'Superfícies' && (
          <div className="section">
            <span className="eyebrow">Superfícies (fundo dos containers)</span>
            <MapField file={file} at={[panel.name, 'surfaces']} value={panel.theme.surfaces} label="Superfície" kinds={['ui']} context={context} />
          </div>
        )}
        {panel && tab === 'Controles' && <ControlsPanel {...panel} styles={styles} />}
        {panel && tab === 'Ícones' && (
          <div className="section">
            <span className="eyebrow">Ícones do tema</span>
            <div className="grid2">
              <NumberField label="Tamanho" value={panel.theme['icon-size']} placeholder="8" onChange={(next) => file.set([panel.name, 'icon-size'], next)} />
              <NumberField label="Espaço ícone→texto" value={panel.theme['icon-gap']} placeholder="3" onChange={(next) => file.set([panel.name, 'icon-gap'], next)} />
            </div>
            <MapField file={file} at={[panel.name, 'icons']} value={panel.theme.icons} label="Ícone" kinds={['ui', 'glyph', 'item']} context={context} />
          </div>
        )}
        {panel && tab === 'Artes' && (
          <div className="section">
            <span className="eyebrow">Artes em ui/{panel.name}</span>
            <ArtsPanel arts={catalog?.ui ?? []} theme={panel.name} version={version} onChanged={() => setVersion((value) => value + 1)} />
          </div>
        )}
        {(file.saveError || (preview?.warnings.length ?? 0) > 0) && (
          <div className="section">
            {file.saveError && <div className="notice bad">{file.saveError}</div>}
            {preview && preview.warnings.length > 0 && (
              <div className="notice warn">
                {preview.warnings.length} aviso(s)
                <ul>
                  {preview.warnings.slice(0, 12).map((warning) => (
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
