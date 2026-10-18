# Vacuum Hopper — Design

**Date:** 2026-07-24
**Status:** Approved (design), pending implementation plan
**Module:** `plugins/oneblock/system/custom-items`

## Goal

Bloco custom colocável que suga itens dropados num raio ao redor, filtra por
material e guarda num armazenamento interno com GUI. Inspirado no Vacuum Hopper /
Vacuum Chest de mods (EnderIO, Industrial Foregoing).

Para isso, o módulo `custom-items` ganha um **eixo de bloco** (`CustomBlock`)
paralelo ao eixo de item (`CustomItem`) que já existe. O módulo passa a ser
"conteúdo custom" — item em mão **e** bloco colocado — em vez de só item em mão.
O Vacuum Hopper é o primeiro (e por ora único) `CustomBlock`.

## Decisões (brainstorm)

- **Destino dos itens:** armazenamento próprio no bloco, com GUI. Não empurra para container adjacente.
- **O que suga:** só `Item` entities (drops). Sem XP, sem puxar de containers.
- **Filtro:** whitelist/blacklist por material, configurável em GUI.
- **Energia:** nenhuma. Roda livre depois de colocado.
- **Representação:** bloco real `CONDUIT`. Sem ArmorStand, sem TextDisplay.
- **Progressão:** upgrades aplicados na GUI (raio, armazenamento, velocidade).
- **Módulo:** dentro de `custom-items`, como eixo `CustomBlock` novo.
- **Obtenção:** receita na Forge + comando admin.
- **Limite:** 5 por ilha (só vacuum hoppers; não compartilha teto com minions).
- **Cheio:** para de sugar e avisa o dono uma vez.
- **Custo de upgrade:** moeda `coins` via `lib currencies`.
- **Acesso:** dono + membros da ilha (mesma regra dos minions).
- **Receita:** 1 Conduit + 4 Barra de Netherite + 8 Peça Mecânica.
- **Configuração:** tudo em código (constantes + enum). **Sem** arquivo YAML.

## Arquitetura

### Estrutura de pacotes

A estrutura atual do módulo é organizada **por tipo** (`api/`, `registry/`,
`listener/`, `command/`). Com um eixo só isso passava; com dois, cada feature
concreta ficaria espalhada por cinco pacotes — o vacuum hopper teria sua classe em
um, seu listener em outro, seu comando em outro, suas views em outro. Reorganizar
faz parte desta feature, não é refactor à parte.

Novo desenho, em dois níveis: **framework separado por eixo**, **conteúdo separado
por feature**.

```
custom_items/
├── CustomItemSystem.kt            (existe — wiring dos dois eixos)
│
├── item/                          FRAMEWORK — eixo item
│   ├── CustomItem.kt              (move de api/)
│   ├── CustomItemKeys.kt          (move de api/)
│   ├── CustomItemTask.kt          (move de api/)
│   ├── CustomItemRegistry.kt      (move de registry/)
│   ├── CustomItemListener.kt      (move de listener/)
│   ├── CustomItemCommand.kt       (move de command/)
│   └── context/
│       ├── InteractContext.kt     (move de api/)
│       ├── EntityContext.kt       (move de api/)
│       └── ConsumeContext.kt      (move de api/)
│
├── block/                         FRAMEWORK — eixo bloco (tudo NOVO)
│   ├── CustomBlock.kt
│   ├── CustomBlockKeys.kt
│   ├── CustomBlockRegistry.kt
│   ├── CustomBlockListener.kt
│   ├── CustomBlockCommand.kt
│   ├── BlockUpgrade.kt
│   ├── FilterMode.kt
│   ├── PlacedBlock.kt
│   ├── PlacedBlockManager.kt
│   ├── PlacedBlockRepository.kt
│   ├── PlacedBlockCodec.kt
│   └── context/
│       ├── PlaceContext.kt
│       ├── BreakContext.kt
│       └── BlockClickContext.kt
│
├── cooldown/
│   └── CooldownStore.kt           (existe — compartilhado pelos dois eixos)
│
└── content/                       CONTEÚDO — uma pasta por feature
    ├── magnet/
    │   └── MagnetItem.kt          (move de item/)
    └── vacuum_hopper/             (tudo NOVO)
        ├── VacuumHopper.kt
        ├── VacuumHopperUpgrade.kt
        └── view/
            ├── VacuumHopperView.kt
            ├── HopperStorageView.kt
            ├── HopperFilterView.kt
            └── HopperUpgradesView.kt
```

Regras que essa divisão estabelece:

