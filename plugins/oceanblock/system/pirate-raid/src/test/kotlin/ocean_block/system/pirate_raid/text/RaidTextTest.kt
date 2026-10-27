package ocean_block.system.pirate_raid.text

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.pirate_raid.notify.RaidCard
import ocean_block.system.pirate_raid.notify.ToastLine

class RaidTextTest : FunSpec({

    test("linha do HUD") {
        RaidHudText.line(2, 3, 5, 192) shouldBe "Invasão pirata · Onda 2/3 · 5 piratas · 3:12"
    }

    test("singular e segundos com zero") {
        RaidHudText.line(3, 3, 1, 5) shouldBe "Invasão pirata · Onda 3/3 · 1 pirata · 0:05"
    }

    test("tempo negativo vira zero") {
        RaidHudText.line(1, 3, 4, -3) shouldBe "Invasão pirata · Onda 1/3 · 4 piratas · 0:00"
    }

    test("cartões de início, vitória e fuga") {
        RaidCard.started() shouldBe listOf(
            ToastLine("<red>Piratas à vista!</red>", "glyph:toast_pirata"),
            ToastLine("<white>Defenda sua ilha</white>", "glyph:toast_linha"),
        )
        RaidCard.won(12000) shouldBe listOf(
            ToastLine("<red>Raid vencida!</red>", "glyph:toast_pirata"),
            ToastLine("<white>+12.000 Coins</white>", "glyph:toast_linha"),
        )
        RaidCard.fled() shouldBe listOf(
            ToastLine("<red>Os piratas fugiram</red>", "glyph:toast_pirata"),
            ToastLine("<white>Mais sorte na próxima</white>", "glyph:toast_linha"),
        )
    }
})
