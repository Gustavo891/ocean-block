# OceanBlock — Progressão: nível do jogador e missões

Data: 2026-10-24 (missões acrescentadas em 2026-10-25). Substitui o sistema de pesquisa (`system/research`, spec `2026-10-21-oceanblock-peneira-fundicao-pesquisa-design.md`) como trava das receitas.

## Intenção

Hoje uma receita avançada só abre depois de uma pesquisa paga em itens e esperada em tempo, por ilha. O usuário quer trocar isso por um nível do jogador: ele ganha XP fazendo as coisas que já faz para coletar recursos (puxar detritos, quebrar o que o gerador forma, fabricar) e cumprindo missões, e cada receita pede um nível mínimo. As missões aparecem no HUD do topo da tela e guiam o jogador pelo jogo ("Colete 10 detritos usando o gancho", "Faça um gerador de pedra", "Faça uma rede"...); depois da trilha, viram diárias. A sidebar já mostra "Nível" (selo, barra e percentual) com um valor fixo; este sistema liga esse campo a dados reais.

## Objetivos

- Remover por inteiro o sistema de pesquisa (módulo, comando `/pesquisa`, menu, dados).
- Nível por jogador, de 1 a 50, alimentado por XP.
- XP vindo das ações manuais que já existem (detritos pelo Gancho e pelo toque, blocos formados pelo gerador quebrados à mão, receitas fabricadas) e das missões.
- Cada receita pede um nível; `/receitas` mostra e aplica a trava.
- Subida de nível visível: título, som e aviso listando o que abriu.
- Missões por jogador: uma trilha fixa de 13, uma por vez, e depois 3 diárias por dia; a missão atual sempre no HUD do topo.
- `/nivel` e `/missoes` como diálogos Textura com um tema novo, "Maré".
- Sidebar mostrando o nível e o progresso reais.

## Não objetivos (ficam para depois, decisão do usuário)

- Mob farm (sistema novo, terá spec própria).
- XP de pesca.
- XP de venda (vender aparece só como objetivo de missão).
- Upgrades do gerador da ilha continuam só por coins, sem exigir nível.

## Anti-objetivos

- Subir de nível parado: a Rede de Detritos e o Minerador **não** dão XP nem avançam missão.
- Farmar XP colocando e quebrando pedra: só conta bloco que o gerador formou.
- Travar o jogador fora de algo que ele já tinha: receitas continuam visíveis no `/receitas`, só bloqueadas com o nível exigido.

## Decisões do usuário

| Tema | Decisão |
|---|---|
| Dono do nível | Por jogador. A receita abre para quem tem o nível, em qualquer ilha. |
| Curva | Máximo 50. XP para ir do nível n ao n+1 = `round(100 × n^1,5)` (1→2: 100; 10→11: 3.162; 49→50: 34.300; total até 50 ≈ 672 mil). |
| Pesquisa já salva | Apagar a coleção Mongo `island_research`. |
| Escopo | Mob farm e pesca ficam para depois. |
| Fontes de XP | Detritos (Gancho/toque), blocos do gerador, receitas fabricadas, missões. Venda não. |
| Coleta automática | Rede e Minerador não dão XP. |
| Subida de nível | Título + som + aviso Textura; ganho de XP aparece como `+N XP` na região de coleta. |
| Tabela de receitas | Aprovada como abaixo. |
| Upgrade do gerador | Continua só coins. |
| Missões | Trilha fixa, uma por vez, e depois diárias; por jogador; recompensa em XP de nível; mesma spec e entrega do nível. |
| Trilha | Aprovada como abaixo. |
| Diárias | 3 por dia, renovam à meia-noite de Brasília; o HUD mostra a primeira não concluída. |
| Definição | Missões no código (lista Kotlin tipada). |
| Telas | `/nivel` e `/missoes` são dois diálogos Textura separados. |
| Tema | Custom "Maré": madeira de deriva clara, faixa de corda azul-petróleo, cartões cor de areia, barra turquesa. |
| HUD | Texto + progresso: `Missão: Colete 10 detritos usando o gancho 3/10`. |

