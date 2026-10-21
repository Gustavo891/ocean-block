import { useId, type ReactNode } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { assetApi, type AssetCatalog } from '../api'
import { isObject } from './tree'

export interface FieldContext {
  catalog: AssetCatalog | null
  theme: string | null
  pages: string[]
}

interface Props<T> {
  label: string
  value: T
  onChange: (value: unknown) => void
}

function Shell({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <div className="field">
      <label>{label}</label>
      {children}
      {hint && <span className="hint">{hint}</span>}
    </div>
  )
}

const str = (value: unknown): string => (value === undefined || value === null ? '' : typeof value === 'string' ? value : JSON.stringify(value))

export function TextField({ label, value, onChange, placeholder, long }: Props<unknown> & { placeholder?: string; long?: boolean }) {
  return (
    <Shell label={label}>
      {long ? (
        <textarea value={str(value)} placeholder={placeholder} onChange={(event) => onChange(event.target.value || undefined)} />
      ) : (
        <input type="text" value={str(value)} placeholder={placeholder} onChange={(event) => onChange(event.target.value || undefined)} />
      )}
    </Shell>
  )
}

export function NumberField({ label, value, onChange, placeholder }: Props<unknown> & { placeholder?: string }) {
  return (
    <Shell label={label}>
      <input
        type="number"
        value={typeof value === 'number' ? value : ''}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value === '' ? undefined : Number(event.target.value))}
      />
    </Shell>
  )
}

export function SizeField({ label, value, onChange }: Props<unknown>) {
  const mode = typeof value === 'number' ? 'px' : value === 'grow' ? 'grow' : 'fit'
  return (
    <Shell label={label}>
      <div className="size-field">
        <div className="seg">
          {(['fit', 'grow', 'px'] as const).map((option) => (
            <button
              key={option}
              className={mode === option ? 'on' : ''}
              onClick={() => onChange(option === 'px' ? (typeof value === 'number' ? value : 60) : option === 'fit' ? undefined : 'grow')}
            >
              {option === 'fit' ? 'Fit' : option === 'grow' ? 'Grow' : 'Px'}
            </button>
          ))}
        </div>
        {mode === 'px' && (
          <input type="number" min={0} value={value as number} onChange={(event) => onChange(Math.max(0, Number(event.target.value) || 0))} />
        )}
      </div>
    </Shell>
  )
}

export function SelectField({ label, value, onChange, options, empty = 'padrão' }: Props<unknown> & { options: string[]; empty?: string }) {
  const current = str(value)
  return (
    <Shell label={label}>
      <select value={current} onChange={(event) => onChange(event.target.value || undefined)}>
        <option value="">{empty}</option>
        {current && !options.includes(current) && <option value={current}>{current}</option>}
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </Shell>
  )
}

export function BoolField({ label, value, onChange }: Props<unknown>) {
  return (
    <label className="check">
      <input type="checkbox" checked={value === true} onChange={(event) => onChange(event.target.checked ? true : undefined)} />
      {label}
    </label>
  )
}

/** true/false ou um {placeholder} da lista. */
export function FlagField({ label, value, onChange }: Props<unknown>) {
  const bound = typeof value === 'string'
  return (
    <Shell label={label}>
      <div className="size-field">
        <div className="seg">
          <button className={!bound && value !== true ? 'on' : ''} onClick={() => onChange(undefined)}>
            Não
          </button>
          <button className={value === true ? 'on' : ''} onClick={() => onChange(true)}>
            Sim
          </button>
          <button className={bound ? 'on' : ''} onClick={() => onChange(bound ? value : '{ativo}')}>
            {'{}'}
          </button>
        </div>
        {bound && <input type="text" value={value} onChange={(event) => onChange(event.target.value)} />}
      </div>
    </Shell>
  )
}

export function ColorField({ label, value, onChange }: Props<unknown>) {
  const hex = typeof value === 'string' && /^#[0-9a-f]{6}$/i.test(value) ? value : '#ffffff'
  return (
    <Shell label={label}>
      <div className="color-field">
        <input type="color" value={hex} onChange={(event) => onChange(event.target.value)} />
        <input type="text" value={str(value)} placeholder="do estilo" onChange={(event) => onChange(event.target.value || undefined)} />
      </div>
    </Shell>
  )
}

