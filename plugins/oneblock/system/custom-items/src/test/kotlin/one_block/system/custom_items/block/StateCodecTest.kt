package one_block.system.custom_items.block

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StateCodecTest : StringSpec({

    "upgrades fazem round-trip" {
        val upgrades = mapOf("vacuum_radius" to 2, "vacuum_storage" to 3)
        StateCodec.decodeUpgrades(StateCodec.encodeUpgrades(upgrades)) shouldBe upgrades
    }

    "mapa vazio vira string vazia e volta vazio" {
        StateCodec.encodeUpgrades(emptyMap()) shouldBe ""
        StateCodec.decodeUpgrades("") shouldBe mutableMapOf()
    }

    "entrada malformada de upgrade é ignorada" {
        StateCodec.decodeUpgrades("vacuum_radius:2;lixo;vacuum_speed:x;vacuum_storage:1") shouldBe
            mutableMapOf("vacuum_radius" to 2, "vacuum_storage" to 1)
    }

    "filtro faz round-trip preservando os nomes" {
        val filter = setOf("COBBLESTONE", "DIRT")
        StateCodec.decodeFilter(StateCodec.encodeFilter(filter)) shouldBe filter
    }

    "filtro vazio não gera entrada fantasma" {
        StateCodec.decodeFilter("") shouldBe mutableSetOf<String>()
    }

    "storage usa quebra de linha e ignora entradas em branco" {
        val entries = listOf("YWJj", "ZGVm")
        StateCodec.encodeStorage(entries) shouldBe "YWJj\nZGVm"
        StateCodec.decodeStorage("YWJj\n\nZGVm") shouldBe mutableListOf("YWJj", "ZGVm")
    }
})