### Nível de cada receita

| Nível | Receitas (id) |
|---|---|
| 1 | Gancho (`gancho`), Fundição (`fundicao`) |
| 3 | Rede de Detritos (`rede`) |
| 8 | Fundição rápida (`fundicao_melhorada`) |
| 12 | Picareta fundidora (`picareta_fundidora`) |
| 15 | Fundição com funil (`fundicao_funil`) |
| 18 | Gerador de energia (`gerador_energia`) |
| 20 | Painel solar (`painel_solar`) |
| 22 | Fornalha elétrica (`fornalha_eletrica`) |
| 25 | Minerador (`minerador`) |
| 28 / 32 / 36 / 40 | Brocas de ouro / diamante / obsidiana / netherite |

As pesquisas "Bancada auxiliar" I e II deixam de existir (serviam só para slots de pesquisa).

### Trilha de missões

| # | Missão (texto no HUD) | Objetivo | XP |
|---|---|---|---|
| 1 | Colete 10 detritos usando o gancho | 10 detritos puxados pelo Gancho | 50 |
| 2 | Faça um gerador de pedra | gerador forma bloco na ilha do jogador, ou ele quebra um bloco de gerador | 50 |
| 3 | Quebre 32 blocos do gerador | 32 blocos do gerador quebrados à mão | 60 |
| 4 | Faça uma Fundição | fabricar `fundicao` | 80 |
| 5 | Chegue ao nível 3 | nível ≥ 3 | 50 |
| 6 | Faça uma Rede de Detritos | fabricar `rede` | 100 |
| 7 | Colete 50 detritos | 50 detritos (Gancho ou toque) | 120 |
| 8 | Venda $1.000 em itens | soma de vendas ≥ 1.000 | 150 |
| 9 | Compre o upgrade do gerador nível 1 | upgrade Gerador da ilha do jogador ≥ 1 | 200 |
| 10 | Quebre 10 minérios de ferro | 10 `IRON_ORE` do gerador quebrados à mão | 150 |
| 11 | Faça uma Fundição rápida | fabricar `fundicao_melhorada` | 300 |
| 12 | Faça um Gerador de energia | fabricar `gerador_energia` | 600 |
| 13 | Faça um Minerador | fabricar `minerador` | 1.000 |

## Desenho

### Módulos

Dois módulos novos, cada um com seu `object XxxSystem` e testes Kotest (ambos dependem de `lib:connection`, então ganham o alias `localTest`):

- `system/level` (`ocean_block.system.level`): curva, XP, dados do nível, trava das receitas, subida de nível, sidebar, diálogo `/nivel`.
- `system/missions` (`ocean_block.system.missions`): missões, HUD, diálogo `/missoes`. Depende `compileOnly` de `level` para dar XP.

Ordem no `setupSystems()`: `LevelSystem.init` depois de `CraftSystem`, `DriftSystem`, `IslandsSystem`, `StationsSystem`, `MachinesSystem`, `SellSystem` e `ScoreboardSystem` (precisa das receitas registradas); `MissionsSystem.init` logo depois. Os dois `onDisable()` entram em `OceanBlockPlugin.onDisable()`.

`level` depende `compileOnly` de `lib:connection`, `lib:instance`, `lib:formatter`, `textura`, `craft`, `drift`, `islands` e `scoreboard`. `missions` depende dos mesmos (menos `scoreboard`) mais `level` e `sell`. Nenhum desses passa a depender de `level` ou `missions`.

### Eventos novos nos módulos existentes

