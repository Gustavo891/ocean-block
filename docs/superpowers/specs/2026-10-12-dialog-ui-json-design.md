# Telas de dialog em JSON — design

**Data:** 2026-10-12
**Módulo:** `plugins/textura`
**Referência:** `dialog-ui.md` (engine "Drawn UI inside the vanilla dialog", branch `feat/dialog-ui` da rede Screen)
**Status:** rascunho para revisão

## Objetivo

Desenhar telas ricas (painéis, cabeçalhos, ícones, switches, seletores segmentados, tooltips) dentro do **dialog vanilla** (1.21.6+), sem mod no cliente, descrevendo cada tela num arquivo JSON em `configs/ui/`. Outros sistemas abrem a tela por id e fornecem o estado dos controles (`bind`). Cada clique volta ao servidor, atualiza o estado e reabre a tela.

A técnica é a do documento de referência: cada imagem vira glyph de fonte, a tela é escrita como linhas de texto de 9 px posicionadas com caracteres de espaço, e áreas clicáveis são runs de espaços com `clickEvent`/`hoverEvent`. O engine de layout e render é **portado** do doc. O que é novo aqui é o front-end JSON, o tema, as surfaces de altura arbitrária e a integração com o pipeline de pack do Textura.

## Decisões

| Decisão | Escolha |
|---|---|
| Onde mora | `plugins/textura`, que já monta o pack com `creative` |
| Origem das telas | Arquivos `configs/ui/<id>.json`, relidos por `/textura reload` |
| Estado dos controles | Quem abre a tela fornece get/set por `bind`; valores do JSON são default |
| Arte | Ícones reaproveitam glyphs existentes; surfaces e controles usam arte placeholder gerada, substituível sem tocar em código |
| Engine | Layout (`row/column/box/grid`, `Grow/Fit/px`, `snap`) e renderer (spans primeiro, `#hit`, espaços binários, largura de linha exata) portados do doc |
| Kit | Construído em memória no `/textura gerar`, sem `mappings.json`: o mesmo processo gera o pack e consome o kit |
| Status do pack | `PlayerResourcePackStatusEvent` direto no Paper (sem proxy) |
| DSL Kotlin pública | Fora do escopo. O compilador JSON gera os nós diretamente |

## Formato do JSON

O arquivo **é** o nó raiz (como no exemplo). O id da tela é o nome do arquivo sem `.json`. Chaves extras do raiz: `theme` (obrigatório), `title` (MiniMessage, opcional) e `close` (rótulo do botão de fechar, opcional, default `"Fechar"`; `null` remove o botão).

### Containers

Um objeto é container quando tem **uma** das chaves `column`, `row`, `box`, `grid` com um array de filhos.

| Chave | Tipo | Default | Significado |
|---|---|---|---|
| `w`, `h` | número, `"grow"`, `"fit"` | `"fit"` | Tamanho. Número = px |
| `gap` | int | 0 | Espaço entre filhos (row/column/grid) |
| `pad` | int | 0 | Padding nos quatro lados |
| `align` | `start`/`center`/`end` | `start` | Eixo principal |
| `cross` | `start`/`center`/`end` | `start` | Eixo cruzado |
| `snap` | int | 0 | Arredonda o y absoluto de cada filho para múltiplo (column/grid) — §6.5 do doc |
| `columns` | int | 1 | Só `grid` |
| `x`, `y` | int | — | Posição absoluta dentro do pai, fora do fluxo |
| `surface` | string | — | Fundo do container, resolvido pelo tema (`sheet`, `head`…) |
| `name` | string | — | Rótulo do editor. Ignorado no render, aparece nos avisos |

### Folhas

