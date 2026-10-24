export type JsonPath = (string | number)[]

export type NodeKind =
  | 'screen'
  | 'row'
  | 'column'
  | 'grid'
  | 'box'
  | 'text'
  | 'button'
  | 'nav'
  | 'dropdown'
  | 'slider'
  | 'progress'
  | 'segmented'
  | 'switch'
  | 'chip'
  | 'pair'
  | 'slots'
  | 'image'
  | 'icon'
  | 'space'
  | 'unknown'

export interface TreeNode {
  /** Chave estável na árvore: o caminho JSON unido por pontos. */
  key: string
  kind: NodeKind
  label: string
  detail: string
  path: JsonPath
  /** Mesmo `path` que o parser do Textura dá ao nó (usado nas caixas do preview). */
  tag: string
  repeat: string | null
  children: TreeNode[]
}

export type Json = Record<string, unknown>

export const CONTAINERS: NodeKind[] = ['column', 'row', 'box', 'grid']
const LEAVES: NodeKind[] = ['text', 'button', 'nav', 'dropdown', 'slider', 'progress', 'segmented', 'switch', 'chip', 'pair', 'slots', 'image', 'icon', 'space']

export const KIND_LABEL: Record<NodeKind, string> = {
  screen: 'Screen',
  row: 'Row',
  column: 'Column',
  grid: 'Grid',
  box: 'Box',
  text: 'Text',
  button: 'Button',
  nav: 'Sidebar button',
  dropdown: 'Dropdown',
  slider: 'Slider',
  progress: 'Progress bar',
  segmented: 'Segmented',
  switch: 'Switch',
  chip: 'Chip',
  pair: 'Label · value',
  slots: 'Item slots',
  image: 'Image',
  icon: 'Icon',
  space: 'Space',
  unknown: 'Desconhecido',
}

export function isObject(value: unknown): value is Json {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export function kindOf(obj: Json): NodeKind {
  return CONTAINERS.find((kind) => kind in obj) ?? LEAVES.find((kind) => kind in obj) ?? 'unknown'
}

export function isContainer(kind: NodeKind): boolean {
  return CONTAINERS.includes(kind)
}

function leafDetail(kind: NodeKind, obj: Json): string {
  const value = obj[kind]
  if (kind === 'pair' && Array.isArray(value)) return value.join(' · ')
  if (kind === 'slots' && Array.isArray(value)) return value.join('×')
  if (kind === 'segmented' && Array.isArray(value)) return value.join(' / ')
  if (kind === 'space') return value === 'grow' ? 'cresce' : `${String(value)} px`
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') return String(value)
  return ''
}

/** Mesmo rótulo do ScreenParser: `name` ou `<forma>#<índice>`. */
function tagOf(obj: Json, kind: NodeKind, parentTag: string, index: number): string {
  const label = typeof obj.name === 'string' ? obj.name : `${kind}#${index}`
  return parentTag ? `${parentTag} > ${label}` : label
}

function build(obj: Json, path: JsonPath, parentTag: string, index: number): TreeNode {
  const kind = kindOf(obj)
  const name = typeof obj.name === 'string' ? obj.name : null
  const repeat = typeof obj.repeat === 'string' ? obj.repeat : null
  const key = path.join('.')
  const tag = tagOf(obj, kind, parentTag, index)

  if (isContainer(kind)) {
    const items = Array.isArray(obj[kind]) ? (obj[kind] as unknown[]) : []
    const children = items.flatMap((child, childIndex) =>
      isObject(child) ? [build(child, [...path, kind, childIndex], tag, childIndex)] : [],
    )
    const count = `${children.length} ${children.length === 1 ? 'item' : 'items'}`
    return { key, kind, label: name ?? KIND_LABEL[kind], detail: name ? `${KIND_LABEL[kind]} · ${count}` : count, path, tag, repeat, children }
  }
  return { key, kind, label: name ?? KIND_LABEL[kind], detail: leafDetail(kind, obj), path, tag, repeat, children: [] }
}

/** Raiz do documento para a página pedida (as páginas sem `document` usam o raiz). */
export function rootPath(doc: Json, page: string | null): JsonPath {
  const pages = Array.isArray(doc.pages) ? doc.pages : []
  const index = pages.findIndex((entry) => isObject(entry) && entry.id === page && isObject(entry.document))
  if (index >= 0) return ['pages', index, 'document']
  return isObject(doc.document) ? ['document'] : []
}

export function getAt(doc: unknown, path: JsonPath): unknown {
  return path.reduce<unknown>((value, segment) => {
    if (Array.isArray(value) && typeof segment === 'number') return value[segment]
    if (isObject(value) && typeof segment === 'string') return value[segment]
    return undefined
  }, doc)
}

/** Cópia do documento com `value` em `path` (sem mutar o original); `undefined` apaga a chave. */
export function setAt(doc: unknown, path: JsonPath, value: unknown): unknown {
  if (path.length === 0) return value
  const [head, ...rest] = path
  if (Array.isArray(doc) && typeof head === 'number') {
    return doc.map((item, index) => (index === head ? setAt(item, rest, value) : item))
  }
  const base = isObject(doc) ? doc : {}
  const next = { ...base }
  if (rest.length === 0 && value === undefined) {
    delete next[head as string]
    return next
  }
  next[head as string] = setAt(base[head as string], rest, value)
  return next
}

export function screenTree(doc: Json, page: string | null): TreeNode {
  const path = rootPath(doc, page)
  const root = getAt(doc, path)
  const pages = Array.isArray(doc.pages) ? doc.pages : []
  const pageIndex = pages.findIndex((entry) => isObject(entry) && entry.id === page && isObject(entry.document))
  const parentTag = pageIndex >= 0 ? String(page) : ''
  const children = isObject(root) ? [build(root, path, parentTag, Math.max(0, pageIndex))] : []
  return { key: 'screen', kind: 'screen', label: 'Screen', detail: '', path: [], tag: '', repeat: null, children }
}

export function pageIds(doc: Json): string[] {
  const pages = Array.isArray(doc.pages) ? doc.pages : []
  return pages.flatMap((entry) => (isObject(entry) && typeof entry.id === 'string' ? [entry.id] : []))
}

export function walk(node: TreeNode, visit: (node: TreeNode, parent: TreeNode | null) => void, parent: TreeNode | null = null): void {
  visit(node, parent)
  node.children.forEach((child) => walk(child, visit, node))
}

export function findNode(root: TreeNode, key: string): TreeNode | null {
  let found: TreeNode | null = null
  walk(root, (node) => {
    if (node.key === key) found = node
  })
  return found
}

export function parentOf(root: TreeNode, key: string): TreeNode | null {
  let found: TreeNode | null = null
  walk(root, (node, parent) => {
    if (node.key === key) found = parent
  })
  return found
}

export function trail(root: TreeNode, key: string): TreeNode[] {
  if (root.key === key) return [root]
  for (const child of root.children) {
    const rest = trail(child, key)
    if (rest.length) return [root, ...rest]
  }
  return []
}

/** Tag do preview → nó da árvore (`a > b[2]` é a instância 2 da repetição `a > b`). */
export function tagIndex(root: TreeNode): Map<string, TreeNode> {
  const index = new Map<string, TreeNode>()
  walk(root, (node) => {
    if (node.tag && !index.has(node.tag)) index.set(node.tag, node)
  })
  return index
}

export function baseTag(tag: string): string {
  return tag.replace(/\[\d+\]$/, '')
}
