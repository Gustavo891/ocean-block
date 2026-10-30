package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.model.MissionObjective

class MissionTrailTest : StringSpec({

    "a trilha tem 15 missoes com ids unicos" {
        MissionTrail.all.size shouldBe 15
        MissionTrail.all.map { it.id }.toSet().size shouldBe 15
    }

    "nomes e xp seguem a proposta aprovada" {
        MissionTrail.all.map { it.name to it.xp } shouldBe listOf(
            "Primeira pescaria" to 50,
            "Fonte de pedra" to 50,
            "Mãos à obra" to 60,
            "Aprendiz do mar" to 50,
            "Rede armada" to 100,
            "Fogo na fornalha" to 80,
            "Primeiro carvão" to 80,
            "Catador do mar" to 120,
            "Primeiro comércio" to 150,
            "Caça ao tesouro" to 120,
            "Marinheiro" to 150,
            "Primeira patrulha" to 120,
            "Veio de ferro" to 150,
            "Mercador" to 200,
            "Capitão da ilha" to 300,
        )
    }

    "objetivos seguem a proposta aprovada" {
        MissionTrail.all.map { it.objective } shouldBe listOf(
            MissionObjective.CollectDebris(10, true),
            MissionObjective.BuildGenerator,
            MissionObjective.BreakGeneratorBlocks(32, null),
            MissionObjective.ReachLevel(3),
            MissionObjective.Craft("rede"),
            MissionObjective.Smelt(10),
            MissionObjective.BreakGeneratorBlocks(10, "COAL_ORE"),
            MissionObjective.CollectDebris(50, false),
            MissionObjective.Sell(1000),
            MissionObjective.CollectDebris(2, false, "barril"),
            MissionObjective.ReachLevel(5),
            MissionObjective.KillMobs(10),
            MissionObjective.BreakGeneratorBlocks(10, "IRON_ORE"),
            MissionObjective.Sell(3000),
            MissionObjective.ReachLevel(6),
        )
    }

    "a trilha nao pede nivel alem do 6 nem receita acima do nivel 3" {
        MissionTrail.all.map { it.objective }.filterIsInstance<MissionObjective.ReachLevel>().forEach {
            it.level shouldBeLessThanOrEqual 6
        }
        MissionTrail.all.map { it.objective }.filterIsInstance<MissionObjective.Craft>().forEach {
            it.recipeId shouldBeIn listOf("gancho", "rede")
        }
        MissionTrail.all.map { it.objective }.filterIsInstance<MissionObjective.IslandUpgrade>() shouldBe emptyList()
    }

    "nenhum nome repete o requisito" {
        (MissionTrail.all + DailyPool.all).forEach { it.name shouldNotBe it.text }
        (MissionTrail.all + DailyPool.all).map { it.name }.toSet().size shouldBe MissionTrail.size + DailyPool.all.size
    }

    "as diarias so pedem minerio que o gerador nivel 0 forma" {
        DailyPool.all.map { it.objective }.filterIsInstance<MissionObjective.BreakGeneratorBlocks>().mapNotNull { it.material }.forEach {
            it shouldBeIn listOf("COAL_ORE", "COPPER_ORE", "IRON_ORE")
        }
        DailyPool.all.size shouldBe 17
    }
})
