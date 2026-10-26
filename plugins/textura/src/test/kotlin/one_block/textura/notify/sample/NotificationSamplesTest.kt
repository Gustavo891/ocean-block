package one_block.textura.notify.sample

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.function.Consumer

class NotificationSamplesTest : StringSpec({

    "envia o exemplo registrado para o alvo" {
        val samples = NotificationSamples<String>()
        val sent = mutableListOf<String>()
        samples.register("nivel", Consumer { sent += "nivel:$it" })

        samples.send("nivel", "Gustaav_") shouldBe true
        sent shouldContainExactly listOf("nivel:Gustaav_")
    }

    "tipo desconhecido nao envia nada" {
        val samples = NotificationSamples<String>()

        samples.send("nada", "Gustaav_") shouldBe false
    }

    "tipo ignora maiusculas" {
        val samples = NotificationSamples<String>()
        val sent = mutableListOf<String>()
        samples.register("Tempo", Consumer { sent += it })

        samples.send("TEMPO", "alvo") shouldBe true
        samples.ids() shouldContainExactly listOf("tempo")
    }

    "lista os tipos em ordem alfabetica" {
        val samples = NotificationSamples<String>()
        listOf("venda", "coleta", "nivel").forEach { samples.register(it, Consumer { }) }

        samples.ids() shouldContainExactly listOf("coleta", "nivel", "venda")
    }

    "registrar de novo troca o exemplo" {
        val samples = NotificationSamples<String>()
        val sent = mutableListOf<String>()
        samples.register("xp", Consumer { sent += "antigo" })
        samples.register("xp", Consumer { sent += "novo" })

        samples.send("xp", "alvo")
        sent shouldContainExactly listOf("novo")
    }
})
