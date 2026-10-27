package ocean_block.system.pirate_raid.crew

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import ocean_block.libs.formatter.Mini
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.Illager
import org.bukkit.entity.Pillager
import org.bukkit.entity.Vindicator
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scoreboard.Team
import java.util.UUID

object PirateFactory {

    private const val FOLLOW_RANGE = 48.0
    private const val RED_TEAM = "oceanblock_piratas"
    private const val GOLD_TEAM = "oceanblock_superpirata"

    private val NAMES = mapOf(
        PirateKind.PILLAGER to "<red>Pirata</red>",
        PirateKind.BRUTE to "<red>Pirata Brutamontes</red>",
        PirateKind.SUPER to "<gold><bold>Super Pirata</bold></gold>",
    )

    fun spawn(location: Location, kind: PirateKind, island: UUID, superHealth: Double): Illager {
        val type = if (kind == PirateKind.PILLAGER) Pillager::class.java else Vindicator::class.java
        return location.world.spawn(location, type) { pirate ->
            pirate.customName(Mini.deserialize(NAMES.getValue(kind)).decoration(TextDecoration.ITALIC, false))
            pirate.isCustomNameVisible = true
            pirate.isGlowing = true
            pirate.removeWhenFarAway = false
            pirate.isPersistent = false
            pirate.isPatrolLeader = false
            pirate.isCanJoinRaid = false
            pirate.canPickupItems = false
            pirate.getAttribute(Attribute.FOLLOW_RANGE)?.baseValue = FOLLOW_RANGE
            equip(pirate, kind)
            if (kind == PirateKind.SUPER) {
                pirate.getAttribute(Attribute.MAX_HEALTH)?.baseValue = superHealth
                pirate.health = superHealth
            }
            pirate.persistentDataContainer.set(PirateKeys.ISLAND, PersistentDataType.STRING, island.toString())
            pirate.persistentDataContainer.set(PirateKeys.KIND, PersistentDataType.STRING, kind.name)
            team(if (kind == PirateKind.SUPER) GOLD_TEAM else RED_TEAM).addEntity(pirate)
        }
    }

    fun forget(entity: Entity) {
        listOf(RED_TEAM, GOLD_TEAM).forEach { name ->
            Bukkit.getScoreboardManager().mainScoreboard.getTeam(name)?.removeEntity(entity)
        }
    }

    fun setupTeams() {
        team(RED_TEAM).color(NamedTextColor.RED)
        team(GOLD_TEAM).color(NamedTextColor.GOLD)
    }

    private fun team(name: String): Team {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        return scoreboard.getTeam(name) ?: scoreboard.registerNewTeam(name)
    }

    private fun equip(pirate: Illager, kind: PirateKind) {
        val equipment = pirate.equipment
        val hand = when (kind) {
            PirateKind.PILLAGER -> Material.CROSSBOW
            PirateKind.BRUTE -> Material.IRON_AXE
            PirateKind.SUPER -> Material.DIAMOND_AXE
        }
        equipment.setItemInMainHand(ItemStack(hand))
        if (kind == PirateKind.SUPER) {
            equipment.setHelmet(ItemStack(Material.IRON_HELMET))
            equipment.setChestplate(ItemStack(Material.IRON_CHESTPLATE))
            equipment.setLeggings(ItemStack(Material.IRON_LEGGINGS))
            equipment.setBoots(ItemStack(Material.IRON_BOOTS))
        }
        EquipmentSlot.entries.filter { pirate.canUseEquipmentSlot(it) }.forEach { equipment.setDropChance(it, 0f) }
    }
}
