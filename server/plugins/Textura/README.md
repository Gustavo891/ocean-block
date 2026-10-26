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
- **PNG de glyph com no máximo 256 px de largura e de altura.** O cliente monta
  as fontes em páginas de 256×256; um PNG maior é descartado sem erro no log e o
  glyph aparece como um quadradinho. Para um glyph largo (ex.: `scoreboard_logo`),
  reduza a resolução do PNG e mantenha o `height` desejado no yml.
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
`DialogApi.open(player, "<id>") { binds -> binds.switch(...); binds.button(...) }`
(ou `DialogApi.openResult(...)`, que diz se abriu, se falta a textura, se a tela não
existe ou se falhou). A API pública só usa tipos Java/Bukkit/Adventure/Textura
(`Supplier`, `IntSupplier`, `Consumer`, `DialogBindsConfig`, `DialogButtonAction`,
`DialogListAction`) e não tem parâmetros com valor padrão: cada plugin traz o
próprio Kotlin, e um `kotlin.*` na assinatura quebra com `LinkageError`.

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
| `{"text": "…"}` | `style` (aceita `"{chave}"`, ex.: `good`/`bad` por entrada), `align` (`left`/`center`/`right`), `w`, `icon`, `color`, `bold`, `tooltip`; `scale` só aceita 1 |
| `{"icon": "ui:x"}` | `size` (px, default 8); `"frame": true` desenha a surface `frame` do tema (no `stone`, o `well`, fundo de slot do inventário) em volta, com 4 px de folga |
| `{"image": "ui:x"}` | `w` (estica se a imagem tiver `.slice`) |
| `{"pixels": "chave"}` | `w`, `h` (px, 1..399, obrigatórios), `scale` (1..4, px de tela por pixel da imagem). Desenha a imagem que o código passa em `binds.pixels("chave") { bufferedImage }`, gerada na hora (mapa, skin); sem imagem, o nó só ocupa o espaço |
| `{"switch": true}` | `bind`, `tooltip` |
| `{"segmented": ["A","B"]}` | `selected`, `bind`, `w`, `tooltip` |
| `{"button": "rótulo"}` | `bind`, `w`, `icon`, `tooltip` |

Tooltip de várias linhas sem tags MiniMessage ganha cores por linha, decididas
depois de trocar os placeholders (um valor com `\n` vira várias linhas): a
primeira é o título; linha começando com `∙` ou `⏵` é destaque; a primeira linha
depois de uma em branco é cabeçalho; a última, se vier depois de uma em branco, é
a ação (`✔` verde, `⌀` ou `✖` vermelho, o resto amarelo). Com `<` no texto, o
tooltip inteiro é MiniMessage e não há cor automática.

Em `tooltip`, `{value}` vira o valor atual do controle. `bind` é a chave que o
código usa para ler e gravar o estado; sem handler, a tela usa o valor do JSON e
guarda o clique só enquanto o dialog estiver aberto.

`{chave}` em `text`, rótulo de `button`, `tooltip` e `title` é trocado pelo valor
de `text("chave") { ... }` fornecido pelo código. O valor entra como texto puro
(sem MiniMessage), então pode vir de jogador. Use `w` fixo em textos dinâmicos
para a largura da tela não mudar a cada valor.

```kotlin
DialogApi.open(player, "settings") { binds ->
    binds.switch("friend_requests", get = { prefs.friends }, set = { prefs.friends = it })
    binds.segmented("private_messages", get = { prefs.pm }, set = { prefs.pm = it })
    binds.text("saldo") { formatar(saldo) }
    binds.button("reset") { click -> prefs.reset() }
}
```

O nó `pixels` vira glyphs brancos de 1 px de altura × `scale` coloridos pela cor do texto,
um por corrida de pixels iguais na linha (pixel com alfa < 128 fica vazio). Os glyphs
(escalas 1..4, larguras 1..64) entram sempre no pack; imagem muito colorida gera muitos
componentes, então mantenha `w`×`h` moderado (o menu da ilha usa 128×128 em escala 2 e
44×76 em escala 1).

