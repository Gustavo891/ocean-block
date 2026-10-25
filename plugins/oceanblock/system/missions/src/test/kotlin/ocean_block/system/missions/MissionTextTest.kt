package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.model.MissionObjective
import ocean_block.system.missions.rules.MissionRules
import ocean_block.system.missions.text.MissionText

class MissionTextTest : StringSpec({

    "trilha mostra texto e contador" {
        MissionText.hud(MissionRules.Current(MissionTrail.all[0], 3, null), trailDone = false) shouldBe
            "<aqua>Missão:</aqua> <white>Colete 10 detritos usando o gancho</white> <gray>3/10"
    }

    "venda mostra o contador em dinheiro" {
        MissionText.counter(MissionObjective.Sell(1000), 350) shouldBe "$350/$1.000"
        MissionText.hud(MissionRules.Current(MissionTrail.all[7], 350, null), trailDone = false) shouldBe
            "<aqua>Missão:</aqua> <white>Venda $1.000 em itens</white> <gray>$350/$1.000"
    }

    "missao de um passo nao tem contador" {
        MissionText.hud(MissionRules.Current(MissionTrail.all[1], 0, null), trailDone = false) shouldBe
            "<aqua>Missão:</aqua> <white>Faça um gerador de pedra</white>"
        MissionText.hud(MissionRules.Current(MissionTrail.all[3], 0, null), trailDone = false) shouldBe
            "<aqua>Missão:</aqua> <white>Faça uma Fundição</white>"
    }

    "diaria mostra a posicao entre as tres" {
        val mission = checkNotNull(DailyPool.byId("gerador_64"))
        MissionText.hud(MissionRules.Current(mission, 12, 1), trailDone = true) shouldBe
            "<aqua>Diária 2/3:</aqua> <white>Quebre 64 blocos do gerador</white> <gray>12/64"
    }

    "tudo feito pede para voltar amanha" {
        MissionText.hud(null, trailDone = true) shouldBe "<gray>Diárias concluídas. Volte amanhã!"
    }
})
