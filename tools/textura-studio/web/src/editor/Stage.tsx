import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Monitor, Plus, X } from 'lucide-react'
import type { Preview } from '../api'

export type MonitorId = '1080p' | '1440p' | '4k'
export type Mode = 'edit' | 'play'

export const MONITORS: Record<MonitorId, { label: string; width: number; height: number }> = {
  '1080p': { label: '1080p', width: 1920, height: 1080 },
  '1440p': { label: '1440p', width: 2560, height: 1440 },
  '4k': { label: '4K', width: 3840, height: 2160 },
}

const SCALES = [0, 1, 2, 3, 4]
const PADDING = 56

interface StageProps {
  preview: Preview | null
  busy: boolean
  mode: Mode
  monitor: MonitorId
  scale: number
  zoom: number
  showHits: boolean
  tools: ReactNode
  pages: string[]
  page: string | null
  breadcrumb: ReactNode
  overlay: (guiToPx: number) => ReactNode
  onMonitor: (monitor: MonitorId) => void
  onScale: (scale: number) => void
  onPage: (page: string) => void
  onAddPage: () => void
  onRenamePage: (page: string) => void
  onDeletePage: (page: string) => void
}

interface Tip {
  text: string
  x: number
  y: number
}

function useSize<T extends HTMLElement>() {
  const ref = useRef<T>(null)
  const [size, setSize] = useState({ width: 0, height: 0 })
  useEffect(() => {
    const element = ref.current
    if (!element) return
    const observer = new ResizeObserver(([entry]) => setSize({ width: entry.contentRect.width, height: entry.contentRect.height }))
    observer.observe(element)
    return () => observer.disconnect()
  }, [])
  return [ref, size] as const
}

export function Stage(props: StageProps) {
  const { preview, busy, mode, monitor, scale, zoom, showHits, pages, page } = props
  const [canvasRef, size] = useSize<HTMLDivElement>()
  const [tip, setTip] = useState<Tip | null>(null)
  const screen = MONITORS[monitor]

  const fit = Math.max(0.05, Math.min((size.width - PADDING) / screen.width, (size.height - PADDING) / screen.height))
  const ratio = fit * zoom
  const displayWidth = Math.round(screen.width * ratio)
  const displayHeight = Math.round(screen.height * ratio)
  const guiToPx = preview ? ratio * preview.scale : 0
  const clickable = preview?.hotspots.filter((hotspot) => hotspot.clickable).length ?? 0

  return (
    <section className="stage">
      <div className="view-bar">
        <div className="page-tabs">
          {pages.length === 0 && <button className="on">Página única</button>}
          {pages.map((id) => (
            <button
              key={id}
              className={id === page ? 'on' : ''}
              onClick={() => props.onPage(id)}
              onDoubleClick={() => props.onRenamePage(id)}
              title="Duplo clique renomeia"
            >
              {id}
              {id === page && pages.length > 1 && (
                <X
                  className="close"
                  onClick={(event) => {
                    event.stopPropagation()
                    props.onDeletePage(id)
                  }}
                />
              )}
            </button>
          ))}
          <button className="add" onClick={props.onAddPage} title="Nova página">
            <Plus />
          </button>
        </div>
        <div className="device">
          <div className="seg">
            {(Object.keys(MONITORS) as MonitorId[]).map((id) => (
              <button key={id} className={id === monitor ? 'on' : ''} onClick={() => props.onMonitor(id)}>
                {id === monitor && <Monitor />}
                {MONITORS[id].label}
              </button>
            ))}
          </div>
          <div className="seg">
            <span className="seg-label">GUI</span>
            {SCALES.map((value) => (
              <button key={value} className={value === scale ? 'on' : ''} onClick={() => props.onScale(value)}>
                {value === 0 ? `Auto${scale === 0 && preview ? ` ${preview.scale}` : ''}` : value}
              </button>
            ))}
          </div>
          <span className="meta">{preview ? `${preview.guiWidth}×${preview.guiHeight}` : '—'}</span>
        </div>
      </div>

      <div className="canvas-wrap">
      <div className="canvas" ref={canvasRef}>
        {preview?.error ? (
          <div className="stage-error">{preview.error}</div>
        ) : preview?.png ? (
          <div
            className={`monitor${busy ? ' busy' : ''}${showHits ? ' show-hits' : ''}${mode === 'play' ? ' playing' : ''}`}
            style={{ width: displayWidth, height: displayHeight }}
          >
            <img src={`data:image/png;base64,${preview.png}`} alt="Preview do dialog" draggable={false} />
            {(mode === 'play' || showHits) &&
              preview.hotspots.map((hotspot, index) => (
                <div
                  key={index}
                  className="hotspot"
                  style={{
                    left: hotspot.x * guiToPx,
                    top: hotspot.y * guiToPx,
                    width: hotspot.w * guiToPx,
                    height: hotspot.h * guiToPx,
                    cursor: hotspot.clickable ? 'pointer' : 'default',
                  }}
                  onMouseMove={(event) => hotspot.tooltip && setTip({ text: hotspot.tooltip, x: event.clientX + 14, y: event.clientY + 12 })}
                  onMouseLeave={() => setTip(null)}
                />
              ))}
            {mode === 'edit' && props.overlay(guiToPx)}
          </div>
        ) : (
          <div className="empty">{busy ? 'Montando preview…' : 'Sem preview'}</div>
        )}
      </div>

      {props.tools}
      </div>

      <div className="stage-foot">
        {preview?.contentWidth != null && (
          <span>
            {preview.contentWidth}×{preview.contentHeight}px de conteúdo · {clickable} clicáveis · {preview.boxes.length} peças
          </span>
        )}
        <span className="path">{props.breadcrumb}</span>
      </div>

      {tip && (
        <div className="mc-tooltip" style={{ left: tip.x, top: tip.y }}>
          {tip.text.split('\n').map((line, index) => (
            <div key={index} className="line">
              {line || ' '}
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
