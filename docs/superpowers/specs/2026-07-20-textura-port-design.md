# Port do sistema Textura (resourcepack/glyphs) para o one-block

**Data:** 2026-07-20
**Origem:** `D:\reallms` — `plugins/resourcepack`, plugin Bukkit `Textura`
**Destino:** `D:\one-block` — novo módulo `:plugins:textura`

## Objetivo

Trazer o sistema de resource pack dinâmico do reallms para o one-block como plugin
separado (jar próprio), assumindo o papel que hoje é do Nexo: fornecer glyphs, fontes
customizadas, itens por `custom_model_data` e a entrega do pack aos jogadores.

## Contexto

O código de origem tem 2044 LOC Kotlin e é autocontido: não importa nenhuma lib
interna do reallms. Depende só de paper-api, Lamp (comandos), inventory-framework e
`team.unnamed:creative` 1.7.3 do Maven Central.

O one-block já consome tags `<glyph:...>`, `<shift:...>` e `<font:oneblock:...>` em 12
call sites (islands, sell, shop, trader). Essas tags vêm do Nexo
(`server/plugins/Nexo`), não do BetterHud que foi removido. O Textura expõe apenas
`GlyphApi` programática — não registra tags MiniMessage. Fechar essa lacuna é parte do
trabalho, não um extra.

## Decisões

| Decisão | Escolha |
|---|---|
| Escopo | Plugin inteiro (glyphs, fonts, items, HUD, servidor HTTP, comandos, chat listener, menu de teste) |
| Localização | `plugins/textura/`, módulo `:plugins:textura` |
| Package | `one_block.textura` |
| Comandos | Convertidos de Lamp para ACF (aikar), já fornecido por `paper-common` |
| Relação com Nexo | Textura substitui o Nexo como fonte de glyphs |
| HUD das ilhas | HUD `phases` reimplementado; `BossbarManager` deletado |
| HUD `default` | Fora desta fase |

## Arquitetura

### Módulo e build

- `plugins/textura/` incluído em `settings.gradle.kts` como `:plugins:textura`
- Convention `paper-plugin`; `plugin.yml` gerado pelo bloco `bukkit {}`:
  name `Textura`, main `one_block.textura.TexturaPlugin`, api-version `1.21`
- `shadowJar` com `archiveFileName = "textura.jar"`
- Catálogo (`gradle/libs.versions.toml`): versão `creative = "1.7.3"` e as libraries
  `creative-api`, `creative-serializer-minecraft`, `creative-server`
- Repositório `https://repo.unnamed.team/repository/unnamed-public/` adicionado em
  `paper-common` caso o Maven Central não resolva os artefatos
- Lamp removido. `inventory-framework` reusa `jars/inventory-framework-paper-3.7.1.jar`,
  o mesmo jar local que `:plugin` já sombreia

### Port do código

Package `org.gustaav.app.resourcepack` → `one_block.textura`, mantendo os subpacotes
`api`, `command`, `http`, `hud`, `listener`, `menu`, `model`, `registry`, `util`.
`ResourcePackPlugin` passa a se chamar `TexturaPlugin`.

`TexturaCommand` e `HudCommand` são reescritos em ACF: `@CommandAlias("textura")` no
lugar de `@Command`, `@Subcommand` e `@CommandPermission` equivalentes, e os parâmetros
`Array<String>` viram varargs ACF. Os 10 subcomandos existentes são preservados um a um.

Resources copiados como estão: `config.yml`, `fonts/` (json + 3 png), `glyphs/emojis.yml`
com `espada.png`, `items/exemplo.yml`, `bossbar.json`. O `plugin.yml` manual é descartado
em favor do DSL.

### Tags MiniMessage

Novo `one_block.textura.api.TexturaTags` expõe um `TagResolver` com:

- `<glyph:id>` — resolve para o char do glyph com o `Component.font(...)` correspondente,
  consultando `GlyphApi`. Glyph inexistente vira componente vazio, não exceção.
- `<shift:n>` — deslocamento horizontal em pixels, positivo ou negativo, apoiado numa
  fonte de espaço negativo que o `ResourcePackBuilder` passa a gerar (provider `space`
  com advances).

