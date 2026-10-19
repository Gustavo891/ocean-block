# Textura — pasta de dados

Tudo que o resource pack do servidor é sai daqui. O plugin lê estas pastas, monta
o zip e serve para os jogadores.

## Pastas

| Pasta | Papel | Versionada |
|---|---|---|
| `configs/` | Definições: o que existe e como se comporta. Editado à mão. | sim |
| `resource/` | Assets crus (PNG, JSON de modelo, lang). A posição do arquivo **é** a chave dele no pack. | sim |
| `data/` | Estado de runtime que não pode ser perdido (`block-allocations.yml`). | sim |
| `generated/` | Saída do build (`resourcepack.zip`). Descartável, some no `.gitignore`. | não |
| `external/` | Packs de terceiros (`.zip` ou pasta com `pack.mcmeta`) mesclados por cima do nosso. | não |

`resource/` segue exatamente a estrutura de um resource pack:
`resource/assets/<namespace>/textures/...`. Um arquivo em
`resource/assets/oneblock/textures/icons/star.png` entra no zip nesse mesmo
caminho e vira a chave `oneblock:icons/star.png`. Não existe campo de chave em
lugar nenhum — o lugar do arquivo é a chave.

## Fluxo de trabalho

1. Coloque o asset em `resource/`.
2. Declare no yml certo dentro de `configs/`.
3. `/textura reload` relê os `configs/`.
4. `/textura gerar` remonta o zip e sobe pro LobFile.
5. O jogador recebe o pack ao entrar, ou por `/textura aplicar <jogador>`.

Passo 3 sozinho não muda nada no cliente: só o `gerar` reescreve o pack.

## Convenção de nome de arquivo

Dentro de `configs/glyphs` e `configs/items` os ymls são **planos**, nomeados
`<dominio>.yml` ou `<dominio>-<sub>.yml` (`gui.yml`, `gui-minion.yml`,
`skills-bar.yml`, `ores-ruby.yml`). Sem subpastas: os registries varrem
recursivamente e tratam tudo igual, então subpasta só cria dois jeitos de fazer
a mesma coisa. O nome do arquivo não é referenciado por nada — serve só para
humanos acharem a entrada.

## `configs/glyphs/*.yml` — imagens no texto

Cada chave de topo é uma entrada. `id` é como o resto do servidor referencia o
glyph (`<glyph:star>`, `GlyphApi.getChar("star")`).

```yaml
star:
  id: star
  caminho: assets/oneblock/textures/icons/star.png
  unicode:      # gerado e persistido se ausente
  ascent: 8           # deslocamento vertical
  height: 16          # altura de renderização
  size: 16            # largura em px, para medir/centralizar texto
  default: true       # também entra na fonte minecraft:default (chat)
  show_in_gui: true   # aparece em /textura glyphs
  fonts:              # opcional: fontes nomeadas, com ascent/height próprios
    bossbar:
      ascent: 3
      height: 8
      namespace: oneblock     # entra em oneblock:bossbar
      unicode-specify:  # opcional, senão herda o unicode global
```

Folha de sprites (uma imagem com N fatias empilhadas):

```yaml
mouse_clicks:
  caminho: assets/oneblock/textures/icons/mouse_clicks.png
  amount: 3
  ids: [mouse_left, mouse_right, mouse_middle]
  unicode: [, , ]
  ascent: 5
  height: 8
```

Regras que quebram o pack se ignoradas:

- **Unicode só na Private Use Area (``+).** Os codepoints herdados do Nexo
  eram Syriac: alguns são marcas combinantes (advance 0), outros são RTL fortes e
  disparam reordenação BiDi no cliente.
- **`size` manda quando declarado.** Sem ele o plugin mede a largura do PNG. Um
  `size` errado desalinha toda centralização de texto que usa o glyph.
- **Conflito de unicode é resolvido reescrevendo o yml.** O primeiro a declarar
  fica com o código; o segundo é trocado por um livre e o arquivo é salvo. Se um
  unicode "mudou sozinho", foi isso.
- `hover_complete` e `hover_current` têm unicode fixo: os mesmos chars estão
  escritos à mão em `configs/fonts/custom.json`. Trocar num lugar só quebra o
  overlay do `/is phases`.

