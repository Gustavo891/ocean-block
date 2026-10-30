package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.dialog.TrailLore
import ocean_block.system.missions.model.DailyMission
import ocean_block.system.missions.model.PlayerMissions

class TrailLoreTest : StringSpec({

    val midway = PlayerMissions("u", 13, 1250, "", emptyList())

    "o caminho cobre as 15 missoes em zigue-zague" {
        TrailLore.order() shouldBe listOf(listOf(0, 1, 2, 3, 4), listOf(9, 8, 7, 6, 5), listOf(10, 11, 12, 13, 14))
        TrailLore.order().flatten().sorted() shouldBe MissionTrail.all.indices.toList()
        TrailLore.indexAt(1, 0) shouldBe 9
        TrailLore.indexAt(3, 0) shouldBe null
    }

    "cada no mostra feita, atual ou trancada" {
        val last = TrailLore.row(midway, 2, 13)

        last.map { it["selo"] } shouldBe listOf("ui:no_feita", "ui:no_feita", "ui:no_feita", "", "")
        last.map { it["estado"] } shouldBe listOf("", "", "", "ui:no_atual", "ui:no_trancada")
        last.map { it["selecionada"] } shouldBe listOf("false", "false", "false", "false", "false")
        TrailLore.row(midway, 2, 11).map { it["selecionada"] } shouldBe listOf("false", "true", "false", "false", "false")
        last[3]["dica"] shouldBe "14. Mercador"
    }

    "ligacoes ficam verdes ate a missao atual" {
        TrailLore.row(midway, 2, 13).map { it["liga"] } shouldBe
            listOf("ui:liga_verde", "ui:liga_verde", "ui:liga_verde", "ui:liga_cinza", "")
        TrailLore.row(midway, 1, 13).map { it["liga"] } shouldBe
            listOf("ui:liga_verde", "ui:liga_verde", "ui:liga_verde", "ui:liga_verde", "")
        TrailLore.drop(midway, 0)["liga"] shouldBe "ui:desce_verde"
        TrailLore.drop(PlayerMissions("u", 4, 0, "", emptyList()), 0)["liga"] shouldBe "ui:desce_cinza"
    }

    "selecao padrao e a missao atual" {
        TrailLore.selection(midway, null) shouldBe 13
        TrailLore.selection(midway, 2) shouldBe 2
        TrailLore.selection(midway, 99) shouldBe 13
        TrailLore.selection(PlayerMissions("u", 15, 0, "", emptyList()), null) shouldBe 14
    }

    "detalhe mostra progresso de feita, atual e trancada" {
        TrailLore.detail(midway, 13).let {
            it["titulo"] shouldBe "Mercador"
            it["requisito"] shouldBe "Venda 3.000 coins em itens no /sell"
            it["progresso"] shouldBe "1.250/3.000"
            it["botao"] shouldBe "Em andamento"
            it["nota"] shouldBe "Missão 14 de 15"
        }
        TrailLore.detail(midway, 0)["botao"] shouldBe "Feita"
        TrailLore.detail(midway, 0)["progresso"] shouldBe "10/10"
        TrailLore.detail(midway, 14)["botao"] shouldBe "Trancada"
        TrailLore.rewards(13).single()["valor"] shouldBe "+200"
    }

    "cabecalho resume a trilha e o que falta para as diarias" {
        TrailLore.summary(midway) shouldBe "13/15 FEITAS"
        TrailLore.xp(midway) shouldBe "| 1.280 DE 1.780 XP"
        val withDailies = PlayerMissions("u", 13, 0, "", listOf(DailyMission("gancho_15", 15, 40, true), DailyMission("mobs_20", 0, 40, false)))
        TrailLore.dailies(midway) shouldBe "--"
        TrailLore.dailies(withDailies) shouldBe "1/2"
        TrailLore.dailiesStyle(withDailies) shouldBe "muted"
        TrailLore.dailiesStyle(withDailies.copy(dailies = withDailies.dailies.map { it.copy(done = true) })) shouldBe "good"
        TrailLore.mini(withDailies) shouldBe listOf(mapOf("icone" to "item:fishing_rod"), mapOf("icone" to "entidade:zombie"))
        TrailLore.summary(PlayerMissions("u", 15, 0, "", emptyList())) shouldBe "TRILHA COMPLETA"
    }
})
