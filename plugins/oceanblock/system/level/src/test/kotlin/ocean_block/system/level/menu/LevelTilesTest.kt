package ocean_block.system.level.menu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ocean_block.system.level.curve.LevelCurve

class LevelTilesTest : StringSpec({

    "nivel abaixo, igual e acima do jogador" {
        LevelTiles.state(5, 7) shouldBe LevelTiles.State.REACHED
        LevelTiles.state(7, 7) shouldBe LevelTiles.State.CURRENT
        LevelTiles.state(8, 7) shouldBe LevelTiles.State.LOCKED
    }

    "nome da casa pela cor do estado" {
        LevelTiles.name(5, LevelTiles.State.REACHED) shouldBe "<green>Nível 5"
        LevelTiles.name(7, LevelTiles.State.CURRENT) shouldBe "<gold>Nível 7"
        LevelTiles.name(8, LevelTiles.State.LOCKED) shouldBe "<gray>Nível 8"
    }

    "nivel alcancado diz que ja foi e lista o que liberou" {
        LevelTiles.lore(3, LevelCurve.totalFor(5), listOf("Rede de Detritos")) shouldContainExactly listOf(
            "<green>✔ Alcançado",
            "",
            "<white>Libera:",
            "<gray>∙ Rede de Detritos",
        )
    }

    "nivel atual mostra o XP dentro dele" {
        LevelTiles.lore(2, 150, emptyList()) shouldContainExactly listOf(
            "<yellow>Você está aqui",
            "<gray>50 / 283 XP",
        )
    }

    "nivel bloqueado diz quanto XP falta" {
        LevelTiles.lore(4, 150, emptyList()) shouldContainExactly listOf(
            "<red>✖ Bloqueado",
            "<gray>Faltam ${LevelCurve.totalFor(4) - 150} XP",
        )
    }

    "no nivel maximo a casa atual nao fala em XP" {
        LevelTiles.lore(50, LevelCurve.totalFor(50), emptyList()) shouldContainExactly listOf(
            "<yellow>Você está aqui",
            "<gray>Nível máximo",
        )
    }
})
