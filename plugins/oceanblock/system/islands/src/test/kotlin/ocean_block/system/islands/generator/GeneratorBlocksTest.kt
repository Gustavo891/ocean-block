package ocean_block.system.islands.generator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GeneratorBlocksTest : StringSpec({

    "consume de posicao nao marcada e falso" {
        GeneratorBlockSet().consume(BlockKey("world", 1, 64, 2)) shouldBe false
    }

    "mark e consume e verdadeiro uma vez so" {
        val blocks = GeneratorBlockSet()
        val key = BlockKey("world", 1, 64, 2)
        blocks.mark(key)

        blocks.consume(key) shouldBe true
        blocks.consume(key) shouldBe false
    }

    "forget tira a marca" {
        val blocks = GeneratorBlockSet()
        val key = BlockKey("world", 1, 64, 2)
        blocks.mark(key)
        blocks.forget(key)

        blocks.consume(key) shouldBe false
    }

    "mundos diferentes nao colidem" {
        val blocks = GeneratorBlockSet()
        blocks.mark(BlockKey("world", 1, 64, 2))

        blocks.consume(BlockKey("ilhas", 1, 64, 2)) shouldBe false
        blocks.consume(BlockKey("world", 1, 64, 2)) shouldBe true
    }
})
