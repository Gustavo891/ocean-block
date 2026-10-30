package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
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

    "veio de ferro so conta minerio de ferro" {
        val (state, _) = feed(at(12), MissionSignal.GeneratorBlock("COBBLESTONE"), MissionSignal.GeneratorBlock("IRON_ORE"))

        state.trailIndex shouldBe 12
        state.trailProgress shouldBe 1
    }

    "primeiro comercio soma as vendas" {
        val (state, done) = feed(at(8), MissionSignal.Sold(350), MissionSignal.Sold(700))

        done.map { it.xp } shouldBe listOf(150)
        state.trailIndex shouldBe 9
    }

    "caca ao tesouro so conta barris" {
        val (state, done) = feed(
            at(9),
            MissionSignal.Debris(byHook = true, type = "sucata"),
            MissionSignal.Debris(byHook = false, type = "barril"),
            MissionSignal.Debris(byHook = true, type = "barril"),
        )

        done.map { it.mission.id } shouldBe listOf("barril_2")
        state.trailIndex shouldBe 10
    }

    "derreter soma os lingotes ate 10" {
        val (state, done) = feed(at(5), MissionSignal.Smelted(4), MissionSignal.Smelted(6))

        done.map { it.mission.id } shouldBe listOf("derreter_10")
        state.trailIndex shouldBe 6
    }

    "estado cumprido conclui so a missao de estado e para na de receita" {
        val (state, done) = MissionRules.apply(at(3), MissionSignal.State(level = 30, upgrades = mapOf("Generator" to 1), hasGenerator = false))

        done.map { it.mission.id } shouldBe listOf(MissionTrail.all[3].id)
        state.trailIndex shouldBe 4
        state.trailProgress shouldBe 0
    }

    "nivel abaixo do pedido segue aberto" {
        val (state, done) = MissionRules.apply(at(10), MissionSignal.State(level = 4, upgrades = null, hasGenerator = false))

        done.shouldBeEmpty()
        state shouldBe at(10)
    }

    "marinheiro conclui ao chegar no nivel 5 mesmo sem ilha" {
        val (state, done) = MissionRules.apply(at(10), noIsland)

        done.map { it.xp } shouldBe listOf(150)
        state.trailIndex shouldBe 11
    }

    "sem diaria escolhida, na trilha so a trilha conta" {
        val dailies = listOf(DailyMission("detritos_30", 0, 40, false))
        val start = PlayerMissions("u", 0, 0, "2026-10-25", dailies)

        val (state, _) = MissionRules.apply(start, MissionSignal.Debris(byHook = true))

        state.dailies shouldBe dailies
        state.trailProgress shouldBe 1
    }

    "na trilha a diaria escolhida avanca junto e aparece no hud" {
        val dailies = listOf(DailyMission("gancho_15", 0, 40, false), DailyMission("receitas_3", 0, 40, false))
        val start = PlayerMissions("u", 0, 0, "2026-10-25", dailies, "gancho_15")

        val (state, _) = MissionRules.apply(start, MissionSignal.Debris(byHook = true))

        state.trailProgress shouldBe 1
        state.dailies[0].progress shouldBe 1
        MissionRules.current(state)?.mission?.id shouldBe "gancho_15"
        MissionRules.current(state.copy(activeDaily = null))?.mission?.id shouldBe MissionTrail.all[0].id
        MissionRules.choose(start.copy(activeDaily = null), "receitas_3")?.activeDaily shouldBe "receitas_3"
    }

    "pular conclui a diaria escolhida antes da trilha" {
        val start = PlayerMissions("u", 2, 0, "2026-10-25", listOf(DailyMission("gancho_15", 0, 40, false)), "gancho_15")

        MissionRules.skip(start).let {
            it.trailIndex shouldBe 2
            it.dailies[0].done shouldBe true
        }
        MissionRules.skip(start.copy(activeDaily = null)).trailIndex shouldBe 3
    }

    val three = listOf(
        DailyMission("receitas_3", 0, 40, false),
        DailyMission("detritos_30", 0, 40, false),
        DailyMission("gancho_15", 0, 40, false),
    )

    "sem diaria escolhida nenhum sinal conta" {
        val start = PlayerMissions("u", trailDone, 0, "2026-10-25", three)

        val (state, done) = MissionRules.apply(start, MissionSignal.Debris(byHook = true))

        done.shouldBeEmpty()
        state shouldBe start
    }

    "so a diaria escolhida avanca" {
        val start = PlayerMissions("u", trailDone, 0, "2026-10-25", three, "gancho_15")

        val (state, done) = MissionRules.apply(start, MissionSignal.Debris(byHook = true))

        done.shouldBeEmpty()
        state.dailies.map { it.progress } shouldBe listOf(0L, 0L, 1L)
    }

    "diaria concluida fica feita, devolve o xp guardado e libera a escolha" {
        val start = PlayerMissions(
            "u", trailDone, 0, "2026-10-25",
            listOf(
                DailyMission("receitas_3", 2, 474, false),
                DailyMission("detritos_30", 0, 474, false),
                DailyMission("gancho_15", 0, 474, false),
            ),
            "receitas_3",
        )

        val (state, done) = MissionRules.apply(start, MissionSignal.Crafted("rede"))

        done.map { it.mission.id to it.xp } shouldBe listOf("receitas_3" to 474)
        state.dailies[0] shouldBe DailyMission("receitas_3", 3, 474, true)
        state.activeDaily shouldBe null

        val (again, _) = MissionRules.apply(state, MissionSignal.Crafted("rede"))
        again.dailies[0].progress shouldBe 3
    }

    "escolher so aceita diaria aberta de hoje com a trilha feita" {
        val start = PlayerMissions(
            "u", trailDone, 0, "2026-10-25",
            listOf(DailyMission("receitas_3", 3, 40, true), DailyMission("detritos_30", 4, 40, false)),
        )

        MissionRules.choose(start, "detritos_30")?.activeDaily shouldBe "detritos_30"
        MissionRules.choose(start, "receitas_3") shouldBe null
        MissionRules.choose(start, "gancho_15") shouldBe null
        MissionRules.choose(at(2), "detritos_30") shouldBe null
    }

    "trocar a escolhida guarda o progresso da anterior" {
        val start = PlayerMissions("u", trailDone, 0, "2026-10-25", three, "detritos_30")
        val (progressed, _) = MissionRules.apply(start, MissionSignal.Debris(byHook = false))

        val switched = MissionRules.choose(progressed, "receitas_3").shouldNotBeNull()

        switched.activeDaily shouldBe "receitas_3"
        switched.dailies.map { it.progress } shouldBe listOf(0L, 1L, 0L)
        MissionRules.choose(switched, "detritos_30")?.let { MissionRules.current(it)?.progress } shouldBe 1
    }

    "precisa escolher quando a trilha acabou, nao ha escolhida e sobra diaria" {
        MissionRules.needsChoice(PlayerMissions("u", trailDone, 0, "2026-10-25", three)) shouldBe true
        MissionRules.needsChoice(PlayerMissions("u", trailDone, 0, "2026-10-25", three, "gancho_15")) shouldBe false
        MissionRules.needsChoice(PlayerMissions("u", trailDone, 0, "2026-10-25", three.map { it.copy(done = true) })) shouldBe false
        MissionRules.needsChoice(at(2)) shouldBe false
    }

    "troca de dia vale mesmo durante a trilha" {
        val today = LocalDate.of(2026, 10, 25)

        MissionRules.needsReset(PlayerMissions("u", trailDone, 0, "2026-10-24", emptyList()), today) shouldBe true
        MissionRules.needsReset(PlayerMissions("u", trailDone, 0, "2026-10-25", emptyList()), today) shouldBe false
        MissionRules.needsReset(PlayerMissions("u", 12, 0, "2026-10-24", emptyList()), today) shouldBe true
        MissionRules.needsReset(PlayerMissions("u", trailDone, 0, "", emptyList()), today) shouldBe true
    }

    "missao atual aponta para a trilha e depois para a diaria escolhida" {
        MissionRules.current(at(2, 5)) shouldBe MissionRules.Current(MissionTrail.all[2], 5, null)

        val list = listOf(DailyMission("receitas_3", 3, 40, true), DailyMission("detritos_30", 7, 40, false))
        val chosen = PlayerMissions("u", trailDone, 0, "2026-10-25", list, "detritos_30")
        val current = MissionRules.current(chosen)
        current?.mission?.id shouldBe "detritos_30"
        current?.progress shouldBe 7
        current?.dailyIndex shouldBe 1

        MissionRules.current(chosen.copy(activeDaily = null)) shouldBe null
        MissionRules.current(PlayerMissions("u", trailDone, 0, "2026-10-25", emptyList())) shouldBe null
    }

    "pular conclui a atual sem xp" {
        MissionRules.skip(at(3, 0)).trailIndex shouldBe 4

        val dailies = PlayerMissions("u", trailDone, 0, "2026-10-25", listOf(DailyMission("receitas_3", 1, 40, false)), "receitas_3")
        val skipped = MissionRules.skip(dailies)
        skipped.dailies[0].done shouldBe true
        skipped.activeDaily shouldBe null
        MissionRules.skip(dailies.copy(activeDaily = null)) shouldBe dailies.copy(activeDaily = null)
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
