import type { Box, PlacedBox } from '../api'
import type { Slot } from './ops'
import { baseTag, isContainer, type TreeNode } from './tree'

export interface Point {
  x: number
  y: number
}

export interface NodeBox extends Box {
  node: TreeNode
}

export interface DropTarget {
  slot: Slot
  /** Linha indicadora em coordenadas de GUI. */
  line: Box
}

const LINE = 1

export function contains(box: Box, point: Point): boolean {
  return point.x >= box.x && point.x < box.x + box.w && point.y >= box.y && point.y < box.y + box.h
}

/** Caixas do preview já ligadas ao nó da árvore (instâncias de listas apontam para o modelo). */
export function nodeBoxes(boxes: PlacedBox[], index: Map<string, TreeNode>): NodeBox[] {
  return boxes.flatMap((box) => {
    const node = index.get(box.tag) ?? index.get(baseTag(box.tag))
    return node ? [{ x: box.x, y: box.y, w: box.w, h: box.h, node }] : []
  })
}

export function boxesOf(all: NodeBox[], key: string): NodeBox[] {
  return all.filter((box) => box.node.key === key)
}

/** Nós sob o ponto, do mais interno (menor área) para o mais externo. */
export function hitsAt(all: NodeBox[], point: Point): NodeBox[] {
  return all
    .filter((box) => contains(box, point) && box.w > 0 && box.h > 0)
    .sort((a, b) => a.w * a.h - b.w * b.h || b.node.key.length - a.node.key.length)
}

function isInside(key: string, ancestor: string | null): boolean {
  return ancestor !== null && (key === ancestor || key.startsWith(`${ancestor}.`))
}

function axisOf(kind: TreeNode['kind']): 'x' | 'y' | 'grid' {
  if (kind === 'row') return 'x'
  if (kind === 'grid') return 'grid'
  return 'y'
}

function firstBox(all: NodeBox[], key: string): NodeBox | undefined {
  return all.find((box) => box.node.key === key)
}

function before(axis: 'x' | 'y' | 'grid', box: Box, point: Point): boolean {
  if (axis === 'x') return point.x < box.x + box.w / 2
  if (axis === 'y') return point.y < box.y + box.h / 2
  if (point.y < box.y) return true
  if (point.y >= box.y + box.h) return false
  return point.x < box.x + box.w / 2
}

function lineFor(axis: 'x' | 'y' | 'grid', box: Box, after: boolean): Box {
  if (axis === 'y') return { x: box.x, y: after ? box.y + box.h : box.y - LINE, w: box.w, h: LINE }
  return { x: after ? box.x + box.w : box.x - LINE, y: box.y, w: LINE, h: box.h }
}

/** Posição dentro de um container: antes do primeiro filho que fica depois do ponto. */
function intoContainer(all: NodeBox[], container: NodeBox, point: Point, dragged: string | null): DropTarget {
  const axis = axisOf(container.node.kind)
  const list = [...container.node.path, container.node.kind]
  const children = container.node.children
    .map((child, index) => ({ child, index, box: firstBox(all, child.key) }))
    .filter((entry) => entry.box && entry.child.key !== dragged)
  const next = children.find((entry) => entry.box && before(axis, entry.box, point))
  if (next?.box) return { slot: { list, index: next.index }, line: lineFor(axis, next.box, false) }
  const last = children[children.length - 1]
  const index = container.node.children.length
  if (last?.box) return { slot: { list, index }, line: lineFor(axis, last.box, true) }
  return { slot: { list, index }, line: { x: container.x + 2, y: container.y + 2, w: Math.max(1, container.w - 4), h: LINE } }
}

/**
 * Onde uma peça arrastada entra. Em cima de uma folha: antes ou depois dela; em cima do
 * espaço livre de um container: na posição correspondente entre os filhos.
 */
export function dropTarget(all: NodeBox[], parents: Map<string, TreeNode>, point: Point, dragged: string | null): DropTarget | null {
  for (const hit of hitsAt(all, point)) {
    if (isInside(hit.node.key, dragged)) continue
    if (isContainer(hit.node.kind)) return intoContainer(all, hit, point, dragged)
    const parent = parents.get(hit.node.key)
    if (!parent || parent.kind === 'screen') continue
    const index = parent.children.findIndex((child) => child.key === hit.node.key)
    const axis = axisOf(parent.kind)
    const isBefore = before(axis, hit, point)
    return {
      slot: { list: [...parent.path, parent.kind], index: isBefore ? index : index + 1 },
      line: lineFor(axis, hit, !isBefore),
    }
  }
  return null
}
