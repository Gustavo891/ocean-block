import type { ReactNode } from 'react'
import { ArrowDown, ArrowUp, CheckCircle2, CircleAlert, Copy, Trash2, TriangleAlert } from 'lucide-react'
import {
  AssetField,
  BoolField,
  ClickField,
  ColorField,
  FlagField,
  GridSizeField,
  ListField,
  NumberField,
  PairField,
  SelectField,
  SizeField,
  SlotItemsField,
  StatesField,
  TextField,
  type FieldContext,
} from './fields'
import { SamplesPanel, type Samples } from './SamplesPanel'
import { KIND_LABEL, getAt, isContainer, isObject, type Json, type JsonPath, type TreeNode } from './tree'

export type SaveState = { kind: 'idle' } | { kind: 'saving' } | { kind: 'saved'; file: string } | { kind: 'error'; message: string }
export type NodeAction = 'duplicate' | 'delete' | 'up' | 'down'

interface InspectorProps {
  doc: Json | null
  selected: TreeNode | null
  context: FieldContext
  warnings: string[]
  dirty: boolean
  save: SaveState
  lists: Record<string, number>
  samples: Samples
  onSamples: (samples: Samples) => void
  onChange: (path: JsonPath, value: unknown) => void
  onAction: (action: NodeAction) => void
}

const ALIGN = ['start', 'center', 'end']

