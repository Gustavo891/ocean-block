import { useEffect, useMemo, useRef, useState, type DragEvent, type MouseEvent as ReactMouseEvent } from 'react'
import type { PlacedBox } from '../api'
import { boxesOf, dropTarget, hitsAt, nodeBoxes, type DropTarget, type NodeBox, type Point } from './geometry'
import { LIBRARY_MIME } from './library'
import type { Slot } from './ops'
import { KIND_LABEL, getAt, isContainer, tagIndex, walk, type Json, type JsonPath, type TreeNode } from './tree'

const DRAG_THRESHOLD = 4
const INLINE_KINDS = new Set(['text', 'button', 'nav', 'chip', 'dropdown'])

interface EditLayerProps {
  doc: Json
  tree: TreeNode
  boxes: PlacedBox[]
  guiToPx: number
  selected: string | null
  onSelect: (key: string | null) => void
  onMove: (from: JsonPath, to: Slot) => void
  onDropNew: (pieceId: string, to: Slot) => void
  onChange: (path: JsonPath, value: unknown, source: string) => void
  onContext: (key: string, x: number, y: number) => void
}

type Gesture =
  | { kind: 'press'; key: string; start: Point; client: Point }
  | { kind: 'drag'; key: string; target: DropTarget | null }
  | { kind: 'resize'; key: string; edge: 'right' | 'bottom' | 'corner'; start: Point; box: NodeBox }

interface Inline {
  node: TreeNode
  box: NodeBox
  field: string
  value: string
}