Em `lib/formatter` entra um objeto `Mini` que expõe uma instância MiniMessage com esses
resolvers. Os 12 call sites que hoje chamam `MiniMessage.miniMessage()` passam a usar
`Mini.mm`. `:plugin` ganha `compileOnly(projects.plugins.textura)` e `Textura` na lista
`depend` do `plugin.yml`, garantindo ordem de carregamento.

### Namespace de fonte

Hoje o Textura emite fontes só sob `minecraft:<font>`; o one-block referencia
`oneblock:gui` e `oneblock:test`. `FontData` e `GlyphFont` ganham campo `namespace`
(default `minecraft`), propagado no `ResourcePackBuilder` e no `GlyphApi.getFontKey`.

### Migração Nexo → Textura

Converter `Nexo/glyphs/oneblock/icons/gui.yml` e `icons.yml` (~14 glyphs) para o formato
yml do Textura, preservando o `char` explícito de cada glyph para não invalidar nada já
renderizado. Os PNGs correspondentes saem de `Nexo/pack/` para `plugins/Textura/glyphs/`.

Os demais namespaces do Nexo (emojis, prefix, progress_bar, item, logo) ficam fora desta
fase e viram pendência registrada. O Nexo só é removido de `server/plugins` depois da
paridade visual validada em servidor.

### HUD das ilhas

`BossbarManager` é deletado. O HUD `phases` do Textura assume a barra de fase: mesmo
texto (`<glyph:floating_islands>` + fase + progresso `grid/blocksToNextPhase`) e mesmo
progresso, agora via `HudApi.show(player, "phases", HudContent(...))`. Os call sites
`initBossBar`, `upgradeBossBar`, `updatePlayerBar` e `clearBossbar` passam a chamar a
`HudApi`. O slot `default` continua disponível na API, sem consumidor.

## Tratamento de erros

- Pack não gerado: `applyPack` avisa o jogador e não lança
- Geração concorrente: `AtomicBoolean` já existente barra a segunda chamada
- Falha ao subir o servidor HTTP: log severo, plugin continua ativo sem entrega de pack
- Glyph ou fonte ausente numa tag: componente vazio, nunca exceção em GUI
- Conflito de porta com o pack server do Nexo enquanto os dois coexistirem: porta do
  Textura configurável em `config.yml` (`host.porta`)

## Testes

O repo não tem infraestrutura de teste hoje — nenhum `src/test`, nenhum
`testImplementation` no catálogo ou nas conventions. Este módulo introduz Kotest e a
plataforma JUnit apenas em `plugins/textura`, sem tocar nos demais módulos:

- `GlyphRegistry` — parse do yml, herança de ascent/height, geração de unicode
- `HudLayout` — `segmentsFor` e `backgroundWidth` nos limites (0, 1, 2, n segmentos)
- `TexturaTags` — `<glyph>` com id válido, id inexistente, `<shift>` positivo e negativo
- Conversor Nexo→Textura — função pura, entrada yml do Nexo, saída yml do Textura

Verificação manual em servidor: `/textura gerar`, pack aplicado no join, títulos de GUI
das ilhas/loja/venda com glyph e shift corretos, barra de fase atualizando ao quebrar
bloco.

## Fora de escopo

- HUD `default`
- Migração dos glyphs Nexo fora do namespace `oneblock`
- Remoção do jar do Nexo de `server/plugins`
- Sistema de itens por `custom_model_data` alimentado com dados reais do one-block
  (o registry é portado, mas nenhum item do servidor é migrado nesta fase)

## Riscos

1. **Espaço negativo** — `<shift>` depende de uma fonte que o `ResourcePackBuilder` do
   reallms não gera hoje. É a parte mais incerta do trabalho e deve ser validada cedo.
2. **Resolução do `creative`** — se os artefatos não estiverem no Maven Central, o repo
   `repo.unnamed.team` precisa entrar em `paper-common`.
3. **Paridade visual com o Nexo** — ascent/height dos glyphs migrados podem exigir
   ajuste fino; só se verifica com o cliente aberto.