- `drift`: `DebrisCollectedEvent(player, typeId, byHook: Boolean)`, disparado em `DebrisMotion.deliver(debris, player)` (o caminho do Gancho e do toque; quem chama sabe se veio do Gancho). A entrega na Rede (`NetDelivery`) não dispara nada.
- `islands`:
  - `GeneratorBlocks`: posições que o `GeneratorListener` formou, em memória (chave mundo+x+y+z). Num `BlockBreakEvent` (MONITOR, ignoreCancelled) numa posição marcada, remove a marca e dispara `GeneratorBlockBrokenEvent(player, material)`. Colocar um bloco numa posição marcada (`BlockPlaceEvent`) remove a marca. O Minerador quebra com `setType`, sem `BlockBreakEvent`, então não dá XP. Perder as marcas num restart só faz o primeiro bloco de cada gerador não render XP.
  - `GeneratorFormedEvent(island)`, disparado pelo `GeneratorListener` a cada bloco formado.
  - `IslandUpgradeEvent(island, upgrade, level)`, disparado quando um upgrade é comprado no `UpgradeView`.
- `craft` e `sell` já têm `CustomCraftEvent` e `SellEvent`.

### Nível (`system/level`)

**Curva (pura, `LevelCurve`)**: `MAX_LEVEL = 50`; `toNext(n) = round(100 × n^1,5)`; `progress(totalXp) -> LevelProgress(level, fraction)`; `totalFor(level)`. No nível 50 a fração é 1,0 e XP novo não é somado (o total para em `totalFor(50)`).

**Dados**: coleção `player_levels`, `data class PlayerLevel(uuid: String, xp: Long)` com índice único em `uuid`. Só o XP total é salvo; o nível é derivado, então mudar a curva recalcula todo mundo. `LevelRepository` + `LevelManager` no padrão Manager + Repository: carrega a coleção inteira no `init`, `award` marca sujo, flush a cada 30 s via `updateAll` limpando o conjunto só depois do sucesso, `onDisable` faz o último flush. Jogador sem documento começa com 0 XP (nível 1).

**Apagar a pesquisa no Mongo**: `LevelSystem.init` chama `drop()` em `island_research` pelo `ScheduledExecutor` (idempotente: sem a coleção não faz nada; falha é logada, nunca fatal).

**API**: `PlayerLevels.award(player, amount)` soma XP (ignora criativo e espectador e `amount <= 0`), dispara `+N XP` e, se mudou de nível, a subida e o evento `PlayerLevelUpEvent(player, from, to)`. `PlayerLevels.levelOf(player)`.

**Fontes de XP**: valores em `server/plugins/ocean-block/nivel.yml` (cópia do arquivo empacotado quando falta, lido uma vez no startup; valores inválidos logados com `[nivel]` e trocados pelo padrão, como `gerador.yml`). Valores escolhidos por mim, para o usuário ajustar:

```yaml
detritos:
  padrao: 4
  tipos:
    tabua: 4
    folhas: 3
    sucata: 6
    barril: 10
gerador:
  COBBLESTONE: 1
  STONE: 1
  COAL_ORE: 3
  COPPER_ORE: 4
  IRON_ORE: 5
  REDSTONE_ORE: 6
  GOLD_ORE: 8
  LAPIS_ORE: 8
  EMERALD_ORE: 15
  DIAMOND_ORE: 20
receitas:
  base: 10
  por-nivel: 3
```

Parser puro `LevelConfigParser`.

- Detritos: ouve `DebrisCollectedEvent` e dá `detritos.tipos[typeId] ?: detritos.padrao`.
- Gerador: ouve `GeneratorBlockBrokenEvent` e dá `gerador[material] ?: 0`.
- Receitas: ouve `CustomCraftEvent` e dá `base + por-nivel × nível da receita` por fabricação (não multiplica pela quantidade do resultado).

**Receitas travadas por nível (`system/craft`)**:

- `OceanRecipe.research: String?` vira `OceanRecipe.level: Int = 1`. `drift`, `stations` e `machines` passam o nível da tabela ao registrar (as brocas deixam de usar `copy(research = null)`).
- `RecipeLocks.install(provider: (Player) -> Int)` recebe o nível do jogador; `requiredLevel(player, recipe)` devolve o nível exigido quando o jogador não o tem, senão `null`. Sem provider instalado nada trava. `level` instala no seu `init`.
- `CraftResult.Locked(research: String)` vira `Locked(level: Int)`. Mensagem: `<red>Você precisa do nível <white>N</white> para craftar isso.`
- `RecipeLore`: `RecipeCard.lockedBy` vira `requiredLevel: Int?`; status `<red>Requer nível N` quando travada; toda receita mostra `<dark_gray>Nível N` abaixo da descrição.

