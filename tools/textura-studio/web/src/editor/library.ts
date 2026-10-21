import type { Json, NodeKind } from './tree'

export interface LibraryItem {
  id: string
  kind: NodeKind
  label: string
  hint: string
  group: 'Layout' | 'Texto' | 'Controles' | 'Mídia'
  make: () => Json
}

/** Peças que o motor do Textura já desenha, com valores iniciais parecidos com os do Studio. */
export const LIBRARY: LibraryItem[] = [
  { id: 'row', kind: 'row', label: 'Row', hint: 'lado a lado', group: 'Layout', make: () => ({ row: [], w: 'grow', h: 'fit', gap: 4, pad: 0, align: 'start', cross: 'center' }) },
  { id: 'column', kind: 'column', label: 'Column', hint: 'um embaixo do outro', group: 'Layout', make: () => ({ column: [], w: 'grow', h: 'fit', gap: 4, pad: 0, align: 'start', cross: 'start' }) },
  { id: 'grid', kind: 'grid', label: 'Grid', hint: 'colunas iguais', group: 'Layout', make: () => ({ grid: [], columns: 2, w: 'grow', h: 'fit', gap: 4, pad: 0 }) },
  { id: 'box', kind: 'box', label: 'Box', hint: 'empilhado / livre', group: 'Layout', make: () => ({ box: [], w: 'grow', h: 40, align: 'center', cross: 'center' }) },
  {
    id: 'card',
    kind: 'column',
    label: 'Card',
    hint: 'painel com fundo',
    group: 'Layout',
    make: () => ({ name: 'Card', column: [{ text: 'Card', style: 'heading', w: 'fit' }], w: 'grow', h: 'fit', gap: 4, pad: 5, surface: 'sheet' }),
  },
  {
    id: 'band',
    kind: 'row',
    label: 'Heading band',
    hint: 'faixa de título',
    group: 'Layout',
    make: () => ({ name: 'Heading band', row: [{ text: 'Título', style: 'heading', w: 'fit' }], w: 'grow', h: 'fit', pad: 4, surface: 'head', cross: 'center' }),
  },
  {
    id: 'well',
    kind: 'column',
    label: 'Well',
    hint: 'área rebaixada',
    group: 'Layout',
    make: () => ({ name: 'Well', column: [], w: 'grow', h: 'fit', gap: 2, pad: 4, surface: 'well' }),
  },
  { id: 'space', kind: 'space', label: 'Space', hint: 'espaço fixo', group: 'Layout', make: () => ({ space: 6 }) },
  { id: 'grow', kind: 'space', label: 'Spacer', hint: 'empurra o resto', group: 'Layout', make: () => ({ space: 'grow' }) },

  { id: 'heading', kind: 'text', label: 'Heading', hint: 'título em negrito', group: 'Texto', make: () => ({ text: 'Título', style: 'heading', align: 'left', w: 'fit' }) },
  { id: 'text', kind: 'text', label: 'Text', hint: 'uma linha', group: 'Texto', make: () => ({ text: 'Texto', style: 'body', align: 'left', w: 'fit' }) },
  { id: 'muted', kind: 'text', label: 'Muted text', hint: 'texto secundário', group: 'Texto', make: () => ({ text: 'Detalhe', style: 'muted', align: 'left', w: 'fit' }) },
  { id: 'pair', kind: 'pair', label: 'Label · value', hint: 'rótulo e valor', group: 'Texto', make: () => ({ pair: ['Preço', '100 Coins'], style: 'body', w: 'grow' }) },
  { id: 'chip', kind: 'chip', label: 'Chip', hint: 'etiqueta com ícone', group: 'Texto', make: () => ({ chip: '100', icon: 'item:gold_nugget' }) },

  { id: 'button', kind: 'button', label: 'Button', hint: 'botão do tema', group: 'Controles', make: () => ({ button: 'Botão', w: 'grow', onClick: { action: 'close' } }) },
  { id: 'theme-button', kind: 'button', label: 'Theme button', hint: 'botão claro', group: 'Controles', make: () => ({ button: 'Comprar', icon: 'ui:cart', look: 'light', w: 'grow' }) },
  { id: 'nav', kind: 'nav', label: 'Sidebar button', hint: 'item de menu lateral', group: 'Controles', make: () => ({ nav: 'Menu', w: 'grow' }) },
  { id: 'switch', kind: 'switch', label: 'Switch', hint: 'liga / desliga', group: 'Controles', make: () => ({ switch: true, bind: 'opcao' }) },
  { id: 'segmented', kind: 'segmented', label: 'Segmented', hint: 'escolha entre opções', group: 'Controles', make: () => ({ segmented: ['A', 'B', 'C'], selected: 0, bind: 'escolha', w: 'grow' }) },
  { id: 'dropdown', kind: 'dropdown', label: 'Dropdown', hint: 'lista de opções', group: 'Controles', make: () => ({ dropdown: 'Opção 1', options: ['Opção 1', 'Opção 2'], bind: 'lista', w: 'grow' }) },
  { id: 'slider', kind: 'slider', label: 'Slider', hint: 'valor numérico', group: 'Controles', make: () => ({ slider: 50, min: 0, max: 100, step: 5, unit: '%', bind: 'valor', w: 'grow' }) },

  { id: 'icon', kind: 'icon', label: 'Icon', hint: 'ícone do tema', group: 'Mídia', make: () => ({ icon: 'ui:chest', size: 9 }) },
  { id: 'item-icon', kind: 'icon', label: 'Item icon', hint: 'item do Minecraft', group: 'Mídia', make: () => ({ icon: 'item:diamond', size: 16 }) },
  { id: 'image', kind: 'image', label: 'Image', hint: 'arte de ui/', group: 'Mídia', make: () => ({ image: 'ui:stone/slot' }) },
  {
    id: 'slots',
    kind: 'slots',
    label: 'Item slots',
    hint: 'grade de itens',
    group: 'Mídia',
    make: () => ({ slots: [3, 1], items: [{ at: 0, item: 'diamond', count: 1 }], align: 'start' }),
  },
]

export const LIBRARY_MIME = 'application/x-textura-piece'
