package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.dialog.MissionLore
import ocean_block.system.missions.model.DailyMission
import ocean_block.system.missions.model.PlayerMissions
import java.time.LocalDateTime

class MissionLoreTest : StringSpec({

    val done = MissionTrail.size
    val dailies = listOf(
        DailyMission("gerador_64", 16, 474, false),
        DailyMission("venda_2000", 0, 474, false),
        DailyMission("receitas_3", 3, 474, true),
    )

    "na trilha o cartao mostra a missao atual e as diarias ficam bloqueadas" {
        val state = PlayerMissions("u", 0, 3, "", emptyList())
        val card = MissionLore.active(state).shouldNotBeNull()

        card["rotulo"] shouldBe "Missão 1 de 13"
        card["texto"] shouldBe "Colete 10 detritos usando o gancho"
        card["requisito"] shouldBe "Puxe 10 detritos com o Gancho"
        card["situacao"] shouldBe "3/10"
        card["xp"] shouldBe "+50 XP"
        card["barra"] shouldBe "30"
        MissionLore.summary(state) shouldBe "TRILHA 0/13"
        MissionLore.notice(state)?.get("texto") shouldBe MissionLore.LOCKED_DAILIES
        MissionLore.listed(state) shouldBe emptyList()
    }

    "requisito igual ao texto some" {
        val nivel = MissionTrail.all.indexOfFirst { it.id == "nivel_3" }
        MissionLore.active(PlayerMissions("u", nivel, 0, "", emptyList()))?.get("requisito") shouldBe ""
    }

    "depois da trilha a escolhida vai para o destaque e sai da lista" {
        val state = PlayerMissions("u", done, 0, "2026-10-28", dailies, "gerador_64")
        val card = MissionLore.active(state).shouldNotBeNull()

        card["rotulo"] shouldBe "Valendo agora"
        card["situacao"] shouldBe "16/64"
        card["xp"] shouldBe "+474 XP"
        MissionLore.listed(state).map { it.id } shouldBe listOf("venda_2000", "receitas_3")
        MissionLore.notice(state) shouldBe null
        MissionLore.summary(state) shouldBe "TRILHA COMPLETA"
    }

    "sem escolhida pede para escolher e lista todas" {
        val state = PlayerMissions("u", done, 0, "2026-10-28", dailies)

        MissionLore.active(state) shouldBe null
        MissionLore.notice(state)?.get("texto") shouldBe MissionLore.CHOOSE_DAILY
        MissionLore.listed(state).size shouldBe 3
    }

    "todas feitas avisa que voltam amanha" {
        val state = PlayerMissions("u", done, 0, "2026-10-28", dailies.map { it.copy(done = true) })
        MissionLore.notice(state)?.get("texto") shouldBe MissionLore.DAILIES_DONE
    }

    "linha da diaria mostra estado, botao e dinheiro sem cifrao" {
        val paused = MissionLore.row(dailies[0])
        paused["situacao"] shouldBe "16/64  Pausada"
        paused["acao"] shouldBe "Escolher"
        paused["bloqueado"] shouldBe "false"

        val sale = MissionLore.row(dailies[1])
        sale["texto"] shouldBe "Venda 2.000 coins em itens"
        sale["situacao"] shouldBe "0/2.000"

        val finished = MissionLore.row(dailies[2])
        finished["situacao"] shouldBe "Concluída"
        finished["situacao_estilo"] shouldBe "good"
        finished["acao"] shouldBe "Feita"
        finished["bloqueado"] shouldBe "true"
    }

    "icone usa entidade quando a missao pede" {
        MissionLore.icon("zombie") shouldBe "item:zombie"
        MissionLore.icon("entidade:zombie") shouldBe "entidade:zombie"
        MissionLore.row(DailyMission("mobs_20", 0, 40, false))["icone"] shouldBe "entidade:zombie"
    }

    "renovacao conta ate a meia-noite" {
        MissionLore.resetIn(LocalDateTime.of(2026, 10, 25, 23, 15)) shouldBe "Renovam em 00:45"
        MissionLore.resetIn(LocalDateTime.of(2026, 10, 25, 9, 30)) shouldBe "Renovam em 14:30"
    }
})
