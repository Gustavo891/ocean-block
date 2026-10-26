package ocean_block.system.playtime_rewards.text

import ocean_block.system.playtime_rewards.model.Milestone
import java.util.Locale

object RewardText {

    private const val SEPARATOR = " · "
    private const val COMMAND = "/recompensas"

    fun duration(minutes: Long): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours == 0L -> "$rest min"
            rest == 0L -> "$hours h"
            else -> "$hours h $rest min"
        }
    }

    fun duration(minutes: Int): String = duration(minutes.toLong())

    fun played(seconds: Long): String = duration(seconds.coerceAtLeast(0) / 60)

    fun remaining(seconds: Long): String = duration(((seconds + 59) / 60).coerceAtLeast(1))

    fun number(value: Int): String = String.format(Locale.ROOT, "%,d", value).replace(',', '.')

    fun prizes(milestone: Milestone, itemName: (String) -> String): String {
        val parts = buildList {
            if (milestone.coins > 0) add("${number(milestone.coins)} Coins")
            if (milestone.xp > 0) add("${number(milestone.xp)} XP")
            if (milestone.pearls > 0) add("${number(milestone.pearls)} ${if (milestone.pearls == 1) "Pérola" else "Pérolas"}")
            milestone.items.forEach { add("${it.amount} ${itemName(it.material)}") }
        }
        return parts.joinToString(SEPARATOR).ifEmpty { "—" }
    }

    fun reached(milestone: Milestone): String = "Recompensa de ${duration(milestone.minutes)} liberada! Use $COMMAND"

    fun pending(count: Int): String {
        val noun = if (count == 1) "recompensa" else "recompensas"
        return "Você tem $count $noun por tempo para resgatar! Use $COMMAND"
    }
}
