package ocean_block.system.level.menu

import ocean_block.system.level.curve.LevelCurve
import java.text.NumberFormat
import java.util.Locale

object LevelTiles {

    enum class State(val color: String) { REACHED("green"), CURRENT("gold"), LOCKED("gray") }

    private val LOCALE: Locale = Locale.forLanguageTag("pt-BR")

    fun state(level: Int, playerLevel: Int): State = when {
        level < playerLevel -> State.REACHED
        level == playerLevel -> State.CURRENT
        else -> State.LOCKED
    }

    fun name(level: Int, state: State): String = "<${state.color}>Nível $level"

    fun lore(level: Int, xp: Long, unlocks: List<String>): List<String> {
        val status = when (state(level, LevelCurve.levelOf(xp))) {
            State.REACHED -> listOf("<green>✔ Alcançado")
            State.CURRENT -> listOf("<yellow>Você está aqui", "<gray>${progress(level, xp)}")
            State.LOCKED -> listOf("<red>✖ Bloqueado", "<gray>Faltam ${number(LevelCurve.totalFor(level) - xp)} XP")
        }
        if (unlocks.isEmpty()) return status
        return status + listOf("", "<white>Libera:") + unlocks.map { "<gray>∙ $it" }
    }

    private fun progress(level: Int, xp: Long): String {
        if (level >= LevelCurve.MAX_LEVEL) return "Nível máximo"
        return "${number(xp - LevelCurve.totalFor(level))} / ${number(LevelCurve.toNext(level))} XP"
    }

    private fun number(value: Long): String = NumberFormat.getIntegerInstance(LOCALE).format(value)
}
