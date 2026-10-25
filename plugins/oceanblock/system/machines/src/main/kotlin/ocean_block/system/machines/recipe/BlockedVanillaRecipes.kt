package ocean_block.system.machines.recipe

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey

object BlockedVanillaRecipes {

    private val KEYS = listOf("blast_furnace", "daylight_detector")

    fun remove(): List<String> = KEYS.filter { Bukkit.removeRecipe(NamespacedKey.minecraft(it)) }
}
