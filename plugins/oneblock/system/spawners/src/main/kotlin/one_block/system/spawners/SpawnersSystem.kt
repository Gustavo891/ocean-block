package one_block.system.spawners

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.Instance
import org.bukkit.plugin.java.JavaPlugin
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.nbt
import one_block.libs.item_builder.setLore
import one_block.system.islands.reset.IslandResetHooks
import one_block.system.spawners.commands.SpawnerCommands
import one_block.system.spawners.reset.SpawnerResetHook
import one_block.system.spawners.listeners.SpawnerBlockListener
import one_block.system.spawners.listeners.SpawnerDeathListener
import one_block.system.spawners.manager.PlacedSpawnerManager
import one_block.system.spawners.manager.SpawnerEntity
import one_block.system.spawners.models.SpawnerType
import one_block.system.spawners.views.SpawnerShopView
import one_block.system.spawners.views.SpawnerView
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object SpawnersSystem {

    val TYPE_ID = NamespacedKey("oneblock", "spawner_type_id")
    val OWNER_SPAWNER = NamespacedKey("oneblock", "spawner_owner")
    private const val CLEANUP_DELAY_TICKS = 80L

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame, commandManager: PaperCommandManager) {
        registerCompletions(commandManager)

        plugin.server.pluginManager.registerEvents(SpawnerBlockListener(viewFrame), plugin)
        plugin.server.pluginManager.registerEvents(SpawnerDeathListener(), plugin)

        viewFrame.with(SpawnerView(), SpawnerShopView())
        commandManager.registerCommand(SpawnerCommands(viewFrame))

        PlacedSpawnerManager.onInit()
        scheduleOrphanCleanup(plugin)

        IslandResetHooks.register(SpawnerResetHook)
    }

    fun build(type: SpawnerType, amount: Int = 1): ItemStack = itemStack(Material.SPAWNER) {
        this.amount = amount.coerceAtLeast(1)
        meta<ItemMeta> {
            name("<yellow>Gerador de Monstros")
            setLore {
                +"<gray>Tipo: <white>${type.name}"
            }
            nbt { string(TYPE_ID, type.name) }
        }
    }

    fun typeOf(item: ItemStack?): SpawnerType? {
        if (item == null || item.type != Material.SPAWNER) return null
        val meta = item.itemMeta ?: return null
        val id = meta.persistentDataContainer.get(TYPE_ID, PersistentDataType.STRING) ?: return null
        return SpawnerType.find(id)
    }

    private fun registerCompletions(commandManager: PaperCommandManager) {
        commandManager.commandCompletions.registerCompletion("spawnertypes") {
            SpawnerType.entries.map { it.name }
        }
    }

    private fun scheduleOrphanCleanup(plugin: JavaPlugin) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            var removed = 0
            Bukkit.getWorlds().forEach { world ->
                world.entities.forEach { entity: Entity ->
                    val owner = SpawnerEntity.ownerOf(entity) ?: return@forEach
                    if (PlacedSpawnerManager.get(owner) == null) {
                        entity.remove()
                        removed++
                    }
                }
            }
            if (removed > 0) {
                Instance.logger.info("[spawners] $removed entidade(s) órfã(s) removida(s).")
            }
        }, CLEANUP_DELAY_TICKS)
    }

    fun onDisable() {
        PlacedSpawnerManager.onDisable()
    }

}