## `configs/fonts/` — fontes nomeadas

`fonts.yml` aponta cada fonte para um JSON na mesma pasta:

```yaml
oneblock_gui:
  id: gui
  namespace: oneblock       # gera assets/oneblock/font/gui.json, para <font:oneblock:gui>
  caminho: fonts/oneblock-gui.json
  ascent: 7                 # deslocamento aplicado sobre o ascent original dos providers
```

O JSON é **interpretado, nunca copiado**: o plugin lê os providers `bitmap`,
aplica o `ascent`/`height` da config e regrava. O `ascent` é relativo — vale como
deslocamento em cima do valor original de cada provider, o que preserva a
diferença entre eles.

As fontes `font-ui-<y>` usadas por `TitleApi` são geradas em runtime a partir de
`custom.json`. Um `y` novo **só existe depois de um `/textura gerar`**, então
registre o ascent no `init` do sistema, não na primeira abertura do menu.

Os PNGs de fonte ficam em `resource/assets/minecraft/textures/font/`. Os arquivos
`ascii_custom.png` e `accented_custom.png` **não podem** se chamar `ascii.png` /
`accented.png`: esses são os caminhos do atlas vanilla, e sobrescrevê-los muda a
fonte de todo o jogo em vez de só a nossa.

## `configs/items/*.yml` — itens customizados

```yaml
magnet:
  material: PAPER
  type: flat                # flat = o plugin gera o modelo da textura
  texture: assets/oneblock/textures/item/magnet.png
  item_model: oneblock:magnet    # componente minecraft:item_model (1.21.4+)
  custom_model_data: 10          # opcional; entra no range_dispatch do material
  name: "<gradient:#aaa:#fff>Ímã</gradient>"
  lore: ["<gray>Puxa os drops."]
  display:                       # opcional, por perspectiva
    gui:
      scale: [1.4, 1.4, 1.4]
      translation: [0, 2, 0]
```

`type: model` troca `texture` por `model:`, apontando para um JSON em `resource/`
ou uma chave direta do jogo (`minecraft:block/acacia_fence`). Nesse caso o
`display` do yml entra por um modelo-filho, sem editar o JSON original.

Um item pode viver só de `item_model`, sem `custom_model_data` — aí ele não
disputa índice com ninguém no material base.

## `configs/blocks/*.yml` — blocos

Dois tipos. `OVERRIDE` troca a textura de um bloco vanilla; `CUSTOM` cria um
bloco novo sobre uma variante de note_block.

```yaml
ruby_block:
  type: CUSTOM
  name: "Bloco de Rubi"
  texture: assets/oneblock/textures/ores/ruby/ruby_block.png
  item_material: note_block
```

A variante de note_block de cada bloco custom mora em
`data/block-allocations.yml`. **Esse arquivo é estado, não artefato**: se ele
sumir, os blocos já colocados no mundo trocam de aparência.

## `configs/ui/` — telas de dialog

Telas desenhadas dentro do dialog vanilla (1.21.6+) com glyphs de fonte. Cada
`<id>.json` é uma tela; cada `*.yml` declara temas (uma chave de topo por tema).
Abra com `/textura dialog <id> [jogador]` ou, de outro plugin,
`DialogApi.open(player, "<id>") { switch(...); segmented(...); button(...) }`.

O arquivo JSON é o nó raiz, que precisa ser um container e ter `theme`. Opcionais
no raiz: `title` (MiniMessage) e `close` (rótulo do botão de fechar; `null`
remove o botão).

Containers têm uma chave `column`, `row`, `box` ou `grid` com a lista de filhos:

| Chave | Default | Significado |
|---|---|---|
| `w`, `h` | `"fit"` | número (px), `"grow"` ou `"fit"` |
| `gap`, `pad` | 0 | espaço entre filhos / padding |
| `align`, `cross` | `start` | `start`, `center`, `end` (eixo principal / cruzado) |
| `snap` | 0 | arredonda o y de cada filho para múltiplo (use 9 para empilhar botões) |
| `columns` | 1 | só `grid` |
| `x`, `y` | — | posição absoluta dentro do pai |
| `surface` | — | fundo do container, nome definido no tema |
| `name` | — | rótulo; aparece nos avisos do console |