function nodeFields(node: TreeNode, obj: Json, set: (key: string, value: unknown) => void, context: FieldContext): ReactNode {
  const theme = context.theme ? context.catalog?.themes[context.theme] : undefined
  const styles = theme?.styles ?? []
  const icon = (label = 'Ícone') => <AssetField label={label} value={obj.icon} onChange={(value) => set('icon', value)} context={context} kinds={['theme', 'ui', 'glyph', 'item']} />
  const tooltip = <TextField label="Tooltip" long value={obj.tooltip} placeholder={'Linha 1\nLinha 2'} onChange={(value) => set('tooltip', value)} />
  const width = <SizeField label="Largura" value={obj.w} onChange={(value) => set('w', value)} />
  const bind = <TextField label="Bind (código do plugin)" value={obj.bind} onChange={(value) => set('bind', value)} />

  if (isContainer(node.kind)) {
    return (
      <>
        {node.kind === 'grid' && <NumberField label="Colunas" value={obj.columns ?? 1} onChange={(value) => set('columns', value)} />}
        <div className="grid2">
          {width}
          <SizeField label="Altura" value={obj.h} onChange={(value) => set('h', value)} />
        </div>
        <div className="grid2">
          <NumberField label="Espaço" value={obj.gap ?? 0} onChange={(value) => set('gap', value)} />
          <NumberField label="Padding" value={obj.pad ?? 0} onChange={(value) => set('pad', value)} />
        </div>
        <div className="grid2">
          <SelectField label="Alinhar" value={obj.align} options={ALIGN} onChange={(value) => set('align', value)} />
          <SelectField label="Cruzado" value={obj.cross} options={ALIGN} onChange={(value) => set('cross', value)} />
        </div>
        <SelectField label="Superfície" value={obj.surface} options={theme?.surfaces ?? []} empty="sem fundo" onChange={(value) => set('surface', value)} />
        <NumberField label="Snap (alinha linhas a N px)" value={obj.snap} onChange={(value) => set('snap', value)} />
      </>
    )
  }

  switch (node.kind) {
    case 'text':
      return (
        <>
          <TextField label="Texto" value={obj.text} onChange={(value) => set('text', value ?? '')} />
          <SelectField label="Estilo" value={obj.style} options={styles} onChange={(value) => set('style', value)} />
          <div className="grid2">
            <SelectField label="Alinhar" value={obj.align} options={['left', 'center', 'right']} onChange={(value) => set('align', value)} />
            {width}
          </div>
          {icon()}
          <ColorField label="Cor" value={obj.color} onChange={(value) => set('color', value)} />
          <BoolField label="Negrito" value={obj.bold} onChange={(value) => set('bold', value)} />
          {tooltip}
        </>
      )
    case 'button':
      return (
        <>
          <TextField label="Texto" value={obj.button} onChange={(value) => set('button', value ?? '')} />
          {icon()}
          <SelectField label="Visual" value={obj.look} options={theme?.looks ?? []} onChange={(value) => set('look', value)} />
          {width}
          <ClickField value={obj.onClick} context={context} onChange={(value) => set('onClick', value)} />
          {bind}
          <FlagField label="Selecionado" value={obj.selected} onChange={(value) => set('selected', value)} />
          <FlagField label="Desativado" value={obj.disabled} onChange={(value) => set('disabled', value)} />
          {tooltip}
        </>
      )
    case 'nav':
      return (
        <>
          <TextField label="Texto" value={obj.nav} onChange={(value) => set('nav', value ?? '')} />
          {icon()}
          {width}
          <ClickField value={obj.onClick} context={context} onChange={(value) => set('onClick', value)} />
          {bind}
          <FlagField label="Ativo" value={obj.selected} onChange={(value) => set('selected', value)} />
          {tooltip}
        </>
      )
    case 'chip':
      return (
        <>
          <TextField label="Texto" value={obj.chip} onChange={(value) => set('chip', value ?? '')} />
          {icon()}
          {tooltip}
        </>
      )
    case 'pair':
      return (
        <>
          <PairField value={obj.pair} onChange={(value) => set('pair', value)} />
          <SelectField label="Estilo" value={obj.style} options={styles} onChange={(value) => set('style', value)} />
          {width}
        </>
      )
    case 'icon':
      return (
        <>
          {icon()}
          <NumberField label="Tamanho (px)" value={obj.size} placeholder={String(theme?.iconSize ?? 8)} onChange={(value) => set('size', value)} />
          <TextField label="Bind (troca o ícone)" value={obj.bind} onChange={(value) => set('bind', value)} />
          {typeof obj.bind === 'string' && <StatesField value={obj.states} context={context} onChange={(value) => set('states', value)} />}
        </>
      )
    case 'image':
      return (
        <>
          <AssetField label="Imagem" value={obj.image} onChange={(value) => set('image', value ?? '')} context={context} kinds={['ui', 'glyph']} />
          {width}
        </>
      )
    case 'space':
      return <SizeField label="Tamanho" value={obj.space === 'grow' ? 'grow' : obj.space} onChange={(value) => set('space', value ?? 0)} />
    case 'switch':
      return (
        <>
          <BoolField label="Ligado por padrão" value={obj.switch} onChange={(value) => set('switch', value === true)} />
          {bind}
          {tooltip}
        </>
      )
    case 'segmented':
      return (
        <>
          <ListField label="Opções" value={obj.segmented} onChange={(value) => set('segmented', value)} />
          <NumberField label="Selecionada (índice)" value={obj.selected ?? 0} onChange={(value) => set('selected', value)} />
          {width}
          {bind}
          {tooltip}
        </>
      )
    case 'dropdown':
      return (
        <>
          <ListField label="Opções" value={obj.options} onChange={(value) => set('options', value)} />
          <SelectField label="Inicial" value={obj.dropdown} options={Array.isArray(obj.options) ? obj.options.map(String) : []} onChange={(value) => set('dropdown', value ?? '')} />
          {width}
          {bind}
          {tooltip}
        </>
      )
    case 'slider':
      return (
        <>
          <div className="grid2">
            <NumberField label="Valor" value={obj.slider} onChange={(value) => set('slider', value)} />
            <TextField label="Unidade" value={obj.unit} onChange={(value) => set('unit', value)} />
            <NumberField label="Mín" value={obj.min} placeholder="0" onChange={(value) => set('min', value)} />
            <NumberField label="Máx" value={obj.max} placeholder="100" onChange={(value) => set('max', value)} />
          </div>
          <NumberField label="Passo" value={obj.step} placeholder="1" onChange={(value) => set('step', value)} />
          {width}
          {bind}
          {tooltip}
        </>
      )
    case 'slots':
      return (
        <>
          <GridSizeField value={obj.slots} onChange={(value) => set('slots', value)} />
          <SelectField label="Alinhar" value={obj.align} options={ALIGN} onChange={(value) => set('align', value)} />
          <SlotItemsField value={obj.items} context={context} onChange={(value) => set('items', value)} />
        </>
      )
    default:
      return <div className="notice warn">Peça desconhecida: edite pelo JSON.</div>
  }
}

function ScreenFields({ doc, themes, onChange }: { doc: Json; themes: string[]; onChange: InspectorProps['onChange'] }) {
  const themeAt: JsonPath = 'theme' in doc || !isObject(doc.document) ? ['theme'] : ['document', 'theme']
  const hideFooter = doc.hideFooter === true
  return (
    <>
      <TextField label="Título" value={doc.title} placeholder="sem título" onChange={(value) => onChange(['title'], value)} />
      <SelectField label="Tema" value={getAt(doc, themeAt)} options={themes} empty="—" onChange={(value) => onChange(themeAt, value)} />
      {!hideFooter && <TextField label="Botão do rodapé" value={doc.close} placeholder="Fechar" onChange={(value) => onChange(['close'], value)} />}
      <BoolField label="Sem rodapé" value={doc.hideFooter} onChange={(value) => onChange(['hideFooter'], value)} />
      <BoolField label="Fecha com Esc" value={doc.canCloseWithEscape !== false} onChange={(value) => onChange(['canCloseWithEscape'], value === true ? undefined : false)} />
      <BoolField label="Pausa o jogo (singleplayer)" value={doc.pause} onChange={(value) => onChange(['pause'], value)} />
    </>
  )
}

