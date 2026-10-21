import { useCallback, useRef, useState } from 'react'

interface History {
  past: string[]
  present: string
  future: string[]
}

const COALESCE_MS = 700
const LIMIT = 200

/** Texto com desfazer/refazer; edições seguidas do mesmo campo viram um passo só. */
export function useHistory(initial: string) {
  const [state, setState] = useState<History>({ past: [], present: initial, future: [] })
  const last = useRef({ at: 0, source: '' })

  const set = useCallback((next: string, source = 'edit') => {
    const now = Date.now()
    const coalesce = source === last.current.source && now - last.current.at < COALESCE_MS
    last.current = { at: now, source }
    setState((current) => {
      if (next === current.present) return current
      const past = coalesce ? current.past : [...current.past, current.present].slice(-LIMIT)
      return { past, present: next, future: [] }
    })
  }, [])

  const reset = useCallback((value: string) => {
    last.current = { at: 0, source: '' }
    setState({ past: [], present: value, future: [] })
  }, [])

  const undo = useCallback(() => {
    last.current = { at: 0, source: '' }
    setState((current) => {
      const previous = current.past[current.past.length - 1]
      if (previous === undefined) return current
      return { past: current.past.slice(0, -1), present: previous, future: [current.present, ...current.future] }
    })
  }, [])

  const redo = useCallback(() => {
    last.current = { at: 0, source: '' }
    setState((current) => {
      const [next, ...rest] = current.future
      if (next === undefined) return current
      return { past: [...current.past, current.present], present: next, future: rest }
    })
  }, [])

  return { text: state.present, canUndo: state.past.length > 0, canRedo: state.future.length > 0, set, reset, undo, redo }
}
