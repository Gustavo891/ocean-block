import { getAt, isObject, kindOf, setAt, type Json, type JsonPath } from './tree'

/** Lugar de inserção: o array de filhos (`[..., 'column']`) e a posição nele. */
export interface Slot {
  list: JsonPath
  index: number
}

export function slotOf(path: JsonPath): Slot | null {
  const index = path[path.length - 1]
  if (typeof index !== 'number') return null
  return { list: path.slice(0, -1), index }
}

function listAt(doc: Json, list: JsonPath): unknown[] {
  const value = getAt(doc, list)
  return Array.isArray(value) ? value : []
}

function samePrefix(a: JsonPath, b: JsonPath): boolean {
  return a.length <= b.length && a.every((segment, index) => b[index] === segment)
}

export function insert(doc: Json, slot: Slot, node: unknown): { doc: Json; path: JsonPath } {
  const items = listAt(doc, slot.list)
  const index = Math.max(0, Math.min(slot.index, items.length))
  const next = [...items.slice(0, index), node, ...items.slice(index)]
  return { doc: setAt(doc, slot.list, next) as Json, path: [...slot.list, index] }
}

export function remove(doc: Json, path: JsonPath): Json {
  const slot = slotOf(path)
  if (!slot) return doc
  const items = listAt(doc, slot.list)
  return setAt(doc, slot.list, items.filter((_, index) => index !== slot.index)) as Json
}

/** Move o nó em `from` para `to`; recusa soltar um nó dentro dele mesmo. */
export function move(doc: Json, from: JsonPath, to: Slot): { doc: Json; path: JsonPath } | null {
  if (samePrefix(from, to.list)) return null
  const source = slotOf(from)
  if (!source) return null
  const node = getAt(doc, from)
  const sameList = source.list.length === to.list.length && samePrefix(source.list, to.list)
  let target = to
  if (sameList && source.index < to.index) target = { ...to, index: to.index - 1 }
  else if (samePrefix(source.list, to.list) && to.list.length > source.list.length) {
    // O destino está dentro de um irmão posterior: o índice desse irmão cai uma posição.
    const sibling = to.list[source.list.length]
    if (typeof sibling === 'number' && sibling > source.index) {
      const list = [...to.list]
      list[source.list.length] = sibling - 1
      target = { ...to, list }
    }
  }
  if (sameList && target.index === source.index) return { doc, path: from }
  return insert(remove(doc, from), target, node)
}

export function duplicate(doc: Json, path: JsonPath): { doc: Json; path: JsonPath } | null {
  const slot = slotOf(path)
  if (!slot) return null
  return insert(doc, { list: slot.list, index: slot.index + 1 }, structuredClone(getAt(doc, path)))
}

export function wrap(doc: Json, path: JsonPath, container: Json): { doc: Json; path: JsonPath } | null {
  const node = getAt(doc, path)
  if (!isObject(node) || !slotOf(path)) return null
  const kind = kindOf(container)
  const wrapper = { ...container, [kind]: [node] }
  return { doc: setAt(doc, path, wrapper) as Json, path }
}

/** Onde entra uma peça nova: dentro do container selecionado, senão logo depois da seleção. */
export function insertionSlot(doc: Json, selected: JsonPath | null, root: JsonPath): Slot {
  const target = selected ?? root
  const node = getAt(doc, target)
  if (isObject(node)) {
    const kind = kindOf(node)
    const children = node[kind]
    if (Array.isArray(children)) return { list: [...target, kind], index: children.length }
  }
  const slot = slotOf(target)
  if (slot) return { list: slot.list, index: slot.index + 1 }
  const rootNode = getAt(doc, root)
  const kind = isObject(rootNode) ? kindOf(rootNode) : 'column'
  return { list: [...root, kind], index: listAt(doc, [...root, kind]).length }
}

export function format(doc: Json): string {
  return `${JSON.stringify(doc, null, 2)}\n`
}
