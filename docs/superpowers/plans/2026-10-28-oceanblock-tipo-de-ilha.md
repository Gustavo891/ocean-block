# Tipo de ilha Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task (o usuário trabalha na thread principal, sem subagentes). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tela de escolha do tipo de ilha (Padrão, Deserto, Gelo, Jangada) no primeiro login e no `/ilha` de quem não tem ilha, criando a ilha do tipo escolhido.

**Architecture:** Regras puras em `system/islands/island/type/` (tipo, troca de blocos, textos e linhas da tela) cobertas por Kotest; `StarterIsland` cola por tipo e pinta o bioma; o fluxo de escolha fica em `system/islands/choice/` (câmera, dialogs Textura, fallback em baú, listeners); Textura ganha `DialogBinds.closed`.

**Tech Stack:** Kotlin 2.3, Paper 26.3, FAWE, Textura dialogs, Kotest 5.9.

**Spec:** `docs/superpowers/specs/2026-10-28-oceanblock-tipo-de-ilha-design.md`

## Global Constraints

- Textos para o jogador em pt-BR; código sem comentários.
- Commits `type(scope): descrição` terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; só arquivos desta feature (outras sessões têm mudanças sem commit em `textura/hud/*`, `missions`, `pirate-raid`).
- Jar gerado de cópia limpa se houver mudanças de outros sem commit; perguntar ao usuário antes de parar/reiniciar o servidor.
- Toda tela `abismo` abaixo de 256 px de altura, todo `repeat` com `perPage`.
- API pública da Textura sem tipos `kotlin.*` (`Runnable`).
- Ids de tipo: `padrao`, `deserto`, `gelo`, `jangada`; desconhecido vira `padrao`.

## Review Focus

- Jogador que sai no meio da escolha fica em espectador: o próximo login precisa reconhecer a marca e devolver `SURVIVAL` se já tiver ilha (Task 6, teste em `ChoiceRulesTest`).
- Membro convidado sem ilha própria não pode ver a tela nem ganhar ilha nova pelo `/ilha` (Task 6, `needsChoice` com participação).
- Clique duplo em CONFIRMAR criaria duas ilhas: o segundo clique precisa ver a ilha já criada (Task 6, `createIsland` recusa dono existente).
- `island.schem` com bloco fora da tabela (ou com propriedades) não pode quebrar a troca (Task 1, teste de bloco intacto e propriedades).
- `resetplayer` em quem não é dono não apaga nada (Task 7, mensagem e retorno antes do regen).

---

### Task 1: Tipo e troca de blocos (puros)

**Files:**
- Create: `plugins/oceanblock/system/islands/src/main/kotlin/ocean_block/system/islands/island/type/IslandType.kt`
- Create: `.../island/type/IslandPalette.kt`
- Test: `plugins/oceanblock/system/islands/src/test/kotlin/ocean_block/system/islands/island/type/IslandTypeTest.kt`, `IslandPaletteTest.kt`

**Interfaces:**
- Produces: `enum class IslandType(val id: String, val nome: String, val descricao: String, val biome: String?)` com `PADRAO`, `DESERTO` (`"desert"`), `GELO` (`"snowy_plains"`), `JANGADA`; `companion fun fromId(id: String?): IslandType`. Nomes/descrições exatamente os da spec.
- Produces: `object IslandPalette { fun swap(type: IslandType, block: String): String }` — recebe e devolve a string de estado do bloco (`minecraft:oak_log[axis=y]`).

