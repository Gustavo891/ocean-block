package ocean_block.system.pirate_raid.text

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import ocean_block.system.pirate_raid.notify.RaidCard
import ocean_block.system.pirate_raid.notify.ToastLine

class RaidTextTest : FunSpec({

    val done = "<glyph:hud_pirata_onda_feita>"
    val next = "<glyph:hud_pirata_onda_vazia>"
    val swords = "<glyph:hud_pirata_espadas>"
    val clock = "<glyph:hud_pirata_ampulheta>"

    test("linha do HUD com bolinhas das ondas, vivos e tempo") {
        RaidHudText.line(2, 3, 5, 192) shouldBe
            "<red>Invasão</red>   <white>$done $done $next</white>   <white>$swords 5</white>   <white>$clock</white> <yellow>3:12</yellow>"
    }

    test("última onda enche todas as bolinhas e segundos ganham zero") {
        RaidHudText.line(3, 3, 1, 5) shouldBe
            "<red>Invasão</red>   <white>$done $done $done</white>   <white>$swords 1</white>   <white>$clock</white> <yellow>0:05</yellow>"
    }

    test("tempo negativo vira zero") {
        RaidHudText.line(1, 3, 4, -3) shouldBe
            "<red>Invasão</red>   <white>$done $next $next</white>   <white>$swords 4</white>   <white>$clock</white> <yellow>0:00</yellow>"
    }

    test("vida no nome arredonda para cima e não fica negativa") {
        RaidHudText.health(17.2, 24.0, false) shouldBe "<red>❤</red> <white>18/24</white>"
        RaidHudText.health(80.0, 80.0, true) shouldBe "<gold>❤</gold> <white>80/80</white>"
        RaidHudText.health(-1.0, 24.0, false) shouldBe "<red>❤</red> <white>0/24</white>"
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