| Forma | Campos | Vira |
|---|---|---|
| `{"space": n}` | — | caixa vazia n×n |
| `{"text": "…"}` | `style`, `align` (`left`/`center`/`right`), `w`, `icon`, `color`, `bold`, `scale`, `tooltip` | texto de uma linha (9 px). Com `icon`, vira `row(gap = tema.iconGap, cross = center) { ícone; texto }` |
| `{"icon": "ui:x"}` | `size` | ícone na caixa `size`×`size` |
| `{"image": "ui:x"}` | `w` | imagem; com `.slice` estica até `w` |
| `{"switch": bool}` | `bind`, `tooltip` | arte on/off do tema + hotspot que inverte o valor |
| `{"segmented": ["A","B"]}` | `selected`, `bind`, `w`, `tooltip` | segmentos lado a lado, cada um com hotspot próprio que seleciona o índice |
| `{"button": "rótulo"}` | `bind`, `w`, `icon`, `tooltip` | botão com arte fatiada do tema; clique dispara a ação do `bind` |

Regras:

- `scale` diferente de `1` gera aviso e é tratado como `1`. Bitmap não escala sem uma fonte por escala.
- `style` resolve no tema para cor + bold. `color`/`bold` no nó sobrepõem o estilo.
- `tooltip` é MiniMessage com `\n`. O placeholder `{value}` é trocado pelo rótulo do valor atual (`switch`: rótulos on/off do tema; `segmented`: o texto do segmento selecionado). Linhas sem cor recebem a cor `tooltip` do tema.
- Ids de imagem/ícone aceitam `[a-z0-9_/-]`.
- Chave desconhecida gera aviso com o caminho do nó (`Social > Body > Private Messages`, montado a partir de `name`). Nó sem forma reconhecível é descartado com aviso. Nada derruba o load das outras telas.

## Tema

`configs/ui/*.yml` (mesma pasta das telas, flat). Cada chave de topo é um tema:

```yaml
stone:
  icon-gap: 3
  text:
    heading: { color: "#ffffff", bold: true }
    body:    { color: "#e6e6e6" }
    muted:   { color: "#9a9a9a" }
    tooltip: { color: "#c8c8c8" }
  surfaces:
    sheet: ui:stone/sheet
    head:  ui:stone/head
  switch:
    on:  ui:stone/switch_on
    off: ui:stone/switch_off
    labels: { on: "On", off: "Off" }
  segmented:
    art: ui:stone/segment
    selected-art: ui:stone/segment_selected
    text-style: muted
    selected-text-style: body
    pad: 4
  button:
    art: ui:stone/button
    text-style: body
  icons:
    users: crown
    mail: scroll
```

`icons` é um alias opcional de `ui:<nome>` para um glyph existente.

## Arte e resolução de ids

Um id `ui:<path>` resolve, em ordem:

1. `resource/assets/oneblock/textures/ui/<path>.png` (arte de UI);
2. alias em `tema.icons[<path>]`, que vira um glyph id;
3. glyph com id `<path>` em `configs/glyphs` (usa o PNG de `caminho`).

Se nada resolver, sai um aviso e a caixa fica vazia (com o tamanho declarado). Os PNGs em `resource/` continuam sendo copiados para o pack como hoje. Os arquivos `*.slice` ficam fora da cópia.

**Fatia horizontal** (`<nome>.png.slice` com `esquerda direita`): igual ao doc §4.3, com tampas e meios de 1, 2, 4 … 256 px. Usada por botão e segmento, peças de uma só altura que esticam na largura.

**Surface** (`<nome>.png.slice` com `esquerda direita topo base`): nine-slice. **Não** é montada por peças em runtime. O motivo:

- O cliente desenha linha por linha, e a linha seguinte desenha por cima da anterior.
- Uma peça de fundo que começa numa linha mais baixa que um ícone sobreposto apaga esse ícone.

Então o pack **pré-renderiza uma textura por tamanho distinto** (`surface`, largura, altura) que as telas carregadas usam. Cada surface vira um glyph único que começa na linha do topo do container. Tamanho máximo 399×256; acima disso, aviso e o fundo não é desenhado.

O conjunto de tamanhos sai rodando o layout de todas as telas no `/textura gerar` com o estado default. Alturas são estáticas por tela (texto ocupa sempre 9 px, controles têm tamanho fixo). Larguras também, porque `segmented` reserva a largura do rótulo no estilo mais largo (com bold). Em runtime, um tamanho que não está no kit (tela editada sem regerar) não desenha o fundo e avisa uma vez: `surface 'sheet' 248x61 não pré-gerada — rode /textura gerar`.

