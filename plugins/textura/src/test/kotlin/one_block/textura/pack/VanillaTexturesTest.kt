package one_block.textura.pack

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class VanillaTexturesTest : StringSpec({

    fun lookup(name: String, present: Set<String>): String? = VanillaTextures.find(name) { it.takeIf(present::contains) }

    "procura a textura do item e depois as do bloco, na ordem" {
        VanillaTextures.candidates("loom") shouldContainExactly listOf(
            "item/loom", "block/loom", "block/loom_front", "block/loom_side", "block/loom_top",
        )
    }

    "item sem textura de item usa a frente do bloco" {
        lookup("loom", setOf("block/loom_front", "block/loom_side", "block/loom_top", "block/loom_bottom")) shouldBe "block/loom_front"
    }

    "sem frente usa a lateral e depois o topo" {
        lookup("x", setOf("block/x_side", "block/x_top")) shouldBe "block/x_side"
        lookup("x", setOf("block/x_top")) shouldBe "block/x_top"
    }

    "textura de item vence a de bloco e o bloco simples vence as faces" {
        lookup("fishing_rod", setOf("item/fishing_rod", "block/fishing_rod")) shouldBe "item/fishing_rod"
        lookup("stone", setOf("block/stone", "block/stone_side")) shouldBe "block/stone"
    }

    "nada encontrado devolve nulo" {
        lookup("nada", setOf("block/nada_bottom")).shouldBeNull()
    }
})
