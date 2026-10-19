package one_block.textura.dialog.json

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SlotContentTest : StringSpec({

    val entry = mapOf("item" to "minecraft:Fishing_Rod", "qtd" to "4", "zero" to "0", "vazio" to "")

    fun expand(text: String): String = Regex("""\{([a-z]+)}""").replace(text) { entry[it.groupValues[1]] ?: it.value }

    "item e count vem da entrada" {
        val slot = SlotContent.resolve(DefSlotItem(2, "{item}", "{qtd}", "<yellow>{nome}", listOf("{custo}")), ::expand)

        slot shouldBe SlotView(2, "fishing_rod", 4, "<yellow>{nome}", listOf("{custo}"))
    }

    "item fixo continua normalizado como antes" {
        SlotContent.resolve(DefSlotItem(0, "minecraft:Diamond_Sword", "1", null, emptyList()), ::expand) shouldBe
            SlotView(0, "diamond_sword", 1, null, emptyList())
    }

    "count invalido ou menor que 1 vira 1" {
        SlotContent.resolve(DefSlotItem(0, "stone", "{zero}", null, emptyList()), ::expand)?.count shouldBe 1
        SlotContent.resolve(DefSlotItem(0, "stone", "{faltando}", null, emptyList()), ::expand)?.count shouldBe 1
    }

    "count acima de 99 e limitado a 99" {
        val big = mapOf("muito" to "500")

        SlotContent.resolve(DefSlotItem(0, "stone", "{muito}", null, emptyList())) { text -> big[text.trim('{', '}')] ?: text }?.count shouldBe 99
        SlotContent.resolve(DefSlotItem(0, "stone", "99", null, emptyList()), ::expand)?.count shouldBe 99
    }

    "item vazio ou sem valor deixa o slot vazio" {
        SlotContent.resolve(DefSlotItem(0, "{vazio}", "1", null, emptyList()), ::expand) shouldBe null
        SlotContent.resolve(DefSlotItem(0, "{faltando}", "1", null, emptyList()), ::expand) shouldBe null
    }
})
