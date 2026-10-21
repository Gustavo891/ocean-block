import { getAt, isObject, rootPath, type Json } from './tree'

/** Chaves que pertencem à tela (wrapper), não ao container raiz. */
const SCREEN_KEYS = new Set(['title', 'theme', 'document', 'pages', 'hideFooter', 'canCloseWithEscape', 'pause', 'close'])

function rootNode(doc: Json, page: string | null): Json {
  const node = getAt(doc, rootPath(doc, page))
  const source = isObject(node) ? node : {}
  return Object.fromEntries(Object.entries(source).filter(([key]) => !SCREEN_KEYS.has(key)))
}

function uniqueId(taken: string[], base: string): string {
  let index = taken.length + 1
  while (taken.includes(`${base}-${index}`)) index++
  return `${base}-${index}`
}

function pageList(doc: Json): Json[] {
  return Array.isArray(doc.pages) ? doc.pages.filter(isObject) : []
}

/** Nova página copiando o conteúdo da atual (a primeira página continua usando o raiz). */
export function addPage(doc: Json, current: string | null): { doc: Json; id: string } {
  const pages = pageList(doc)
  const base = pages.length ? pages : [{ id: 'inicio' }]
  const id = uniqueId(base.map((page) => String(page.id)), 'pagina')
  const document = structuredClone(rootNode(doc, current))
  return { doc: { ...doc, pages: [...base, { id, document }] }, id }
}

function renameRefs(value: unknown, from: string, to: string): unknown {
  if (Array.isArray(value)) return value.map((item) => renameRefs(item, from, to))
  if (!isObject(value)) return value
  const next: Json = {}
  for (const [key, child] of Object.entries(value)) {
    if (key === 'onClick' && isObject(child) && child.action === 'page' && child.value === from) next[key] = { ...child, value: to }
    else if (key === 'onClick' && isObject(child) && typeof child.value === 'string') {
      next[key] = { ...child, value: child.value.replace(new RegExp(`^\\[page\\]\\s*${from}$`, 'gm'), `[page] ${to}`) }
    } else next[key] = renameRefs(child, from, to)
  }
  return next
}

export function renamePage(doc: Json, from: string, to: string): Json {
  const pages = pageList(doc).map((page) => (page.id === from ? { ...page, id: to } : page))
  return renameRefs({ ...doc, pages }, from, to) as Json
}

export function deletePage(doc: Json, id: string): Json {
  const pages = pageList(doc).filter((page) => page.id !== id)
  if (pages.length <= 1 && !pages.some((page) => isObject(page.document))) {
    const { pages: _removed, ...rest } = doc
    return rest
  }
  return { ...doc, pages }
}

export const PAGE_ID = /^[A-Za-z0-9_-]{1,32}$/
