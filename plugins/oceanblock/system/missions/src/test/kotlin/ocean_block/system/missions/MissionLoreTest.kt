package ocean_block.system.missions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.dialog.MissionLore
import ocean_block.system.missions.model.DailyMission
import ocean_block.system.missions.model.PlayerMissions
import java.time.LocalDateTime

class MissionLoreTest : StringSpec({

    "trilha no inicio mostra a missao 1 com contador e xp" {
        val trail = MissionLore.trail(PlayerMissions("u", 0, 3, "", emptyList()))

        trail["titulo"] shouldBe "Missão 1 de 13"
        trail["texto"] shouldBe "Colete 10 detritos usando o gancho"
        trail["contador"] shouldBe "3/10"
        trail["xp"] shouldBe "+50 XP"
        trail["barra"] shouldBe "30"
    }

    "trilha completa avisa que acabou" {
        val trail = MissionLore.trail(PlayerMissions("u", MissionTrail.size, 0, "2026-10-25", emptyList()))

        trail["titulo"] shouldBe "Trilha concluída!"
        trail["barra"] shouldBe "100"
    }

    "diaria em andamento mostra o progresso" {
        val daily = MissionLore.daily(DailyMission("gerador_64", 16, 474, false))

        daily["texto"] shouldBe "Quebre 64 blocos do gerador"
        daily["contador"] shouldBe "16/64"
        daily["xp"] shouldBe "+474 XP"
        daily["barra"] shouldBe "25"
        daily["feito"] shouldBe "false"
    }

    "diaria feita fica cheia" {
        val daily = MissionLore.daily(DailyMission("receitas_3", 3, 40, true))

        daily["feito"] shouldBe "true"
        daily["barra"] shouldBe "100"
        daily["estado"] shouldBe "Concluída"
    }

    "renovacao conta ate a meia-noite" {
        MissionLore.resetIn(LocalDateTime.of(2026, 10, 25, 23, 15)) shouldBe "Renovam em 00:45"
        MissionLore.resetIn(LocalDateTime.of(2026, 10, 25, 9, 30)) shouldBe "Renovam em 14:30"
    }
})
