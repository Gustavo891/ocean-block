# Design — Minion Minerador (Miner)

**Data:** 2026-07-22
**Módulo:** `plugins/oneblock/system/minions`
**Status:** Aprovado para planejamento

## Objetivo

Adicionar um novo tipo de minion, `Miner`, que minera o bloco diretamente
**abaixo** de si em intervalo fixo, guarda os drops num storage interno com
capacidade limitada, e consome energia por ação minerada.

Cenários suportados:

- Minion colocado **acima do one block central da ilha** → minera o one block,
  que avança a fase (gera o próximo bloco do pool), **sem spawnar mob/monster-party**.
- Minion colocado **acima de um gerador de pedra** (ou bloco comum) → quebra o
  bloco, coleta os drops, deixa `AIR`; a regeneração fica por conta da física do
  gerador (água + lava).

Fora de escopo (upgrades futuros): mineração 3x3, intervalo de ação configurável,
storage ilimitado como upgrade.

## Estado atual do sistema

- `MinionType` (enum): `Terminator`, `Farmer`. Cada um define `spawn(location)` e
  `executeAction(placedMinion)`.
- `PlacedMinion(uuid, location, type, energy)` — data class persistida no Mongo via
  `PlacedMinionRepository` (coleção `minions`).
- `PlacedMinionManager` — tick a cada `20L` (1s) chamando `type.executeAction` para
  cada minion registrado. **Hoje nunca checa nem consome energia.**
- `Minions` — helpers de item: `createFuelItem` (COAL com NBT `fuel`), `getFuelAmount`,
  `machinePart`, `createMinionItem`.
- `PlayerListener.onInteractAtEntity` — clicar no minion só manda mensagem; **não
  aplica fuel** (fluxo de energia quebrado hoje).
- `MinionView` — só tem botão "Remover minion".
- Regen do one block: `islands/PlayerListener.onBlockBreak` cancela o evento no bloco
  central e chama `Island.processBlockBreak(player, block)`, que: escolhe próximo
  material do pool, seta `block.type`, `grid++`, avança fase, e (por chance) spawna
  mob / monster-party. Usa `Player` para drops e desgaste de ferramenta.

## Arquitetura

### 1. `MinionType.Miner` (novo)

```kotlin
Miner {
    override fun spawn(location: Location): ArmorStand   // visual de minerador
    override fun executeAction(placedMinion: PlacedMinion) {
        // 1. Se storage cheio → idle (não gasta energia, não minera)
        // 2. Se sem energia → idle
        // 3. Localiza bloco abaixo (location.blockY - 1)
        // 4. Coleta drops:
        //    - se bloco == one block central da ilha → island.processMinionBreak(block)
        //    - senão, se bloco minerável (não AIR/líquido/bedrock) → block.getDrops(); block.type = AIR
        //    - senão → idle
        // 5. Adiciona drops ao storage (merge por material)
        // 6. Consome energyCost do minion
    }
}
```

A lógica de energia e storage mora **dentro** do `executeAction` do Miner (é onde se
sabe se houve mineração de fato). `Terminator`/`Farmer` continuam ignorando energia
— `PlacedMinionManager` **não muda**.

`MinionType` ganha uma propriedade aberta `energyCost: Int` (default 0; `Miner = 1`).

### 2. Refactor `Island` — core de avanço reutilizável

Extrair o núcleo de "avançar o one block" para reuso pelo minion, sem duplicar a
progressão de fase:

```kotlin
/** Avança o one block e RETORNA os drops. Não dropa no chão, não spawna mob,
 *  não usa Player/ferramenta. Usado pela mineração automática do minion. */
fun processMinionBreak(block: Block): List<ItemStack> {
    val phase = getCurrentPhase() ?: return emptyList()
    val drops = block.getDrops().toList()          // sem ferramenta
    val nextMaterial = phase.blockPool.getRandomByWeight() ?: Material.GRASS_BLOCK
    block.type = nextMaterial
    grid++
    if (grid >= phase.blocksToNextPhase) advancePhase( /* ver nota */ )
    markDirty()
    return drops
}
```

Nota sobre `advancePhase`: hoje a assinatura é `advancePhase(player: Player)`, mas o
corpo **não usa** o parâmetro `player` — busca os players via `getIslandOnlinePlayers()`.
Ação: remover o parâmetro (`fun advancePhase()`) e atualizar o único chamador
(`processBlockBreak`). Assim `processMinionBreak` chama `advancePhase()` sem player.
`processBlockBreak(player, block)` existente permanece com o mesmo comportamento
(drops no chão + `handleMobSpawn` + `handleMonsterParty` + desgaste de ferramenta).