**Ícone com `size`**: o PNG é desenhado escalado para `size` px de altura. Cada par `(ícone, size)` usado pelas telas vira uma variante no pack, com provider `height` proporcional. O conjunto é coletado no `/textura gerar`, do mesmo jeito que as surfaces. O advance segue a conta do cliente: `round(largura opaca × escala) + 1`. Variante com menos de 8 px recebe padding transparente embaixo (doc §4.2). Par ausente em runtime cai no PNG em tamanho nativo, centralizado na caixa, com aviso.

**Placeholder**: arte do tema `stone` gerada uma vez (retângulos com borda de 1 px nas cores do tema, switch 16×8, segmento 24×12 com slice `2 2`, botão 40×16 com slice `2 2`) e versionada em `resource/assets/oneblock/textures/ui/stone/`. Para trocar o visual basta substituir os PNGs.

## Pack (`pack/UiPackWriter.kt`)

Roda dentro do `ResourcePackBuilder.build()`, depois de `fonts.write`:

1. Constrói o kit-base a partir dos arquivos: imagens, fatias e larguras de texto. As larguras são medidas dos PNGs de `configs/fonts/custom.json`: `round((coluna opaca mais à direita + 1) × height / altura da célula) + 1`, espaço = 4 e bold = +1.
2. Faz o layout de cada tela com o kit-base e coleta `(surface, w, h)` e `(ícone, size)`.
3. Compõe as texturas de surface, as peças de fatia e as imagens com menos de 8 px de altura (com padding embaixo, doc §4.2). Grava em `assets/oneblock/textures/ui_gen/…`.
4. Atribui codepoints estáveis no plano 15 (`UiCodepoints`, hash SHA-1 + probing linear, doc §4.6).
5. Escreve as fontes:
   - `oneblock:ui/image_0..8`: toda peça, `ascent = 7 - k`;
   - `oneblock:ui/text_0..8`: providers de `custom.json` com `ascent - k`, mais o space `" " → 4`;
   - `oneblock:ui/space`: `±1 … ±256`;
   - o glyph `ui:#hit` (1×9 transparente).
6. Devolve o `UiKit` imutável. O plugin publica esse kit (`@Volatile`) só quando a geração termina com sucesso.

Advance de imagem = `GlyphAdvance.of` (coluna opaca mais à direita + 2), sempre calculado sobre a imagem original, nunca sobre a versão com padding.

**Shader** (`gui.vsh/fsh` que descarta o contorno branco do corpo do dialog, doc §4.4): opcional, atrás de `ui.shader: true` no `config.yml`, e é a última tarefa. Ele é cosmético, e o doc foi medido num cliente 26.1.2; o servidor aqui é 1.21.11. Se o shader quebrar, desligar a flag mantém tudo funcionando com o contorno visível.

## Engine (`dialog/`)

| Pacote | Conteúdo | Origem |
|---|---|---|
| `dialog/node` | `UiNode` (`ImageNode`, `TextNode`, `HotspotNode`, `ContainerNode` + `surface`), `UiSize`, `UiAlign`, `UiAction` | doc §5.1 + `surface` |
| `dialog/layout` | `UiLayout`, `LaidOut`, `Placed*` (+ `PlacedSurface`) | doc §6 |
| `dialog/render` | `UiRenderer`, `Spaces`, `TextMeasurer` | doc §8 |
| `dialog/kit` | `UiKit`, `UiKitBuilder`, `UiCodepoints`, `GlyphAdvance`, `SurfaceComposer` | doc §4 |
| `dialog/json` | `ScreenParser` (JSON → `ScreenDefinition` imutável), `ScreenCompiler` (definição + tema + estado → `UiNode`) | novo |
| `dialog/theme` | `UiTheme` | novo |
| `dialog` | `DialogOpener`, `DialogTokens`, `DialogState` | doc §9 |

Divergências do doc:

