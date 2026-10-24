package ocean_block.system.scoreboard.board

import me.clip.placeholderapi.PlaceholderAPI
import ocean_block.system.scoreboard.level.AccountLevel
import ocean_block.system.scoreboard.level.LevelText
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal object Placeholders {

    private val LEGACY = mapOf(
        '0' to "black",
        '1' to "dark_blue",
        '2' to "dark_green",
        '3' to "dark_aqua",
        '4' to "dark_red",
        '5' to "dark_purple",
        '6' to "gold",
        '7' to "gray",
        '8' to "dark_gray",
        '9' to "blue",
        'a' to "green",
        'b' to "aqua",
        'c' to "red",
        'd' to "light_purple",
        'e' to "yellow",
        'f' to "white",
        'k' to "obfuscated",
        'l' to "bold",
        'm' to "strikethrough",
        'n' to "underlined",
        'o' to "italic",
        'r' to "reset",
    )

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy")
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

    private val serverId: String by lazy {
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
        (1..5).map { alphabet.random() }.joinToString("")
    }

    fun apply(player: Player, text: String): String {
        val now = LocalDateTime.now()
        val level = AccountLevel.source(player)

        val withBuiltIn = text
            .replace("%nivel_barra%", LevelText.bar(level.progress))
            .replace("%nivel_progresso%", LevelText.percent(level.progress))
            .replace("%nivel%", LevelText.badge(level.level))
            .replace("%player%", player.name)
            .replace("%online%", Bukkit.getOnlinePlayers().size.toString())
            .replace("%date%", DATE_FORMAT.format(now))
            .replace("%time%", TIME_FORMAT.format(now))
            .replace("%serverid%", serverId)

        val resolved = if (isPlaceholderApiEnabled()) {
            PlaceholderAPI.setPlaceholders(player, withBuiltIn)
        } else {
            withBuiltIn
        }

        return toMiniTags(resolved)
    }

    private fun isPlaceholderApiEnabled(): Boolean =
        Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")

    private fun toMiniTags(text: String): String {
        if (!text.contains('§')) return text

        val builder = StringBuilder(text.length)
        var index = 0

        while (index < text.length) {
            val current = text[index]
            val next = text.getOrNull(index + 1)?.lowercaseChar()
            val tag = if (current == '§' && next != null) LEGACY[next] else null

            if (tag == null) {
                builder.append(current)
                index++
                continue
            }

            builder.append('<').append(tag).append('>')
            index += 2
        }

        return builder.toString()
    }
}
