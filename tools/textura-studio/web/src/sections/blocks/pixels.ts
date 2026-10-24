export type Tool = 'lapis' | 'borracha' | 'conta-gotas' | 'selecionar'

export interface Cell {
  x: number
  y: number
}

export interface Rect {
  x: number
  y: number
  w: number
  h: number
}

export function hex(r: number, g: number, b: number): string {
  return `#${[r, g, b].map((value) => value.toString(16).padStart(2, '0')).join('')}`
}

export function cellAt(canvas: HTMLCanvasElement, clientX: number, clientY: number): Cell {
  const rect = canvas.getBoundingClientRect()
  const x = Math.floor(((clientX - rect.left) / rect.width) * canvas.width)
  const y = Math.floor(((clientY - rect.top) / rect.height) * canvas.height)
  return { x: Math.min(Math.max(x, 0), canvas.width - 1), y: Math.min(Math.max(y, 0), canvas.height - 1) }
}

export function rectBetween(a: Cell, b: Cell): Rect {
  return { x: Math.min(a.x, b.x), y: Math.min(a.y, b.y), w: Math.abs(a.x - b.x) + 1, h: Math.abs(a.y - b.y) + 1 }
}

export function colorAt(canvas: HTMLCanvasElement, cell: Cell): string | null {
  const context = canvas.getContext('2d')
  if (!context) return null
  const [r, g, b, a] = context.getImageData(cell.x, cell.y, 1, 1).data
  return a === 0 ? null : hex(r, g, b)
}

export function copyRect(canvas: HTMLCanvasElement, rect: Rect): ImageData | null {
  return canvas.getContext('2d')?.getImageData(rect.x, rect.y, rect.w, rect.h) ?? null
}

export function stamp(canvas: HTMLCanvasElement, data: ImageData, at: Cell): void {
  const context = canvas.getContext('2d')
  if (!context) return
  const buffer = document.createElement('canvas')
  buffer.width = data.width
  buffer.height = data.height
  buffer.getContext('2d')?.putImageData(data, 0, 0)
  context.imageSmoothingEnabled = false
  context.clearRect(at.x, at.y, data.width, data.height)
  context.drawImage(buffer, at.x, at.y)
}

export function drawScaled(target: HTMLCanvasElement, source: CanvasImageSource): void {
  const context = target.getContext('2d')
  if (!context) return
  context.imageSmoothingEnabled = false
  context.clearRect(0, 0, target.width, target.height)
  context.drawImage(source, 0, 0, target.width, target.height)
}

export function pickPng(): Promise<File | null> {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = 'image/png'
    input.onchange = () => resolve(input.files?.[0] ?? null)
    input.click()
  })
}

export function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('Imagem ilegível'))
    image.src = src
  })
}
