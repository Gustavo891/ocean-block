package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.model.MissionObjective

class MissionTrailTest : StringSpec({

    "a trilha tem 13 missoes com ids unicos" {
        MissionTrail.all.size shouldBe 13
        MissionTrail.all.map { it.id }.toSet().size shouldBe 13
    }

    "textos e xp seguem a tabela da spec" {
        MissionTrail.all.map { it.text to it.xp } shouldBe listOf(
            "Colete 10 detritos usando o gancho" to 50,
            "Faça um gerador de pedra" to 50,
            "Quebre 32 blocos do gerador" to 60,
            "Derreta 10 minérios brutos" to 80,
            "Chegue ao nível 3" to 50,
            "Faça uma Rede de Detritos" to 100,
            "Colete 50 detritos" to 120,
            "Venda $1.000 em itens" to 150,
            "Compre o upgrade do gerador nível 1" to 200,
            "Quebre 10 minérios de ferro" to 150,
            "Faça uma Fornalha elétrica" to 300,
            "Faça um Gerador de energia" to 600,
            "Faça um Minerador" to 1000,
        )
    }

    "objetivos seguem a tabela da spec" {
        MissionTrail.all.map { it.objective } shouldBe listOf(
            MissionObjective.CollectDebris(10, true),
            MissionObjective.BuildGenerator,
            MissionObjective.BreakGeneratorBlocks(32, null),
            MissionObjective.Smelt(10),
            MissionObjective.ReachLevel(3),
            MissionObjective.Craft("rede"),
            MissionObjective.CollectDebris(50, false),
            MissionObjective.Sell(1000),
            MissionObjective.IslandUpgrade("Generator", 1),
            MissionObjective.BreakGeneratorBlocks(10, "IRON_ORE"),
            MissionObjective.Craft("fornalha_eletrica"),
            MissionObjective.Craft("gerador_energia"),
            MissionObjective.Craft("minerador"),
        )
    }

    "toda receita citada existe na tabela de niveis" {
        val known = listOf(
            "gancho", "rede",
            "picareta_fundidora", "gerador_energia", "painel_solar", "fornalha_eletrica", "minerador",
        )
        MissionTrail.all.map { it.objective }.filterIsInstance<MissionObjective.Craft>().forEach {
            it.recipeId shouldBeIn known
        }
    }
})