- **Ordem de pintura.** O doc ordena imagens por `x` dentro da linha. Aqui elas saem na **ordem de emissão do layout**: surface do pai, depois os filhos, depois os textos. Com fundos aninhados, ordenar por `x` faria um fundo cobrir um filho que começa antes dele. Spans continuam primeiro e ordenados por `x` (exigência do hit-test).
- **Surface.** `ContainerNode.surface` emite `PlacedSurface(id, x, y, w, h)` depois de medir o container e antes dos filhos. O renderer desenha como imagem simples (glyph pré-renderizado) com fonte `y % 9`.
- **Sem `UiActionBinder` plugável.** Só existe callback. Comandos/URL ficam fora até alguém precisar.

## Estado e API

```kotlin
DialogApi.open(player, "settings") {
    switch("friend_requests", get = { prefs.friendRequests }, set = { prefs.friendRequests = it })
    segmented("private_messages", get = { prefs.pm.ordinal }, set = { prefs.pm = Pm.entries[it] })
    button("reset") { prefs.reset() }
}
```

- `DialogApi` (em `api/`, `@Volatile` sobre o opener vivo) é a única superfície que o one-block toca.
- Na abertura, cada `bind` lê `get`. Um `bind` sem handler usa o valor do JSON e fica num `DialogState` da sessão: a tela continua interativa, mas o valor não persiste. É o que o comando de preview usa.
- Clique → `set(novo)` → recompila a tela com o novo estado → `showDialog` substitui o dialog aberto (`afterAction(NONE)`) → novo token. Callbacks com token velho são ignorados (doc §9.4). `set` roda na main thread.
- Callback vive 5 min; o botão fechar, 12 h (doc §9.3).
- Tela inexistente ou tema ausente → `open` devolve `false` e loga. Kit não pronto (pack nunca gerado) → mensagem no chat `Interface indisponível agora, tente de novo em instantes.`.

## Status do pack

`listener/PackStatusTracker`:
- `SUCCESSFULLY_LOADED` → carregado;
- `DECLINED`, `FAILED_*`, `DISCARDED`, `INVALID_URL` → não carregado;
- `PlayerQuitEvent` → esquece o jogador e os tokens dele.

Jogador sem pack recebe um dialog de fallback com o texto `Aceite a textura do servidor para ver esta tela.` e um botão `Aplicar textura` que chama `plugin.applyPack(player)`.

## Comando

- `/textura dialog <id> [jogador]`: abre a tela com estado de sessão (preview), com tab-complete pelos ids carregados.
- `/textura reload`: relê telas e temas. Arte nova, ícone novo ou tamanho de surface novo exigem `/textura gerar`, e o reload avisa quais telas usam tamanhos que não estão no kit atual.

## Erros e avisos

Seguindo o doc, nada que transborde a largura vira quebra de linha:
- texto é cortado com `...`;
- imagem que passa da largura é descartada;
- hotspots sobrepostos na mesma linha geram aviso.

Avisos são deduplicados por kit e logados com id da tela e caminho do nó. Erros de parsing de uma tela invalidam só aquela tela.

## Verificação

Sem testes automatizados (preferência do projeto). A verificação é in-game no servidor local:
- deploy com `./gradlew :plugins:textura:localDeploy` (servidor parado);
- o roteiro é `/textura gerar` → `/textura dialog settings` com o JSON do exemplo salvo em `configs/ui/settings.json`.

Checklist:
- linhas alinhadas, sem deslocamento lateral;
- tooltip correto em cada controle;
- switch e segmented alternam e reabrem;
- a borda inferior dos controles é clicável;
- texto cortado com `...` quando força largura;
- jogador sem pack recebe o fallback.

**Primeiro passo do plano: spike** com uma tela de uma linha (imagem + texto + hotspot), para confirmar no cliente 1.21.11 as medidas do doc: linha de 9 px, largura 400 + 8, centralização por linha e hit-test só dentro de glyphs desenhados. Se algo divergir, ajustar as constantes antes de portar o resto.

## Fora do escopo

- DSL Kotlin pública para montar telas em código.
- Ações de comando/URL/copiar.
- Inputs nativos do dialog (campo de texto, slider).
- Arte final.
- `scale` ≠ 1.
- Slider, checkbox, progress bar e chip do doc. Entram depois como novas folhas JSON sobre as mesmas primitivas.
