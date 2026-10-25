package ocean_block.system.drift.net

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID

private fun net(x: Double, y: Double = 0.0, z: Double = 0.0, full: Boolean = false) =
    NetPoint(UUID.randomUUID(), x, y, z, full)

class NetRulesTest : StringSpec({

    "rede fora do raio nao coleta" {
        NetRules.nearest(0.0, 0.0, 0.0, listOf(net(3.5)), 3.0).shouldBeNull()
    }

    "sem redes retorna null" {
        NetRules.nearest(0.0, 0.0, 0.0, emptyList(), 3.0).shouldBeNull()
    }

    "distancia conta as tres dimensoes" {
        NetRules.nearest(0.0, 0.0, 0.0, listOf(net(2.0, 2.0, 2.0)), 3.0).shouldBeNull()
    }

    "rede exatamente no raio coleta" {
        val edge = net(0.0, 3.0)
        NetRules.nearest(0.0, 0.0, 0.0, listOf(edge), 3.0) shouldBe edge
    }

    "escolhe a rede mais proxima" {
        val far = net(2.5)
        val near = net(0.0, 0.0, -1.0)
        NetRules.nearest(0.0, 0.0, 0.0, listOf(far, near), 3.0) shouldBe near
    }

    "ignora rede cheia mesmo mais proxima" {
        val full = net(0.5, full = true)
        val free = net(2.0)
        NetRules.nearest(0.0, 0.0, 0.0, listOf(full, free), 3.0) shouldBe free
    }

    "todas cheias retorna null" {
        NetRules.nearest(0.0, 0.0, 0.0, listOf(net(1.0, full = true), net(2.0, full = true)), 3.0).shouldBeNull()
    }

    "empate fica com a primeira da lista" {
        val first = net(2.0)
        val second = net(-2.0)
        NetRules.nearest(0.0, 0.0, 0.0, listOf(first, second), 3.0) shouldBe first
        NetRules.nearest(0.0, 0.0, 0.0, listOf(second, first), 3.0) shouldBe second
    }

    "armazenamento vazio nao esta cheio" {
        NetRules.isFull(0, 27) shouldBe false
    }

    "um slot livre nao esta cheio" {
        NetRules.isFull(26, 27) shouldBe false
    }

    "sem slot livre esta cheio mesmo com pilhas incompletas" {
        NetRules.isFull(27, 27) shouldBe true
    }

    "mais pilhas que a capacidade esta cheio" {
        NetRules.isFull(30, 27) shouldBe true
    }

    "rede antiga em tear e adotada como movel, o resto nao" {
        NetRules.adopts("LOOM") shouldBe true
        NetRules.adopts("BARRIER") shouldBe false
        NetRules.adopts("AIR") shouldBe false
    }
})
