import type { RefObject } from 'react'
import { cellAt, type Cell, type Rect } from './pixels'

interface GridCanvasProps {
  canvas: RefObject<HTMLCanvasElement>
  width: number
  height: number
  scale: number
  selection: Rect | null
  ghost: Rect | null
  onDown: (cell: Cell) => void
  onMove: (cell: Cell, pressed: boolean) => void
  onUp?: () => void
}

function box(rect: Rect, scale: number) {
  return { left: rect.x * scale, top: rect.y * scale, width: rect.w * scale, height: rect.h * scale }
}

export function GridCanvas({ canvas, width, height, scale, selection, ghost, onDown, onMove, onUp }: GridCanvasProps) {
  const cell = (event: React.PointerEvent<HTMLCanvasElement>) => cellAt(event.currentTarget, event.clientX, event.clientY)
  return (
    <div className="grid-canvas" style={{ width: width * scale, height: height * scale }}>
      <canvas
        ref={canvas}
        className="pixel-canvas"
        style={{ width: width * scale, height: height * scale, backgroundSize: `${Math.max(scale, 4) * 2}px ${Math.max(scale, 4) * 2}px` }}
        onPointerDown={(event) => {
          event.currentTarget.setPointerCapture(event.pointerId)
          onDown(cell(event))
        }}
        onPointerMove={(event) => onMove(cell(event), event.buttons === 1)}
        onPointerUp={onUp}
        onPointerCancel={onUp}
      />
      {selection && <span className="pixel-selection" style={box(selection, scale)} />}
      {ghost && <span className="pixel-ghost" style={box(ghost, scale)} />}
    </div>
  )
}
