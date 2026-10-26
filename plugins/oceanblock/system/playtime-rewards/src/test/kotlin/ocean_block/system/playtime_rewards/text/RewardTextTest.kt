package ocean_block.system.playtime_rewards.text

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.playtime_rewards.model.ItemPrize
import ocean_block.system.playtime_rewards.model.Milestone

class RewardTextTest : StringSpec({

    "duração em minutos e horas" {
        RewardText.duration(30) shouldBe "30 min"
        RewardText.duration(60) shouldBe "1 h"
        RewardText.duration(90) shouldBe "1 h 30 min"
        RewardText.duration(6000) shouldBe "100 h"
    }

    "tempo jogado em segundos vira horas e minutos" {
        RewardText.played(0) shouldBe "0 min"
        RewardText.played(59) shouldBe "0 min"
        RewardText.played(45_300) shouldBe "12 h 35 min"
    }

    "tempo faltando arredonda para cima e nunca é menos de 1 min" {
        RewardText.remaining(1) shouldBe "1 min"
        RewardText.remaining(61) shouldBe "2 min"
        RewardText.remaining(12_000) shouldBe "3 h 20 min"
    }

    "números usam ponto como separador de milhar" {
        RewardText.number(2000) shouldBe "2.000"
        RewardText.number(300_000) shouldBe "300.000"
        RewardText.number(25) shouldBe "25"
    }

    "prêmios listam só o que o marco paga" {
        val milestone = Milestone(
            "8h", 480, coins = 35_000, pearls = 2, xp = 1000,
            items = listOf(ItemPrize("IRON_INGOT", 16)),
        )
        RewardText.prizes(milestone) { "Barra de ferro" } shouldBe
            "35.000 Coins · 1.000 XP · 2 Pérolas · 16 Barra de ferro"
    }

    "uma pérola fica no singular e marco sem prêmio mostra traço" {
        RewardText.prizes(Milestone("4h", 240, coins = 0, pearls = 1, xp = 0)) { it } shouldBe "1 Pérola"
        RewardText.prizes(Milestone("x", 1, coins = 0, pearls = 0, xp = 0)) { it } shouldBe "—"
    }

    "lembrete no login no singular e no plural" {
        RewardText.pending(1) shouldBe "Você tem 1 recompensa por tempo para resgatar! Use /recompensas"
        RewardText.pending(3) shouldBe "Você tem 3 recompensas por tempo para resgatar! Use /recompensas"
    }
})
