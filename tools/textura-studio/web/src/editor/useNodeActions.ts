import { useCallback } from 'react'
import { LIBRARY } from './library'
import { duplicate, insert, insertionSlot, move, remove, slotOf, wrap, type Slot } from './ops'
import { getAt, isObject, rootPath, type Json, type JsonPath, type TreeNode } from './tree'

const CLIPBOARD = 'textura-studio:clipboard'

export type WrapKind = 'row' | 'column' | 'card'

const WRAPPERS: Record<WrapKind, Json> = {
  row: { row: [], w: 'grow', h: 'fit', gap: 4, cross: 'center' },
  column: { column: [], w: 'grow', h: 'fit', gap: 4 },
  card: { name: 'Card', column: [], w: 'grow', h: 'fit', gap: 4, pad: 5, surface: 'sheet' },
}

function readClipboard(): unknown {
  try {
    const raw = window.localStorage.getItem(CLIPBOARD)
    return raw ? (JSON.parse(raw) as unknown) : null
  } catch {
    return null
  }
}

interface Options {
  doc: Json | null
  page: string | null
  selected: TreeNode | null
  /** Grava o novo documento (um passo do histórico) e seleciona o caminho dado. */
  commit: (doc: Json, select: JsonPath | null, source: string) => void
}

/** Operações de estrutura sobre a peça selecionada (mover, duplicar, apagar, colar...). */
export function useNodeActions({ doc, page, selected, commit }: Options) {
  const path = selected && selected.kind !== 'screen' ? selected.path : null
  const movable = path !== null && slotOf(path) !== null

  const add = useCallback(
    (pieceId: string, to: Slot | null) => {
      const piece = LIBRARY.find((item) => item.id === pieceId)
      if (!doc || !piece) return
      const slot = to ?? insertionSlot(doc, path, rootPath(doc, page))
      const result = insert(doc, slot, piece.make())
      commit(result.doc, result.path, 'add')
    },
    [doc, path, page, commit],
  )

  const moveTo = useCallback(
    (from: JsonPath, to: Slot) => {
      if (!doc) return
      const result = move(doc, from, to)
      if (result) commit(result.doc, result.path, 'move')
    },
    [doc, commit],
  )

  const step = useCallback(
    (direction: -1 | 1) => {
      if (!doc || !path) return
      const slot = slotOf(path)
      if (!slot) return
      const length = (getAt(doc, slot.list) as unknown[] | undefined)?.length ?? 0
      const index = slot.index + direction
      if (index < 0 || index >= length) return
      const result = move(doc, path, { list: slot.list, index: direction > 0 ? index + 1 : index })
      if (result) commit(result.doc, result.path, 'move')
    },
    [doc, path, commit],
  )

  const removeSelected = useCallback(() => {
    if (!doc || !path || !movable) return
    const slot = slotOf(path)
    const next = remove(doc, path)
    const siblings = slot ? ((getAt(next, slot.list) as unknown[] | undefined) ?? []) : []
    const select = slot && siblings.length ? [...slot.list, Math.min(slot.index, siblings.length - 1)] : slot ? slot.list.slice(0, -1) : null
    commit(next, select, 'delete')
  }, [doc, path, movable, commit])

  const duplicateSelected = useCallback(() => {
    if (!doc || !path) return
    const result = duplicate(doc, path)
    if (result) commit(result.doc, result.path, 'duplicate')
  }, [doc, path, commit])

  const copy = useCallback(() => {
    if (!doc || !path) return
    try {
      window.localStorage.setItem(CLIPBOARD, JSON.stringify(getAt(doc, path)))
    } catch {
      // armazenamento indisponível: a cópia só não fica salva
    }
  }, [doc, path])

  const paste = useCallback(() => {
    const node = readClipboard()
    if (!doc || !isObject(node)) return
    const result = insert(doc, insertionSlot(doc, path, rootPath(doc, page)), structuredClone(node))
    commit(result.doc, result.path, 'paste')
  }, [doc, path, page, commit])

  const wrapSelected = useCallback(
    (kind: WrapKind) => {
      if (!doc || !path || !movable) return
      const result = wrap(doc, path, structuredClone(WRAPPERS[kind]))
      if (result) commit(result.doc, result.path, 'wrap')
    },
    [doc, path, movable, commit],
  )

  return { add, moveTo, step, removeSelected, duplicateSelected, copy, paste, wrapSelected, movable, canPaste: () => isObject(readClipboard()) }
}
