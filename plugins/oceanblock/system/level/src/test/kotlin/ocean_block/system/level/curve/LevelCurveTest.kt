package ocean_block.system.level.curve

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import ocean_block.system.scoreboard.level.LevelProgress

class LevelCurveTest : StringSpec({

    "xp para o proximo nivel segue 100 vezes n elevado a 1,5" {
        LevelCurve.toNext(1) shouldBe 100L
        LevelCurve.toNext(10) shouldBe 3162L
        LevelCurve.toNext(49) shouldBe 34300L
    }

    "total para chegar a um nivel soma os degraus anteriores" {
        LevelCurve.totalFor(1) shouldBe 0L
        LevelCurve.totalFor(2) shouldBe 100L
        LevelCurve.totalFor(3) shouldBe 100L + LevelCurve.toNext(2)
    }

    "sem xp e nivel 1 com barra vazia" {
        LevelCurve.progress(0) shouldBe LevelProgress(1, 0.0)
    }

    "xp no meio de um nivel vira fracao do degrau" {
        val progress = LevelCurve.progress(150)

        progress.level shouldBe 2
        progress.progress shouldBe (50.0 / LevelCurve.toNext(2)).plusOrMinus(1e-9)
    }

    "nivel 50 e o teto, mesmo com xp sobrando" {
        LevelCurve.progress(LevelCurve.totalFor(50)) shouldBe LevelProgress(50, 1.0)
        LevelCurve.progress(LevelCurve.totalFor(50) + 999_999) shouldBe LevelProgress(50, 1.0)
        LevelCurve.MAX_LEVEL shouldBe 50
    }

    "xp negativo conta como nivel 1" {
        LevelCurve.levelOf(-5) shouldBe 1
    }
})
