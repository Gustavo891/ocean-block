# Glyph sprite-sheet (Textura)

Data: 2026-07-25
Plugin: `plugins/textura`

## Problema

Hoje cada entrada de glyph no yml aponta para um PNG inteiro e vira um char. `GlyphRegistry.loadYaml` lê um `unicode` por entrada e `PackBuilder.addGlyph` emite `FontProvider.bitMap(textureKey, height, ascent, listOf(char))` — uma linha, uma coluna.

Texturas que já vêm como folha (ex.: `assets/oneblock/textures/icons/mouse_clicks.png`, 16x48, três ícones de 16x16 empilhados) não têm como ser fatiadas. Apontar três entradas para o mesmo `caminho` renderiza a imagem inteira três vezes.

O provider `bitmap` do Minecraft já resolve isso: `chars` é uma matriz, cada string do array é uma linha da textura. Um provider com três entradas em `chars` sobre a textura 16x48 fatia em três linhas de 16x16, uma por char:

```json
{
  "type": "bitmap",
  "file": "oneblock:icons/mouse_clicks.png",
  "height": 8,
  "ascent": 5,
  "chars": ["\uE200", "\uE201", "\uE202"]
}
```

## Escopo

Suporte a folha **vertical**: N linhas de uma coluna, uma entrada do array `chars` por unicode. Grade 2D (múltiplas colunas) fica fora — não há caso de uso e o formato de `chars` mudaria.

## Schema yml

```yaml
mouse_clicks:
  caminho: assets/oneblock/textures/icons/mouse_clicks.png
  amount: 3
  ids: [mouse_left, mouse_right, mouse_middle]
  unicode:
    - \uE200
    - \uE201
    - \uE202
  ascent: 5
  height: 8
  default: true
  show_in_gui: true
  size: 12
```

- `amount` — número de linhas da folha. Presença de `amount` é o que marca a entrada como folha; sem ele o formato antigo (`id` string, `unicode` string) continua valendo sem mudança.
- `ids` — lista de ids, um por linha, na ordem de cima para baixo. É por esses ids que `<glyph:mouse_left>` e `GlyphApi.getChar("mouse_left")` resolvem.
- `unicode` — lista paralela a `ids`. Quando ausente ou vazia, o registry gera `amount` unicodes livres na Private Use Area e grava a lista de volta no yml, mesmo comportamento do glyph simples.
- `ascent`, `height`, `size`, `default`, `show_in_gui`, `fonts` — valem para a folha inteira; toda fatia compartilha a mesma caixa de render.

### Validação

Em `GlyphRegistry`, ao carregar uma entrada com `amount`:

- `ids` ausente ou vazia: warning com id e nome do arquivo, entrada pulada. Sem nomes não há como referenciar as fatias.
- `ids.size != amount`: warning, entrada pulada.
- `unicode` presente com tamanho diferente de `amount`: warning, entrada pulada.
- `amount <= 0`: warning, entrada pulada.

Erros são logados e a entrada é ignorada; nunca fatais, alinhado com o resto do carregamento de config do projeto.

### Reserva de unicodes

`GlyphRegistry.reserveExplicitUnicodes` hoje lê `$key.unicode` como `String`. Passa a tratar os dois formatos: string única (glyph simples) ou lista (folha), reservando cada elemento. A resolução de conflito existente — trocar por um unicode livre e persistir — vale por elemento da lista.

## Modelo

`GlyphData` ganha dois campos:

- `sheetKey: String?` — a chave da entrada no yml (`mouse_clicks`), `null` em glyph simples.
- `sheetIndex: Int` — índice da linha, 0-based, `0` em glyph simples.

O registry **expande** a folha em `amount` instâncias de `GlyphData`, uma por id, todas com o mesmo `caminho`, `ascent`, `height`, `size`, `default`, `showInGui` e `fonts`, cada uma com seu `unicode` e seu `sheetIndex`.

`_glyphs` continua `Map<id, GlyphData>`. Consequência: `GlyphApi`, `TexturaTags`, `HudBackground`, `ChatListener` e `/textura glyphs` não mudam — cada fatia é um glyph comum do ponto de vista deles.

## Build do pack

`ResourcePackBuilder.addGlyphs` passa a agrupar `glyphRegistry.glyphs.values` por `sheetKey` antes de emitir:

- `sheetKey == null` — caminho atual, um `PackBuilder.addGlyph` por glyph, um provider com um char.
- `sheetKey != null` — um `PackBuilder.addGlyphSheet` por grupo, emitindo **um** `FontProvider.bitMap(textureKey, height, ascent, chars)` onde `chars` é a lista de chars ordenada por `sheetIndex`.

A regra por fonte se mantém: a fonte global (`Font.MINECRAFT_DEFAULT`, apenas se `default`) e cada fonte nomeada em `fonts` recebem seu próprio provider da folha, com o `ascent`/`height` daquela fonte. Numa fonte nomeada, o char de cada fatia vem do `GlyphFont` correspondente (`unicode-specify` quando definido, senão o unicode global da fatia).

`PackBuilder.addGlyphSheet` grava a textura sob a mesma guarda de `addGlyph` (`writeTexture` + `pack.texture(key) == null`), então a textura entra uma vez só no zip mesmo com várias fontes.

## Largura

`GlyphWidthMeasurer.measure` usa `image.width`. Em folha vertical a largura da fatia é a largura da imagem, então a medição continua correta sem mudança. O `size` explícito no yml não é lido hoje — a medição sempre sobrescreve — e esse comportamento fica como está.

## Testes

Kotest já está wired em `plugins/textura`. Cobertura em `GlyphRegistry`:

- folha válida expande em `amount` glyphs, com os ids, unicodes e `sheetIndex` esperados;
- folha compartilha `caminho`, `ascent`, `height` e `fonts` entre as fatias;
- folha sem `ids` é pulada;
- `ids.size != amount` é pulada;
- `unicode` ausente gera `amount` unicodes livres, sem colidir com os já reservados, e persiste como lista no yml;
- glyph simples (sem `amount`) carrega exatamente como antes.

E em `ResourcePackBuilder`/`PackBuilder`: folha de N fatias produz um único provider com `chars` de N entradas na ordem do `sheetIndex`, e uma única textura no pack.
