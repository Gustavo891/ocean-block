package ocean_block.system.islands.choice

import ocean_block.system.islands.island.type.IslandType

object ChoiceRules {

    const val SEA_Y = 62
    const val CAMERA_Y = 63.0
    const val EYE_HEIGHT = 1.62
    const val CAMERA_PITCH = 0f
    const val CHAT_CLEAR_LINES = 100

    enum class LoginAction { CHOOSE, RESTORE, NOTHING }

    fun needsChoice(ownsIsland: Boolean, memberSomewhere: Boolean): Boolean = !ownsIsland && !memberSomewhere

    fun afterLogin(marked: Boolean, hasHome: Boolean): LoginAction = when {
        !hasHome -> LoginAction.CHOOSE
        marked -> LoginAction.RESTORE
        else -> LoginAction.NOTHING
    }

    fun countText(n: Int): String = when (n) {
        0 -> "nenhuma ilha"
        1 -> "1 ilha"
        else -> "$n ilhas"
    }

    fun rows(counts: Map<IslandType, Int>): List<Map<String, String>> = IslandType.entries.map { type ->
        mapOf(
            "id" to type.id,
            "nome" to type.nome.uppercase(),
            "descricao" to type.descricao,
            "contagem" to countText(counts[type] ?: 0),
            "icone" to "ui:ilha_tipo/${type.id}",
        )
    }
}
