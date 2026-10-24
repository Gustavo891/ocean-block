import { useCallback, useEffect, useRef, useState } from 'react'
import { BoxSelect, ClipboardPaste, Copy, Eraser, ImagePlus, Pencil, Pipette, Save, Undo2, Upload, Wand2, X } from 'lucide-react'
import { GridCanvas } from './GridCanvas'
import { colorAt, copyRect, drawScaled, loadImage, pickPng, rectBetween, stamp, type Cell, type Rect, type Tool } from './pixels'

interface PixelEditorProps {
  src: string
  onSave: (png: Blob) => Promise<void>
  onUpload: (png: File) => Promise<void>
}

type Target = 'main' | 'ref'

interface Selection {
  target: Target
  rect: Rect
}

const MAX_HISTORY = 50
const MAIN_VIEW = 384
const REF_VIEW = 320

export function PixelEditor({ src, onSave, onUpload }: PixelEditorProps) {
  const main = useRef<HTMLCanvasElement>(null)
  const ref = useRef<HTMLCanvasElement>(null)
  const history = useRef<ImageData[]>([])
  const drawing = useRef(false)
  const anchor = useRef<Cell | null>(null)
  const [tool, setTool] = useState<Tool>('lapis')
  const [color, setColor] = useState('#8a6a3c')
  const [size, setSize] = useState({ w: 16, h: 16 })
  const [refSize, setRefSize] = useState<{ w: number; h: number } | null>(null)
  const [selection, setSelection] = useState<Selection | null>(null)
  const [clipboard, setClipboard] = useState<ImageData | null>(null)
  const [pasting, setPasting] = useState(false)
  const [hover, setHover] = useState<Cell | null>(null)
  const [dirty, setDirty] = useState(false)
  const [busy, setBusy] = useState(false)
  const [undoCount, setUndoCount] = useState(0)

  useEffect(() => {
    let alive = true
    void loadImage(src).then((image) => {
      const target = main.current
      if (!alive || !target) return
      target.width = image.naturalWidth
      target.height = image.naturalHeight
      drawScaled(target, image)
      history.current = []
      setUndoCount(0)
      setSize({ w: image.naturalWidth, h: image.naturalHeight })
      setDirty(false)
    })
    return () => {
      alive = false
    }
  }, [src])

  const remember = () => {
    const target = main.current
    const context = target?.getContext('2d')
    if (!target || !context) return
    history.current = [...history.current.slice(-(MAX_HISTORY - 1)), context.getImageData(0, 0, target.width, target.height)]
    setUndoCount(history.current.length)
  }

  const undo = useCallback(() => {
    const context = main.current?.getContext('2d')
    const last = history.current.at(-1)
    if (!context || !last) return
    history.current = history.current.slice(0, -1)
    setUndoCount(history.current.length)
    context.putImageData(last, 0, 0)
    setDirty(history.current.length > 0)
  }, [])

  const copy = useCallback(() => {
    if (!selection) return
    const canvas = selection.target === 'main' ? main.current : ref.current
    const data = canvas ? copyRect(canvas, selection.rect) : null
    if (data) setClipboard(data)
  }, [selection])

  const startPaste = useCallback(() => {
    if (clipboard) setPasting(true)
  }, [clipboard])

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.target instanceof HTMLInputElement) return
      const key = event.key.toLowerCase()
      if (event.ctrlKey && key === 'c') copy()
      else if (event.ctrlKey && key === 'v') startPaste()
      else if (event.ctrlKey && key === 'z') undo()
      else if (key === 'escape') {
        setPasting(false)
        setSelection(null)
      } else return
      event.preventDefault()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [copy, startPaste, undo])

  const paint = (cell: Cell) => {
    const context = main.current?.getContext('2d')
    if (!context) return
    if (tool === 'borracha') context.clearRect(cell.x, cell.y, 1, 1)
    else {
      context.fillStyle = color
      context.fillRect(cell.x, cell.y, 1, 1)
    }
    setDirty(true)
  }

  const pick = (target: Target, cell: Cell) => {
    const canvas = target === 'main' ? main.current : ref.current
    const picked = canvas ? colorAt(canvas, cell) : null
    if (picked) setColor(picked)
    setTool('lapis')
  }

  const down = (target: Target, cell: Cell) => {
    if (target === 'main' && pasting && clipboard && main.current) {
      remember()
      stamp(main.current, clipboard, cell)
      setPasting(false)
      setDirty(true)
      return
    }
    if (tool === 'conta-gotas') return pick(target, cell)
    if (tool === 'selecionar') {
      anchor.current = cell
      setSelection({ target, rect: rectBetween(cell, cell) })
      return
    }
    if (target !== 'main') return pick(target, cell)
    remember()
    drawing.current = true
    paint(cell)
  }

  const move = (target: Target, cell: Cell, pressed: boolean) => {
    if (target === 'main') setHover(cell)
    if (!pressed) return
    if (tool === 'selecionar' && anchor.current && selection?.target === target) {
      setSelection({ target, rect: rectBetween(anchor.current, cell) })
    } else if (target === 'main' && drawing.current) paint(cell)
  }

  const up = () => {
    drawing.current = false
    anchor.current = null
  }

  const loadReference = async () => {
    const file = await pickPng()
    const canvas = ref.current
    if (!file || !canvas) return
    const url = URL.createObjectURL(file)
    try {
      const image = await loadImage(url)
      canvas.width = image.naturalWidth
      canvas.height = image.naturalHeight
      drawScaled(canvas, image)
      setRefSize({ w: image.naturalWidth, h: image.naturalHeight })
    } finally {
      URL.revokeObjectURL(url)
    }
  }

  const useAsBase = () => {
    if (!main.current || !ref.current || !refSize) return
    remember()
    drawScaled(main.current, ref.current)
    setDirty(true)
  }

  const save = async () => {
    const target = main.current
    if (!target) return
    setBusy(true)
    try {
      const blob = await new Promise<Blob | null>((resolve) => target.toBlob(resolve, 'image/png'))
      if (blob) await onSave(blob)
      setDirty(false)
    } finally {
      setBusy(false)
    }
  }

  const upload = async () => {
    const file = await pickPng()
    if (!file) return
    setBusy(true)
    try {
      await onUpload(file)
    } finally {
      setBusy(false)
    }
  }

  const scale = Math.max(4, Math.floor(MAIN_VIEW / Math.max(size.w, size.h)))
  const refScale = refSize ? Math.min(16, REF_VIEW / Math.max(refSize.w, refSize.h)) : 1
  const ghost = pasting && clipboard && hover ? { x: hover.x, y: hover.y, w: clipboard.width, h: clipboard.height } : null
  const toolButton = (id: Tool, title: string, Icon: typeof Pencil) => (
    <button className={`icon-btn${tool === id ? ' on' : ''}`} title={title} onClick={() => setTool(id)}>
      <Icon />
    </button>
  )

  return (
    <div className="pixel-editor">
      <div className="pixel-tools">
        {toolButton('lapis', 'Lápis', Pencil)}
        {toolButton('borracha', 'Borracha (transparente)', Eraser)}
        {toolButton('conta-gotas', 'Conta-gotas (também na referência)', Pipette)}
        {toolButton('selecionar', 'Selecionar área (aqui ou na referência)', BoxSelect)}
        <input type="color" value={color} title="Cor" onChange={(event) => setColor(event.target.value)} />
        <span className="tool-sep" />
        <button className="icon-btn" title="Copiar seleção (Ctrl+C)" disabled={!selection} onClick={copy}>
          <Copy />
        </button>
        <button className={`icon-btn${pasting ? ' on' : ''}`} title="Colar (Ctrl+V) e clicar onde vai o canto" disabled={!clipboard} onClick={startPaste}>
          <ClipboardPaste />
        </button>
        <button className="icon-btn" title="Desfazer (Ctrl+Z)" disabled={undoCount === 0} onClick={undo}>
          <Undo2 />
        </button>
        <span style={{ flex: 1 }} />
        <button className="btn ghost" disabled={busy} onClick={() => void upload()}>
          <Upload /> Enviar PNG
        </button>
        <button className={`btn primary${dirty ? '' : ' done'}`} disabled={busy || !dirty} onClick={() => void save()}>
          <Save /> {busy ? 'Salvando…' : 'Salvar'}
        </button>
      </div>

      <div className="pixel-boards">
        <div className="pixel-board">
          <span className="eyebrow">
            Textura · {size.w}×{size.h} px{pasting ? ' · clique para colar (Esc cancela)' : ''}
          </span>
          <GridCanvas
            canvas={main}
            width={size.w}
            height={size.h}
            scale={scale}
            selection={selection?.target === 'main' ? selection.rect : null}
            ghost={ghost}
            onDown={(cell) => down('main', cell)}
            onMove={(cell, pressed) => move('main', cell, pressed)}
            onUp={up}
          />
        </div>

        <div className="pixel-board">
          <div className="pixel-ref-head">
            <span className="eyebrow">Referência{refSize ? ` · ${refSize.w}×${refSize.h}` : ''}</span>
            <span style={{ flex: 1 }} />
            <button className="icon-btn" title="Carregar PNG de referência (não é salvo)" onClick={() => void loadReference()}>
              <ImagePlus />
            </button>
            <button className="icon-btn" title="Usar como base: desenha a referência inteira na textura" disabled={!refSize} onClick={useAsBase}>
              <Wand2 />
            </button>
            <button
              className="icon-btn"
              title="Tirar referência"
              disabled={!refSize}
              onClick={() => {
                setRefSize(null)
                if (selection?.target === 'ref') setSelection(null)
              }}
            >
              <X />
            </button>
          </div>
          <div style={{ display: refSize ? 'block' : 'none' }}>
            <GridCanvas
              canvas={ref}
              width={refSize?.w ?? 1}
              height={refSize?.h ?? 1}
              scale={refScale}
              selection={selection?.target === 'ref' ? selection.rect : null}
              ghost={null}
              onDown={(cell) => down('ref', cell)}
              onMove={(cell, pressed) => move('ref', cell, pressed)}
              onUp={up}
            />
          </div>
          {!refSize && <p className="hint-text">Carregue um PNG para pegar cores, selecionar e copiar pedaços ou usar como base. Ele não é salvo.</p>}
        </div>
      </div>
    </div>
  )
}
