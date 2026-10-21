import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Maximize, Monitor, ZoomIn, ZoomOut } from 'lucide-react'
import { MONITORS, type MonitorId } from '../editor/Stage'

const SCALES = [0, 1, 2, 3, 4]
const ZOOMS = [0.5, 0.75, 1, 1.25, 1.5, 2, 3]
const PADDING = 56

export interface ViewState {
  monitor: MonitorId
  scale: number
}

interface ViewportProps {
  view: ViewState
  onView: (view: ViewState) => void
  png: string | null
  /** Escala de GUI que o servidor usou (para o "Auto N"). */
  appliedScale: number | null
  guiSize: string | null
  busy: boolean
  error?: string | null
  bar?: ReactNode
  overlay?: (guiToPx: number) => ReactNode
  foot?: ReactNode
}

/** Monitor com seletor de resolução e GUI scale; a camada `overlay` recebe px por pixel de GUI. */
export function Viewport({ view, onView, png, appliedScale, guiSize, busy, error, bar, overlay, foot }: ViewportProps) {
  const ref = useRef<HTMLDivElement>(null)
  const [size, setSize] = useState({ width: 0, height: 0 })
  const [zoom, setZoom] = useState(1)

  useEffect(() => {
    const element = ref.current
    if (!element) return
    const observer = new ResizeObserver(([entry]) => setSize({ width: entry.contentRect.width, height: entry.contentRect.height }))
    observer.observe(element)
    return () => observer.disconnect()
  }, [])

  const screen = MONITORS[view.monitor]
  const fit = Math.max(0.05, Math.min((size.width - PADDING) / screen.width, (size.height - PADDING) / screen.height))
  const ratio = fit * zoom
  const guiToPx = appliedScale ? ratio * appliedScale : 0
  const step = (direction: 1 | -1) => {
    const index = ZOOMS.findIndex((value) => value >= zoom)
    setZoom(ZOOMS[Math.min(ZOOMS.length - 1, Math.max(0, (index < 0 ? ZOOMS.length - 1 : index) + direction))])
  }

  return (
    <section className="stage">
      <div className="view-bar">
        <div className="view-extra">{bar}</div>
        <div className="device">
          <div className="seg">
            {(Object.keys(MONITORS) as MonitorId[]).map((id) => (
              <button key={id} className={id === view.monitor ? 'on' : ''} onClick={() => onView({ ...view, monitor: id })}>
                {id === view.monitor && <Monitor />}
                {MONITORS[id].label}
              </button>
            ))}
          </div>
          <div className="seg">
            <span className="seg-label">GUI</span>
            {SCALES.map((value) => (
              <button key={value} className={value === view.scale ? 'on' : ''} onClick={() => onView({ ...view, scale: value })}>
                {value === 0 ? `Auto${view.scale === 0 && appliedScale ? ` ${appliedScale}` : ''}` : value}
              </button>
            ))}
          </div>
          <span className="meta">{guiSize ?? '—'}</span>
        </div>
      </div>
      <div className="canvas-wrap">
        <div className="canvas" ref={ref}>
          {error ? (
            <div className="stage-error">{error}</div>
          ) : png ? (
            <div className={`monitor${busy ? ' busy' : ''}`} style={{ width: Math.round(screen.width * ratio), height: Math.round(screen.height * ratio) }}>
              <img src={`data:image/png;base64,${png}`} alt="Preview" draggable={false} />
              {overlay?.(guiToPx)}
            </div>
          ) : (
            <div className="empty">{busy ? 'Montando preview…' : 'Sem preview'}</div>
          )}
        </div>
        <div className="canvas-tools">
          <button title="Afastar" onClick={() => step(-1)}>
            <ZoomOut />
          </button>
          <button className="zoom-value" title="Caber na tela" onClick={() => setZoom(1)}>
            {Math.round(zoom * 100)}%
          </button>
          <button title="Aproximar" onClick={() => step(1)}>
            <ZoomIn />
          </button>
          <button title="Caber na tela" className={zoom === 1 ? 'on' : ''} onClick={() => setZoom(1)}>
            <Maximize />
          </button>
        </div>
      </div>
      {foot && <div className="stage-foot">{foot}</div>}
    </section>
  )
}

/** Monitor e GUI scale lembrados entre as seções. */
export function useView(): [ViewState, (view: ViewState) => void] {
  const [view, setView] = useState<ViewState>(() => {
    try {
      const raw = window.localStorage.getItem('textura-studio:view')
      return raw ? (JSON.parse(raw) as ViewState) : { monitor: '1080p', scale: 0 }
    } catch {
      return { monitor: '1080p', scale: 0 }
    }
  })
  const update = (next: ViewState) => {
    setView(next)
    try {
      window.localStorage.setItem('textura-studio:view', JSON.stringify(next))
    } catch {
      // sem armazenamento: vale só nesta aba
    }
  }
  return [view, update]
}
