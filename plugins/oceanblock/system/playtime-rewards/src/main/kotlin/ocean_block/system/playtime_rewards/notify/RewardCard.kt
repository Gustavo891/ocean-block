package ocean_block.system.playtime_rewards.notify

import ocean_block.system.playtime_rewards.text.RewardText

object RewardCard {

    const val HINT = "Resgate: /recompensas"

    private const val WIDTH = 160
    private const val TEXT_X = 30

    fun lines(minutes: Int): List<String> {
        val room = WIDTH - TEXT_X
        val title = "${RewardText.duration(minutes.toLong())} online!"
        return listOf(
            "<shadow:#00000000><white><glyph:toast_tempo></white><shift:-$room>" +
                "<dark_purple>$title</dark_purple><shift:${room - LineText.width(title)}></shadow>",
            "<shadow:#00000000><shift:$TEXT_X><black>$HINT</black><shift:${room - LineText.width(HINT)}></shadow>",
        )
    }
}
