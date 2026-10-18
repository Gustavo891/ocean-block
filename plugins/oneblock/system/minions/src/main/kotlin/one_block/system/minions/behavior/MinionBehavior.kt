package one_block.system.minions.behavior

import one_block.system.minions.placed_minion.PlacedMinion
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

interface MinionBehavior {

    fun spawn(placedMinion: PlacedMinion, location: Location)

    fun tick(placedMinion: PlacedMinion): Boolean

    fun placement(clickedBlock: Block, face: BlockFace): MinionPlacement? =
        MinionPlacement(clickedBlock.location.clone().add(0.5, 1.0, 0.5), null)

    fun spawnDisplay(placedMinion: PlacedMinion, armorStand: ArmorStand): TextDisplay? = null

    fun updateDisplay(placedMinion: PlacedMinion)

    fun onUnregister(uuid: UUID) {}

    fun storageFullWarning(placedMinion: PlacedMinion): String =
        if (placedMinion.isPaused()) "<red>📦 Armazém cheio"
        else "<gold>📦 Armazém cheio"

    fun spawnArmorStand(location: Location, block: ArmorStand.() -> Unit): ArmorStand {
        return location.world.spawn(location, ArmorStand::class.java).apply {
            isInvulnerable = true
            setGravity(false)
            isPersistent = true
            removeWhenFarAway = false
            isMarker = false
            isSmall = true
            setArms(true)
            block(this)
        }
    }

    fun spawnDisplay(location: Location, block: TextDisplay.() -> Unit): TextDisplay {
        return location.world.spawn(location, TextDisplay::class.java).apply {
            isInvulnerable = true
            persistentDataContainer.set(
                NamespacedKey("oneblock", "minion"),
                PersistentDataType.BOOLEAN,
                true
            )
            isPersistent = true
            backgroundColor = Color.fromARGB(0, 0, 0, 0)
            billboard = Billboard.CENTER
            isShadowed = true
            block(this)
        }
    }

}