- `item/` e `block/` são **framework**: cada eixo carrega junto seu modelo,
  registry, listener, comando e contextos. Quem mexe no eixo bloco não abre nem um
  arquivo do eixo item.
- `content/` é **conteúdo de jogo**: cada feature em uma pasta com tudo que é dela.
  Adicionar um item ou bloco novo passa a ser criar uma pasta em `content/` e uma
  linha de registro — sem tocar em nenhum pacote do framework.
- `content/` nunca é importado por `item/` nem por `block/`. A dependência aponta
  só para dentro do framework, nunca de volta. Mesmo princípio que o CLAUDE.md já
  aplica entre systems e o módulo do plugin.

Duas consequências dessa reorganização:

**`CustomBlockCommand` genérico em vez de `VacuumHopperCommand`.** O eixo item já
tem um `/customitem give` genérico; um comando específico do hopper seria a
exceção sem motivo. Vira `/customblock give <id> <player> [qtd]`, com completion
alimentada pelo `CustomBlockRegistry` — o mesmo padrão de completion que
`CustomItemSystem.registerCompletions` já usa hoje.

**`PlacedBlockCodec` separado.** É a leitura/escrita do estado no PDC. Em
`minions` essa lógica mora dentro do objeto `Minions`, que tem 258 linhas fazendo
cinco coisas (item de fuel, peça mecânica, perfis de cabeça, marcação de entidade
e a serialização). Isolar o codec num arquivo só dele mantém `CustomBlock.buildItem`
fino e deixa o formato de serialização num lugar óbvio para quem for debugar
estado perdido.

**Custo da reorganização.** Verificado: só dois pontos fora do módulo importam de
`custom_items` — `OneBlockPlugin` importa `CustomItemSystem` (que fica na raiz, não
move) e `CustomItemCommand` (que move de `command/` para `item/`, exigindo ajustar
esse import). Todo o resto dos moves é interno ao módulo, resolvido pelo refactor
de pacote da IDE. `CooldownStoreTest` acompanha `CooldownStore` e continua em
`cooldown/`.

Nota para a implementação: hoje `CustomItemCommand` é registrado pelo
`OneBlockPlugin.setupCommands()`, enquanto o registro de completions mora no
`CustomItemSystem`. `CustomBlockCommand` segue o registro dentro do
`CustomItemSystem.init`, junto da sua completion, mantendo comando e completion no
mesmo lugar. Unificar o comando de item nesse padrão é uma limpeza óbvia e barata,
mas fica fora do escopo desta feature para não misturar as coisas.

**Nome do módulo.** O módulo se chama `custom-items` mas passa a abrigar blocos.
Renomear para `custom-content` seria mais honesto, mas obriga a mexer em
`settings.gradle.kts`, no build do plugin, no `OneBlockPlugin` e em todo import
existente — churn desproporcional. Fica como está; o KDoc do `CustomItemSystem`
registra que o módulo cobre os dois eixos.

### Dependências do módulo

`plugins/oneblock/system/custom-items/build.gradle.kts` passa a ter, além do que já tem:

```kotlin
compileOnly(libs.mongodb)
compileOnly(projects.lib.connection)
compileOnly(projects.lib.coordinate)
compileOnly(projects.plugins.oneblock.system.islands)
compileOnly(projects.plugins.oneblock.system.craft)
compileOnly(projects.plugins.oneblock.system.currencies)
compileOnly(projects.plugins.oneblock.system.minions)
```

`minions` entra só para `Minions.machinePart()` na receita. Verificado: nenhum dos
quatro systems depende de `custom-items`, então não há ciclo. Mesmo conjunto que
`minions` já declara hoje (`islands` + `craft`).

`lib:connection` traz o split `isLocal` — build local usa `src/local/kotlin`. Nada
especial a fazer, `minions` já depende dele.

### Wiring

`CustomItemSystem.init` passa a receber o `ViewFrame`:

```kotlin
fun init(plugin: JavaPlugin, viewFrame: ViewFrame, commandManager: PaperCommandManager)
```

Dentro dele, além do que já faz:

- `PlacedBlockManager.onInit()`
- registra `CustomBlockListener`
- `viewFrame.with(VacuumHopperView(), HopperStorageView(), HopperFilterView(), HopperUpgradesView())`
- `registerBuiltInBlocks()` → `CustomBlockRegistry.register(VacuumHopper())`
- `commandManager.registerCommand(CustomBlockCommand())` e a completion `customblocks`
- registra a `ForgeRecipe` via `Crafts.register(...)`

`OneBlockPlugin.setupSystems()` atualiza a chamada para passar o `viewFrame`.

