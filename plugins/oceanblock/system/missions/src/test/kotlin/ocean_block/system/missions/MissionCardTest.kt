package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.rules.MissionRules
import ocean_block.system.missions.text.MissionCard
import ocean_block.system.missions.text.MissionCardText

class MissionCardTest : StringSpec({

    val names: (String) -> String = { if (it == "rede") "Rede de Detritos" else it }

    "trilha mostra a tarefa com o XP em cima e o progresso embaixo" {
        MissionCard.current(MissionRules.Current(MissionTrail.all[0], 3, null), names) shouldBe
            MissionCardText(
                "<white>Puxe 10 detritos com o Gancho</white>",
                "<gold>+50 XP</gold>",
                "<aqua>3/10</aqua>",
                0.3f,
            )
    }

    "missão de fabricar mostra 0/1 e o nome da receita" {
        val rede = MissionTrail.all.first { it.id == "rede" }
        MissionCard.current(MissionRules.Current(rede, 0, null), names) shouldBe
            MissionCardText(
                "<white>Fabrique em /receitas: Rede de Detritos</white>",
                "<gold>+100 XP</gold>",
                "<aqua>0/1</aqua>",
                0f,
            )
    }

    "venda mostra o progresso em dinheiro" {
        MissionCard.current(MissionRules.Current(MissionTrail.all[8], 350, null), names)?.bottomRight shouldBe
            "<aqua>$350/$1.000</aqua>"
    }

    "diária troca o XP pela posição da diária" {
        MissionCard.current(MissionRules.Current(MissionTrail.all[6], 20, 1), names)?.topRight shouldBe
            "<gray>Diária 2/3</gray>"
    }

    "progresso acima da meta fica cheio" {
        MissionCard.current(MissionRules.Current(MissionTrail.all[0], 99, null), names)?.progress shouldBe 1f
    }

    "sem missão ativa não há cartão" {
        MissionCard.current(null, names) shouldBe null
    }

    "conclusão mostra a missão, o XP e a barra cheia" {
        MissionCard.completed(MissionTrail.all[0], 50) shouldBe
            MissionCardText(
                "<green>Concluída:</green> <white>Primeira pescaria</white>",
                "<gold>+50 XP</gold>",
                "<green>10/10</green>",
                1f,
            )
        MissionCard.completed(MissionTrail.all[0], 0).topRight shouldBe ""
    }
})
