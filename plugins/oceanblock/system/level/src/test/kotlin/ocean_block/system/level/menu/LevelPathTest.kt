package ocean_block.system.level.menu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs

class LevelPathTest : StringSpec({

    "o caminho tem 25 casas distintas nas 5 primeiras linhas" {
        LevelPath.SLOTS.size shouldBe 25
        LevelPath.SLOTS.shouldBeUnique()
        LevelPath.SLOTS.forEach { it shouldBeLessThan 45 }
    }

    "cada casa encosta na anterior, em serpente como no print" {
        LevelPath.SLOTS.zipWithNext().forEach { (a, b) ->
            abs(a / 9 - b / 9) + abs(a % 9 - b % 9) shouldBe 1
        }
        LevelPath.SLOTS.first() shouldBe 0
        LevelPath.SLOTS[11] shouldBe 3
        LevelPath.SLOTS[12] shouldBe 4
        LevelPath.SLOTS.last() shouldBe 8
    }

    "duas paginas cobrem os 50 niveis" {
        LevelPath.pages() shouldBe 2
        LevelPath.levelAt(0, 0) shouldBe 1
        LevelPath.levelAt(0, 24) shouldBe 25
        LevelPath.levelAt(1, 0) shouldBe 26
        LevelPath.levelAt(1, 24) shouldBe 50
    }

    "a pagina inicial e a do nivel do jogador" {
        LevelPath.pageOf(1) shouldBe 0
        LevelPath.pageOf(25) shouldBe 0
        LevelPath.pageOf(26) shouldBe 1
        LevelPath.pageOf(50) shouldBe 1
    }
})
