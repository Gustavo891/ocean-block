package ocean_block.system.drift.debris

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val WATER: (Int) -> BlockKind = { BlockKind.WATER }

private fun column(vararg top: BlockKind, below: BlockKind = BlockKind.WATER): (Int) -> BlockKind =
    { depth -> top.getOrElse(depth) { below } }

private fun passage(
    depth: Int,
    levelDepth: Int,
    front: (Int) -> BlockKind,
    here: (Int) -> BlockKind = WATER,
    loaded: Boolean = true,
    maxDepth: Int = 3,
): Passage = DiveRules.passage(loaded, depth, levelDepth, maxDepth, here, front)

class PassageRulesTest : StringSpec({

    "coluna nao carregada vem antes de tudo" {
        passage(0, 0, WATER, loaded = false) shouldBe Passage.Unloaded
    }

    "sem agua no proprio nivel cai" {
        passage(0, 0, WATER, here = column(BlockKind.AIR)) shouldBe Passage.Unsupported
    }

    "mar normal segue na superficie" {
        passage(0, 0, WATER) shouldBe Passage.Open(0, true)
    }

    "borda do raft mergulha um bloco sem superficie" {
        passage(0, 0, column(BlockKind.SOLID)) shouldBe Passage.Open(1, false)
    }

    "ar a frente no nivel assentado faz cair" {
        passage(0, 0, column(BlockKind.AIR, BlockKind.AIR, below = BlockKind.AIR)) shouldBe Passage.Drop
    }

    "subindo de 1 para 0 em 62.2 contra borda drenada nao entra no raft" {
        val front = column(BlockKind.SOLID, BlockKind.AIR)
        val result = passage(depth = 1, levelDepth = 0, front = front)

        result shouldNotBe Passage.Drop
        result shouldBe Passage.Open(2, false)
    }

    "descendo de 0 para 1 em 61.9 contra bloco solido nao entra nele" {
        val front = column(BlockKind.AIR, BlockKind.SOLID)
        val result = passage(depth = 0, levelDepth = 1, front = front)

        result shouldNotBe Passage.Drop
        result shouldBe Passage.Open(2, false)
    }

    "no meio do mergulho com ar no bloco da altura atual cai" {
        passage(depth = 0, levelDepth = 1, front = column(BlockKind.AIR, BlockKind.AIR)) shouldBe Passage.Drop
    }

    "sem agua alcancavel fica bloqueado" {
        passage(0, 0, column(below = BlockKind.SOLID)) shouldBe Passage.Blocked
    }

    "poco fundo nao le alem de profundidade maxima mais 4" {
        val asked = mutableListOf<Int>()
        val front: (Int) -> BlockKind = { depth -> asked += depth; if (depth >= 30) BlockKind.WATER else BlockKind.AIR }

        passage(depth = 30, levelDepth = 30, front = front, here = { if (it >= 30) BlockKind.WATER else BlockKind.AIR }) shouldBe
            Passage.Blocked
        asked.filter { it != 30 }.forEach { it shouldBeLessThanOrEqual 7 }
    }

    "cache de coluna classifica cada bloco uma vez" {
        var reads = 0
        val kinds = ColumnKinds { reads++; BlockKind.WATER }

        repeat(4) { kinds.at(62) }
        kinds.at(61)

        reads shouldBe 2
    }

    "apoio no chao usa o bloco onde pousou mesmo com escala grande" {
        GravityRules.restingSupport(61, { false }, { it == 60 }) shouldBe Support.GROUND
        GravityRules.restingSupport(61, { it == 61 }, { it == 60 }) shouldBe Support.FLOAT
        GravityRules.restingSupport(61, { false }, { false }) shouldBe Support.FALL
    }
})