Identificação do one block central a partir do minion:
`IslandManager.getIslandAt(blockLocation)` → `island.center.isSameBlock(block.location)`.

### 3. `PlacedMinion` — storage + capacidade

```kotlin
data class PlacedMinion(
    val uuid: UUID,
    val location: SimpleLocation,
    val type: MinionType,
    var energy: Int,
    val storage: MutableList<ItemStack> = mutableListOf(),   // NOVO, persistido no Mongo
) {
    fun isStorageFull(): Boolean = storage.size >= STORAGE_CAPACITY
    fun addDrops(items: List<ItemStack>)   // merge por material até maxStackSize; respeita cap
    // hasEnergy / consumeEnergy já existem
}
```

- `STORAGE_CAPACITY = 15` (slots). Storage cheio → Miner fica idle (não gasta energia).
- Merge: agrupa por `Material` empilhando até `maxStackSize`; excedente ocupa novos
  slots até o cap.
- Persistência: `ItemStack` é serializável pelo driver Mongo POJO? **Verificar na fase
  de planejamento.** Se o codec não suportar `ItemStack` direto, serializar via
  `ItemStack.serializeAsBytes()` / `deserializeBytes()` num campo `List<ByteArray>` ou
  Base64. Decisão de serialização fica para o plano.

### 4. Fuel wiring (`PlayerListener.onInteractAtEntity`)

Clicar (botão direito) no armor stand do minion com um item de fuel na mão:

```kotlin
val item = player.inventory.itemInMainHand
val fuel = Minions.getFuelAmount(item)
if (fuel > 0) {
    placedMinion.addEnergy(fuel)
    item.amount -= 1                          // consome 1 item de fuel
    PlacedMinionManager.register(placedMinion) // persiste energia nova
    player.sendRichMessage("<green>+$fuel de energia ao minion!")
} else {
    // abrir MinionView (config/coleta)
}
```

Sem esse fluxo o Miner nunca liga.

### 5. `MinionView` — coleta de storage

Adicionar botão "Coletar itens":

- Renderiza um baú/ícone mostrando quantos itens há no storage.
- Ao clicar: transfere `storage` para o inventário do jogador (o que couber; resto
  fica), limpa os transferidos, persiste, e re-renderiza.
- Botão "Remover minion" existente permanece.

### 6. Dependências / build

`plugins/oneblock/system/minions/build.gradle.kts`:

```kotlin
compileOnly(projects.plugins.oneblock.system.islands)
```

Sem ciclo: `islands` não referencia `minions`. O plugin já shada ambos os módulos.

## Fluxo de dados (tick do Miner)

```
PlacedMinionManager.tick (1s)
  └─ Miner.executeAction(minion)
       ├─ storage cheio?  → return (idle)
       ├─ sem energia?    → return (idle)
       ├─ bloco abaixo:
       │    ├─ one block central → island.processMinionBreak(block) → drops, avança fase
       │    └─ bloco comum/gen   → block.getDrops(); block.type = AIR → drops
       ├─ drops vazio? → return (idle, sem gastar energia)
       ├─ minion.addDrops(drops)
       └─ minion.consumeEnergy(type.energyCost)
```

## Tratamento de erros

- Bloco abaixo inexistente / mundo não carregado → idle silencioso.
- `getIslandAt` nulo (minion fora de ilha) → trata como bloco comum.
- Falhas em `advancePhase` (recompensa/animação) já são `runCatching` isoladas em
  `Island`; mantém-se.

## Testes

Módulo `minions` hoje não tem `src/test`. `PlacedMinion` (storage/energia) é lógica
pura e testável sem Bukkit:

- `addDrops` respeita `STORAGE_CAPACITY` e faz merge por material.
- `isStorageFull` correto no limite.
- `consumeEnergy` / `hasEnergy` (já existem) — cobrir o gating.

Lógica que toca Bukkit/Island (`executeAction`, `processMinionBreak`) fica em
verificação manual no servidor local (`localDeploy`), dado o acoplamento com o mundo.

## Convenções

- Strings ao jogador em Português (MiniMessage).
- `val` por padrão; imutabilidade onde prático (storage é `MutableList` por necessidade
  de persistência mutável do estado do minion).
