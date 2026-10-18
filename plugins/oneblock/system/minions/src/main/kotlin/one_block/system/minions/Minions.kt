package one_block.system.minions

import com.destroystokyo.paper.profile.ProfileProperty
import one_block.libs.coordinate.SimpleLocation
import one_block.libs.formatter.NumberFormatter
import one_block.libs.instance.Instance
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.nbt
import one_block.libs.item_builder.setLore
import one_block.system.minions.enums.FilterMode
import one_block.system.minions.enums.FullMode
import one_block.system.minions.enums.MinionType
import one_block.system.minions.enums.MinionUpgrade
import one_block.system.minions.placed_minion.PlacedMinion
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object Minions {

    private val FUEL_KEY = NamespacedKey("minions", "fuel")
    private val MINION_TYPE_KEY = NamespacedKey("minions", "minion_type")
    private val MINION_UUID_KEY = NamespacedKey("minions", "minion_uuid")
    private val MINION_ENERGY_KEY = NamespacedKey("minions", "minion_energy")
    private val MINION_UPGRADES_KEY = NamespacedKey("minions", "minion_upgrades")
    private val MINION_ENABLED_KEY = NamespacedKey("minions", "minion_enabled")
    private val MINION_STORAGE_KEY = NamespacedKey("minions", "minion_storage")
    private val MINION_XP_KEY = NamespacedKey("minions", "minion_xp")
    private val MINION_OWNER_KEY = NamespacedKey("minions", "minion_owner")
    private val MINION_PRODUCED_KEY = NamespacedKey("minions", "minion_produced")
    private val MINION_ENERGY_SPENT_KEY = NamespacedKey("minions", "minion_energy_spent")
    private val MINION_PLACED_AT_KEY = NamespacedKey("minions", "minion_placed_at")
    private val MINION_FULL_MODE_KEY = NamespacedKey("minions", "minion_full_mode")
    private val MINION_FILTER_MODE_KEY = NamespacedKey("minions", "minion_filter_mode")
    private val MINION_FILTER_KEY = NamespacedKey("minions", "minion_filter")

    private const val STORAGE_DELIMITER = "\n"
    private const val FILTER_DELIMITER = ";"
    private val MACHINE_PART_KEY by lazy {
        NamespacedKey(Instance, "machine_part")
    }
    private val MINION_ID_KEY by lazy {
        NamespacedKey(Instance, "minion_id")
    }

    fun createFuelItem(amount: Int) = itemStack(Material.PAPER) {
        meta<ItemMeta> {
            name("<yellow>Energia")
            setLore {
                +"<white>Valor: <gray>${NumberFormatter.format(amount.toDouble())}"
            }
            itemModel = NamespacedKey("oneblock", "energy")
            nbt {
                int(FUEL_KEY, amount)
            }
        }
    }

    fun getFuelAmount(item: ItemStack): Int {
        return item.itemMeta?.persistentDataContainer?.get(FUEL_KEY, PersistentDataType.INTEGER) ?: 0
    }

    fun getMinionType(item: ItemStack): MinionType? {
        val typeName = item.itemMeta?.persistentDataContainer?.get(MINION_TYPE_KEY, PersistentDataType.STRING) ?: return null
        return runCatching { MinionType.valueOf(typeName) }.getOrNull()
    }

    fun readPlacedMinionFromItem(item: ItemStack, location: SimpleLocation): PlacedMinion? {
        val meta = item.itemMeta ?: return null
        val container = meta.persistentDataContainer

        val typeName = container.get(MINION_TYPE_KEY, PersistentDataType.STRING) ?: return null
        val type = runCatching { MinionType.valueOf(typeName) }.getOrNull() ?: return null

        val uuidString = container.get(MINION_UUID_KEY, PersistentDataType.STRING)
        val uuid = uuidString?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: UUID.randomUUID()

        val energy = container.get(MINION_ENERGY_KEY, PersistentDataType.INTEGER) ?: 0

        val upgradesEncoded = container.get(MINION_UPGRADES_KEY, PersistentDataType.STRING)
        val upgrades = upgradesEncoded?.let { deserializeUpgrades(it) } ?: mutableMapOf()


        val enabled = (container.get(MINION_ENABLED_KEY, PersistentDataType.INTEGER) ?: 1) != 0

        val storage = container.get(MINION_STORAGE_KEY, PersistentDataType.STRING)
            ?.split(STORAGE_DELIMITER)
            ?.filter { it.isNotBlank() }
            ?.toMutableList()
            ?: mutableListOf()

        val fullMode = container.get(MINION_FULL_MODE_KEY, PersistentDataType.STRING)
            ?.let { runCatching { FullMode.valueOf(it) }.getOrNull() }
            ?: FullMode.STOP

        val filterMode = container.get(MINION_FILTER_MODE_KEY, PersistentDataType.STRING)
            ?.let { runCatching { FilterMode.valueOf(it) }.getOrNull() }
            ?: FilterMode.BLACKLIST

        val filter = container.get(MINION_FILTER_KEY, PersistentDataType.STRING)
            ?.split(FILTER_DELIMITER)
            ?.filter { it.isNotBlank() }
            ?.toMutableSet()
            ?: mutableSetOf()

        val xp = container.get(MINION_XP_KEY, PersistentDataType.INTEGER)
        val owner = container.get(MINION_OWNER_KEY, PersistentDataType.STRING)
        val produced = container.get(MINION_PRODUCED_KEY, PersistentDataType.INTEGER)
        val energySpent = container.get(MINION_ENERGY_SPENT_KEY, PersistentDataType.INTEGER)
        val placedAt = container.get(MINION_PLACED_AT_KEY, PersistentDataType.LONG)

        return PlacedMinion(
            uuid = uuid,
            location = location,
            type = type,
            energy = energy,
            upgrades = upgrades,
            enabled = enabled,
            storage = storage,
            fullMode = fullMode,
            filterMode = filterMode,
            filter = filter,
            xp = xp,
            owner = owner,
            produced = produced,
            energySpent = energySpent,
            placedAt = placedAt,
        )
    }

    fun machinePart(): ItemStack {
        val item = ItemStack(Material.OAK_BUTTON)
        val meta = item.itemMeta ?: return item

        meta.setDisplayName("§ePeça Mecânica")

        meta.lore = listOf(
            "§7Fragmento usado para",
            "§7construir Minions."
        )

        meta.persistentDataContainer.set(
            MACHINE_PART_KEY,
            PersistentDataType.BOOLEAN,
            true
        )

        item.itemMeta = meta
        return item
    }

    private fun serializeUpgrades(upgrades: Map<String, Int>): String {
        if (upgrades.isEmpty()) return ""

        return upgrades.entries.joinToString(";") { (key, value) -> "$key:$value" }
    }

    private fun deserializeUpgrades(data: String): MutableMap<String, Int> {
        if (data.isBlank()) return mutableMapOf()

        val result = mutableMapOf<String, Int>()

        data.split(';').forEach { entry ->
            if (entry.isBlank()) return@forEach

            val parts = entry.split(':')
            if (parts.size != 2) return@forEach

            val key = parts[0]
            val level = parts[1].toIntOrNull() ?: return@forEach
            result[key] = level
        }

        return result
    }

    fun markAsMinion(armorStand: ArmorStand, id: UUID) {
        armorStand.persistentDataContainer.set(
            MINION_ID_KEY,
            PersistentDataType.STRING,
            id.toString()
        )
    }

    fun getMinionId(entity: Entity): UUID? {
        val raw = entity.persistentDataContainer.get(MINION_ID_KEY, PersistentDataType.STRING)
            ?: return null

        return runCatching { UUID.fromString(raw) }.getOrNull()
    }

    fun createMinionItem(type: MinionType) = buildMinionItem(type, null)

    fun createMinionItem(placedMinion: PlacedMinion) = buildMinionItem(placedMinion.type, placedMinion)

    private fun buildMinionItem(type: MinionType, placed: PlacedMinion?) =
        itemStack(Material.PLAYER_HEAD) {
            meta<SkullMeta> {
                val energy = placed?.energy ?: 0
                val upgrades = placed?.upgrades ?: emptyMap()
                val maxEnergy = placed?.getMaxEnergy() ?: PlacedMinion.MAX_ENERGY
                val percent = if (maxEnergy > 0) ((energy.toDouble() / maxEnergy) * 100).toInt() else 0

                placed?.sync()

                name("<white>${type.prettyName}")

                setLore {
                    type.desc.forEach { +"<gray>$it</gray>" }
                    +""
                    +"<dark_gray>▸ <white>Energia: <aqua>$percent%"
                    MinionUpgrade.valuesFor(type).forEach { upgrade ->
                        val level = upgrades[upgrade.id] ?: 0
                        if (level > 0) {
                            +"<dark_gray>▸ <white>${upgrade.prettyName}: ${upgrade.getStatLine(level, upgrade.maxLevel)}"
                        }
                    }
                    +""
                    +"<dark_gray>Clique em um bloco para utiliza-lo."
                }

                nbt {
                    string(MINION_TYPE_KEY, type.name)

                    if (placed != null) {
                        string(MINION_UUID_KEY, placed.uuid.toString())
                        int(MINION_ENERGY_KEY, placed.energy)
                        if (placed.xpAmount > 0) int(MINION_XP_KEY, placed.xpAmount)
                        placed.owner?.let { string(MINION_OWNER_KEY, it) }
                        if (placed.producedCount > 0) int(MINION_PRODUCED_KEY, placed.producedCount)
                        if (placed.energySpentTotal > 0) int(MINION_ENERGY_SPENT_KEY, placed.energySpentTotal)
                        placed.placedAt?.let { long(MINION_PLACED_AT_KEY, it) }
                        int(MINION_ENABLED_KEY, if (placed.enabled) 1 else 0)
                        string(MINION_FULL_MODE_KEY, placed.fullMode.name)
                        string(MINION_FILTER_MODE_KEY, placed.filterMode.name)

                        val upgradesEncoded = serializeUpgrades(placed.upgrades)
                        if (upgradesEncoded.isNotEmpty()) string(MINION_UPGRADES_KEY, upgradesEncoded)

                        if (placed.filter.isNotEmpty()) {
                            string(MINION_FILTER_KEY, placed.filter.joinToString(FILTER_DELIMITER))
                        }
                        if (placed.storage.isNotEmpty()) {
                            string(MINION_STORAGE_KEY, placed.storage.joinToString(STORAGE_DELIMITER))
                        }
                    }
                }

                playerProfile = headProfile(type)
                type.itemModel?.let { model -> itemModel = model }
            }
        }

    private fun headProfile(type: MinionType) = Bukkit.createProfile(UUID.randomUUID(), "1").apply {
        setProperty(ProfileProperty("textures", type.headValue))
    }

}