- [ ] **Step 1: testes** — `fromId("gelo") shouldBe GELO`, `fromId("x")`/`fromId(null)` viram `PADRAO`; cada linha da tabela da spec para Deserto e Gelo; `swap(DESERTO, "minecraft:oak_log[axis=x]") shouldBe "minecraft:stripped_oak_log[axis=x]"`; `swap(GELO, "minecraft:oak_leaves[distance=3,persistent=true,waterlogged=false]") shouldBe "minecraft:spruce_leaves[distance=3,persistent=true,waterlogged=false]"`; `swap(DESERTO, "minecraft:mossy_cobblestone_stairs[facing=west,half=bottom,shape=straight,waterlogged=true]")` mantém as quatro propriedades em `sandstone_stairs`; `swap(GELO, "minecraft:short_grass") shouldBe "minecraft:snow[layers=1]"`; `tall_grass[half=lower]` → `snow[layers=2]`, `[half=upper]` → `air`; `swap(DESERTO, "minecraft:cactus[age=5]")` intacto, `swap(GELO, "minecraft:cactus[age=5]") shouldBe "minecraft:air"`; `lava[level=0]`, `iron_trapdoor[...]` intactos; `PADRAO` e `JANGADA` devolvem sempre a entrada.
- [ ] **Step 2:** `./gradlew :plugins:oceanblock:system:islands:test --tests "*island.type*"` → FAIL (não compila).
- [ ] **Step 3:** implementar. `swap` separa id e `[props]`, procura o id sem namespace na tabela do tipo (alvo + propriedades fixas do alvo, ex. `layers`) e mantém das propriedades originais só as que o alvo aceita: logs `axis`; folhas `distance`,`persistent`,`waterlogged`; escadas `facing`,`half`,`shape`,`waterlogged`; cacto `age`. Ordem das propriedades: a original.
- [ ] **Step 4:** mesmo comando → PASS.
- [ ] **Step 5:** commit `feat(ilha): tipos de ilha e troca de blocos`.

### Task 2: Regras da escolha (puras)

**Files:**
- Create: `.../islands/choice/ChoiceRules.kt`
- Test: `.../test/.../choice/ChoiceRulesTest.kt`

**Interfaces:**
- Produces: `object ChoiceRules`:
  - `fun needsChoice(ownsIsland: Boolean, memberSomewhere: Boolean): Boolean` (true só se os dois forem false).
  - `fun afterLogin(marked: Boolean, hasHome: Boolean): LoginAction` com `enum LoginAction { CHOOSE, RESTORE, NOTHING }` (sem casa → `CHOOSE`; marcado com casa → `RESTORE`; resto `NOTHING`).
  - `fun countText(n: Int): String` → `nenhuma ilha`, `1 ilha`, `N ilhas`.
  - `fun rows(counts: Map<IslandType, Int>): List<Map<String, String>>` → uma entrada por `IslandType.entries` com `id`, `nome` (maiúsculo), `descricao`, `contagem`, `icone` (`ui:ilha_tipo/<id>`).
- [ ] **Step 1: testes** com esses valores (inclui `countText(0)`, `1`, `12`; `rows` com mapa vazio dá `nenhuma ilha` nos quatro; ordem PADRAO, DESERTO, GELO, JANGADA; `afterLogin(true, true) shouldBe RESTORE`, `afterLogin(false, true) shouldBe NOTHING`, `afterLogin(any, false) shouldBe CHOOSE`).
- [ ] **Step 2:** `--tests "*ChoiceRules*"` → FAIL. **Step 3:** implementar. **Step 4:** PASS.
- [ ] **Step 5:** commit `feat(ilha): regras da tela de escolha`.

### Task 3: Ilha com tipo, terreno e bioma