## Modelo de dados

### `PlacedBlock`

Coleção Mongo `custom_blocks`. Campos genéricos o bastante para um segundo
`CustomBlock` reusar — mesma escolha que `PlacedMinion` já faz hoje (ele carrega
`toolBase64`, que só o Miner usa).

```kotlin
data class PlacedBlock(
    val uuid: UUID,
    val location: SimpleLocation,
    val blockId: String,                 // "vacuum_hopper" — resolve no CustomBlockRegistry
    val ownerUuid: UUID,
    var enabled: Boolean = true,
    val upgrades: MutableMap<String, Int> = mutableMapOf(),
    /** Itens guardados, cada entrada é um ItemStack serializado em base64. */
    val storage: MutableList<String> = mutableListOf(),
    var filterMode: FilterMode = FilterMode.BLACKLIST,
    /** Materiais na lista de filtro, pelo nome do [Material]. */
    val filter: MutableSet<String> = mutableSetOf(),
    /** Evita spam do aviso de armazenamento cheio. */
    var notifiedFull: Boolean = false,
)
```

Métodos espelhando `PlacedMinion` (mesma semântica, mesmo uso de `Bukkit.createInventory`
como inventário virtual para reaproveitar o stacking do Bukkit):

- `accepts(material): Boolean` — filtro vazio aceita tudo; senão whitelist aceita só os listados, blacklist rejeita os listados.
- `storageCapacity(): Int` — `BASE_STORAGE_SLOTS + nível * PER_LEVEL`.
- `storageItems(): List<ItemStack>` — desserializa, ignorando entradas inválidas.
- `setStorage(items)`, `isStorageFull()`, `addToStorage(items): List<ItemStack>` (devolve a sobra).

`FilterMode` é um enum novo local ao `custom-items` (`WHITELIST`, `BLACKLIST`).
Não reusa o de `minions` — são módulos irmãos e o enum é trivial; importar de lá
acoplaria o modelo de bloco ao de minion sem ganho.

### `PlacedBlockRepository`

Cópia estrutural de `PlacedMinionRepository`: `Mongo.getCollection("custom_blocks")`,
índice único em `uuid`, `toList()`, `update(one)`, `updateAll(list)` via `bulkWrite`,
`remove(uuid)`.

## API `CustomBlock`

```kotlin
abstract class CustomBlock {
    abstract val id: String
    abstract val material: Material
    abstract val displayName: String
    open val lore: List<String> = emptyList()
    open val itemModel: NamespacedKey? = null

    open val baseRadius: Double = 4.0
    open val baseStorageSlots: Int = 27
    open val baseTickInterval: Long = 20L
    open val limitPerIsland: Int = 5
    open val upgrades: List<BlockUpgrade> = emptyList()

    /** Item que representa o bloco. Com [placed], embute o estado no PDC. */
    open fun buildItem(placed: PlacedBlock? = null): ItemStack

    open fun onPlace(ctx: PlaceContext) {}
    open fun onBreak(ctx: BreakContext) {}
    open fun onRightClick(ctx: BlockClickContext) {}
    open fun tick(placed: PlacedBlock) {}
}
```

`BlockUpgrade` é interface (não enum), para que cada `CustomBlock` declare o seu
próprio conjunto sem um enum global:

```kotlin
interface BlockUpgrade {
    val id: String
    val prettyName: String
    val maxLevel: Int
    val lore: List<String>
    fun costFor(level: Int): Double
    fun statLine(level: Int): String
}
```

`VacuumHopperUpgrade` implementa como enum, espelhando o formato do `MinionUpgrade`
(que já usa membro abstrato `getStatLine` sobrescrito por constante).

### Estado no item

`buildItem(placed)` serializa o estado inteiro no PDC, mesmo esquema de
`Minions.buildMinionItem` / `readPlacedMinionFromItem`:

| Chave (`NamespacedKey("oneblock", ...)`) | Tipo | Conteúdo |
|---|---|---|
| `custom_block_id` | STRING | id do `CustomBlock` (identidade — sempre presente) |
| `custom_block_uuid` | STRING | uuid do `PlacedBlock` |
| `custom_block_upgrades` | STRING | `id:nivel;id:nivel` |
| `custom_block_storage` | STRING | base64 dos stacks, separados por `\n` |
| `custom_block_filter_mode` | STRING | nome do `FilterMode` |
| `custom_block_filter` | STRING | nomes de `Material` separados por `;` |
| `custom_block_enabled` | INTEGER | `0`/`1` |

Delimitadores seguem a justificativa já usada em `Minions`: base64 padrão não
contém `\n`, e nome de `Material` não contém `;`.

