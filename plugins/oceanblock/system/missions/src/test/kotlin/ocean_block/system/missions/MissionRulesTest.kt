package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.model.DailyMission
import ocean_block.system.missions.model.MissionObjective
import ocean_block.system.missions.model.MissionSignal
import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.rules.MissionRules
import java.time.LocalDate

class MissionRulesTest : StringSpec({

    fun at(index: Int, progress: Long = 0) = PlayerMissions("u", index, progress, "", emptyList())

    fun feed(state: PlayerMissions, vararg signals: MissionSignal): Pair<PlayerMissions, List<MissionRules.Completed>> =
        signals.fold(state to emptyList()) { (current, done), signal ->
            val (next, completed) = MissionRules.apply(current, signal)
            next to done + completed
        }

    val noIsland = MissionSignal.State(level = 30, upgrades = null, hasGenerator = false)
    val trailDone = MissionTrail.all.size

    "missao 1 conta so detritos puxados pelo gancho" {
        val (state, done) = feed(at(0), *Array(9) { MissionSignal.Debris(byHook = true) }, MissionSignal.Debris(byHook = false))

        state.trailIndex shouldBe 0
        state.trailProgress shouldBe 9
        done.shouldBeEmpty()
    }

    "o decimo detrito do gancho conclui a missao 1 com 50 xp" {
        val (state, done) = feed(at(0, 9), MissionSignal.Debris(byHook = true))

        done.map { it.mission.id to it.xp } shouldBe listOf(MissionTrail.all[0].id to 50)
        state.trailIndex shouldBe 1
        state.trailProgress shouldBe 0
    }

    "missao 2 conclui com gerador formado" {
        val (state, done) = MissionRules.apply(at(1), MissionSignal.GeneratorFormed)

        done.size shouldBe 1
        state.trailIndex shouldBe 2
    }

    "missao 2 conclui quebrando um bloco do gerador" {
        val (state, done) = MissionRules.apply(at(1), MissionSignal.GeneratorBlock("COBBLESTONE"))

        done.size shouldBe 1
        state.trailIndex shouldBe 2
    }

    "missao 10 so conta minerio de ferro" {
        val (state, _) = feed(at(9), MissionSignal.GeneratorBlock("COBBLESTONE"), MissionSignal.GeneratorBlock("IRON_ORE"))

        state.trailIndex shouldBe 9
        state.trailProgress shouldBe 1
    }

    "missao 8 soma as vendas" {
        val (state, done) = feed(at(7), MissionSignal.Sold(350), MissionSignal.Sold(700))

        done.map { it.xp } shouldBe listOf(150)
        state.trailIndex shouldBe 8
    }

    "derreter soma os lingotes ate 10 e o estado seguinte conclui a 5" {
        val (afterCraft, craftDone) = feed(at(3), MissionSignal.Smelted(4), MissionSignal.Smelted(6))
        craftDone.size shouldBe 1
        afterCraft.trailIndex shouldBe 4

        val (afterState, stateDone) = MissionRules.apply(afterCraft, MissionSignal.State(level = 4, upgrades = emptyMap(), hasGenerator = false))
        stateDone.map { it.mission.id } shouldBe listOf(MissionTrail.all[4].id)
        afterState.trailIndex shouldBe 5
    }

    "estado cumprido conclui so a missao de estado e para na de receita" {
        val (state, done) = MissionRules.apply(at(4), MissionSignal.State(level = 30, upgrades = mapOf("Generator" to 1), hasGenerator = false))

        done.map { it.mission.id } shouldBe listOf(MissionTrail.all[4].id)
        state.trailIndex shouldBe 5
        state.trailProgress shouldBe 0
    }

    "sem ilha a missao do upgrade segue aberta" {
        val (state, done) = MissionRules.apply(at(8), noIsland)

        done.shouldBeEmpty()
        state shouldBe at(8)
    }

    "ilha que ja tem o upgrade conclui a missao 9" {
        val (state, done) = MissionRules.apply(at(8), MissionSignal.State(level = 30, upgrades = mapOf("Generator" to 1), hasGenerator = false))

        done.map { it.xp } shouldBe listOf(200)
        state.trailIndex shouldBe 9
    }

    "diarias nao contam antes do fim da trilha" {
        val dailies = listOf(DailyMission("detritos_30", 0, 40, false))
        val start = PlayerMissions("u", 0, 0, "2026-10-25", dailies)

        val (state, _) = MissionRules.apply(start, MissionSignal.Debris(byHook = true))

        state.dailies shouldBe dailies
        state.trailProgress shouldBe 1
    }

    "com a trilha feita o sinal conta so na primeira diaria aberta que aceita" {
        val start = PlayerMissions(
            "u", trailDone, 0, "2026-10-25",
            listOf(
                DailyMission("receitas_3", 0, 40, false),
                DailyMission("detritos_30", 0, 40, false),
                DailyMission("gancho_15", 0, 40, false),
            ),
        )

        val (state, done) = MissionRules.apply(start, MissionSignal.Debris(byHook = true))

        done.shouldBeEmpty()
        state.dailies.map { it.progress } shouldBe listOf(0L, 1L, 0L)
    }

    "diaria concluida fica feita e devolve o xp guardado" {
        val start = PlayerMissions(
            "u", trailDone, 0, "2026-10-25",
            listOf(
                DailyMission("receitas_3", 2, 474, false),
                DailyMission("detritos_30", 0, 474, false),
                DailyMission("gancho_15", 0, 474, false),
            ),
        )

        val (state, done) = MissionRules.apply(start, MissionSignal.Crafted("rede"))

        done.map { it.mission.id to it.xp } shouldBe listOf("receitas_3" to 474)
        state.dailies[0] shouldBe DailyMission("receitas_3", 3, 474, true)

        val (again, _) = MissionRules.apply(state, MissionSignal.Crafted("rede"))
        again.dailies[0].progress shouldBe 3
    }

    "troca de dia so depois da trilha" {
        val today = LocalDate.of(2026, 10, 25)

        MissionRules.needsReset(PlayerMissions("u", trailDone, 0, "2026-10-24", emptyList()), today) shouldBe true
        MissionRules.needsReset(PlayerMissions("u", trailDone, 0, "2026-10-25", emptyList()), today) shouldBe false
        MissionRules.needsReset(PlayerMissions("u", 12, 0, "2026-10-24", emptyList()), today) shouldBe false
        MissionRules.needsReset(PlayerMissions("u", trailDone, 0, "", emptyList()), today) shouldBe true
    }

    "missao atual aponta para a trilha e depois para a primeira diaria aberta" {
        MissionRules.current(at(2, 5)) shouldBe MissionRules.Current(MissionTrail.all[2], 5, null)

        val dailies = PlayerMissions(
            "u", trailDone, 0, "2026-10-25",
            listOf(DailyMission("receitas_3", 3, 40, true), DailyMission("detritos_30", 7, 40, false)),
        )
        val current = MissionRules.current(dailies)
        current?.mission?.id shouldBe "detritos_30"
        current?.progress shouldBe 7
        current?.dailyIndex shouldBe 1

        MissionRules.current(PlayerMissions("u", trailDone, 0, "2026-10-25", emptyList())) shouldBe null
    }

    "pular conclui a atual sem xp" {
        MissionRules.skip(at(3, 0)).trailIndex shouldBe 4

        val dailies = PlayerMissions("u", trailDone, 0, "2026-10-25", listOf(DailyMission("receitas_3", 1, 40, false)))
        MissionRules.skip(dailies).dailies[0].done shouldBe true
    }

    "matar mobs conta um por morte so para o objetivo de mobs" {
        MissionRules.credit(MissionObjective.KillMobs(20), MissionSignal.MobKilled) shouldBe 1
        MissionRules.credit(MissionObjective.CraftAny(3), MissionSignal.MobKilled) shouldBe 0
        MissionRules.credit(MissionObjective.KillMobs(20), MissionSignal.Smelted(3)) shouldBe 0
    }

    "as diarias incluem matar mobs" {
        DailyPool.all.filter { it.objective is MissionObjective.KillMobs }.map { it.id } shouldBe listOf("mobs_20", "mobs_50")
    }
})
