package one_block.system.craft.forge

import org.bukkit.entity.Player

interface RecipeUnlock {

    fun isUnlocked(player: Player): Boolean

    fun requirementLines(player: Player): List<String>
}

object RecipeUnlocks {

    fun all(vararg unlocks: RecipeUnlock): RecipeUnlock = AllOf(unlocks.toList())

    fun any(vararg unlocks: RecipeUnlock): RecipeUnlock = AnyOf(unlocks.toList())

    private class AllOf(private val unlocks: List<RecipeUnlock>) : RecipeUnlock {

        override fun isUnlocked(player: Player) = unlocks.all { it.isUnlocked(player) }

        override fun requirementLines(player: Player) = unlocks.flatMap { it.requirementLines(player) }
    }

    private class AnyOf(private val unlocks: List<RecipeUnlock>) : RecipeUnlock {

        override fun isUnlocked(player: Player) = unlocks.any { it.isUnlocked(player) }

        override fun requirementLines(player: Player) = buildList {
            unlocks.forEachIndexed { index, unlock ->
                if (index > 0) add("<dark_gray>  ou")
                addAll(unlock.requirementLines(player))
            }
        }
    }
}