export function EditLayer(props: EditLayerProps) {
  const { doc, tree, boxes, guiToPx, selected } = props
  const layer = useRef<HTMLDivElement>(null)
  const [hover, setHover] = useState<NodeBox | null>(null)
  const [gesture, setGesture] = useState<Gesture | null>(null)
  const [external, setExternal] = useState<DropTarget | null>(null)
  const [inline, setInline] = useState<Inline | null>(null)

  const index = useMemo(() => tagIndex(tree), [tree])
  const all = useMemo(() => nodeBoxes(boxes, index), [boxes, index])
  const parents = useMemo(() => {
    const map = new Map<string, TreeNode>()
    walk(tree, (node, parent) => parent && map.set(node.key, parent))
    return map
  }, [tree])
  const selectedBoxes = selected ? boxesOf(all, selected) : []

  const toGui = (clientX: number, clientY: number): Point => {
    const rect = layer.current?.getBoundingClientRect()
    if (!rect || guiToPx === 0) return { x: 0, y: 0 }
    return { x: (clientX - rect.left) / guiToPx, y: (clientY - rect.top) / guiToPx }
  }

  const px = (box: { x: number; y: number; w: number; h: number }) => ({
    left: box.x * guiToPx,
    top: box.y * guiToPx,
    width: Math.max(1, box.w * guiToPx),
    height: Math.max(1, box.h * guiToPx),
  })

  useEffect(() => {
    if (!gesture) return
    const onMove = (event: MouseEvent) => {
      const point = toGui(event.clientX, event.clientY)
      if (gesture.kind === 'press') {
        const moved = Math.hypot(event.clientX - gesture.client.x, event.clientY - gesture.client.y)
        if (moved > DRAG_THRESHOLD && gesture.key !== tree.children[0]?.key) setGesture({ kind: 'drag', key: gesture.key, target: null })
        return
      }
      if (gesture.kind === 'drag') {
        setGesture({ ...gesture, target: dropTarget(all, parents, point, gesture.key) })
        return
      }
      const node = gesture.box.node
      const width = Math.max(1, Math.round(gesture.box.w + point.x - gesture.start.x))
      const height = Math.max(1, Math.round(gesture.box.h + point.y - gesture.start.y))
      if (node.kind === 'icon') props.onChange([...node.path, 'size'], Math.max(width, height), 'resize')
      else if (node.kind === 'space') props.onChange([...node.path, 'space'], gesture.edge === 'bottom' ? height : width, 'resize')
      else {
        if (gesture.edge !== 'bottom') props.onChange([...node.path, 'w'], width, 'resize')
        if (gesture.edge !== 'right' && isContainer(node.kind)) props.onChange([...node.path, 'h'], height, 'resize')
      }
    }
    const onUp = () => {
      if (gesture.kind === 'drag' && gesture.target) {
        const node = all.find((box) => box.node.key === gesture.key)?.node
        if (node) props.onMove(node.path, gesture.target.slot)
      }
      setGesture(null)
    }
    window.addEventListener('mousemove', onMove)
    window.addEventListener('mouseup', onUp)
    return () => {
      window.removeEventListener('mousemove', onMove)
      window.removeEventListener('mouseup', onUp)
    }
  })

  const onMouseDown = (event: ReactMouseEvent) => {
    if (event.button !== 0) return
    const point = toGui(event.clientX, event.clientY)
    const hit = hitsAt(all, point)[0]
    props.onSelect(hit?.node.key ?? null)
    if (hit) setGesture({ kind: 'press', key: hit.node.key, start: point, client: { x: event.clientX, y: event.clientY } })
  }

  const onDoubleClick = (event: ReactMouseEvent) => {
    const point = toGui(event.clientX, event.clientY)
    const hit = hitsAt(all, point).find((box) => INLINE_KINDS.has(box.node.kind))
    if (!hit) return
    const value = getAt(doc, [...hit.node.path, hit.node.kind])
    if (typeof value !== 'string') return
    props.onSelect(hit.node.key)
    setInline({ node: hit.node, box: hit, field: hit.node.kind, value })
  }

  const onContextMenu = (event: ReactMouseEvent) => {
    event.preventDefault()
    const hit = hitsAt(all, toGui(event.clientX, event.clientY))[0]
    if (!hit) return
    props.onSelect(hit.node.key)
    props.onContext(hit.node.key, event.clientX, event.clientY)
  }

  const onDragOver = (event: DragEvent) => {
    if (!event.dataTransfer.types.includes(LIBRARY_MIME)) return
    event.preventDefault()
    setExternal(dropTarget(all, parents, toGui(event.clientX, event.clientY), null))
  }

  const onDrop = (event: DragEvent) => {
    const piece = event.dataTransfer.getData(LIBRARY_MIME)
    const target = external
    setExternal(null)
    if (!piece || !target) return
    event.preventDefault()
    props.onDropNew(piece, target.slot)
  }

  const commitInline = () => {
    if (!inline) return
    props.onChange([...inline.node.path, inline.field], inline.value, `inline:${inline.node.key}`)
    setInline(null)
  }

  const target = gesture?.kind === 'drag' ? gesture.target : external
  const dragging = gesture?.kind === 'drag' ? gesture.key : null
  const primary = selectedBoxes[0]
  const resizable = primary && primary.node.kind !== 'screen'

  return (
    <div
      ref={layer}
      className={`edit-layer${dragging ? ' dragging' : ''}`}
      onMouseDown={onMouseDown}
      onMouseMove={(event) => !gesture && setHover(hitsAt(all, toGui(event.clientX, event.clientY))[0] ?? null)}
      onMouseLeave={() => setHover(null)}
      onDoubleClick={onDoubleClick}
      onContextMenu={onContextMenu}
      onDragOver={onDragOver}
      onDragLeave={() => setExternal(null)}
      onDrop={onDrop}
    >
      {hover && hover.node.key !== selected && !dragging && (
        <div className="box-hover" style={px(hover)}>
          <span className="box-label">
            {KIND_LABEL[hover.node.kind]} <i>{hover.node.label !== KIND_LABEL[hover.node.kind] ? hover.node.label : hover.node.detail}</i>
          </span>
        </div>
      )}
      {selectedBoxes.map((box, position) => (
        <div key={position} className={`box-selected${position > 0 ? ' instance' : ''}${dragging ? ' ghost' : ''}`} style={px(box)} />
      ))}
      {primary && !dragging && (
        <span className="box-tag" style={{ left: primary.x * guiToPx, top: primary.y * guiToPx }}>
          {KIND_LABEL[primary.node.kind]} · {primary.w}×{primary.h}
        </span>
      )}
      {resizable && !dragging && !inline &&
        (['right', 'bottom', 'corner'] as const).map((edge) => (
          <span
            key={edge}
            className={`handle ${edge}`}
            style={{
              left: (primary.x + (edge === 'bottom' ? primary.w / 2 : primary.w)) * guiToPx,
              top: (primary.y + (edge === 'right' ? primary.h / 2 : primary.h)) * guiToPx,
            }}
            onMouseDown={(event) => {
              event.stopPropagation()
              setGesture({ kind: 'resize', key: primary.node.key, edge, start: toGui(event.clientX, event.clientY), box: primary })
            }}
          />
        ))}
      {target && <div className="drop-line" style={px(target.line)} />}
      {inline && (
        <input
          className="inline-edit"
          autoFocus
          value={inline.value}
          style={{ ...px(inline.box), fontSize: Math.max(11, 7 * guiToPx) }}
          onMouseDown={(event) => event.stopPropagation()}
          onChange={(event) => setInline({ ...inline, value: event.target.value })}
          onBlur={commitInline}
          onKeyDown={(event) => {
            if (event.key === 'Enter') commitInline()
            if (event.key === 'Escape') setInline(null)
          }}
        />
      )}
    </div>
  )
}
