/** API dos arquivos de config e imagens que o servidor do Studio libera. */

async function failure(response: Response): Promise<never> {
  const text = await response.text()
  let message = text
  try {
    message = (JSON.parse(text) as { error?: string }).error ?? text
  } catch {
    // resposta não era JSON
  }
  throw new Error(message || `HTTP ${response.status}`)
}

const at = (path: string) => `path=${encodeURIComponent(path)}`

export interface TextureFace {
  face: string
  path: string
}

export interface BlockEntry {
  id: string
  name: string
  kind: 'bloco' | 'item'
  file: string
  faces: TextureFace[]
}

export const filesApi = {
  async list(dir: string, ext = 'yml'): Promise<string[]> {
    const response = await fetch(`/api/files?dir=${encodeURIComponent(dir)}&ext=${ext}`)
    return response.ok ? ((await response.json()) as string[]) : failure(response)
  },
  async read(path: string): Promise<string | null> {
    const response = await fetch(`/api/file?${at(path)}`)
    if (response.status === 404) return null
    return response.ok ? response.text() : failure(response)
  },
  async write(path: string, text: string): Promise<void> {
    const response = await fetch(`/api/file?${at(path)}`, { method: 'PUT', body: text, headers: { 'Content-Type': 'text/plain' } })
    if (!response.ok) await failure(response)
  },
  async remove(path: string): Promise<void> {
    const response = await fetch(`/api/file?${at(path)}`, { method: 'DELETE' })
    if (!response.ok) await failure(response)
  },
  async upload(path: string, file: Blob): Promise<void> {
    const response = await fetch(`/api/upload?${at(path)}`, { method: 'POST', body: file, headers: { 'Content-Type': 'image/png' } })
    if (!response.ok) await failure(response)
  },
  rawUrl: (path: string, version = 0) => `/api/raw?${at(path)}&v=${version}`,
  async blocks(): Promise<BlockEntry[]> {
    const response = await fetch('/api/blocks')
    return response.ok ? ((await response.json()) as BlockEntry[]) : failure(response)
  },
  async saveBlockTexture(path: string, file: Blob): Promise<void> {
    const response = await fetch(`/api/blocks/texture?${at(path)}`, { method: 'POST', body: file, headers: { 'Content-Type': 'image/png' } })
    if (!response.ok) await failure(response)
  },
  async post<T>(url: string, body: unknown): Promise<T> {
    const response = await fetch(url, { method: 'POST', body: JSON.stringify(body), headers: { 'Content-Type': 'application/json' } })
    return response.ok ? ((await response.json()) as T) : failure(response)
  },
}