Depois de qualquer clique a tela é redesenhada. Num `button`, `click.close()`
fecha o dialog e `click.open("outra") { ... }` troca de tela. Os nomes de `bind`
que o código passa e os que a tela declara são conferidos na abertura; diferenças
viram aviso no console.

Um estilo de texto pode usar uma fonte bitmap própria com `font: <nome>`, que lê `configs/fonts/<nome>.json` (providers `bitmap`/`space`, texturas em `resource/`); o texto é medido com as larguras dessa fonte. Para espaçar as letras, deixe uma coluna com alfa 1 à direita de cada glifo. O tema `abismo` usa `pequena` (caixa alta 5×5 própria, 2/3 de px por ponto, nítida em GUI 3), `titulo` e `cabecalho` (rasterizadas da Minecraft Ten, de KendroType: licença gratuita só para uso não comercial; servidor com venda precisa da licença comercial do autor).

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
| `{"slots": [7, 1], "items": [{"at": 0, "item": "diamond_sword", "count": 1}]}` | grade de slots (arte `slots.art` do tema) com ícone do item, quantidade e nome traduzido no tooltip; `align` posiciona a grade. Por item, `item` e `count` aceitam `{chave}`; `name` (MiniMessage) e `lore` (lista de linhas MiniMessage) trocam o tooltip do item por um texto sem itálico. Item vazio ou sem valor deixa o slot vazio. Um `item` que começa com `ui:` desenha esse ícone do tema no slot (assado no tamanho do slot quando o item tem `{chave}`). `missing` (`true`, `false` ou `"{chave}"`) por item troca a moldura pela arte `slots.missing-art` do tema (no `stone`, o slot vermelho). `"hideEmpty": true` não desenha os slots vazios (a linha mantém a altura). `"bind": "chave"` torna cada slot com item clicável: dentro de um `repeat` chama `listButton(chave)` com o índice da entrada, fora dele `button(chave)`. Com `align` `start` (o padrão) a grade ocupa só a própria largura, então cabe numa `row` ao lado de outros nós |
| `{"dropdown": "Inicial"}` | `options`, `bind`, `w`, `tooltip`. Cada clique avança para a próxima opção |
| `{"slider": 40}` | `min`, `max`, `step`, `unit`, `bind`, `w`, `tooltip`. Metade esquerda −step, direita +step |
| `{"progress": "{valor}"}` | `max` (padrão 100; aceita `{campo}`), `track`, `fill` (ids `ui:` com `.png.slice`, assados no gerar), `w` em px (padrão 100). Barra só de leitura: o preenchimento tem `w * valor / max` px |
| `{"icon": "ui:x", "bind": "k", "states": {"a": "ui:y"}}` | troca o ícone pelo valor de `k` |
| `{"button": "Rótulo", "onClick": {...}}` | executa ações; com `bind` também chama o código antes. `selected` e `disabled` aceitam `true`, `false` ou `"{chave}"` (só o texto `true` liga) |
| `{"space": "grow"}` | ocupa o espaço que sobra na linha |

