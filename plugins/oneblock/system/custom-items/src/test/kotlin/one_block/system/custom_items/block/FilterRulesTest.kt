package one_block.system.custom_items.block

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FilterRulesTest : StringSpec({

    "lista vazia aceita tudo em qualquer modo" {
        FilterRules.accepts(FilterMode.WHITELIST, emptySet(), "COBBLESTONE") shouldBe true
        FilterRules.accepts(FilterMode.BLACKLIST, emptySet(), "COBBLESTONE") shouldBe true
    }

    "whitelist aceita só o que está listado" {
        val filter = setOf("DIAMOND", "EMERALD")
        FilterRules.accepts(FilterMode.WHITELIST, filter, "DIAMOND") shouldBe true
        FilterRules.accepts(FilterMode.WHITELIST, filter, "COBBLESTONE") shouldBe false
    }

    "blacklist rejeita o que está listado" {
        val filter = setOf("COBBLESTONE")
        FilterRules.accepts(FilterMode.BLACKLIST, filter, "COBBLESTONE") shouldBe false
        FilterRules.accepts(FilterMode.BLACKLIST, filter, "DIAMOND") shouldBe true
    }

    "next alterna entre os dois modos e volta" {
        FilterMode.WHITELIST.next() shouldBe FilterMode.BLACKLIST
        FilterMode.BLACKLIST.next() shouldBe FilterMode.WHITELIST
    }
})
