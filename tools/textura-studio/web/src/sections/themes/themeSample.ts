import { isObject } from '../../editor/tree'

type Json = Record<string, unknown>

const keys = (value: unknown): string[] => (isObject(value) ? Object.keys(value) : [])

/** Uma tela de amostra com uma peça de cada tipo, usando tudo que o tema declara. */
export function themeSample(name: string, theme: Json): string {
  const styles = keys(theme.text)
  const surfaces = keys(theme.surfaces)
  const looks = keys(isObject(theme.button) ? theme.button.looks : undefined)
  const icons = keys(theme.icons)
  const iconSize = typeof theme['icon-size'] === 'number' ? theme['icon-size'] : 9
  const onSurface = styles.filter((style) => !style.endsWith('-bare'))
  const bare = styles.filter((style) => style.endsWith('-bare'))
  const surface = surfaces[0]

  const card = (children: unknown[], extra: Json = {}) => ({ column: children, w: 'grow', h: 'fit', gap: 3, pad: 5, ...(surface ? { surface } : {}), ...extra })
  const buttons = [
    { button: 'Padrão', w: 'grow' },
    ...looks.flatMap((look) => [
      { button: look, look, w: 'grow' },
      { button: 'ativo', look, selected: true, w: 'grow' },
      { button: 'off', look, disabled: true, w: 'grow' },
    ]),
  ]

  const doc = {
    title: `Tema ${name}`,
    theme: name,
    close: 'Fechar',
    column: [
      surfaces.length > 0 && {
        name: 'Superfícies',
        row: surfaces.map((id) => ({ column: [{ text: id, style: 'heading', w: 'fit' }, { text: 'surface', style: 'muted', w: 'fit' }], surface: id, w: 'grow', h: 'fit', pad: 5, gap: 2 })),
        w: 'grow',
        h: 'fit',
        gap: 4,
      },
      {
        name: 'Estilos',
        row: [
          card(onSurface.map((style) => ({ text: `${style} · Aa`, style, w: 'fit' }))),
          { column: bare.map((style) => ({ text: `${style} · Aa`, style, w: 'fit' })), w: 'grow', h: 'fit', gap: 3, pad: 5 },
        ],
        w: 'grow',
        h: 'fit',
        gap: 4,
      },
      {
        name: 'Botões',
        grid: buttons,
        columns: Math.min(4, Math.max(1, buttons.length)),
        w: 'grow',
        h: 'fit',
        gap: 3,
      },
      {
        name: 'Controles',
        row: [
          card([
            { nav: 'Menu', icon: icons[0] ? `ui:${icons[0]}` : undefined, w: 'grow' },
            { nav: 'Ativo', selected: true, w: 'grow' },
          ], { gap: 1, pad: 2 }),
          card([
            { row: [{ chip: '128', icon: 'item:gold_nugget' }, { space: 'grow' }, { switch: true, bind: 'a' }, { switch: false, bind: 'b' }], w: 'grow', h: 'fit', gap: 4, cross: 'center' },
            { segmented: ['Um', 'Dois', 'Três'], selected: 1, bind: 'seg', w: 'grow' },
            { dropdown: 'Opção', options: ['Opção', 'Outra'], bind: 'drop', w: 'grow' },
            { slider: 60, min: 0, max: 100, step: 10, unit: '%', bind: 'slide', w: 'grow' },
          ]),
        ],
        w: 'grow',
        h: 'fit',
        gap: 4,
      },
      {
        name: 'Slots e ícones',
        row: [
          { slots: [5, 1], items: [{ at: 0, item: 'diamond', count: 3 }, { at: 1, item: 'oak_planks', count: 64 }, { at: 3, item: 'iron_sword' }] },
          { space: 'grow' },
          { row: icons.slice(0, 14).map((icon) => ({ icon: `ui:${icon}`, size: iconSize })), w: 'fit', h: 'fit', gap: 3, cross: 'center' },
        ],
        w: 'grow',
        h: 'fit',
        gap: 4,
        cross: 'center',
      },
    ].filter(Boolean),
    w: 390,
    h: 'fit',
    gap: 6,
  }
  return JSON.stringify(doc)
}
