import { useEffect, useState } from 'react'
import { api, type Preview, type PreviewParams } from '../api'

const DEBOUNCE_MS = 180

const EMPTY: Preview = {
  png: null,
  width: null,
  height: null,
  guiWidth: 0,
  guiHeight: 0,
  scale: 1,
  body: null,
  hotspots: [],
  boxes: [],
  lists: {},
  pages: [],
  page: null,
  title: null,
  theme: null,
  contentWidth: null,
  contentHeight: null,
  warnings: [],
  error: null,
}

/** Pede o preview ao motor do Textura sempre que o texto ou a tela mudam (com debounce). */
export function usePreview(params: PreviewParams | null, refresh: number) {
  const [preview, setPreview] = useState<Preview | null>(null)
  const [busy, setBusy] = useState(false)
  const key = params ? JSON.stringify(params) : null

  useEffect(() => {
    if (!params) return
    const controller = new AbortController()
    setBusy(true)
    const timer = window.setTimeout(() => {
      api
        .preview(params, controller.signal)
        .then((result) => {
          setPreview(result)
          setBusy(false)
        })
        .catch((cause: unknown) => {
          if (controller.signal.aborted) return
          const message = cause instanceof Error ? cause.message : 'Falha no preview'
          setPreview((current) => ({ ...(current ?? EMPTY), png: null, error: message }))
          setBusy(false)
        })
    }, DEBOUNCE_MS)
    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [key, refresh])

  return { preview, busy }
}
