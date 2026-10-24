import { useState, type DragEvent } from 'react'
import {
  Box,
  ChevronDown,
  ChevronRight,
  CircleDot,
  Columns3,
  Grid3x3,
  Image,
  Layers,
  LayoutList,
  ListChecks,
  Monitor,
  MousePointerClick,
  MoveVertical,
  PanelLeft,
  Plus,
  Rows3,
  SlidersHorizontal,
  Smile,
  Tag,
  ToggleLeft,
  Type,
  type LucideIcon,
} from 'lucide-react'
import { LIBRARY, LIBRARY_MIME } from './library'
import type { Slot } from './ops'
import { isContainer, type NodeKind, type TreeNode } from './tree'

export const ICONS: Record<NodeKind, LucideIcon> = {
  screen: Monitor,
  row: Columns3,
  column: Rows3,
  grid: Grid3x3,
  box: Box,
  text: Type,
  button: MousePointerClick,
  nav: PanelLeft,
  dropdown: ChevronDown,
  slider: SlidersHorizontal,
  progress: SlidersHorizontal,
  segmented: ListChecks,
  switch: ToggleLeft,
  chip: Tag,
  pair: LayoutList,
  slots: Grid3x3,
  image: Image,
  icon: Smile,
  space: MoveVertical,
  unknown: CircleDot,
}

const NODE_MIME = 'application/x-textura-node'

type Zone = 'before' | 'after' | 'inside'

interface DropState {
  key: string
  zone: Zone
}

interface TreeActions {
  selected: string | null
  collapsed: Set<string>
  drop: DropState | null
  onToggle: (key: string) => void
  onSelect: (node: TreeNode) => void
  onContext: (node: TreeNode, x: number, y: number) => void
  onDragOver: (node: TreeNode, event: DragEvent) => void
  onDrop: (node: TreeNode, event: DragEvent) => void
}

function zoneOf(node: TreeNode, event: DragEvent): Zone {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const ratio = (event.clientY - rect.top) / rect.height
  if (isContainer(node.kind)) return ratio < 0.25 ? 'before' : ratio > 0.75 ? 'after' : 'inside'
  return ratio < 0.5 ? 'before' : 'after'
}

function Row({ node, depth, actions }: { node: TreeNode; depth: number; actions: TreeActions }) {
  const Icon = ICONS[node.kind]
  const open = !actions.collapsed.has(node.key)
  const hasChildren = node.children.length > 0
  const dropZone = actions.drop?.key === node.key ? actions.drop.zone : null
  const movable = node.kind !== 'screen' && node.path.length > 0 && typeof node.path[node.path.length - 1] === 'number'

  return (
    <>
      <div
        className={`tree-row${actions.selected === node.key ? ' selected' : ''}${dropZone ? ` drop-${dropZone}` : ''}`}
        style={{ paddingLeft: 6 + depth * 14 }}
        draggable={movable}
        onClick={() => actions.onSelect(node)}
        onContextMenu={(event) => {
          event.preventDefault()
          actions.onSelect(node)
          actions.onContext(node, event.clientX, event.clientY)
        }}
        onDragStart={(event) => {
          event.dataTransfer.setData(NODE_MIME, node.key)
          event.dataTransfer.effectAllowed = 'move'
        }}
        onDragOver={(event) => actions.onDragOver(node, event)}
        onDrop={(event) => actions.onDrop(node, event)}
      >
        <span
          className="twist"
          onClick={(event) => {
            event.stopPropagation()
            if (hasChildren) actions.onToggle(node.key)
          }}
        >
          {hasChildren && (open ? <ChevronDown /> : <ChevronRight />)}
        </span>
        <span className="glyph">
          <Icon />
        </span>
        <span className="name">{node.label}</span>
        <span className="detail">{node.detail}</span>
        {node.repeat && <span className="badge">↻ {node.repeat}</span>}
      </div>
      {open && node.children.map((child) => <Row key={child.key} node={child} depth={depth + 1} actions={actions} />)}
    </>
  )
}

