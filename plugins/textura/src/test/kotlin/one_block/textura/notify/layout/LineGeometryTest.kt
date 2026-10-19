package one_block.textura.notify.layout

import one_block.textura.notify.NotificationLine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import one_block.textura.hud.anchor.ScreenAnchor
import one_block.textura.notify.layout.LineGeometry
import one_block.textura.notify.layout.LineSlot
import one_block.textura.notify.layout.LineSpan
import one_block.textura.notify.layout.PlaquePieces
import one_block.textura.notify.region.NotificationRegion
import one_block.textura.notify.region.StackDirection

class LineGeometryTest : StringSpec({

    fun region(anchor: ScreenAnchor, x: Int, y: Int, direction: StackDirection, background: Boolean = true, lines: Int = 6, spacing: Int = 1) =
        NotificationRegion("r", anchor, x, y, lines, spacing, background, direction)

    "para baixo cada linha desce a altura da linha mais o espaçamento" {
        val coleta = region(ScreenAnchor.MIDDLE_RIGHT, 0, 70, StackDirection.DOWN)
        (0 until 3).map { LineGeometry.slot(coleta, it) }
            .shouldContainExactly(LineSlot(70, 0), LineSlot(82, 0), LineSlot(94, 0))
    }

    "espaçamento muda o passo" {
        val wide = region(ScreenAnchor.TOP_LEFT, 0, 0, StackDirection.DOWN, spacing = 4)
        LineGeometry.slot(wide, 2) shouldBe LineSlot(30, 0)
    }

    "para cima com passo 12 o lift é o índice da linha" {
        val base = region(ScreenAnchor.BOTTOM_LEFT, 0, 0, StackDirection.UP)
        (0 until 4).map { LineGeometry.slot(base, it) }
            .shouldContainExactly(LineSlot(0, 0), LineSlot(0, 1), LineSlot(0, 2), LineSlot(0, 3))
    }

    "deslocamento para cima que não fecha no passo vira lift mais descida pela fonte" {
        val base = region(ScreenAnchor.BOTTOM_RIGHT, 0, -5, StackDirection.UP, spacing = 3)
        LineGeometry.slot(base, 0) shouldBe LineSlot(7, 1)
        LineGeometry.slot(base, 1) shouldBe LineSlot(5, 2)
    }

    "linhas além do alcance do lift não cabem" {
        val tall = region(ScreenAnchor.BOTTOM_LEFT, 0, -150, StackDirection.UP, lines = 15)
        LineGeometry.fittingLines(tall) shouldBe 3
        LineGeometry.slot(tall, 3) shouldBe null
    }

    "fontes de linha cobrem todas as regiões sem repetir" {
        val a = region(ScreenAnchor.TOP_LEFT, 4, 4, StackDirection.DOWN, lines = 3)
        val b = region(ScreenAnchor.BOTTOM_LEFT, 0, 0, StackDirection.UP, lines = 3)
        LineGeometry.textTops(listOf(a, b)).toList().shouldContainExactly(0, 4, 16, 28)
    }

    "largura do conteúdo soma a caixa do ícone" {
        LineGeometry.contentWidth(30, false) shouldBe 30
        LineGeometry.contentWidth(30, true) shouldBe 30 + LineGeometry.ICON_BOX
    }

    "âncora à esquerda começa no x e o fundo soma o padding dos dois lados" {
        val left = region(ScreenAnchor.TOP_LEFT, 4, 0, StackDirection.DOWN)
        LineGeometry.span(left, 40) shouldBe LineSpan(4, 40 + 2 * LineGeometry.PADDING_X, 4 + LineGeometry.PADDING_X)
    }

    "âncora à direita termina no x" {
        val right = region(ScreenAnchor.MIDDLE_RIGHT, -2, 0, StackDirection.DOWN)
        val span = LineGeometry.span(right, 40)
        (span.boxLeft + span.boxWidth) shouldBe -2
        span.contentLeft + 40 + LineGeometry.PADDING_X shouldBe -2
    }

    "sem fundo a caixa é o próprio conteúdo" {
        val right = region(ScreenAnchor.TOP_RIGHT, 0, 0, StackDirection.DOWN, background = false)
        LineGeometry.span(right, 25) shouldBe LineSpan(-25, 25, -25)
    }

    "ascents da linha: texto, ícone e placa descem juntos" {
        LineGeometry.textAscent(7, 10) shouldBe -3
        LineGeometry.iconAscent(10) shouldBe -3
        LineGeometry.plaqueAscent(10) shouldBe -1
    }

    "placa é decomposta em potências de dois entre as tampas" {
        PlaquePieces.body(2) shouldBe emptyList()
        PlaquePieces.body(13) shouldBe listOf(8, 2, 1)
        PlaquePieces.body(300).sum() shouldBe 298
    }
})
