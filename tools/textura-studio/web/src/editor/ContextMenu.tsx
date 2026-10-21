import { useEffect } from 'react'

export interface MenuItem {
  label: string
  shortcut?: string
  danger?: boolean
  disabled?: boolean
  run: () => void
}

export type MenuEntry = MenuItem | 'divider'

interface ContextMenuProps {
  x: number
  y: number
  items: MenuEntry[]
  onClose: () => void
}

export function ContextMenu({ x, y, items, onClose }: ContextMenuProps) {
  useEffect(() => {
    const close = () => onClose()
    const onKey = (event: KeyboardEvent) => event.key === 'Escape' && onClose()
    window.addEventListener('mousedown', close)
    window.addEventListener('keydown', onKey)
    window.addEventListener('blur', close)
    return () => {
      window.removeEventListener('mousedown', close)
      window.removeEventListener('keydown', onKey)
      window.removeEventListener('blur', close)
    }
  }, [onClose])

  const left = Math.min(x, window.innerWidth - 230)
  const top = Math.min(y, window.innerHeight - items.length * 30 - 16)

  return (
    <div className="context-menu" style={{ left, top }} onMouseDown={(event) => event.stopPropagation()}>
      {items.map((item, index) =>
        item === 'divider' ? (
          <hr key={index} />
        ) : (
          <button
            key={index}
            className={item.danger ? 'danger' : ''}
            disabled={item.disabled}
            onClick={() => {
              item.run()
              onClose()
            }}
          >
            {item.label}
            {item.shortcut && <kbd>{item.shortcut}</kbd>}
          </button>
        ),
      )}
    </div>
  )
}
