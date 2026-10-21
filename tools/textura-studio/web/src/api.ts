export interface DialogSummary {
  id: string
  title: string | null
  theme: string | null
  pages: number
  modified: number
}

export interface Box {
  x: number
  y: number
  w: number
  h: number
}

export interface Hotspot extends Box {
  tooltip: string | null
  clickable: boolean
}

export interface Preview {
  png: string | null
  width: number | null
  height: number | null
  guiWidth: number
  guiHeight: number
  scale: number
  body: Box | null
  hotspots: Hotspot[]
  boxes: PlacedBox[]
  lists: Record<string, number>
  pages: string[]
  page: string | null
  title: string | null
  theme: string | null
  contentWidth: number | null
  contentHeight: number | null
  warnings: string[]
  error: string | null
}

export interface PreviewParams {
  id: string
  json: string
  page: string | null
  monitorWidth: number
  monitorHeight: number
  scale: number
  lists?: SampleLists
  values?: Record<string, string>
  themeFiles?: Record<string, string>
}

export interface SaveResult {
  saved: string
  warnings: string[]
  error: string | null
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)
  const text = await response.text()
  if (!response.ok) {
    let message = text
    try {
      message = (JSON.parse(text) as { error?: string }).error ?? text
    } catch {
      // resposta não era JSON; usa o texto cru
    }
    throw new Error(message || `HTTP ${response.status}`)
  }
  return JSON.parse(text) as T
}

export const api = {
  dialogs: () => request<DialogSummary[]>('/api/dialogs'),
  themes: () => request<string[]>('/api/themes'),
  async dialog(id: string): Promise<string> {
    const response = await fetch(`/api/dialogs/${encodeURIComponent(id)}`)
    if (!response.ok) throw new Error(`Tela '${id}' não encontrada`)
    return response.text()
  },
  save: (id: string, json: string) =>
    request<SaveResult>(`/api/dialogs/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: json,
    }),
  preview: (params: PreviewParams, signal?: AbortSignal) =>
    request<Preview>('/api/preview', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
      signal,
    }),
  reload: () => request<{ warnings: string[] }>('/api/reload', { method: 'POST' }),
  thumbUrl: (id: string, version: number) => `/api/thumb/${encodeURIComponent(id)}?v=${version}`,
}

export interface PlacedBox extends Box {
  tag: string
}

export interface ThemeInfo {
  styles: string[]
  surfaces: string[]
  icons: string[]
  looks: string[]
  iconSize: number
}

export interface AssetCatalog {
  themes: Record<string, ThemeInfo>
  ui: string[]
  glyphs: string[]
  items: string[]
}

export type SampleLists = Record<string, Record<string, string>[]>

export const assetApi = {
  catalog: () => request<AssetCatalog>('/api/assets'),
  artUrl: (id: string, theme: string | null) =>
    `/api/art?id=${encodeURIComponent(id)}${theme ? `&theme=${encodeURIComponent(theme)}` : ''}`,
}