Folhas:

| Forma | Campos |
|---|---|
| `{"space": n}` | — |
| `{"text": "…"}` | `style`, `align` (`left`/`center`/`right`), `w`, `icon`, `color`, `bold`, `tooltip`; `scale` só aceita 1 |
| `{"icon": "ui:x"}` | `size` (px, default 8) |
| `{"image": "ui:x"}` | `w` (estica se a imagem tiver `.slice`) |
| `{"switch": true}` | `bind`, `tooltip` |
| `{"segmented": ["A","B"]}` | `selected`, `bind`, `w`, `tooltip` |
| `{"button": "rótulo"}` | `bind`, `w`, `icon`, `tooltip` |

Em `tooltip`, `{value}` vira o valor atual do controle. `bind` é a chave que o
código usa para ler e gravar o estado; sem handler, a tela usa o valor do JSON e
guarda o clique só enquanto o dialog estiver aberto.

`{chave}` em `text`, rótulo de `button`, `tooltip` e `title` é trocado pelo valor
de `text("chave") { ... }` fornecido pelo código. O valor entra como texto puro
(sem MiniMessage), então pode vir de jogador. Use `w` fixo em textos dinâmicos
para a largura da tela não mudar a cada valor.

```kotlin
DialogApi.open(player, "settings") {
    switch("friend_requests", get = { prefs.friends }, set = { prefs.friends = it })
    segmented("private_messages", get = { prefs.pm }, set = { prefs.pm = it })
    text("saldo") { formatar(saldo) }
    button("reset") { click -> prefs.reset() }
}
```

Depois de qualquer clique a tela é redesenhada. Num `button`, `click.close()`
fecha o dialog e `click.open("outra") { ... }` troca de tela. Os nomes de `bind`
que o código passa e os que a tela declara são conferidos na abertura; diferenças
viram aviso no console.

Tema:

```yaml
stone:
  icon-gap: 3
  icon-size: 8
  text:
    heading: { color: "#ffffff", bold: true }
    body: { color: "#e6e6e6" }
    muted: { color: "#9a9a9a" }
    tooltip: { color: "#c8c8c8" }
  surfaces:
    sheet: ui:stone/sheet
  switch: { on: ui:stone/switch_on, off: ui:stone/switch_off, labels: { on: "On", off: "Off" } }
  segmented: { art: ui:stone/segment, selected-art: ui:stone/segment_selected, text-style: muted, selected-text-style: body, pad: 4 }
  button: { art: ui:stone/button, text-style: body }
  icons:
    mail: scroll
```

Um id `ui:<path>` resolve, em ordem, para `resource/assets/oneblock/textures/ui/<path>.png`,
para o glyph apontado em `icons` do tema, ou para o glyph de id `<path>`. Imagens
até 399×256 px, nomes em `[a-z0-9_/-]`.

`<nome>.png.slice` ao lado do PNG (não entra no pack):

- `esquerda direita` — a imagem estica na largura (botões, segmentos);
- `esquerda direita topo base` — nine-slice, obrigatório para surfaces.

**Surfaces são pré-renderizadas por altura e ícones com `size` por tamanho**, no
`/textura gerar`. A largura de uma surface é livre em runtime; a altura não. Mudou
a altura de um painel ou o `size` de um ícone? O `/textura reload` avisa o que
ficou fora do pack; rode `/textura gerar`. Quem estiver online recebe o pack novo
automaticamente quando o hash muda.

### Formato do Studio

As telas aceitam as chaves do editor Studio: dá para colar o `document` exportado
de lá. Exemplo completo em `configs/ui/social.json`.

**Raiz com páginas:**

```json
{
  "title": "Social", "theme": "stone",
  "hideFooter": false, "canCloseWithEscape": true, "pause": false,
  "document": { "row": [ ... ] },
  "pages": [ { "id": "profile", "name": "Perfil" }, { "id": "audio", "name": "Som", "document": { ... } } ]
}
```

`document` é a primeira página; a primeira página sem `document` usa ele. `hideFooter: true`
tira o botão de fechar.

