package one_block.textura.text

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DefaultFontWidthsTest : StringSpec({

    "larguras reais da fonte padrão do cliente" {
        DefaultFontWidths.of('-'.code) shouldBe 6
        DefaultFontWidths.of('+'.code) shouldBe 6
        DefaultFontWidths.of('i'.code) shouldBe 2
        DefaultFontWidths.of(' '.code) shouldBe 4
        DefaultFontWidths.of('ã'.code) shouldBe 6
        DefaultFontWidths.of('❤'.code) shouldBe 8
        DefaultFontWidths.of(0xE000) shouldBe null
    }

    "a medição usa as larguras reais" {
        TextMetrics.calculateWidth("1-2") shouldBe 18
        TextMetrics.calculateWidth("Invasão") shouldBe 40
    }
})