**Subida de nível**: quando um `award` cruza um ou mais níveis:

- Título `<aqua>NÍVEL N` com subtítulo `<gray>Você subiu de nível!` (fade 10/50/15 ticks).
- Som `ENTITY_PLAYER_LEVELUP`.
- Notificação Textura na região `avisos` (100 ticks, chave `nivel`): `<green>Nível N!</green>` e, se abriu receitas entre o nível antigo e o novo (puro `LevelUps.unlocked(from, to, recipes)`), ` <white>Liberado: A, B</white>`, com o ícone do item da primeira receita liberada (sem receita, `item:experience_bottle`).

**Ganho de XP comum**: notificação na região `coleta`, chave `coleta:xp`, ícone `item:experience_bottle`, texto `<green>+N</green> <white>XP</white>`, 60 ticks, somando ganhos seguidos enquanto a linha vive (mesmo esquema do `CollectTally` do drift, com um `XpTally` próprio). O ícone é registrado com `NotificationApi.registerItemIcons` no `init`.

**Sidebar**: `LevelSystem.init` define `AccountLevel.source = { LevelCurve.progress(LevelManager.xp(it.uniqueId)) }`. Os placeholders `%nivel%`, `%nivel_barra%`, `%nivel_progresso%` passam a mostrar o valor real; o `scoreboard` não muda.

**Diálogo `/nivel`** (alias `/level`, `/niveis`): tela Textura `configs/ui/nivel.json`, tema `mare`, aberta por `DialogApi.openResult`:

- Card do nível: `Nível N` em destaque, barra (`progress`) do XP no nível, `XP atual / XP do próximo nível` e percentual; no 50, `Nível máximo`.
- Card "Como ganhar XP": três linhas com ícone (Gancho, pedregulho, bancada) e os valores do `nivel.yml` (detritos: faixa de valores; gerador: pedra e minério mais raro; receitas: `base + por-nivel × nível`), mais "Missões: veja /missoes".
- Card das receitas: lista `receitas` (repeat, paginada se não couber) em ordem de nível, ícone do resultado, nome, `Nível N` e `✔ Liberada` ou `✖ Bloqueada`. Os ícones das receitas são registrados com `DialogApi.registerItemIcons` no `init` (antes do build do pack).

Se o diálogo não abre (`MissingScreen` ou `Failed`), cai num resumo no chat (nível, XP, próxima receita). Comandos de admin (`oneblock.admin`): `/nivel dar <jogador> <xp>` e `/nivel definir <jogador> <nivel>` (XP vira `totalFor(nivel)`).

### Missões (`system/missions`)

**Modelo (puro)**: `Mission(id, text, objective, xp)`. `MissionObjective` selado:

- `CollectDebris(amount, hookOnly)`
- `BuildGenerator`
- `BreakGeneratorBlocks(amount, material: String?)`
- `Craft(recipeId)`
- `CraftAny(amount)`
- `ReachLevel(level)`
- `Sell(value)`
- `IslandUpgrade(upgrade, level)`

`MissionTrail.all` é a lista fixa da trilha (tabela acima). `DailyPool` define as diárias sorteáveis:

| id | Texto | Objetivo |
|---|---|---|
| `detritos_30` | Colete 30 detritos | CollectDebris(30, false) |
| `gancho_15` | Colete 15 detritos usando o gancho | CollectDebris(15, true) |
| `gerador_64` | Quebre 64 blocos do gerador | BreakGeneratorBlocks(64, null) |
| `gerador_128` | Quebre 128 blocos do gerador | BreakGeneratorBlocks(128, null) |
| `receitas_3` | Fabrique 3 receitas | CraftAny(3) |
| `venda_2000` | Venda $2.000 em itens | Sell(2000) |
| `venda_5000` | Venda $5.000 em itens | Sell(5000) |

