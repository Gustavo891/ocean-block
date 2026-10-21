import { RotateCcw } from 'lucide-react'
import { isObject, type Json } from './tree'

export interface Samples {
  lists: Record<string, Record<string, string>[]>
  values: Record<string, string>
}

export const EMPTY_SAMPLES: Samples = { lists: {}, values: {} }

const TOKEN = /\{([A-Za-z0-9_.-]+)/g
const BUILTIN = new Set(['player', 'player_uuid', 'online', 'page'])

function tokens(value: unknown): Set<string> {
  const found = new Set<string>()
  for (const match of JSON.stringify(value).matchAll(TOKEN)) found.add(match[1])
  return found
}

/** Listas repetidas e os campos `{campo}` usados dentro de cada uma. */
export function listFields(doc: Json): Map<string, string[]> {
  const result = new Map<string, Set<string>>()
  const visit = (value: unknown) => {
    if (Array.isArray(value)) return value.forEach(visit)
    if (!isObject(value)) return
    if (typeof value.repeat === 'string') {
      const fields = result.get(value.repeat) ?? new Set<string>()
      tokens(value).forEach((token) => fields.add(token))
      result.set(value.repeat, fields)
    }
    Object.values(value).forEach(visit)
  }
  visit(doc)
  return new Map([...result].map(([list, fields]) => [list, [...fields].filter((field) => !BUILTIN.has(field)).sort()]))
}

function screenValues(doc: Json, lists: Map<string, string[]>): string[] {
  const inLists = new Set([...lists.values()].flat())
  return [...tokens(doc)].filter((token) => !inLists.has(token) && !BUILTIN.has(token)).sort()
}

function sampleEntry(fields: string[], index: number): Record<string, string> {
  return Object.fromEntries(fields.map((field) => [field, `${field} ${index + 1}`]))
}

interface SamplesPanelProps {
  doc: Json
  lists: Record<string, number>
  samples: Samples
  onChange: (samples: Samples) => void
}

export function SamplesPanel({ doc, lists, samples, onChange }: SamplesPanelProps) {
  const fields = listFields(doc)
  const names = [...new Set([...Object.keys(lists), ...fields.keys()])].sort()
  const values = screenValues(doc, fields)
  if (names.length === 0 && values.length === 0) return null

  const setList = (list: string, entries: Record<string, string>[] | undefined) => {
    const next = { ...samples.lists }
    if (entries) next[list] = entries
    else delete next[list]
    onChange({ ...samples, lists: next })
  }

  return (
    <div className="section">
      <span className="eyebrow">Dados de exemplo</span>
      <p className="hint-text">Só para o preview: simulam o que o plugin manda. Ficam neste navegador, não vão para o arquivo.</p>
      {names.map((list) => {
        const listFieldNames = fields.get(list) ?? []
        const custom = samples.lists[list]
        const count = custom?.length ?? lists[list] ?? 0
        return (
          <div key={list} className="sample-list">
            <div className="sample-head">
              <b>↻ {list}</b>
              <input
                type="number"
                min={0}
                max={64}
                value={count}
                title="Quantidade de entradas"
                onChange={(event) => {
                  const size = Math.max(0, Math.min(64, Number(event.target.value) || 0))
                  const base = custom ?? []
                  setList(list, Array.from({ length: size }, (_, index) => base[index] ?? sampleEntry(listFieldNames, index)))
                }}
              />
              {custom && (
                <button className="icon-btn" title="Voltar ao padrão ({campo} literal)" onClick={() => setList(list, undefined)}>
                  <RotateCcw />
                </button>
              )}
            </div>
            {custom?.map((entry, index) => (
              <details key={index} className="sample-entry" open={index === 0}>
                <summary>
                  #{index + 1} <span>{Object.values(entry)[0] ?? ''}</span>
                </summary>
                {listFieldNames.map((field) => (
                  <div key={field} className="field compact">
                    <label>{field}</label>
                    <input
                      type="text"
                      value={entry[field] ?? ''}
                      onChange={(event) => setList(list, custom.map((old, at) => (at === index ? { ...old, [field]: event.target.value } : old)))}
                    />
                  </div>
                ))}
              </details>
            ))}
            {!custom && <span className="hint-text">Mostrando {count} entradas com {'{campo}'} literal. Mude a quantidade para editar valores.</span>}
          </div>
        )
      })}
      {values.length > 0 && (
        <div className="sample-list">
          <div className="sample-head">
            <b>Valores da tela</b>
          </div>
          {values.map((key) => (
            <div key={key} className="field compact">
              <label>{key}</label>
              <input
                type="text"
                value={samples.values[key] ?? ''}
                placeholder={`{${key}}`}
                onChange={(event) => {
                  const next = { ...samples.values }
                  if (event.target.value) next[key] = event.target.value
                  else delete next[key]
                  onChange({ ...samples, values: next })
                }}
              />
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