export function AssetField({ label, value, onChange, context, kinds }: Props<unknown> & { context: FieldContext; kinds: ('theme' | 'ui' | 'glyph' | 'item')[] }) {
  const list = useId()
  const catalog = context.catalog
  const theme = context.theme ? catalog?.themes[context.theme] : undefined
  const options = [
    ...(kinds.includes('theme') ? (theme?.icons ?? []).map((icon) => `ui:${icon}`) : []),
    ...(kinds.includes('ui') ? (catalog?.ui ?? []) : []),
    ...(kinds.includes('glyph') ? (catalog?.glyphs ?? []) : []),
    ...(kinds.includes('item') ? (catalog?.items ?? []).map((item) => `item:${item}`) : []),
  ]
  const current = str(value)
  const previewable = current && !current.includes('{')
  return (
    <Shell label={label} hint={kinds.includes('item') ? 'ui:<ícone>, item:<nome>, glyph:<id> ou {campo} da lista' : undefined}>
      <div className="asset-field">
        <span className="asset-thumb">{previewable && <img src={assetApi.artUrl(current, context.theme)} alt="" onError={(event) => (event.currentTarget.style.visibility = 'hidden')} onLoad={(event) => (event.currentTarget.style.visibility = 'visible')} />}</span>
        <input type="text" list={list} value={current} placeholder="nenhum" onChange={(event) => onChange(event.target.value || undefined)} />
        <datalist id={list}>
          {options.map((option) => (
            <option key={option} value={option} />
          ))}
        </datalist>
      </div>
    </Shell>
  )
}

export function ListField({ label, value, onChange, placeholder = 'Opção' }: Props<unknown> & { placeholder?: string }) {
  const items = Array.isArray(value) ? value.map(str) : []
  const set = (next: string[]) => onChange(next)
  return (
    <Shell label={label}>
      <div className="list-field">
        {items.map((item, index) => (
          <div key={index} className="list-row">
            <input type="text" value={item} onChange={(event) => set(items.map((old, at) => (at === index ? event.target.value : old)))} />
            <button className="icon-btn" onClick={() => set(items.filter((_, at) => at !== index))} title="Remover">
              <Trash2 />
            </button>
          </div>
        ))}
        <button className="add-row" onClick={() => set([...items, `${placeholder} ${items.length + 1}`])}>
          <Plus /> Adicionar
        </button>
      </div>
    </Shell>
  )
}

export function PairField({ value, onChange }: { value: unknown; onChange: (value: unknown) => void }) {
  const [left, right] = Array.isArray(value) ? [str(value[0]), str(value[1])] : ['', '']
  return (
    <div className="grid2">
      <TextField label="Rótulo" value={left} onChange={(next) => onChange([str(next), right])} />
      <TextField label="Valor" value={right} onChange={(next) => onChange([left, str(next)])} />
    </div>
  )
}

export function GridSizeField({ value, onChange }: { value: unknown; onChange: (value: unknown) => void }) {
  const [columns, rows] = Array.isArray(value) ? [Number(value[0]) || 1, Number(value[1]) || 1] : [1, 1]
  return (
    <div className="grid2">
      <NumberField label="Colunas" value={columns} onChange={(next) => onChange([Number(next) || 1, rows])} />
      <NumberField label="Linhas" value={rows} onChange={(next) => onChange([columns, Number(next) || 1])} />
    </div>
  )
}