XP de diária: `round(0,15 × LevelCurve.toNext(nível no sorteio))`, mínimo 30 (escolha minha: acompanha o nível). Sorteio sem repetir id no mesmo dia; não sorteia duas do mesmo tipo de objetivo (por exemplo, `gerador_64` e `gerador_128` juntas).

**Regras puras (`MissionRules`)**: dado o estado e um `MissionSignal` (detrito com `byHook`, bloco do gerador com material, gerador formado, receita com id, venda com valor, nível atual, upgrade com nível), devolve o novo estado e as missões concluídas. Objetivos de estado (`ReachLevel`, `IslandUpgrade`, `BuildGenerator` quando a ilha já tem gerador) também são checados quando a missão vira a atual e na entrada do jogador. Uma conclusão na trilha avança para a próxima e checa a nova na hora (podem concluir várias seguidas). `MissionRules.rollDailies(date, level, random)` e `needsReset(state, today)`.

**Dados**: coleção `player_missions`, `data class PlayerMissions(uuid, trailIndex: Int, trailProgress: Long, dailyDate: String, dailies: List<DailyMission>)`, `DailyMission(id, progress: Long, xp: Int, done: Boolean)`. Manager + Repository igual ao nível (flush 30 s, `onDisable`). Sem documento: trilha na missão 1, sem diárias. As diárias só são sorteadas quando `trailIndex` passa da última missão; a data é `LocalDate` em `America/Sao_Paulo`, checada na entrada, a cada sinal e a cada minuto para os jogadores online (renova sem precisar relogar).

**Quem conta**: só o próprio jogador. `BuildGenerator` conclui quando um `GeneratorFormedEvent` acontece numa ilha onde o jogador é dono ou membro e está online, ou quando ele quebra um bloco de gerador. `IslandUpgrade` lê a ilha do jogador (`IslandManager.getIslandByPlayer`) na entrada, ao virar atual e em cada `IslandUpgradeEvent` dessa ilha. Criativo e espectador não avançam missão.

**HUD**: slot `missao` do `HudApi`, mostrado na entrada e atualizado a cada progresso:

- Trilha: `<aqua>Missão:</aqua> <white>Colete 10 detritos usando o gancho</white> <gray>3/10` (progresso em `$` para vendas: `$350/$1.000`; objetivos de um passo sem contador).
- Diárias: `<aqua>Diária 1/3:</aqua> <white>Quebre 64 blocos do gerador</white> <gray>12/64`.
- Todas feitas: `<gray>Diárias concluídas. Volte amanhã!`.

O fundo do HUD usa os glyphs globais `hud_left`/`hud_body`/`hud_right`, redesenhados no estilo Maré (o OceanBlock não usa o HUD para mais nada hoje). Textos pelo puro `MissionText`.

**Conclusão**: `PlayerLevels.award(player, xp)`, notificação em `avisos` (100 ticks, chave `missao`): `<green>Missão concluída:</green> <white>texto</white> <gold>+N XP`, som `UI_TOAST_CHALLENGE_COMPLETE`, e o HUD passa para a próxima.

**Diálogo `/missoes`** (alias `/missao`): tela `configs/ui/missoes.json`, tema `mare`:

- Card "Trilha": `Missão N de 13`, texto da atual, barra de progresso, contador e recompensa; trilha completa mostra `Trilha concluída!`.
- Card "Diárias": lista `diarias` com as 3 do dia (texto, barra, contador, XP, `✔` quando feita) e `Renovam em HH:MM`; antes de terminar a trilha mostra `Termine a trilha para liberar as diárias.`.

Fallback no chat como o `/nivel`. Admin (`oneblock.admin`): `/missoes pular <jogador>` (conclui a atual sem XP) e `/missoes resetar <jogador>`.

### Tema "Maré" (Textura)

