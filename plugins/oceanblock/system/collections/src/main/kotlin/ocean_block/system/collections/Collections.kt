package ocean_block.system.collections

import ocean_block.system.collections.collection.CollectionData
import ocean_block.system.collections.collection.CollectionEntry
import ocean_block.system.collections.collection.CollectionManager
import ocean_block.system.collections.event.CollectionTierUnlockEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.UUID

object Collections {

    fun data(uuid: UUID): CollectionData? = CollectionManager.get(uuid)

    fun amount(uuid: UUID, entry: CollectionEntry): Long = data(uuid)?.amountOf(entry) ?: 0L

    fun tier(uuid: UUID, entry: CollectionEntry): Int = data(uuid)?.tierOf(entry) ?: 0

    fun hasTier(uuid: UUID, entry: CollectionEntry, tier: Int): Boolean = tier(uuid, entry) >= tier

    fun add(uuid: UUID, material: Material, amount: Long) {
        val entry = CollectionEntry.byMaterial(material) ?: return
        add(uuid, entry, amount)
    }

    fun add(uuid: UUID, entry: CollectionEntry, amount: Long) {
        val data = data(uuid) ?: return
        val gained = data.add(entry, amount)
        CollectionManager.markDirty(uuid)

        if (gained <= 0) return

        val player = Bukkit.getPlayer(uuid) ?: return
        val tier = data.tierOf(entry)

        announceTier(player, entry, tier)
        Bukkit.getPluginManager().callEvent(CollectionTierUnlockEvent(player, entry, tier))
    }

    fun set(uuid: UUID, entry: CollectionEntry, amount: Long) {
        val data = data(uuid) ?: return
        data.set(entry, amount)
        CollectionManager.markDirty(uuid)
    }

    private fun announceTier(player: Player, entry: CollectionEntry, tier: Int) {
        player.sendRichMessage("<green>Coleção de <white>${entry.prettyName}<green> alcançou o tier <white>$tier<green>!")
    }
}
