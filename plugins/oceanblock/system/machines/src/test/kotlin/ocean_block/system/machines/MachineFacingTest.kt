package ocean_block.system.machines

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MachineFacingTest : StringSpec({

    "a frente fica virada para quem colocou" {
        MachineFacing.frontFor("NORTH") shouldBe MachineFacing.SOUTH
        MachineFacing.frontFor("SOUTH") shouldBe MachineFacing.NORTH
        MachineFacing.frontFor("EAST") shouldBe MachineFacing.WEST
        MachineFacing.frontFor("WEST") shouldBe MachineFacing.EAST
        MachineFacing.frontFor("UP") shouldBe MachineFacing.NORTH
    }

    "cada frente tem a sua variante no Textura" {
        MachineFacing.NORTH.textureId("gerador_energia") shouldBe "gerador_energia"
        MachineFacing.EAST.textureId("gerador_energia") shouldBe "gerador_energia_leste"
        MachineFacing.SOUTH.textureId("gerador_energia") shouldBe "gerador_energia_sul"
        MachineFacing.WEST.textureId("gerador_energia") shouldBe "gerador_energia_oeste"
    }

    "counter guarda a frente e sem counter vale o norte" {
        MachineFacing.fromCounter(null) shouldBe MachineFacing.NORTH
        MachineFacing.fromCounter(MachineFacing.WEST.ordinal.toLong()) shouldBe MachineFacing.WEST
        MachineFacing.fromCounter(9) shouldBe MachineFacing.NORTH
    }
})
