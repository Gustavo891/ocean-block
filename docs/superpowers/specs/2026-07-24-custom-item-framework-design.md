# Custom Item Framework — Design

**Date:** 2026-07-24
**Status:** Approved (design), pending implementation plan
**Module:** `plugins/oneblock/system/custom-items`

## Goal

Framework genérico para itens custom no oneblock: identidade por id, cosmético
(material, item model, nome, lore) e comportamento (hooks de evento + task opcional)
declarados juntos em código Kotlin. Registry central com API aberta para qualquer
system registrar seus próprios itens. Substitui o padrão atual de escrever um
`Listener` Bukkit à mão por item (ex: `BeaconListener`).

## Decisões (brainstorm)

- **Local/API:** system module próprio + `CustomItemRegistry.register(...)` aberto a outros systems.
- **Comportamento:** hooks no `CustomItem` (dispatcher central roteia por PDC id) + escape hatch (`listeners()` cru) para casos raros.
- **Definição:** tudo em código (classe = propriedades + hooks juntos).
- **Task:** held-tick por player segurando + tasks livres (intervalo + runnable).
- **Built-ins:** comando give, cooldown por item, dados extras por item (PDC). **Sem** proteção de identidade anti-craft/bigorna.

## Arquitetura

### Módulo & wiring

- Novo módulo `plugins/oneblock/system/custom-items`, convention `paper-common` (shadowJar, shaded no jar do oneblock).
- `settings.gradle.kts`: adicionar `":plugins:oneblock:system:custom-items"`.
- `build.gradle.kts` do módulo: `compileOnly` em `lib:item-builder`, `lib:instance`, `lib:formatter`. paper-api/ACF vêm da convention.
- Confirmar loop de dependências do plugin em `plugins/oneblock/plugin/build.gradle.kts`: se `:plugins:oneblock:system:*` é auto-incluído, nada a fazer; senão adicionar `implementation(projects.plugins.oneblock.system.customItems)`.
- `OneBlockPlugin.setupSystems()`: `{ CustomItemSystem.init(this, paperCommandManager) }` como **primeiro** item da lista — o registry, o dispatcher listener, o held-tick task e o command completion precisam existir antes de outros systems chamarem `register(...)` nos seus próprios `init`.
- `OneBlockPlugin.setupCommands()`: `registerCommand(CustomItemCommand())`.

### `CustomItem` (classe abstrata)

```kotlin
abstract class CustomItem {
    abstract val id: String                 // único; gravado no PDC como identidade
    abstract val material: Material
    open val itemModel: NamespacedKey? = null
    open val displayName: String? = null    // MiniMessage
    open val lore: List<String> = emptyList()
    open val cooldownMillis: Long = 0L
    open val heldTickInterval: Long = 5L    // ticks entre chamadas de onHeldTick p/ este item

    // Base grava id + itemModel + nome + lore. Override para adicionar NBT extra
    // (dados por item) via nbt{} do item-builder, chamando super.build() primeiro.
    open fun build(amount: Int = 1): ItemStack

    // Hooks de comportamento (default no-op). Dispatcher chama o do item resolvido pelo PDC.
    open fun onRightClick(ctx: InteractContext) {}
    open fun onLeftClick(ctx: InteractContext) {}
    open fun onInteractEntity(ctx: EntityContext) {}
    open fun onConsume(ctx: ConsumeContext) {}
    open fun onHeldTick(player: Player, item: ItemStack) {}

    // Escape hatch: listeners crus que o framework registra em register().
    open fun listeners(): List<Listener> = emptyList()
    // Tasks livres agendadas em register(), canceladas no disable do plugin.
    open fun tasks(): List<CustomItemTask> = emptyList()
}
```

`build()` base (pseudo):
```kotlin
open fun build(amount: Int) = itemStack(material) {
    this.amount = amount
    meta {
        itemModel?.let { this.itemModel = it }
        displayName?.let { name(it) }
        if (lore.isNotEmpty()) setLore { lore.forEach { +it } }
        nbt { string(CustomItemKeys.ID, id) }
    }
}
```

### Contexts

Wrappers passados aos hooks, com utilitários centralizados:

```kotlin
class InteractContext(
    val player: Player,
    val item: ItemStack,
    val customItem: CustomItem,
    val clickedBlock: Block?,
    val hand: EquipmentSlot,          // mão que disparou (HAND ou OFF_HAND)
    private val event: Cancellable,
) {
    fun cancel() { event.isCancelled = true }
    fun markCooldown() = CooldownStore.stamp(player.uniqueId, customItem.id, customItem.cooldownMillis)
    fun consumeOne() { item.amount -= 1 }
    fun setItem(stack: ItemStack)      // grava stack de volta no slot da mão `hand`
}
```