**Files:**
- Modify: `island/Island.kt` (campo `var tipo: String = "padrao"` depois de `settings`; `fun type(): IslandType = IslandType.fromId(tipo)`)
- Modify: `island/IslandRepository.kt` (`"tipo" to "padrao"` em `migrateMissingFields`)
- Modify: `island/IslandManager.kt` (`createIsland(player: Player, type: IslandType, onReady: (Island) -> Unit = {}): Island?`, recusa e loga se `islandsByOwnerUuid` já tem o jogador; grava `tipo = type.id`)
- Modify: `island/StarterIsland.kt` (por tipo: `templates/<id>.schem` se existir; Deserto/Gelo derivam de `island.schem` com `IslandPalette.swap` aplicado a cada bloco do clipboard via `BlockState` → `getAsString()` → `BlockState.get(...)`; Jangada 3×3 `OAK_PLANKS` em Y 62 com `IslandPlacement.fallbackFootprint`; depois `IslandBiome.paint(island)`)
- Create: `island/type/IslandBiome.kt` (`fun paint(island: Island)`: se `type().biome != null`, `world.setBiome(x, y, z, biome)` em toda a `IslandArea` a cada 4 blocos, depois `refreshChunk` nos chunks da área)
- Create: `island/type/IceGuardListener.kt` (cancela `BlockFormEvent` com `newState.type == ICE` em lote cuja ilha é `GELO`)
- Modify: `IslandsSystem.kt` (registra `IceGuardListener`), `command/IslandCommand.kt` e `listener/PlayerListener.kt` (chamadas antigas de `createIsland` passam `IslandType.PADRAO` até a Task 6)

- [ ] **Step 1:** implementar e compilar: `./gradlew :plugins:oceanblock:plugin:localJar` → BUILD SUCCESSFUL; `:plugins:oceanblock:system:islands:test` continua PASS.
- [ ] **Step 2:** commit `feat(ilha): ilha guarda o tipo e cola o terreno do tipo`.

### Task 4: Gancho de fechar na Textura

**Files:**
- Modify: `plugins/textura/src/main/kotlin/one_block/textura/api/DialogBinds.kt` (`internal var closed: Runnable? = null`; `fun closed(action: Runnable)`)
- Modify: `plugins/textura/src/main/kotlin/one_block/textura/dialog/DialogOpener.kt` (`handle`: no `CLOSE`, guarda `current[player.uniqueId]?.session?.binds?.closed`, chama `close(player)` e depois roda o `Runnable` com log em erro)
- Modify: `server/plugins/Textura/README.md` (uma linha sobre `binds.closed`)

- [ ] **Step 1:** `./gradlew :plugins:textura:localTest` → PASS (o `PublicApiKotlinTypesTest` cobre a assinatura).
- [ ] **Step 2:** commit `feat(textura): dialog avisa quando o botão de fechar é clicado`.

### Task 5: Telas e artes

**Files:**
- Create: `server/plugins/Textura/configs/ui/ilha_tipo.json` e `ilha_tipo_confirmar.json` (layout da spec; lista `tipos` `perPage: 4` com `{icone}`, `{nome}`, `{descricao}`, `{contagem}`, `listButton("escolher")`; confirmação com `{icone}` numa lista de uma entrada `tipo` `perPage: 1`, textos `{pergunta}`, botões `confirmar` e `voltar`; ambas `"close": "Sair do servidor"`, `"canCloseWithEscape": false`)
- Create: `server/plugins/Textura/resource/assets/oneblock/textures/ui/ilha_tipo/{padrao,deserto,gelo,jangada}.png` (160×160, render isométrico do terreno de cada tipo sobre mar, borda de 4 px na cor do selo: Padrão `#5FA83A`, Deserto `#E3C46A`, Gelo `#8FD3F0`, Jangada `#9C6B3C`), gerados por script descartável no scratchpad lendo `island.schem` e as texturas do jar do cliente em `generated/vanilla/`

- [ ] **Step 1:** `./gradlew :plugins:textura:localTest --tests "*ScreenListsPaged*"` → PASS; preview no Studio (`http://127.0.0.1:7070`) mostra as duas telas com altura < 256.
- [ ] **Step 2:** commit `feat(ilha): telas e artes da escolha do tipo de ilha`.

### Task 6: Fluxo da escolha

