import { useEffect, useState } from 'react'
import { Save, Trash2, Upload } from 'lucide-react'
import { filesApi } from '../files'

const UI_ROOT = 'resource/assets/oneblock/textures/ui'

function pngOf(art: string): string {
  return `${UI_ROOT}/${art.replace(/^ui:/, '')}.png`
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

/** Uma arte do tema com as tampas do `.slice` (esquerda direita [topo base]). */
function ArtEditor({ art, version, onChanged }: { art: string; version: number; onChanged: () => void }) {
  const png = pngOf(art)
  const [slice, setSlice] = useState<number[] | null>(null)
  const [saved, setSaved] = useState<string | null>(null)
  const [size, setSize] = useState<{ w: number; h: number } | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    filesApi
      .read(`${png}.slice`)
      .then((text) => {
        setSaved(text?.trim() ?? '')
        setSlice(text ? text.trim().split(/\s+/).map(Number) : null)
      })
      .catch(() => setSlice(null))
  }, [png, version])

  const text = slice ? slice.join(' ') : ''
  const nine = (slice?.length ?? 0) === 4
  const set = (index: number, value: number) => setSlice((current) => (current ? current.map((old, at) => (at === index ? Math.max(0, value) : old)) : current))
  const zoom = size ? Math.max(1, Math.min(8, Math.floor(200 / Math.max(size.w, size.h)))) : 4

  return (
    <div className="art-editor">
      <div className="art-view" style={size ? { width: size.w * zoom, height: size.h * zoom } : undefined}>
        <img
          src={filesApi.rawUrl(png, version)}
          alt=""
          style={size ? { width: size.w * zoom, height: size.h * zoom } : undefined}
          onLoad={(event) => setSize({ w: event.currentTarget.naturalWidth, h: event.currentTarget.naturalHeight })}
        />
        {slice && size && (
          <>
            <i className="slice-v" style={{ left: slice[0] * zoom }} />
            <i className="slice-v" style={{ left: (size.w - slice[1]) * zoom }} />
            {nine && <i className="slice-h" style={{ top: slice[2] * zoom }} />}
            {nine && <i className="slice-h" style={{ top: (size.h - slice[3]) * zoom }} />}
          </>
        )}
      </div>
      <div className="list-row" style={{ marginTop: 8 }}>
        <span className="slot-at">
          {art} {size && `· ${size.w}×${size.h}`}
        </span>
        <button
          className="icon-btn"
          title="Trocar PNG"
          onClick={async () => {
            const file = await pickPng()
            if (!file) return
            try {
              await filesApi.upload(png, file)
              onChanged()
            } catch (cause: unknown) {
              setError(cause instanceof Error ? cause.message : 'Falha no upload')
            }
          }}
        >
          <Upload />
        </button>
      </div>
      {slice ? (
        <>
          <div className="grid4">
            {['Esq.', 'Dir.', ...(nine ? ['Topo', 'Base'] : [])].map((label, index) => (
              <div key={label} className="field compact">
                <label>{label}</label>
                <input type="number" min={0} value={slice[index] ?? 0} onChange={(event) => set(index, Number(event.target.value))} />
              </div>
            ))}
          </div>
          <div className="list-row" style={{ marginTop: 8 }}>
            <button className="add-row" style={{ flex: 1 }} onClick={() => setSlice(nine ? slice.slice(0, 2) : [...slice, 0, 0])}>
              {nine ? 'Só horizontal' : '9-slice (topo/base)'}
            </button>
            <button
              className="icon-btn"
              title="Salvar .slice"
              disabled={text === saved}
              onClick={async () => {
                try {
                  await filesApi.write(`${png}.slice`, `${text}\n`)
                  setSaved(text)
                  onChanged()
                } catch (cause: unknown) {
                  setError(cause instanceof Error ? cause.message : 'Falha ao salvar')
                }
              }}
            >
              <Save />
            </button>
            <button
              className="icon-btn danger"
              title="Remover .slice (vira imagem fixa)"
              onClick={async () => {
                await filesApi.remove(`${png}.slice`).catch(() => undefined)
                setSlice(null)
                setSaved('')
                onChanged()
              }}
            >
              <Trash2 />
            </button>
          </div>
        </>
      ) : (
        <button className="add-row" style={{ marginTop: 8, width: '100%' }} onClick={() => setSlice([2, 2])}>
          Adicionar fatias (.slice)
        </button>
      )}
      {error && <div className="notice bad">{error}</div>}
    </div>
  )
}

export function ArtsPanel({ arts, theme, onChanged, version }: { arts: string[]; theme: string; onChanged: () => void; version: number }) {
  const [selected, setSelected] = useState<string | null>(null)
  const own = arts.filter((art) => art.startsWith(`ui:${theme}/`))
  const list = own.length ? own : arts
  return (
    <>
      <div className="art-grid">
        {list.map((art) => (
          <button key={art} className={`glyph-card${art === selected ? ' on' : ''}`} onClick={() => setSelected(art)} title={art}>
            <span className="glyph-thumb">
              <img src={filesApi.rawUrl(pngOf(art), version)} alt="" />
            </span>
            <small>{art.split('/').pop()}</small>
          </button>
        ))}
      </div>
      {selected && <ArtEditor key={selected} art={selected} version={version} onChanged={onChanged} />}
      <p className="hint-text">
        Fatias: 2 números esticam na horizontal (botões), 4 fazem 9-slice (superfícies). Mudou arte ou fatia? <code>/textura gerar</code>.
      </p>
    </>
  )
}