`EntityContext` = análogo com `target: Entity` (right-click em entidade).
`ConsumeContext` = análogo com o `PlayerItemConsumeEvent`.

### `CustomItemRegistry` (API aberta)

```kotlin
object CustomItemRegistry {
    private val items = mutableMapOf<String, CustomItem>()

    fun register(item: CustomItem)              // idempotente por id; loga e ignora id duplicado.
                                                // Registra item.listeners() no pluginManager e agenda item.tasks().
    fun get(id: String): CustomItem?
    fun all(): Collection<CustomItem>
    fun ids(): Set<String>
    fun fromItemStack(item: ItemStack?): CustomItem?   // lê PDC id via CustomItemKeys.ID; null se não for custom
}
```

`CustomItemKeys.ID = NamespacedKey("oneblock", "custom_item_id")` (PersistentDataType.STRING).

### Dispatcher — `CustomItemListener`

Um único `Listener` central roteando por PDC id:

- `PlayerInteractEvent` (RIGHT/LEFT, air/block) → `onRightClick` / `onLeftClick`; usa `event.hand`/`event.item` (a mão que clicou — funciona em main **e** off-hand); `ctx.clickedBlock` nullable (block quando `*_CLICK_BLOCK`). `PlayerInteractEvent` pode disparar 1x por mão; cada disparo resolve o item daquela mão independentemente.
- `PlayerInteractEntityEvent` → `onInteractEntity` (usa `event.hand`).
- `PlayerItemConsumeEvent` → `onConsume`.

Fluxo por evento ativo:
1. Resolver o item da mão do evento (`event.item`); `registry.fromItemStack(item)` → se null, ignora.
2. Se `cooldownMillis > 0` e `!CooldownStore.isReady(uuid, id)` → mensagem `<red>Aguarde <Xs>` + som de negação, `return` (não chama hook).
3. Chama o hook. O hook decide o que fazer e chama `ctx.markCooldown()` no sucesso.

Prioridade e cancelamento: hooks decidem cancelar via `ctx.cancel()`.

### `CooldownStore`

```kotlin
object CooldownStore {
    private val map = ConcurrentHashMap<UUID, MutableMap<String, Long>>()  // expiry epoch millis
    fun isReady(uuid: UUID, id: String): Boolean
    fun remaining(uuid: UUID, id: String): Long      // millis até liberar; 0 se pronto
    fun stamp(uuid: UUID, id: String, durationMillis: Long)
}
```

Puro Kotlin (sem Bukkit) → unit-testável.

### Tasks

```kotlin
data class CustomItemTask(
    val intervalTicks: Long,
    val async: Boolean = false,
    val run: () -> Unit,
)
```

- **Held-tick global:** 1 task repetindo a cada **1 tick** com um contador global (`Long`). Varre `Bukkit.getOnlinePlayers()` e, para cada player, **as duas mãos** (main + off-hand); para cada item custom resolvido, se `contador % customItem.heldTickInterval == 0L`, chama `onHeldTick(player, item)`. Se o mesmo id estiver nas duas mãos, chama uma vez por mão. Cadência decidida por item via `heldTickInterval` (default 5) — sem precisar de task própria. Iniciada no `CustomItemSystem.init`.
- **Tasks livres:** agendadas em `register()` via scheduler (sync/async conforme `async`). Bukkit cancela automaticamente tasks e listeners do plugin no disable — sem bookkeeping manual necessário.

### Comando — `CustomItemCommand`

```kotlin
@CommandAlias("customitem|citem")
@CommandPermission("screen.dev")
class CustomItemCommand : BaseCommand() {
    @Subcommand("give")
    @CommandCompletion("@customitems @players @range:1-64")
    fun give(sender: Player, id: String, @Optional target: OnlinePlayer?, @Optional @Default("1") amount: Int)

    @Subcommand("list")
    fun list(sender: CommandSender)
}
```

`CustomItemSystem.init` registra a completion `@customitems` → `registry.ids()`, via `paperCommandManager.commandCompletions.registerCompletion("customitems") { registry.ids() }`.

`give`: valida id (erro `<red>Item desconhecido` se ausente), `target ?: sender`, entrega `registry.get(id)!!.build(amount)` no inventário; overflow dropa no chão.

### `CustomItemSystem`

```kotlin
object CustomItemSystem {
    lateinit var plugin: JavaPlugin

    fun init(plugin: JavaPlugin, commandManager: PaperCommandManager) {
        this.plugin = plugin
        registerCompletions(commandManager)
        plugin.server.pluginManager.registerEvents(CustomItemListener(), plugin)
        startHeldTickTask()
        registerBuiltInItems()   // MagnetItem
    }
}
```

