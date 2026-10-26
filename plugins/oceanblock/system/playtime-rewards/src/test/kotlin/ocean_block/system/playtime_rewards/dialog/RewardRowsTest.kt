package ocean_block.system.playtime_rewards.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainAll
import ocean_block.system.playtime_rewards.model.Milestone

private val hour = Milestone("1h", 60, coins = 5000, pearls = 0, xp = 200)

class RewardRowsTest : StringSpec({

    "marco liberado tem botão ativo e barra cheia" {
        RewardRows.row(hour, 4000, emptySet()) { it } shouldContainAll mapOf(
            "icone" to "item:clock",
            "nome" to "1 h",
            "premios" to "5.000 Coins · 200 XP",
            "situacao" to "Liberado!",
            "situacao_estilo" to "accent",
            "barra" to "100",
            "bloqueado" to "false",
        )
    }

    "marco bloqueado mostra o que falta e trava o botão" {
        RewardRows.row(hour, 1800, emptySet()) { it } shouldContainAll mapOf(
            "situacao" to "Faltam 30 min",
            "situacao_estilo" to "muted",
            "barra" to "50",
            "bloqueado" to "true",
        )
    }

    "marco resgatado fica verde e travado" {
        RewardRows.row(hour, 4000, setOf("1h")) { it } shouldContainAll mapOf(
            "situacao" to "Resgatado",
            "situacao_estilo" to "good",
            "bloqueado" to "true",
        )
    }
})
