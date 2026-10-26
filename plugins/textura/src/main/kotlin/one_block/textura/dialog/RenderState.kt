package one_block.textura.dialog

import one_block.textura.dialog.node.UiAction

class RenderState private constructor(
    val token: Long,
    val session: DialogSession,
    private val current: List<UiAction>,
    private val previousToken: Long?,
    private val previous: List<UiAction>?,
) {

    fun actions(token: Long): List<UiAction>? = when (token) {
        this.token -> current
        previousToken -> previous
        else -> null
    }

    companion object {
        fun next(old: RenderState?, token: Long, session: DialogSession, actions: List<UiAction>): RenderState {
            val kept = old?.takeIf { it.session === session }
            return RenderState(token, session, actions, kept?.token, kept?.current)
        }
    }
}