export function SlotItemsField({ value, onChange, context }: { value: unknown; onChange: (value: unknown) => void; context: FieldContext }) {
  const items = Array.isArray(value) ? value.filter(isObject) : []
  const set = (index: number, key: string, next: unknown) =>
    onChange(items.map((item, at) => (at === index ? { ...item, [key]: next === '' ? undefined : next } : item)))
  return (
    <Shell label="Itens">
      <div className="list-field">
        {items.map((item, index) => (
          <div key={index} className="slot-item">
            <div className="list-row">
              <span className="slot-at">#{str(item.at ?? index)}</span>
              <button className="icon-btn" onClick={() => onChange(items.filter((_, at) => at !== index))} title="Remover">
                <Trash2 />
              </button>
            </div>
            <AssetField
              label="Item"
              value={typeof item.item === 'string' ? `item:${item.item}` : ''}
              onChange={(next) => set(index, 'item', str(next).replace(/^item:/, ''))}
              context={context}
              kinds={['item']}
            />
            <div className="grid2">
              <NumberField label="Slot" value={item.at ?? index} onChange={(next) => set(index, 'at', next)} />
              <TextField label="Qtd." value={item.count} onChange={(next) => set(index, 'count', /^\d+$/.test(str(next)) ? Number(next) : next)} />
            </div>
            <TextField label="Nome" value={item.name} onChange={(next) => set(index, 'name', next)} />
            <TextField label="Lore" long value={Array.isArray(item.lore) ? item.lore.join('\n') : item.lore} onChange={(next) => set(index, 'lore', next ? str(next).split('\n') : undefined)} />
          </div>
        ))}
        <button className="add-row" onClick={() => onChange([...items, { at: items.length, item: 'stone', count: 1 }])}>
          <Plus /> Item
        </button>
      </div>
    </Shell>
  )
}

export function StatesField({ value, onChange, context }: { value: unknown; onChange: (value: unknown) => void; context: FieldContext }) {
  const entries = isObject(value) ? Object.entries(value).map(([state, icon]) => [state, str(icon)] as const) : []
  const write = (next: (readonly [string, string])[]) => onChange(next.length ? Object.fromEntries(next) : undefined)
  return (
    <Shell label="Ícone por valor do bind">
      <div className="list-field">
        {entries.map(([state, icon], index) => (
          <div key={index} className="list-row">
            <input type="text" className="narrow" value={state} onChange={(event) => write(entries.map((entry, at) => (at === index ? [event.target.value, entry[1]] : entry)))} />
            <AssetField label="" value={icon} context={context} kinds={['theme', 'ui', 'glyph', 'item']} onChange={(next) => write(entries.map((entry, at) => (at === index ? [entry[0], str(next)] : entry)))} />
            <button className="icon-btn" onClick={() => write(entries.filter((_, at) => at !== index))}>
              <Trash2 />
            </button>
          </div>
        ))}
        <button className="add-row" onClick={() => write([...entries, [`valor${entries.length + 1}`, 'ui:check']])}>
          <Plus /> Estado
        </button>
      </div>
    </Shell>
  )
}

const STEPS = ['[page] ', '[open] ', '[set] chave=valor', '[message] ', '[console] ', '[player] ', '[sound] minecraft:ui.button.click 1 1', '[close]', '[refresh]']

export function ClickField({ value, onChange, context }: { value: unknown; onChange: (value: unknown) => void; context: FieldContext }) {
  const click = isObject(value) ? value : null
  const action = typeof click?.action === 'string' ? click.action : ''
  const target = str(click?.value)
  return (
    <>
      <SelectField
        label="Ao clicar"
        value={action}
        empty="Nada (só o código do plugin)"
        options={['page', 'close', 'run_command']}
        onChange={(next) => onChange(next ? { action: next, ...(next === 'close' ? {} : { value: next === 'page' ? (context.pages[0] ?? '') : '' }) } : undefined)}
      />
      {action === 'page' && <SelectField label="Página" value={target} options={context.pages} onChange={(next) => onChange({ action, value: str(next) })} />}
      {action === 'run_command' && (
        <Shell label="Ações (uma por linha)" hint="Sem tag roda como o jogador. {campo} da lista funciona aqui.">
          <textarea className="mono" value={target} onChange={(event) => onChange({ action, value: event.target.value })} />
          <div className="chips">
            {STEPS.map((step) => (
              <button key={step} onClick={() => onChange({ action, value: target ? `${target}\n${step}` : step })}>
                {step.split(' ')[0]}
              </button>
            ))}
          </div>
        </Shell>
      )}
    </>
  )
}
