package ocean_block.system.playtime_rewards.notify

import ocean_block.system.playtime_rewards.text.RewardText

data class ToastLine(val text: String, val icon: String)

object RewardCard {

    const val HINT = "Use /recompensas"

    private const val BACKDROP = "glyph:toast_tempo"
    private const val LINE = "glyph:toast_linha"

    fun lines(minutes: Int): List<ToastLine> = listOf(
        ToastLine("<shadow:#00000000><yellow>${RewardText.duration(minutes.toLong())} online!</yellow></shadow>", BACKDROP),
        ToastLine("<shadow:#00000000><white>$HINT</white></shadow>", LINE),
    )
}
