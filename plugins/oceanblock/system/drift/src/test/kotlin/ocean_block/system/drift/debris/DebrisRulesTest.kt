package ocean_block.system.drift.debris

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.bukkit.Material

class DebrisRulesTest : StringSpec({

    "agua e ar deixam passar, bloco do raft trava" {
        DebrisRules.isPassable(Material.WATER) shouldBe true
        DebrisRules.isPassable(Material.AIR) shouldBe true
        DebrisRules.isPassable(Material.CAVE_AIR) shouldBe true
        DebrisRules.isPassable(Material.OAK_PLANKS) shouldBe false
        DebrisRules.isPassable(Material.KELP) shouldBe false
    }

    "passo de movimento vale dois ticks" {
        DebrisRules.stepDistance(0.05) shouldBe (0.1 plusOrMinus 1e-9)
    }

    "detrito solto nunca expira" {
        DebrisRules.stuckExpired(null, 1_000_000L, 60) shouldBe false
    }

    "preso expira ao completar o tempo" {
        DebrisRules.stuckExpired(0L, 59_999L, 60) shouldBe false
        DebrisRules.stuckExpired(0L, 60_000L, 60) shouldBe true
    }

    "puxao chega ao jogador em dez ticks" {
        var ticksLeft = DebrisRules.PULL_TICKS
        var remaining = 1.0
        var steps = 0

        while (ticksLeft > 0) {
            remaining *= 1.0 - DebrisRules.pullFraction(ticksLeft)
            ticksLeft -= DebrisRules.TICKS_PER_STEP
            steps++
        }

        steps shouldBe 5
        remaining shouldBe (0.0 plusOrMinus 1e-9)
    }

    "fracao do puxao cresce a cada passo" {
        DebrisRules.pullFraction(10) shouldBe (0.2 plusOrMinus 1e-9)
        DebrisRules.pullFraction(4) shouldBe (0.5 plusOrMinus 1e-9)
        DebrisRules.pullFraction(2) shouldBe 1.0
        DebrisRules.pullFraction(1) shouldBe 1.0
    }
})
