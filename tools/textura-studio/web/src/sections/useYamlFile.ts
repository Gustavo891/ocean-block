import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { isMap, isNode, parseDocument, type Document } from 'yaml'
import { useHistory } from '../editor/useHistory'
import { filesApi } from './files'

const CRLF = '\r\n'
const EOL = /\r?\n/g

export type YamlPath = (string | number)[]

export interface YamlFile {
  text: string
  doc: Document | null
  error: string | null
  loaded: boolean
  dirty: boolean
  saving: boolean
  saveError: string | null
  canUndo: boolean
  canRedo: boolean
  get: (path: YamlPath) => unknown
  /** Grava um valor (undefined apaga a chave) mantendo comentários e ordem do arquivo. */
  set: (path: YamlPath, value: unknown, source?: string) => void
  /** Troca várias chaves de uma vez (um passo no desfazer). */
  edit: (change: (doc: Document) => void, source?: string) => void
  setText: (text: string) => void
  undo: () => void
  redo: () => void
  save: () => Promise<boolean>
}

function parse(text: string): { doc: Document | null; error: string | null } {
  const doc = parseDocument(text, { keepSourceTokens: false })
  if (doc.errors.length) return { doc: null, error: doc.errors[0].message }
  return { doc, error: null }
}

/** Um arquivo YAML do pack aberto para edição, com histórico e salvar direto no servidor. */
export function useYamlFile(path: string | null, fallback = ''): YamlFile {
  const history = useHistory('')
  const [saved, setSaved] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const crlf = useRef(false)

  useEffect(() => {
    setSaved(null)
    if (!path) return
    filesApi
      .read(path)
      .then((text) => {
        crlf.current = (text ?? '').includes(CRLF)
        history.reset(text ?? fallback)
        setSaved(text ?? '')
      })
      .catch((cause: unknown) => setSaveError(cause instanceof Error ? cause.message : 'Falha ao abrir'))
  }, [path, fallback, history.reset])

  const text = history.text
  const { doc, error } = useMemo(() => parse(text), [text])

  const edit = useCallback(
    (change: (doc: Document) => void, source = 'edit') => {
      const draft = parse(text).doc
      if (!draft) return
      if (!isMap(draft.contents)) draft.contents = draft.createNode({}) as typeof draft.contents
      change(draft)
      const out = draft.toString({ indentSeq: false })
      history.set(crlf.current ? out.replace(EOL, CRLF) : out, source)
    },
    [text, history],
  )

  const set = useCallback(
    (path: YamlPath, value: unknown, source?: string) =>
      edit((draft) => {
        if (value === undefined) draft.deleteIn(path)
        else draft.setIn(path, value)
      }, source ?? path.join('.')),
    [edit],
  )

  const save = useCallback(async () => {
    if (!path || error) {
      setSaveError(error ? `YAML inválido: ${error}` : 'Nada para salvar')
      return false
    }
    setSaving(true)
    setSaveError(null)
    try {
      await filesApi.write(path, text)
      setSaved(text)
      return true
    } catch (cause: unknown) {
      setSaveError(cause instanceof Error ? cause.message : 'Falha ao salvar')
      return false
    } finally {
      setSaving(false)
    }
  }, [path, text, error])

  return {
    text,
    doc,
    error,
    loaded: saved !== null,
    dirty: saved !== null && text !== saved,
    saving,
    saveError,
    canUndo: history.canUndo,
    canRedo: history.canRedo,
    get: (at) => {
      const value: unknown = doc?.getIn(at)
      return isNode(value) ? value.toJSON() : value
    },
    set,
    edit,
    setText: (value) => history.set(value, 'text'),
    undo: history.undo,
    redo: history.redo,
    save,
  }
}

/** Ctrl+S / Ctrl+Z / Ctrl+Y nas seções que editam YAML. */
export function useFileShortcuts(file: YamlFile) {
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      const mod = event.ctrlKey || event.metaKey
      if (!mod) return
      const key = event.key.toLowerCase()
      const typing = event.target instanceof HTMLElement && Boolean(event.target.closest('input, textarea, select'))
      if (key === 's') {
        event.preventDefault()
        void file.save()
      } else if (!typing && key === 'z') {
        event.preventDefault()
        if (event.shiftKey) file.redo()
        else file.undo()
      } else if (!typing && key === 'y') {
        event.preventDefault()
        file.redo()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [file])

  useEffect(() => {
    const onLeave = (event: BeforeUnloadEvent) => {
      if (file.dirty) event.preventDefault()
    }
    window.addEventListener('beforeunload', onLeave)
    return () => window.removeEventListener('beforeunload', onLeave)
  }, [file.dirty])
}
