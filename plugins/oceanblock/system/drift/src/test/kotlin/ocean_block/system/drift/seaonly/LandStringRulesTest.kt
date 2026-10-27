package ocean_block.system.drift.seaonly

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.bukkit.Material
import org.bukkit.entity.EntityType

class LandStringRulesTest : StringSpec({

    "linha e o unico item exclusivo do mar" {
        LandStringRules.isSeaOnly(Material.STRING) shouldBe true
        LandStringRules.isSeaOnly(Material.SPIDER_EYE) shouldBe false
        LandStringRules.isSeaOnly(Material.COBWEB) shouldBe false
    }

    "aranha e aranha da caverna perdem a linha, outros mobs nao" {
        LandStringRules.stripsMobDrops(EntityType.SPIDER) shouldBe true
        LandStringRules.stripsMobDrops(EntityType.CAVE_SPIDER) shouldBe true
        LandStringRules.stripsMobDrops(EntityType.ZOMBIE) shouldBe false
        LandStringRules.stripsMobDrops(EntityType.DROWNED) shouldBe false
    }

    "so a teia perde a linha ao quebrar" {
        LandStringRules.stripsBlockDrops(Material.COBWEB) shouldBe true
        LandStringRules.stripsBlockDrops(Material.WHITE_WOOL) shouldBe false
    }

    "presente de gato com linha e bloqueado, outro presente passa" {
        LandStringRules.blocksGift(EntityType.CAT, Material.STRING) shouldBe true
        LandStringRules.blocksGift(EntityType.CAT, Material.RABBIT_HIDE) shouldBe false
        LandStringRules.blocksGift(EntityType.ZOMBIE, Material.STRING) shouldBe false
    }

    "loot de pesca mantem a linha, baus de estrutura nao" {
        LandStringRules.stripsLoot("gameplay/fishing/junk") shouldBe false
        LandStringRules.stripsLoot("gameplay/fishing") shouldBe false
        LandStringRules.stripsLoot("chests/simple_dungeon") shouldBe true
        LandStringRules.stripsLoot("gameplay/cat_morning_gift") shouldBe true
    }

    "remover itens do mar preserva a ordem do resto" {
        val loot = listOf(Material.BONE, Material.STRING, Material.BREAD, Material.STRING)

        LandStringRules.withoutSeaOnly(loot) { it } shouldBe listOf(Material.BONE, Material.BREAD)
    }
})