**Files:**
- Create: `choice/ChoiceCamera.kt` (`CAMERA` = mundo das ilhas (0.5, 80, 0.5) yaw 0 pitch 10; `enter(player)`: teleporta, `SPECTATOR`, PDC `oceanblock:escolhendo_ilha`; `leave(player)`: se marcado, `SURVIVAL` e remove a marca; `isMarked(player)`)
- Create: `choice/IslandTypeDialog.kt` (`open(player, fromLogin: Boolean)`; lista e confirmação via `IslandScreens.show` com fallback `IslandTypeMenu`; `binds.closed` → `fromLogin` kicka com `Até a próxima!`, senão nada)
- Create: `choice/IslandTypeMenu.kt` + `IslandTypeConfirmMenu.kt` (baús vanilla de 3 linhas da spec; fechar no login reabre em 20 ticks)
- Create: `choice/IslandChoice.kt` (`confirm(player, type, fromLogin)`: se já tem casa só leva até ela; senão `createIsland(player, type)`; `null` → mensagem vermelha da spec e reabre em 100 ticks; `onReady` → `ChoiceCamera.leave`, teleporte `spawn + (0,2,0)`, `onJoin`, `Sua ilha foi criada!`)
- Create: `choice/ChoiceListener.kt` (join: `ChoiceRules.afterLogin`; `CHOOSE` → `ChoiceCamera.enter` e agenda abrir em 40 ticks; `RESTORE` → `leave` e teleporte para casa; pack `SUCCESSFULLY_LOADED` com marca e sem casa → abre)
- Modify: `listener/PlayerListener.kt` (sem criação automática: sem casa, `spawnLocation = ChoiceCamera.CAMERA`)
- Modify: `command/IslandCommand.kt` (`onDefault`: `getHomeIsland == null` → `IslandTypeDialog.open(sender, false)`; remove `createAndEnter`)
- Modify: `IslandsSystem.kt` (registra `ChoiceListener`)

- [ ] **Step 1:** compilar `localJar` → SUCCESS; testes do módulo PASS.
- [ ] **Step 2:** commit `feat(ilha): tela de escolha do tipo no primeiro login e no /ilha`.

### Task 7: `/ilha resetplayer`

**Files:**
- Modify: `island/IslandManager.kt` (`fun deleteIsland(island: Island)`: tira de todos os mapas e de `playerIsland` de todos, `dirtyIslands -= uuid`, `repository.remove(uuid)`)
- Modify: `reset/IslandResetService.kt` (`previewDelete(sender, island)` e `deleteIsland(sender, island)`: regen como `resetTerrain`, `fireTerrainReset`, remove entidades, expulsa membros, `IslandManager.deleteIsland`, dono online → `ChoiceCamera.enter` + `IslandTypeDialog.open(owner, true)`)
- Modify: `command/IslandCommand.kt` (`@Subcommand("resetplayer") @CommandPermission("screen.dev") fun onResetPlayer(sender: Player, targetName: String, @Optional flag: String?)`, mesmo padrão `-confirm`)

- [ ] **Step 1:** compilar → SUCCESS.
- [ ] **Step 2:** commit `feat(ilha): /ilha resetplayer apaga a ilha e reabre a escolha`.

### Task 8: Documentação, deploy e teste manual

- Modify: `CLAUDE.md` (Starting island: tipos, `templates/<id>.schem`, bioma, gelo; Island dialog: tela de escolha, `resetplayer`; Textura: `binds.closed`)
- Create: seção do checklist manual em `docs/superpowers/plans/2026-10-18-oceanblock-aceite-manual.md` (login novo vê a tela; Esc não fecha; Sair desconecta; cada tipo cria a ilha certa e o bioma; gelo não congela; membro convidado não vê a tela; sair no meio e voltar; `/ilha` sem ilha; `resetplayer` online e offline; fallback em baú com pack recusado)
- [ ] **Step 1:** commit `docs(ilha): tipo de ilha no CLAUDE.md e no aceite manual`.
- [ ] **Step 2:** gerar os jars de cópia limpa (`git worktree` temporário do HEAD), **perguntar ao usuário** antes de parar o servidor, instalar, `/textura gerar`, rodar o checklist com ele.
