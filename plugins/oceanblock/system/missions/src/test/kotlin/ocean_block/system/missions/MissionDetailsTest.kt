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

    "a trilha traz icone e requisito com o nome da receita" {
        val trail = MissionLore.detail(PlayerMissions("u", 0, 3, "", emptyList()), MissionLore.TRAIL) { names[it] ?: it }

        trail["icone"] shouldBe "item:fishing_rod"
        trail["requisito"] shouldBe "Puxe 10 detritos com o Gancho"
    }

    "a diaria traz icone, xp e requisito" {
        val state = PlayerMissions("u", MissionTrail.size, 0, "", listOf(DailyMission("gerador_64", 16, 474, false)))
        val daily = MissionLore.detail(state, "gerador_64") { it }

        daily["icone"] shouldBe "item:cobblestone"
        daily["recompensa"] shouldBe "+474 XP"
        daily["requisito"] shouldBe "Quebre 64 blocos do gerador à mão"
    }
})
