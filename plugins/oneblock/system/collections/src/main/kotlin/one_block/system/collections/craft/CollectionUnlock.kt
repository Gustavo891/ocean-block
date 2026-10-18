package one_block.system.collections.craft

import one_block.system.collections.Collections
import one_block.system.collections.collection.CollectionEntry
import one_block.system.craft.forge.RecipeUnlock
import org.bukkit.entity.Player

data class CollectionUnlock(
    val entry: CollectionEntry,
    val tier: Int,
) : RecipeUnlock {

    override fun isUnlocked(player: Player): Boolean = Collections.hasTier(player.uniqueId, entry, tier)

    override fun requirementLines(player: Player): List<String> {
        val current = Collections.tier(player.uniqueId, entry)

        return if (current >= tier) {
            listOf("<dark_gray>▪ <green>✔ Coleção de ${entry.prettyName} tier $tier")
        } else {
            listOf("<dark_gray>▪ <red>✖ Coleção de ${entry.prettyName} tier $tier <dark_gray>($current)")
        }
    }
}