Atualização de 2026-10-25: na sessão do tema o usuário trocou a direção para um visual minimalista no estilo das telas do Minecraft (painel cinza vanilla, cartões rebaixados, slots vanilla, barra verde como a de XP), mantendo o nome `mare`; o fundo do HUD virou uma placa preta translúcida. A paleta abaixo é a proposta original.

Novo tema `configs/ui/mare.yml` com arte em `resource/assets/oneblock/textures/ui/mare/`, desenhada por script descartável no mesmo esquema do `maquina` (peças com `.png.slice`):

- `sheet`: madeira de deriva clara (bege acinzentado `#b89a72`), veio horizontal de tábuas, borda mais escura `#7a5f3e`.
- `head`: faixa azul-petróleo `#2f7f86` com linha de corda tracejada cor de areia embaixo.
- `well` / `frame`: cartão cor de areia `#d9c79a` com borda `#9c7f58`.
- `slot`: areia mais escura com cantinhos de prego.
- `progress_track` `#1d4e63` e `progress_fill` turquesa `#3fd0c9`.
- `button`: azul-petróleo com destaque de espuma no topo.
- Textos: título claro sobre a faixa, corpo marrom-escuro `#3b2a17`, `good` verde-mar, `accent` turquesa escuro, `bad` coral.

Peças que nenhuma das duas telas usa (segmented, nav, dropdown, slider) apontam para a arte `stone`, como o `maquina` faz.

### Menu da ilha

No `IslandView`, o botão "Pesquisa" (`WRITABLE_BOOK`, roda `pesquisa`) vira "Nível" (`EXPERIENCE_BOTTLE`, "Mostra seu nível e as receitas.", roda `nivel`), e entra um botão "Missões" (`MAP`, "Mostra sua missão e as diárias.", roda `missoes`) num slot livre do hub.

### Remoção da pesquisa

- Apagar `plugins/oceanblock/system/research` inteiro e tirá-lo de `settings.gradle.kts`.
- Tirar `ResearchSystem.init` e `ResearchSystem.onDisable` do `OceanBlockPlugin`.
- `/pesquisa` deixa de existir (sem alias de transição).
- CLAUDE.md: seção Research substituída por Level e Missions; textos sobre `research = ...` em craft, stations e machines atualizados; lista de módulos com testes.
- Manual de aceite: seção de pesquisa vira seção de nível e missões.

## Testes

Kotest nos puros: `LevelCurve` (toNext, progress nos limites, teto 50, XP além do teto), `LevelConfigParser` (válidos, inválidos, faltando), `XpTally`, `LevelUps.unlocked`, `MissionRules` (cada objetivo, avanço encadeado, objetivos de estado, diária não conta antes da trilha, troca de dia), `rollDailies` (3 distintas, sem tipo repetido, XP mínimo), `MissionText`, `MissionTrail` (ids únicos, receitas citadas existem na tabela), e os testes de `craft` (`RecipeLore`, `RecipeBook`) ajustados para `level`. O lado Bukkit (eventos, título, HUD, diálogos, marcas do gerador) entra no manual de aceite.

## Fica a critério de quem constrói

- Nomes internos de classes e pacotes dentro dos módulos novos.
- Layout fino dos dois diálogos dentro dos cards descritos.
- Pixel art exata do tema Maré dentro da paleta descrita.

## Escolhas minhas (para o usuário conferir)

- Valores de XP do `nivel.yml`.
- XP de receita por fabricação, sem multiplicar pela quantidade.
- Pool de diárias e XP de diária = 15% do XP do próximo nível (mínimo 30).
- Criativo e espectador não ganham XP nem avançam missão.
- Só o XP total é salvo; nível derivado.
- Aliases `/level`, `/niveis`, `/missao`; admin `/nivel dar|definir`, `/missoes pular|resetar`.
- Fallback dos diálogos em chat.
- Fundo do HUD global redesenhado no estilo Maré.
- Botão "Missões" novo no menu da ilha.
- Marcas do gerador só em memória.
- O `drop()` de `island_research` fica no código (é no-op depois da primeira vez).
