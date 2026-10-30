package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
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
    val today = PlayerMissions("u", done, 0, "2026-10-29", dailies, "venda_2000")

    "na trilha as diarias ja aparecem" {
        val midTrail = PlayerMissions("u", 3, 0, "2026-10-29", dailies)

        MissionLore.listed(midTrail) shouldBe dailies
        MissionLore.notice(midTrail) shouldBe null
        MissionLore.dailiesDone(midTrail) shouldBe "| 1/3 DIÁRIAS"
        MissionLore.selection(midTrail, null) shouldBe "gerador_64"
    }

    "na trilha sem diarias sorteadas o detalhe mostra a missao atual" {
        val state = PlayerMissions("u", 0, 3, "", emptyList())
        MissionLore.selection(state, null) shouldBe MissionLore.TRAIL
        val detail = MissionLore.detail(state, MissionLore.TRAIL)

        detail["titulo"] shouldBe "Primeira pescaria"
        detail["subtitulo"] shouldBe "Missão 1 de 15"
        detail["requisito"] shouldBe "Puxe 10 detritos com o Gancho"
        detail["progresso"] shouldBe "3/10"
        detail["recompensa"] shouldBe "+50 XP"
        detail["bloqueado"] shouldBe "true"
        MissionLore.summary(state) shouldBe "TRILHA 0/15"
        MissionLore.notice(state)?.get("texto") shouldBe MissionLore.NO_DAILIES
        MissionLore.listed(state) shouldBe emptyList()
    }

    "selecao padrao vai para a diaria valendo, depois a primeira aberta" {
        MissionLore.selection(today, null) shouldBe "venda_2000"
        MissionLore.selection(today.copy(activeDaily = null), null) shouldBe "gerador_64"
        MissionLore.selection(today, "receitas_3") shouldBe "receitas_3"
        MissionLore.selection(today, MissionLore.TRAIL) shouldBe MissionLore.TRAIL
        MissionLore.selection(today, "sumiu") shouldBe "venda_2000"
    }

    "linhas mostram faixa, recompensa e contagem" {
        val rows = MissionLore.listed(today).map { MissionLore.row(it, today, "gerador_64") }

        rows.map { it["faixa"] } shouldBe listOf("ui:faixa_laranja", "ui:faixa_azul", "ui:faixa_verde")
        rows.map { it["selecionada"] } shouldBe listOf("true", "false", "false")
        rows[0]["contagem"] shouldBe "16/64"
        rows[0]["xp"] shouldBe "+474 XP"
        rows[1]["texto"] shouldBe "Feirante"
        rows[1]["contagem_estilo"] shouldBe "accent"
        rows[2]["contagem"] shouldBe "Feita"
        rows[2]["xp_estilo"] shouldBe "good"
    }

    "detalhe da diaria muda o botao conforme a situacao" {
        MissionLore.detail(today, "gerador_64").let {
            it["subtitulo"] shouldBe "Diária 1 de 3 · pausada"
            it["botao"] shouldBe "Escolher esta diária"
            it["bloqueado"] shouldBe "false"
        }
        MissionLore.detail(today, "venda_2000").let {
            it["titulo"] shouldBe "Feirante"
            it["requisito"] shouldBe "Venda 2.000 coins em itens no /sell"
            it["botao"] shouldBe "Valendo agora"
            it["bloqueado"] shouldBe "true"
            it["progresso"] shouldBe "0/2.000"
        }
        MissionLore.detail(today, "receitas_3")["botao"] shouldBe "Feita"
        MissionLore.detail(today, MissionLore.TRAIL)["titulo"] shouldBe "Trilha completa"
    }

    "cabecalho resume trilha e diarias" {
        MissionLore.summary(today) shouldBe "TRILHA COMPLETA"
        MissionLore.dailiesDone(today) shouldBe "| 1/3 DIÁRIAS"
        MissionLore.legend(today).size shouldBe 1
        MissionLore.legend(PlayerMissions("u", 0, 0, "", emptyList())) shouldBe emptyList()
        MissionLore.trailCount(today) shouldBe "15/15"
        MissionLore.trailPercent(today) shouldBe "100"
        MissionLore.trailStyle(today) shouldBe "good"
        MissionLore.trailPercent(PlayerMissions("u", 3, 0, "", emptyList())) shouldBe "20"
        MissionLore.trailStyle(PlayerMissions("u", 3, 0, "", emptyList())) shouldBe "muted"
        MissionLore.notice(today) shouldBe null
    }

    "recompensas listam o xp da selecao" {
        MissionLore.rewards(today, "gerador_64") shouldBe listOf(
            mapOf("icone" to "item:experience_bottle", "nome" to "XP de nível", "valor" to "+474", "valor_estilo" to "ouro"),
        )
        MissionLore.rewards(today, "receitas_3").single()["valor_estilo"] shouldBe "good"
        MissionLore.rewards(today, MissionLore.TRAIL) shouldBe emptyList()
        MissionLore.rewards(PlayerMissions("u", 0, 0, "", emptyList()), MissionLore.TRAIL).single()["valor"] shouldBe "+50"
    }

    "icone usa entidade quando a missao pede" {
        MissionLore.icon("zombie") shouldBe "item:zombie"
        MissionLore.icon("entidade:zombie") shouldBe "entidade:zombie"
        val mobs = PlayerMissions("u", done, 0, "2026-10-29", listOf(DailyMission("mobs_20", 0, 40, false)))
        MissionLore.row(mobs.dailies[0], mobs, "mobs_20")["icone"] shouldBe "entidade:zombie"
    }

    "renovacao conta ate a meia-noite" {
        MissionLore.resetIn(LocalDateTime.of(2026, 10, 25, 23, 15)) shouldBe "Renovam em 00:45"
        MissionLore.resetIn(LocalDateTime.of(2026, 10, 25, 9, 30)) shouldBe "Renovam em 14:30"
    }
})