function count(node: TreeNode): number {
  return node.children.reduce((total, child) => total + 1 + count(child), 0)
}

function slotFor(node: TreeNode, zone: Zone, parent: TreeNode | null): Slot | null {
  if (zone === 'inside') return { list: [...node.path, node.kind], index: node.children.length }
  if (!parent || parent.kind === 'screen') return null
  const index = parent.children.findIndex((child) => child.key === node.key)
  return { list: [...parent.path, parent.kind], index: zone === 'before' ? index : index + 1 }
}

function findParent(root: TreeNode, key: string): TreeNode | null {
  for (const child of root.children) {
    if (child.key === key) return root
    const found = findParent(child, key)
    if (found) return found
  }
  return null
}

interface ComponentsPanelProps {
  tree: TreeNode | null
  selected: string | null
  onSelect: (node: TreeNode) => void
  onContext: (node: TreeNode, x: number, y: number) => void
  onMove: (key: string, to: Slot) => void
  onAdd: (pieceId: string, to: Slot | null) => void
}

export function ComponentsPanel({ tree, selected, onSelect, onContext, onMove, onAdd }: ComponentsPanelProps) {
  const [tab, setTab] = useState<'layers' | 'add'>('layers')
  const [collapsed, setCollapsed] = useState<Set<string>>(() => new Set())
  const [drop, setDrop] = useState<DropState | null>(null)

  const actions: TreeActions = {
    selected,
    collapsed,
    drop,
    onSelect,
    onContext,
    onToggle: (key) =>
      setCollapsed((current) => {
        const next = new Set(current)
        if (next.has(key)) next.delete(key)
        else next.add(key)
        return next
      }),
    onDragOver: (node, event) => {
      const types = event.dataTransfer.types
      if (!types.includes(NODE_MIME) && !types.includes(LIBRARY_MIME)) return
      if (node.kind === 'screen') return
      event.preventDefault()
      setDrop({ key: node.key, zone: zoneOf(node, event) })
    },
    onDrop: (node, event) => {
      event.preventDefault()
      setDrop(null)
      if (!tree) return
      const slot = slotFor(node, zoneOf(node, event), findParent(tree, node.key))
      if (!slot) return
      const moving = event.dataTransfer.getData(NODE_MIME)
      const piece = event.dataTransfer.getData(LIBRARY_MIME)
      if (moving) onMove(moving, slot)
      else if (piece) onAdd(piece, slot)
    },
  }

  const groups = [...new Set(LIBRARY.map((item) => item.group))]

  return (
    <aside className="components" onDragEnd={() => setDrop(null)}>
      <div className="panel-head tabs">
        <button className={tab === 'layers' ? 'on' : ''} onClick={() => setTab('layers')}>
          <Layers /> Components<span className="count">{tree ? count(tree) : 0}</span>
        </button>
        <button className={tab === 'add' ? 'on' : ''} onClick={() => setTab('add')}>
          <Plus /> Adicionar
        </button>
      </div>
      {tab === 'layers' ? (
        <div className="tree" onDragLeave={(event) => event.currentTarget === event.target && setDrop(null)}>
          {tree ? <Row node={tree} depth={0} actions={actions} /> : <div className="empty">JSON inválido</div>}
        </div>
      ) : (
        <div className="library-list">
          {groups.map((group) => (
            <div key={group}>
              <span className="eyebrow">{group}</span>
              <div className="pieces">
                {LIBRARY.filter((item) => item.group === group).map((item) => {
                  const Icon = ICONS[item.kind]
                  return (
                    <button
                      key={item.id}
                      className="piece"
                      draggable
                      onDragStart={(event) => {
                        event.dataTransfer.setData(LIBRARY_MIME, item.id)
                        event.dataTransfer.effectAllowed = 'copy'
                      }}
                      onClick={() => onAdd(item.id, null)}
                      title="Clique para inserir na seleção ou arraste para o preview"
                    >
                      <Icon />
                      <b>{item.label}</b>
                      <small>{item.hint}</small>
                    </button>
                  )
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </aside>
  )
}
