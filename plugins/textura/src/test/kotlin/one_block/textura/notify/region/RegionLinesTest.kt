package one_block.textura.notify.region

import one_block.textura.notify.NotificationLine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import one_block.textura.notify.region.RegionLines

class RegionLinesTest : StringSpec({

    fun texts(lines: RegionLines) = lines.lines().map { it.line.text }

    "linhas ficam na ordem em que chegaram" {
        val lines = RegionLines(3)
        lines.push(NotificationLine("a", 20), 0)
        lines.push(NotificationLine("b", 20), 1)
        lines.push(NotificationLine("c", 20), 2)
        texts(lines).shouldContainExactly("a", "b", "c")
    }

    "região cheia descarta a mais antiga" {
        val lines = RegionLines(2)
        lines.push(NotificationLine("a", 20), 0)
        lines.push(NotificationLine("b", 20), 1)
        lines.push(NotificationLine("c", 20), 2)
        texts(lines).shouldContainExactly("b", "c")
    }

    "mesma chave substitui a linha no lugar e reinicia o tempo" {
        val lines = RegionLines(3)
        lines.push(NotificationLine("+1 Tábua", null, 60, "coleta:oak_planks"), 0)
        lines.push(NotificationLine("+1 Linha", null, 60, "coleta:string"), 5)
        lines.push(NotificationLine("+2 Tábua", null, 60, "coleta:oak_planks"), 40)

        texts(lines).shouldContainExactly("+2 Tábua", "+1 Linha")
        lines.lines().first().expiresAt shouldBe 100
    }

    "linhas sem chave nunca se substituem" {
        val lines = RegionLines(3)
        lines.push(NotificationLine("x", 20), 0)
        lines.push(NotificationLine("x", 20), 0)
        texts(lines).shouldContainExactly("x", "x")
    }

    "expira só as linhas vencidas e avisa quando mudou" {
        val lines = RegionLines(3)
        lines.push(NotificationLine("curta", 10), 0)
        lines.push(NotificationLine("longa", 100), 0)

        lines.expire(9) shouldBe false
        lines.expire(10) shouldBe true
        texts(lines).shouldContainExactly("longa")
        lines.expire(99) shouldBe false
        lines.expire(100) shouldBe true
        lines.isEmpty() shouldBe true
    }

    "clear esvazia e diz se havia algo" {
        val lines = RegionLines(3)
        lines.clear() shouldBe false
        lines.push(NotificationLine("a", 20), 0)
        lines.clear() shouldBe true
        lines.isEmpty() shouldBe true
    }

    "duração precisa ser positiva" {
        shouldThrow<IllegalArgumentException> { NotificationLine("a", 0) }
    }

    "withKey e withIcon devolvem cópias" {
        val base = NotificationLine("a", 20)
        base.withKey("k").withIcon("item:loom") shouldBe NotificationLine("a", "item:loom", 20, "k")
        base.key shouldBe null
    }
})