Consequência: quebrar e recolocar preserva armazenamento, filtro e upgrades — o
item dropado carrega tudo. É também o que faz o limite por ilha ser justo (mover
um hopper não custa nada além do trabalho).

O item **não** guarda `ownerUuid`; o dono é definido por quem coloca.

## Ciclo de vida

### Colocar — `BlockPlaceEvent`

1. `CustomBlockRegistry.fromItemStack(event.itemInHand)`; `null` → bloco vanilla, retorna sem tocar no evento.
2. `IslandManager.getIslandAt(location)`; sem ilha, ou jogador não é `ownerUuid` nem `isMember(name)` → cancela, avisa.
3. Conta os `PlacedBlock` registrados com o mesmo `blockId` cuja `location`
   resolve para essa mesma ilha (`IslandManager.getIslandAt` de cada um). Varredura
   linear sobre o mapa em memória — a ordem de grandeza é dezenas de blocos, não
   justifica índice por ilha. `>= limitPerIsland` → cancela, avisa.
4. Lê o estado do PDC; se ausente (item novo de craft), cria `PlacedBlock` zerado com `ownerUuid = player.uniqueId`.
5. `PlacedBlockManager.register(placed)` → mapas + persistência imediata.

### Quebrar — `BlockBreakEvent`

1. `PlacedBlockManager.get(SimpleLocation.fromBukkit(block.location))`; `null` → conduit vanilla, ignora.
2. Sem permissão de ilha → cancela, avisa.
3. `event.isDropItems = false` — senão dropa um conduit vanilla junto e duplica material.
4. `unregister` e dropa `buildItem(placed)` no local, com o estado inteiro dentro.

### Interagir — `PlayerInteractEvent`

Right-click no bloco, mão principal, sem shift (shift+click deve continuar
permitindo colocar bloco na face). Checa permissão e abre `VacuumHopperView`.

### Explosão e pistão

`EntityExplodeEvent` / `BlockExplodeEvent`: remove os blocos registrados da lista
`blockList()`, protegendo-os. `BlockPistonExtendEvent` / `BlockPistonRetractEvent`:
cancela se algum bloco movido for registrado.

Sem isso o bloco físico some mas a entrada continua no Mongo — vira um registro
fantasma que ocupa slot do limite por ilha e tenta sugar num lugar vazio para
sempre. É a falha mais provável do sistema, por isso é tratada explicitamente e
não deixada para "depois".

### Startup e validação

`onInit` (agendado com atraso, como `PlacedMinionManager` faz) carrega tudo do
Mongo para os mapas. Bloco real não tem entidade, então **nada** do cleanup pesado
de chunk/ArmorStand que `minions` precisa.

Validação preguiçosa em vez de eager: no primeiro tick de cada bloco em que o
chunk está carregado, confere se o material bate. Se não bate (WorldEdit, rollback
de chunk, restauração de backup), desregistra e loga. Forçar o carregamento de
todos os chunks no boot só para validar seria caro e derrotaria a checagem de
chunk carregado do tick.

## Sucção

Uma task global de 1 tick em `PlacedBlockManager` com contador; cada bloco roda
quando `tickCounter % tickInterval(placed) == 0`. Mesma ideia do `heldTickInterval`
que `CustomItemSystem` já usa para itens em mão — um timer só, não um por bloco.

Por bloco, a cada execução:

1. `enabled` e não cheio; senão pula.
2. Chunk carregado; senão pula. `getNearbyEntities` em chunk descarregado força o carregamento — exatamente o que não se quer num tick de fundo.
3. `getNearbyEntities(centro, r, r, r) { it is Item }`, centro no meio do bloco.
4. Por drop:
   - `pickupDelay >= Short.MAX_VALUE` → pula (marcado para nunca ser coletado).
   - `!placed.accepts(stack.type)` → pula, fica no chão.
   - `addToStorage(listOf(stack))`; sem sobra → `entity.remove()`; com sobra → ajusta `amount` da entity e marca que encheu.
5. Marca sujo para o flush.

## Armazenamento cheio

Para de sugar. Ao encher com `notifiedFull == false`: mensagem + som ao dono se
estiver online, `notifiedFull = true`. Ao abrir espaço (retirada na GUI), volta a
`false`. Um aviso por vez que encher, sem spam por tick.

## Upgrades

Constantes no companion do `VacuumHopper`:

```kotlin
const val BASE_RADIUS = 4.0
const val BASE_STORAGE_SLOTS = 27
const val BASE_TICK_INTERVAL = 20L
const val LIMIT_PER_ISLAND = 5
const val UPGRADE_CURRENCY = "coins"
```

