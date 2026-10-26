package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.node.UiAction
import one_block.textura.dialog.node.UiOutcome

class RenderStateTest : StringSpec({

    val session = DialogSession("tela", DialogBinds())
    val first = listOf(UiAction { UiOutcome.Refresh })
    val second = listOf(UiAction { UiOutcome.Close })

    "o clique vale para o desenho atual" {
        RenderState.next(null, 1, session, first).actions(1) shouldBe first
    }

    "um refresh da mesma sessão mantém o desenho anterior clicável" {
        val refreshed = RenderState.next(RenderState.next(null, 1, session, first), 2, session, second)

        refreshed.actions(2) shouldBe second
        refreshed.actions(1) shouldBe first
    }

    "só o desenho imediatamente anterior continua valendo" {
        val state = RenderState.next(RenderState.next(RenderState.next(null, 1, session, first), 2, session, first), 3, session, second)

        state.actions(1) shouldBe null
        state.actions(2) shouldBe first
    }

    "outra sessão não herda os cliques da tela antiga" {
        val other = DialogSession("outra", DialogBinds())

        RenderState.next(RenderState.next(null, 1, session, first), 2, other, second).actions(1) shouldBe null
    }
})
