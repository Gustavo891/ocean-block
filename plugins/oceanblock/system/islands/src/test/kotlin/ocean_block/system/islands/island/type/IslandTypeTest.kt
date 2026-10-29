package ocean_block.system.islands.island.type

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class IslandTypeTest : StringSpec({
    "id conhecido devolve o tipo" {
        IslandType.fromId("gelo") shouldBe IslandType.GELO
        IslandType.fromId("deserto") shouldBe IslandType.DESERTO
        IslandType.fromId("jangada") shouldBe IslandType.JANGADA
        IslandType.fromId("padrao") shouldBe IslandType.PADRAO
    }

    "id desconhecido ou nulo vira padrao" {
        IslandType.fromId("x") shouldBe IslandType.PADRAO
        IslandType.fromId(null) shouldBe IslandType.PADRAO
    }

    "ordem dos tipos e a da tela" {
        IslandType.entries.map { it.id } shouldBe listOf("padrao", "deserto", "gelo", "jangada")
    }

    "so deserto e gelo pintam bioma" {
        IslandType.PADRAO.biome shouldBe null
        IslandType.JANGADA.biome shouldBe null
        IslandType.DESERTO.biome shouldBe "desert"
        IslandType.GELO.biome shouldBe "snowy_plains"
    }

    "nomes e descricoes da spec" {
        IslandType.PADRAO.nome shouldBe "Padrão"
        IslandType.JANGADA.nome shouldBe "Jangada"
        IslandType.PADRAO.descricao shouldBe "A ilha de sempre: grama, árvore e lava."
        IslandType.DESERTO.descricao shouldBe "Areia, arenito e cactos sob o sol."
        IslandType.GELO.descricao shouldBe "Neve, abetos e gelo que não derrete."
        IslandType.JANGADA.descricao shouldBe "Só três por três de tábuas. Para quem quer o desafio."
    }
})
