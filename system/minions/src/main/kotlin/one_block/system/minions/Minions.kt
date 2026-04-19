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
import javax.xml.stream.events.Namespace

object Minions {

    private val FUEL_KEY = NamespacedKey("minions", "fuel")
    private val MINION_TYPE_KEY = NamespacedKey("minions", "minion_type")
    private val MINION_UUID_KEY = NamespacedKey("minions", "minion_uuid")
    private val MINION_ENERGY_KEY = NamespacedKey("minions", "minion_energy")
    private val MINION_UPGRADES_KEY = NamespacedKey("minions", "minion_upgrades")
    private val MINION_TOOL_KEY = NamespacedKey("minions", "minion_tool")
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

        val toolBase64 = container.get(MINION_TOOL_KEY, PersistentDataType.STRING)

        return PlacedMinion(uuid, location, type, energy, upgrades, toolBase64 = toolBase64)
    }

    fun machinePart(): ItemStack {
        val item = ItemStack(Material.OAK_BUTTON)
        val meta = item.itemMeta!!

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

    fun isMachinePart(item: ItemStack?): Boolean {
        if (item == null) return false
        val meta = item.itemMeta ?: return false

        return meta.persistentDataContainer.has(
           MACHINE_PART_KEY,
            PersistentDataType.BOOLEAN
        )
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

    fun createMinionItem(type: MinionType) = buildMinionItem(type, 0, emptyMap(), null)

    fun createMinionItem(placedMinion: PlacedMinion) =
        buildMinionItem(placedMinion.type, placedMinion.energy, placedMinion.upgrades, placedMinion.uuid, placedMinion.toolBase64)

    private fun buildMinionItem(type: MinionType, energy: Int, upgrades: Map<String, Int>, uuid: UUID?, toolBase64: String? = null) =
        itemStack(Material.PLAYER_HEAD) {
            meta<SkullMeta> {
                val percent = ((energy.toDouble() / PlacedMinion.MAX_ENERGY) * 100).toInt()

                tooltipStyle = NamespacedKey("oneblock", "minion")
                name("          <white>${type.prettyName}")

                setLore {
                    +" <yellow><glyph:miner>  <white><glyph:legendary>"
                    +""
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
                    if (uuid != null) {
                        string(MINION_UUID_KEY, uuid.toString())
                        int(MINION_ENERGY_KEY, energy)
                        val upgradesEncoded = serializeUpgrades(upgrades)
                        if (upgradesEncoded.isNotEmpty()) string(MINION_UPGRADES_KEY, upgradesEncoded)
                        if (toolBase64 != null) string(MINION_TOOL_KEY, toolBase64)
                    }
                }

                playerProfile = headProfile(type)
            }
        }

    private fun headProfile(type: MinionType) = Bukkit.createProfile(UUID.randomUUID(), "1").apply {
        setProperty(ProfileProperty("textures", type.headValue))
    }

}