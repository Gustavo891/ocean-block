import { useEffect, useRef, useState } from 'react'
import { Eraser, Pipette, Pencil, Save, Undo2, Upload } from 'lucide-react'

type Tool = 'lapis' | 'conta-gotas' | 'borracha'

interface PixelEditorProps {
  src: string
  onSave: (png: Blob) => Promise<void>
  onUpload: (png: File) => Promise<void>
}

const MAX_HISTORY = 50

function hex(r: number, g: number, b: number): string {
  return `#${[r, g, b].map((value) => value.toString(16).padStart(2, '0')).join('')}`
}

function pickPng(): Promise<File | null> {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = 'image/png'
    input.onchange = () => resolve(input.files?.[0] ?? null)
    input.click()
  })
}

export function PixelEditor({ src, onSave, onUpload }: PixelEditorProps) {
  const canvas = useRef<HTMLCanvasElement>(null)
  const history = useRef<ImageData[]>([])
  const drawing = useRef(false)
  const [tool, setTool] = useState<Tool>('lapis')
  const [color, setColor] = useState('#8a6a3c')
  const [size, setSize] = useState({ w: 16, h: 16 })
  const [dirty, setDirty] = useState(false)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const image = new Image()
    image.onload = () => {
      const target = canvas.current
      const context = target?.getContext('2d')
      if (!target || !context) return
      target.width = image.naturalWidth
      target.height = image.naturalHeight
      context.clearRect(0, 0, target.width, target.height)
      context.drawImage(image, 0, 0)
      history.current = []
      setSize({ w: image.naturalWidth, h: image.naturalHeight })
      setDirty(false)
    }
    image.src = src
  }, [src])

  const pixelAt = (event: React.PointerEvent<HTMLCanvasElement>) => {
    const target = event.currentTarget
    const rect = target.getBoundingClientRect()
    const x = Math.floor(((event.clientX - rect.left) / rect.width) * target.width)
    const y = Math.floor(((event.clientY - rect.top) / rect.height) * target.height)
    return { x: Math.min(Math.max(x, 0), target.width - 1), y: Math.min(Math.max(y, 0), target.height - 1) }
  }

  const apply = (event: React.PointerEvent<HTMLCanvasElement>) => {
    const context = canvas.current?.getContext('2d')
    if (!context) return
    const { x, y } = pixelAt(event)
    if (tool === 'conta-gotas') {
      const [r, g, b] = context.getImageData(x, y, 1, 1).data
      setColor(hex(r, g, b))
      setTool('lapis')
      return
    }
    if (tool === 'borracha') context.clearRect(x, y, 1, 1)
    else {
      context.fillStyle = color
      context.fillRect(x, y, 1, 1)
    }
    setDirty(true)
  }

  const start = (event: React.PointerEvent<HTMLCanvasElement>) => {
    const target = canvas.current
    const context = target?.getContext('2d')
    if (!target || !context) return
    if (tool !== 'conta-gotas') {
      history.current = [...history.current.slice(-(MAX_HISTORY - 1)), context.getImageData(0, 0, target.width, target.height)]
      drawing.current = true
      event.currentTarget.setPointerCapture(event.pointerId)
    }
    apply(event)
  }

  const undo = () => {
    const context = canvas.current?.getContext('2d')
    const last = history.current.at(-1)
    if (!context || !last) return
    history.current = history.current.slice(0, -1)
    context.putImageData(last, 0, 0)
    setDirty(history.current.length > 0)
  }

  const save = async () => {
    const target = canvas.current
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

  const scale = Math.max(4, Math.floor(256 / Math.max(size.w, size.h)))

  return (
    <div className="pixel-editor">
      <div className="pixel-tools">
        <button className={`icon-btn${tool === 'lapis' ? ' on' : ''}`} title="Lápis" onClick={() => setTool('lapis')}>
          <Pencil />
        </button>
        <button className={`icon-btn${tool === 'borracha' ? ' on' : ''}`} title="Borracha (transparente)" onClick={() => setTool('borracha')}>
          <Eraser />
        </button>
        <button className={`icon-btn${tool === 'conta-gotas' ? ' on' : ''}`} title="Conta-gotas" onClick={() => setTool('conta-gotas')}>
          <Pipette />
        </button>
        <input type="color" value={color} title="Cor" onChange={(event) => setColor(event.target.value)} />
        <span style={{ flex: 1 }} />
        <button className="icon-btn" title="Desfazer" disabled={history.current.length === 0} onClick={undo}>
          <Undo2 />
        </button>
      </div>
      <canvas
        ref={canvas}
        className="pixel-canvas"
        style={{ width: size.w * scale, height: size.h * scale, backgroundSize: `${scale * 2}px ${scale * 2}px` }}
        onPointerDown={start}
        onPointerMove={(event) => drawing.current && apply(event)}
        onPointerUp={() => (drawing.current = false)}
        onPointerCancel={() => (drawing.current = false)}
      />
      <span className="meta">
        {size.w}×{size.h} px
      </span>
      <div className="pixel-actions">
        <button className="btn ghost" disabled={busy} onClick={() => void upload()}>
          <Upload /> Enviar PNG
        </button>
        <button className={`btn primary${dirty ? '' : ' done'}`} disabled={busy || !dirty} onClick={() => void save()}>
          <Save /> {busy ? 'Salvando…' : 'Salvar'}
        </button>
      </div>
    </div>
  )
}