### Primeiro item — `MagnetItem` (Ímã)

Registrado em `registerBuiltInItems`. Puxa itens dropados num raio de 5 blocos
enquanto segurado (main ou off-hand); right-click liga/desliga.

- `id = "magnet"`, `material` = a definir (ex: `IRON_INGOT`), `itemModel` opcional, `displayName = "<gray>Ímã"`.
- `heldTickInterval` ~5 ticks. Sem cooldown no toggle (ou pequeno anti-spam).
- Estado on/off no PDC: `NamespacedKey("oneblock","magnet_active")` (boolean, default `true` no `build()`).

`build()`: `super.build()` + grava `magnet_active = true` + lore mostrando estado.

`onHeldTick(player, item)`:
```kotlin
if (!item.getNbtBoolean(ACTIVE_KEY, true)) return
val loc = player.location
player.world.getNearbyEntities(loc, 5.0, 5.0, 5.0) { it is Item }
    .forEach { entity ->
        val drop = entity as Item
        if (drop.pickupDelay == Short.MAX_VALUE.toInt()) return@forEach   // item "travado"
        val dir = loc.toVector().add(Vector(0.0, 0.5, 0.0)).subtract(drop.location.toVector())
        if (dir.lengthSquared() < 0.6) return@forEach                     // já em cima
        drop.velocity = dir.normalize().multiply(0.45)
    }
```
(raio 5 constante; puxa por velocity em direção ao player.)

`onRightClick(ctx)`: alterna o estado, regrava o item na mão, feedback:
```kotlin
val active = !ctx.item.getNbtBoolean(ACTIVE_KEY, true)
val updated = ctx.item.clone().apply {
    meta { nbt { boolean(ACTIVE_KEY, active) }; setLore { /* estado atual */ } }
}
ctx.setItem(updated)
ctx.player.sendRichMessage(if (active) "<green>Ímã ativado" else "<red>Ímã desativado")
ctx.player.playSound(ctx.player, if (active) Sound.BLOCK_NOTE_BLOCK_PLING else Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
ctx.cancel()
```

`onLeftClick`: idem toggle (opcional — o pedido é right-click pra toggle; left-click **segurar** já puxa via held-tick nas duas mãos). Manter só right-click no toggle.

### Mudança na lib `item-builder`

`NbtBuilder` hoje só tem `string/int/long/double`. Adicionar:
```kotlin
fun boolean(namespace: String, key: String, value: Boolean) =
    container.set(NamespacedKey(namespace, key), PersistentDataType.BOOLEAN, value)
fun boolean(key: NamespacedKey, value: Boolean) =
    container.set(key, PersistentDataType.BOOLEAN, value)
```
(pareia com o `getNbtBoolean` já existente em `ItemStackExt`.)

## Fluxo de dados

```
Sistema X.init() ──register(item)──> CustomItemRegistry ──registra──> item.listeners(), item.tasks()
                                                     │
build() grava PDC id ─── ItemStack no mundo/inventário
                                                     │
Evento Bukkit ──> CustomItemListener ──fromItemStack(id)──> CustomItem ──hook(ctx)──> lógica do item
                                       │
                                CooldownStore (gate + stamp)

held-tick global (1t + contador) ──> onHeldTick(player, item) quando contador % heldTickInterval == 0
```

## Tratamento de erro

- `register` com id duplicado: loga warning e ignora (não sobrescreve).
- `fromItemStack` com item null/sem PDC/id não-registrado: retorna null (dispatcher ignora silenciosamente).
- `give` com id inválido: mensagem de erro ao sender, sem exceção.
- Hooks lançando exceção: dispatcher envolve em try/catch, loga com o id do item, não propaga pro Bukkit.

## Testes

Módulo não depende de `lib:connection` → compila/testa em modo produção sem o `com.redescreen:sdk`. Testes em `src/test/kotlin`:

- `CooldownStore`: `isReady` antes/depois de `stamp`, `remaining` decrescente, expiração.
- `CustomItemRegistry`: `register`/`get`/`ids`, rejeição de id duplicado. (`fromItemStack`/`build` dependem de Bukkit → fora do escopo unit; sem MockBukkit no projeto.)

## Fora de escopo

- Proteção de identidade (anti-craft/bigorna/stack indevido).
- Persistência de estado por-instância no mundo (ex: SellBeacon com Mongo). Itens que precisam disso usam o escape hatch (`listeners()` + repo próprio).
- Migração dos itens existentes (SellBeacon, MultiTool) para o framework — pode ser trabalho futuro separado.
- (resolvido) Intervalo de held-tick por item: agora suportado via `heldTickInterval`.