| Upgrade | Base | Níveis | Por nível | Resultado | Custo (coins) |
|---|---|---|---|---|---|
| Raio | 4 | 3 | +2 | 4 / 6 / 8 / 10 | 5.000 / 15.000 / 40.000 |
| Armazenamento | 27 | 3 | +9 | 27 / 36 / 45 / 54 | 7.500 / 20.000 / 50.000 |
| Velocidade | 20t | 3 | −5 | 20 / 15 / 10 / 5 | 10.000 / 25.000 / 60.000 |

`maxLevel = 3` no armazenamento não é arbitrário: `27 + 3×9 = 54`, que é o máximo
de um inventário Bukkit. Subir esse `maxLevel` estoura a GUI em runtime — fica um
comentário marcando isso no enum.

Pagamento via `Currencies` na moeda `coins`; débito e aplicação do nível numa
operação só, persistindo em seguida. Se o saldo não cobre, avisa e não aplica.

## Persistência

`PlacedMinionManager` hoje faz um `replaceOne` por tick, por minion. Com sucção a
cada 20 ticks e vários hoppers por ilha isso multiplica escrita à toa.

Aqui: `PlacedBlockManager` mantém um set de uuids sujos e dá flush em lote a cada
`SAVE_INTERVAL_SECONDS = 10` usando o `updateAll`/`bulkWrite` do repositório.
Flush imediato (fora do lote) em place, break, upgrade e retirada de item — as
operações em que perder estado por crash seria visível para o jogador.

## GUIs (inventory-framework)

- **`VacuumHopperView`** — status: raio atual, `X/Y` slots usados, ligado/desligado. Botões para Armazenamento, Filtro, Upgrades e toggle do `enabled`.
- **`HopperStorageView`** — itens guardados; clique retira para o inventário do jogador. Retirar limpa `notifiedFull`.
- **`HopperFilterView`** — adiciona/remove material, alterna whitelist/blacklist.
- **`HopperUpgradesView`** — nível atual, estrelas, custo, compra. Formato do `UpgradesView` dos minions.

Todas checam permissão de ilha ao abrir.

## Obtenção

**Forge** (`Crafts.register`, formato do `MinionsSystem.setupRecipes`):

| Ingrediente | Qtd |
|---|---|
| Conduit | 1 |
| Barra de Netherite | 4 |
| Peça Mecânica (`Minions.machinePart()`) | 8 |

**Comando:** `/customblock give <id> <player> [qtd]`, com `@CommandPermission` de
admin e completion de `<id>` vinda do `CustomBlockRegistry`. Genérico, não
específico do hopper — ver a seção de estrutura.

## Testes

Kotest está configurado no módulo (`CooldownStoreTest` existe) e **não** há MockBukkit
no catálogo de versões. Logo, só a lógica que não toca em `Bukkit` é testável em
unidade — e é essa a linha divisória usada abaixo.

Unitário (Kotest, sem servidor):

- `accepts()` — filtro vazio, whitelist com e sem match, blacklist com e sem match.
- `storageCapacity()` — nível 0 a 3, confirmando que o topo é exatamente 54.
- `radiusFor(level)` e `tickIntervalFor(level)` — nível 0 a 3.
- `costFor(level)` de cada upgrade, incluindo nível fora da faixa.
- Serialização/desserialização do mapa de upgrades e do set de filtro (o formato
  `id:nivel;id:nivel` e `MATERIAL;MATERIAL`), que é manipulação pura de string.

Fora do alcance do teste unitário, por dependerem de `Bukkit.createInventory`,
`ItemSerializer` e do `PersistentDataContainer`: `addToStorage`, `isStorageFull`,
`storageItems` e o round-trip `PlacedBlock` ↔ item. Ficam em verificação manual.
Introduzir MockBukkit só para isso está fora do escopo desta feature.

Manual, no servidor: colocar/quebrar preservando estado, limite por ilha, permissão
de membro e de não-membro, explosão e pistão não criando registro fantasma,
sucção respeitando filtro, sobra parcial ajustando a entity, aviso de cheio sem
spam, compra de upgrade debitando `coins`.

## Fora de escopo

- Sugar XP.
- Puxar itens de containers adjacentes.
- Empurrar itens para container adjacente / auto-venda.
- Tiers de item (I/II/III) — a progressão é por upgrade in-GUI.
- Teto compartilhado com minions — o limite conta só vacuum hoppers.
- Configuração por YAML — tudo em código, por decisão explícita.
