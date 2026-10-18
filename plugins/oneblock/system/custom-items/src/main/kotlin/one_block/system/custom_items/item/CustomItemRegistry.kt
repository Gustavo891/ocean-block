package one_block.system.custom_items.item

import one_block.libs.instance.Instance
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object CustomItemRegistry {

    private val items = mutableMapOf<String, CustomItem>()

    fun register(item: CustomItem) {
        if (items.containsKey(item.id)) {
            Instance.logger.warning("[custom-items] id duplicado ignorado: ${item.id}")
            return
        }
        items[item.id] = item

        item.listeners().forEach { listener ->
            Instance.server.pluginManager.registerEvents(listener, Instance)
        }
        item.tasks().forEach { task ->
            val runnable = Runnable {
                runCatching { task.run() }
                    .onFailure { Instance.logger.warning("[custom-items] task de '${item.id}' falhou: ${it.message}") }
            }
            if (task.async) {
                Instance.server.scheduler.runTaskTimerAsynchronously(Instance, runnable, task.intervalTicks, task.intervalTicks)
            } else {
                Instance.server.scheduler.runTaskTimer(Instance, runnable, task.intervalTicks, task.intervalTicks)
            }
        }
    }

    fun get(id: String): CustomItem? = items[id]

    fun all(): Collection<CustomItem> = items.values

    fun ids(): Set<String> = items.keys.toSet()

    fun fromItemStack(item: ItemStack?): CustomItem? {
        if (item == null || item.type.isAir) return null
        val meta = item.itemMeta ?: return null
        val id = meta.persistentDataContainer.get(CustomItemKeys.ID, PersistentDataType.STRING) ?: return null
        return items[id]
    }
}