**Folhas:**

| Forma | Campos |
|---|---|
| `{"nav": "Rótulo"}` | `icon`, `w`, `onClick`, `tooltip`, `selected`, `bind`. Fica ativo quando o `onClick` é `page` para a página atual; antes de qualquer página, vale `selected` (`true`, `false` ou `"{chave}"`). Com `bind`, o clique chama o `button`/`listButton` do código |
| `{"slots": [7, 1], "items": [{"at": 0, "item": "diamond_sword", "count": 1}]}` | grade de slots (arte `slots.art` do tema) com ícone do item, quantidade e nome traduzido no tooltip; `align` posiciona a grade. Por item, `item` e `count` aceitam `{chave}`; `name` (MiniMessage) e `lore` (lista de linhas MiniMessage) trocam o tooltip do item por um texto sem itálico. Item vazio ou sem valor deixa o slot vazio |
| `{"dropdown": "Inicial"}` | `options`, `bind`, `w`, `tooltip`. Cada clique avança para a próxima opção |
| `{"slider": 40}` | `min`, `max`, `step`, `unit`, `bind`, `w`, `tooltip`. Metade esquerda −step, direita +step |
| `{"icon": "ui:x", "bind": "k", "states": {"a": "ui:y"}}` | troca o ícone pelo valor de `k` |
| `{"button": "Rótulo", "onClick": {...}}` | executa ações; com `bind` também chama o código antes. `selected` e `disabled` aceitam `true`, `false` ou `"{chave}"` (só o texto `true` liga) |
| `{"space": "grow"}` | ocupa o espaço que sobra na linha |

Ícones `item:<material>` (ex.: `item:diamond_sword`) usam a textura vanilla do item. No primeiro
`/textura gerar` o client da versão é baixado da Mojang para `generated/vanilla/` (uma vez só).
No tema, `icons:` aceita alias com prefixo (`apple: item:golden_apple`).

**`onClick`:** `{"action": "page", "value": "id"}`, `{"action": "close"}`, ou
`{"action": "run_command", "value": "linhas"}`, uma por linha:

| Linha | Efeito |
|---|---|
| `[open] id k=v ...` | abre outra tela levando os valores |
| `[page] id` / `[close]` / `[refresh]` | troca de página / fecha / redesenha |
| `[set] k=v ...` | grava valores |
| `[message] texto` | MiniMessage para o jogador |
| `[console] cmd` / `[player] cmd` / sem tag | executa como console / como o jogador |
| `[sound] id [volume] [pitch]` | toca um som |

Em `[console]`/`[player]`, todo valor substituído precisa ser `[A-Za-z0-9_.:-]`;
se não for, a linha e as seguintes não rodam e o console avisa. `{chave|raw}` libera.

**Placeholders** (em textos, rótulos, tooltips, título e ações): `{chave}`,
`{player}`, `{player_uuid}`, `{online}`, `{page}`, `%papi%` (com PlaceholderAPI),
e escolha `{chave?a:Texto A|b:Texto B|Padrão}` (`$value` dentro do texto é o valor).

**Valores:** switch `true`/`false`, dropdown e segmented o id da opção (rótulo em
minúsculas, espaço vira `_`), slider o número. Ordem: bind do código → salvo no
jogador → sessão → default do JSON. Controle com `bind` e sem handler no código
fica salvo no jogador (PDC `textura:ui_<bind>`); outros sistemas leem com
`DialogApi.setting(player, "bind")` e gravam com `DialogApi.setSetting(...)`.
Valores iniciais: `DialogApi.open(player, "id", mapOf("target" to nome)) { ... }`.

Chaves do Studio só de render são aceitas e ignoradas: `nudge`, `drawButtons`,
`contentWidth`, `showWarning`, `look`, `compact`, `accent`, `face`, `frame`,
`style`, `buttons`, `inputs`, `afterAction`, `command`, `commandPermission`,
`perPage` (fora de `repeat`), `scale`, `columns`.

### Listas repetíveis

