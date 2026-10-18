package one_block.system.scoreboard.config

import me.clip.placeholderapi.PlaceholderAPI
import one_block.libs.instance.Instance
import one_block.system.spawn.spawn.SpawnManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player

sealed interface BoardCondition {

    fun test(player: Player): Boolean

    data object AtSpawn : BoardCondition {
        override fun test(player: Player): Boolean = SpawnManager.isAtSpawn(player)
    }

    data class InWorld(val world: String) : BoardCondition {
        override fun test(player: Player): Boolean = player.world.name.equals(world, ignoreCase = true)
    }

    data class HasPermission(val node: String) : BoardCondition {
        override fun test(player: Player): Boolean = player.hasPermission(node)
    }

    data class PlaceholderEquals(val placeholder: String, val expected: String) : BoardCondition {
        override fun test(player: Player): Boolean {
            if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return false
            return PlaceholderAPI.setPlaceholders(player, placeholder).equals(expected, ignoreCase = true)
        }
    }

    data class Unknown(val raw: String) : BoardCondition {
        override fun test(player: Player): Boolean = false
    }

    companion object {

        fun parse(raw: String): BoardCondition {
            val parts = raw.trim().split(" ", limit = 3)

            return when (parts.firstOrNull()?.lowercase()) {
                "spawn" -> AtSpawn
                "world" -> parts.getOrNull(1)?.let(::InWorld) ?: unknown(raw)
                "permission" -> parts.getOrNull(1)?.let(::HasPermission) ?: unknown(raw)
                "placeholder" -> {
                    val placeholder = parts.getOrNull(1)
                    val expected = parts.getOrNull(2)
                    if (placeholder == null || expected == null) unknown(raw)
                    else PlaceholderEquals(placeholder, expected)
                }

                else -> unknown(raw)
            }
        }

        private fun unknown(raw: String): BoardCondition {
            Instance.logger.warning("[scoreboard] condição '$raw' não reconhecida; o quadro nunca vai aparecer.")
            return Unknown(raw)
        }
    }
}