Ícones `item:<material>` (ex.: `item:diamond_sword`) usam a textura vanilla do item. Item sem
textura própria (ex.: `item:loom`, que usa o modelo do bloco) cai na textura do bloco,
nesta ordem: `block/<nome>`, `<nome>_front`, `<nome>_side`, `<nome>_top`. No primeiro
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
Valores iniciais: `DialogApi.open(player, "id", mapOf("target" to nome)) { binds -> ... }`.

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
DialogApi.open(player, "receitas") { binds ->
    binds.list("receitas") { receitas.map { mapOf("nome" to it.nome, "item" to "fishing_rod", "qtd" to "1", "falta" to (!pode(it)).toString()) } }
    binds.listPage("receitas") { estado.pagina }
    binds.listButton("craftar") { click, index -> craftar(receitas[index]) }
    binds.button("proxima") { estado.pagina++ }
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

`icon` de `nav`, `chip`, `button` e `text` também aceita `{campo}` (ex.: cada
categoria com `"icone": "ui:pickaxe"`). Como o gerar não sabe o valor, uma tela
com ícone por placeholder assa, no `icon-size` do tema, **todos os aliases de
`icons:` do tema** (`ui:<alias>`) e os itens de `registerItemIcons`
(`item:<material>`). O nó `{"icon": "item:{campo}", "size": 18}` também aceita placeholder: o gerar
assa os mesmos ícones no `size` do nó; enquanto o valor não resolve, o nó ocupa o espaço vazio. Por isso o valor do campo precisa ser um desses: um
`ui:<caminho>` que não seja alias, ou um glyph solto, fica sem ícone. Valor vazio
ou sem resolver também não desenha ícone.

Limites: `perPage` acima de 64 vira 64 (com aviso); `count` de slot fica entre
1 e 99.

## `configs/hud/notificacoes.yml` — notificações na bossbar

As notificações saem numa única bossbar por jogador. Ela junta as linhas de todas
as regiões, e o shader `core/text.vsh` do pack move cada linha para o seu canto
real da tela, em qualquer GUI scale ou resolução. A barra fica sempre presente
para todo jogador online, com nome vazio quando não há linha viva. Os sprites
YELLOW/PROGRESS do pack são transparentes, então ela não desenha nada, e as
outras bossbars não pulam 19 px quando notificações vêm e vão. Ela é sempre a
primeira bossbar do jogador: na entrada, esconde as outras e mostra elas de
novo depois dela. Sem o pack (ou com um shaderpack que troca os core shaders),
as linhas aparecem empilhadas no topo, nas cores do marcador.

Cada chave de topo é uma região:

```yaml
coleta:
  id: coleta
  ancora: MEIO_DIREITA
  x: 0
  y: 70
  linhas: 6
  espacamento: 1
  fundo: true
  direcao: baixo
```

| Campo | Valor | Padrão |
|---|---|---|
| `id` | nome usado no código (`NotificationApi.push(player, "coleta", ...)`) | a chave |
| `ancora` | `TOPO_ESQUERDA`, `TOPO_DIREITA`, `MEIO_ESQUERDA`, `MEIO_DIREITA`, `BASE_ESQUERDA`, `BASE_DIREITA`, `CENTRO` | obrigatório |
| `x`, `y` | deslocamento em px de GUI (x para a direita, y para baixo) a partir da âncora | 0 |
| `linhas` | máximo de linhas visíveis (1..15). Cheia, a mais antiga sai | 3 |
| `espacamento` | px entre uma linha e a próxima (0..32). O passo é 13 + espaçamento | 1 |
| `fundo` | placa escura semitransparente na largura do conteúdo | true |
| `direcao` | `baixo` ou `cima` | `cima` nas âncoras `BASE_*`, `baixo` nas outras |
| `animacao` | `true`: a linha fica parada 2 s e depois some aos poucos em 1,5 s, no mesmo lugar, animada pelo próprio cliente; precisa de `y >= 0` | false |

`CENTRO` alinha pela esquerda a partir do centro da tela, e `y: 0` fica 32 px acima do centro (a região `xp`, com `x: 8` e `y: 28`, põe o texto logo à direita da mira). Numa região animada o marcador leva a fase de início (tempo do mundo no ciclo de 200 ticks, que divide os 24000 do dia) nos 4 bits baixos do G e no lift do B; o shader (âncora 8) calcula a idade pelo `GameTime` do cliente e anima liso, sem o servidor redesenhar (constantes `TEXTURA_HUD_ANIM_*` no `text.vsh`; a duração da linha deve cobrir parada + sumiço, 70 ticks).

`y: 0` põe a primeira linha onde o texto da bossbar ficaria. Âncoras à direita
alinham pela direita: o fundo termina em `x`. Valor inválido gera um aviso
`[notificacoes]` no console e vira o padrão ou o limite. Âncora inválida
descarta a região. Linhas que subiriam além do alcance do shader (15 passos de
12 px) são cortadas, também com aviso.

O arquivo é lido no `onEnable` e no `/textura reload`. As fontes de cada linha
(`textura:notify/<altura>`) nascem no build do pack, então mudar `y`, `linhas`,
`espacamento` ou `direcao` precisa de `/textura gerar`.

### API

```kotlin
NotificationApi.registerItemIcons(listOf("oak_planks", "string", "loom"))
NotificationApi.push(player, "coleta", NotificationLine("<green>+3</green> <lang:block.minecraft.oak_planks>", "item:oak_planks", 60, "coleta:oak_planks"))
NotificationApi.push(player, "avisos", NotificationLine("<yellow>Sua Rede de Detritos ficou cheia!</yellow>", "item:loom", 100))
NotificationApi.clear(player, "coleta")
```

- `NotificationLine(texto, icone, duracaoTicks, chave)` também tem as formas
  `(texto, duracaoTicks)` e `(texto, icone, duracaoTicks)`, além de `withIcon` e
  `withKey`. O texto é MiniMessage com as tags do Textura. `<lang:...>` é
  resolvido no servidor em pt_br (vanilla baixado da Mojang mais
  `resource/assets/minecraft/lang/pt_br.json`), para que a largura medida bata
  com a desenhada.
- Ícone: `item:<material>` ou `glyph:<id>`, desenhado com 8 px de altura antes do
  texto. Ícone que não está no pack é omitido. **Use sempre o campo de ícone.**
  Um `<glyph:id>` (ou outro texto com fonte própria) dentro do texto fica na
  altura da fonte dele, não na da linha. Um caractere que não existe na fonte de
  texto das notificações (`ui.fonte-texto`) sai como o quadradinho de glyph
  ausente e é medido com 6 px.
- Cores: só existem as 16 cores legadas. Uma cor hex (`<#ff8800>`, gradiente)
  vira a mais próxima delas. O negrito soma 1 px por caractere, espaço
  incluído.
- Um push com a mesma chave na mesma região troca a linha no lugar e reinicia o
  tempo. Quem chama acumula o valor (ex.: `+1` vira `+2`).
- As linhas vencidas saem num task de 5 ticks. A barra só é reenviada quando o
  conteúdo muda.

### Ícones de item

O `/textura gerar` assa como glyph das fontes de notificação a textura vanilla
de cada material registrado com `NotificationApi.registerItemIcons(...)`. A
busca é a mesma dos ícones `item:` das telas: `item/<nome>`, depois
`block/<nome>`, `_front`, `_side` e `_top`, lidos do client da Mojang. Os glyphs
de `configs/glyphs` (fora das folhas) entram sozinhos como `glyph:<id>`. O
build do pack no startup roda no primeiro tick depois de todos os plugins
habilitarem, então **registre os ícones durante o `onEnable`**. Ícones
registrados depois só entram no próximo `/textura gerar`.

### Contrato do marcador (shader)

Texto de GUI com cor exatamente `R=254`, `G = âncora × 16` (1 TOPO_ESQUERDA,
2 TOPO_DIREITA, 3 MEIO_ESQUERDA, 4 MEIO_DIREITA, 5 BASE_ESQUERDA,
6 BASE_DIREITA) e `B = paleta × 16 + lift` é movido pelo `text.vsh`:

- a paleta (0..15) é a dos códigos `§0`..`§f`, e a cor real é pintada no lugar da
  cor do marcador;
- o `lift` (0..15) sobe a linha `lift × 12` px. É assim que linhas sobem a partir
  da base: as fontes só conseguem descer o texto;
- a sombra vai como `shadow_color` explícita com `R=253` e o mesmo G/B, e o
  shader escurece a paleta como a sombra vanilla (×0,25).

Qualquer outra cor fica vanilla. Não use essas cores em outros textos (chat,
scoreboard, nomes de item): o shader também os moveria. O `/textura hudtest
[on|off]` mostra uma bossbar de teste com as 6 âncoras e dois controles sem
marcador.

## `config.yml`

`ui.fonte-texto` é o JSON (em `configs/fonts/`) cujos providers viram a fonte do
texto das telas de dialog; as larguras são medidas dos PNGs dele. A mesma fonte é a do
texto das notificações. `notificacoes.idioma` (padrão `pt_br`) é o idioma em que
o `<lang:...>` das notificações é resolvido.

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
| `/textura hudtest [on\|off]` | Bossbar de teste das 6 âncoras do shader (só op) |

Tudo sob a permissão `textura.admin`. O HUD tem comando próprio: `/hud
show|hide|sec|unsec`.