function Status({ dirty, save, warnings }: Pick<InspectorProps, 'dirty' | 'save' | 'warnings'>) {
  return (
    <div className="section">
      {save.kind === 'error' ? (
        <div className="notice bad">
          <CircleAlert size={14} /> {save.message}
        </div>
      ) : dirty ? (
        <div className="notice warn">
          Alterações não salvas
          <small style={{ color: '#bfa775' }}>Ctrl+S grava em configs/ui no servidor</small>
        </div>
      ) : save.kind === 'saved' ? (
        <div className="notice">
          <CheckCircle2 size={14} /> Salvo em {save.file}
          <small>Dê /textura reload no jogo</small>
        </div>
      ) : null}
      {warnings.length > 0 && (
        <div className="notice warn">
          <TriangleAlert size={14} /> {warnings.length} aviso{warnings.length > 1 ? 's' : ''} do Textura
          <ul>
            {warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

export function Inspector(props: InspectorProps) {
  const { doc, selected, context } = props
  const obj = selected && selected.kind !== 'screen' && doc ? getAt(doc, selected.path) : null
  const isRoot = selected ? typeof selected.path[selected.path.length - 1] !== 'number' : true
  const set = (key: string, value: unknown) => selected && props.onChange([...selected.path, key], value)
  const themes = Object.keys(context.catalog?.themes ?? {})

  return (
    <aside className="inspector">
      <div className="section">
        <span className="eyebrow">{isObject(obj) && selected ? KIND_LABEL[selected.kind] : 'Tela'}</span>
        <div className="inspector-head">
          <h3>{isObject(obj) && selected ? selected.label : 'Screen'}</h3>
          {isObject(obj) && !isRoot && (
            <div className="head-actions">
              <button className="icon-btn" title="Subir (Alt+↑)" onClick={() => props.onAction('up')}>
                <ArrowUp />
              </button>
              <button className="icon-btn" title="Descer (Alt+↓)" onClick={() => props.onAction('down')}>
                <ArrowDown />
              </button>
              <button className="icon-btn" title="Duplicar (Ctrl+D)" onClick={() => props.onAction('duplicate')}>
                <Copy />
              </button>
              <button className="icon-btn danger" title="Apagar (Delete)" onClick={() => props.onAction('delete')}>
                <Trash2 />
              </button>
            </div>
          )}
        </div>
        {!doc && <div className="notice bad">Corrija o JSON para editar as propriedades.</div>}
        {doc && !isObject(obj) && <ScreenFields doc={doc} themes={themes} onChange={props.onChange} />}
        {isObject(obj) && selected && (
          <>
            <TextField label="Nome na árvore" value={obj.name} placeholder={KIND_LABEL[selected.kind]} onChange={(value) => set('name', value)} />
            {nodeFields(selected, obj, set, context)}
          </>
        )}
      </div>

      {isObject(obj) && selected && !isRoot && (
        <div className="section">
          <span className="eyebrow">Repetir e posicionar</span>
          <div className="grid2">
            <TextField label="Repetir lista" value={obj.repeat} placeholder="nenhuma" onChange={(value) => set('repeat', value)} />
            <NumberField label="Por página" value={obj.perPage} onChange={(value) => set('perPage', value)} />
          </div>
          <BoolField
            label="Posição livre (x/y dentro do pai)"
            value={'x' in obj || 'y' in obj}
            onChange={(value) => {
              const { x: _x, y: _y, ...rest } = obj
              props.onChange(selected.path, value ? { ...rest, x: 0, y: 0 } : rest)
            }}
          />
          {('x' in obj || 'y' in obj) && (
            <div className="grid2">
              <NumberField label="X" value={obj.x ?? 0} onChange={(value) => set('x', value ?? 0)} />
              <NumberField label="Y" value={obj.y ?? 0} onChange={(value) => set('y', value ?? 0)} />
            </div>
          )}
        </div>
      )}

      {doc && !isObject(obj) && <SamplesPanel doc={doc} lists={props.lists} samples={props.samples} onChange={props.onSamples} />}

      <Status dirty={props.dirty} save={props.save} warnings={props.warnings} />
    </aside>
  )
}