Qualquer nó (menos o raiz) aceita `"repeat": "<lista>"` e, opcional,
`"perPage": N`. O código fornece a lista; o nó é desenhado uma vez por entrada,
no lugar dele, antes do layout (dentro de um `grid`, cada entrada vira uma
célula). Com `perPage`, aparecem no máximo `N` entradas a partir de
`página × N`; a página fora do intervalo é limitada à primeira ou à última. Sem
`perPage`, aparecem todas e a página é ignorada.

Dentro do nó repetido, `{campo}` resolve primeiro na entrada (e nas entradas das
listas de fora, se houver repetição aninhada) e depois nos valores globais, em
textos, rótulos, tooltips, `item`/`count`/`name`/`lore` de slots, `selected`/
`disabled` e nas ações do `onClick`. Um `button` ou `nav` repetido com `bind`
chama `listButton(bind)` com o **índice da entrada na lista inteira** (não na
página); sem `listButton`, cai no `button(bind)` comum.

```json
{"grid": [
  {"repeat": "receitas", "perPage": 6, "column": [
    {"slots": [1, 1], "items": [{"at": 0, "item": "{item}", "count": "{qtd}", "name": "<yellow>{nome}", "lore": ["{custo}"]}]},
    {"text": "{nome}", "style": "heading", "w": 90},
    {"button": "Craftar", "icon": "ui:cart", "look": "light", "bind": "craftar", "disabled": "{falta}", "tooltip": "{dica}"}
  ], "surface": "sheet", "pad": 5}
], "columns": 2}
```

```kotlin
DialogApi.open(player, "receitas") {
    list("receitas") { receitas.map { mapOf("nome" to it.nome, "item" to "fishing_rod", "qtd" to "1", "falta" to (!pode(it)).toString()) } }
    listPage("receitas") { estado.pagina }
    listButton("craftar") { click, index -> craftar(receitas[index]) }
    button("proxima") { estado.pagina++ }
}
```

Trocar de página é só mudar o valor que `listPage` devolve: depois de qualquer
clique a tela é redesenhada e lê a lista e a página de novo.

**Pré-render:** o `/textura gerar` compila cada tela com listas falsas de 0 até
`perPage` entradas em todas as combinações (acima de 512, uma lista por vez com
as outras cheias), para que toda página parcial tenha a altura das surfaces
pronta. Lista sem `perPage` avisa no console e só pré-renderiza 0 e 1 entrada.
As entradas falsas não têm campos, então `{campo}` aparece literal e o slot com
`item` dinâmico fica vazio; alturas não dependem do texto. Itens que só aparecem
por `{campo}` precisam ser registrados pelo código antes do gerar, senão o
ícone não entra no pack:

```kotlin
DialogApi.registerItemIcons(listOf("fishing_rod", "cobweb"))
```

## `config.yml`

`ui.fonte-texto` é o JSON (em `configs/fonts/`) cujos providers viram a fonte do
texto das telas de dialog; as larguras são medidas dos PNGs dele.

`pack.formato` tem que bater com a versão do cliente (97 = 26.3). Abaixo
disso o Minecraft trata o pack como de outra versão e as fontes customizadas
param de valer — os caracteres de espaçamento viram quadradinho.

A chave do LobFile fica em `host.lobfile.api-key`. A variável de ambiente
`TEXTURA_LOBFILE_API_KEY`, se definida, tem prioridade sobre ela — útil para
sobrescrever a chave num ambiente específico sem editar o arquivo.

## Comandos

| Comando | O que faz |
|---|---|
| `/textura gerar` | Remonta o zip e publica (assíncrono, um por vez) |
| `/textura reload` | Relê `configs/` sem regerar o pack |
| `/textura aplicar <jogador>` / `aplicarall` | Reenvia o pack |
| `/textura glyphs` / `fonts` / `itens` / `blocos` | Lista o que está carregado |
| `/textura importglyph <nome> [unicode] <link.png>` | Baixa um PNG e registra como glyph |
| `/textura testfont <fonte> <texto>` | Inspeção visual de uma fonte |
| `/textura dialog <id> [jogador]` | Abre a tela `configs/ui/<id>.json` (estado só da sessão) |

Tudo sob a permissão `textura.admin`. O HUD tem comando próprio: `/hud
show|hide|sec|unsec`.
