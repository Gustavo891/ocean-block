package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.dialog.MissionLore
import ocean_block.system.missions.model.DailyMission
import ocean_block.system.missions.model.MissionObjective
import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.text.MissionText

class MissionDetailsTest : StringSpec({

    val names = mapOf("rede" to "Rede de Detritos")
    fun requirement(objective: MissionObjective) = MissionText.requirement(objective) { names[it] ?: it }

    "cada objetivo vira uma frase de requisito" {
        requirement(MissionObjective.CollectDebris(10, true)) shouldBe "Puxe 10 detritos com o Gancho"
        requirement(MissionObjective.CollectDebris(50, false)) shouldBe "Colete 50 detritos (Gancho ou toque)"
        requirement(MissionObjective.BuildGenerator) shouldBe "Monte um gerador de pedra na sua ilha"
        requirement(MissionObjective.BreakGeneratorBlocks(32, null)) shouldBe "Quebre 32 blocos do gerador à mão"
        requirement(MissionObjective.BreakGeneratorBlocks(10, "IRON_ORE")) shouldBe "Quebre 10 minérios de ferro do gerador"
        requirement(MissionObjective.Smelt(10)) shouldBe "Derreta 10 minérios brutos"
        requirement(MissionObjective.Craft("rede")) shouldBe "Fabrique em /receitas: Rede de Detritos"
        requirement(MissionObjective.CraftAny(3)) shouldBe "Fabrique 3 receitas em /receitas"
        requirement(MissionObjective.ReachLevel(3)) shouldBe "Chegue ao nível 3"
        requirement(MissionObjective.Sell(1000)) shouldBe "Venda $1.000 em itens no /sell"
        requirement(MissionObjective.IslandUpgrade("Generator", 1)) shouldBe "Compre o Gerador nível 1 em /ilha"
        requirement(MissionObjective.KillMobs(20)) shouldBe "Mate 20 mobs"
    }

    "toda missao tem um icone proprio" {
        (MissionTrail.all + DailyPool.all).map { it.icon } shouldNotContain "map"
        MissionTrail.all[0].icon shouldBe "fishing_rod"
    }

    "a trilha traz icone, requisito e recompensa" {
        val trail = MissionLore.trail(PlayerMissions("u", 0, 3, "", emptyList())) { names[it] ?: it }

        trail["icone"] shouldBe "item:fishing_rod"
        trail["requisito"] shouldBe "Puxe 10 detritos com o Gancho"
        trail["recompensa"] shouldBe "+50 XP de nível"
    }

    "a diaria traz icone, requisito e recompensa" {
        val daily = MissionLore.daily(DailyMission("gerador_64", 16, 474, false)) { it }

        daily["icone"] shouldBe "item:cobblestone"
        daily["requisito"] shouldBe "Quebre 64 blocos do gerador à mão"
        daily["recompensa"] shouldBe "+474 XP de nível"
        daily["situacao"] shouldBe "16/64"
        daily["situacao_estilo"] shouldBe "muted"
    }

    "diaria feita troca o contador por concluida em verde" {
        val daily = MissionLore.daily(DailyMission("receitas_3", 3, 40, true)) { it }

        daily["situacao"] shouldBe "Concluída"
        daily["situacao_estilo"] shouldBe "good"
    }
})
