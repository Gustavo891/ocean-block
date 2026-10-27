package ocean_block.system.pirate_raid.notify

data class ToastLine(val text: String, val icon: String)

object RaidCard {

    private const val BACKDROP = "glyph:toast_pirata"
    private const val LINE = "glyph:toast_linha"

    fun started(): List<ToastLine> = card("Piratas à vista!", "Defenda sua ilha")

    fun won(coins: Int): List<ToastLine> = card("Raid vencida!", "+${thousands(coins)} Coins")

    fun fled(): List<ToastLine> = card("Os piratas fugiram", "Mais sorte na próxima")

    fun thousands(value: Int): String =
        value.toString().reversed().chunked(3).joinToString(".").reversed()

    private fun card(title: String, detail: String): List<ToastLine> = listOf(
        ToastLine("<red>$title</red>", BACKDROP),
        ToastLine("<white>$detail</white>", LINE),
    )
}
