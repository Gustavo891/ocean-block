package ocean_block.system.research.screen

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import ocean_block.system.research.model.IslandResearch
import ocean_block.system.research.model.ResearchCategory
import ocean_block.system.research.model.RunningResearch
import ocean_block.system.research.tree.ResearchTree
import java.util.UUID

private const val NOW = 5_000_000L
private fun def(id: String) = ResearchTree.byId.getValue(id)

class ResearchEntriesTest : StringSpec({

    "cartao da rede sem entregas mostra o custo e deixa entregar" {
        val card = ResearchEntries.card(def("rede"), IslandResearch(UUID.randomUUID()), NOW)

        card["custo_lista"]!! shouldContain "∙ 0/10 Tábuas"
        card["custo_lista"]!! shouldContain "∙ 0/6 Linhas"
        card["pode_entregar"] shouldBe "true"
        card["bloqueado"] shouldBe "false"
        card["acao"] shouldBe "Entregar"
        card["tempo"] shouldBe "leva 20s"
    }

    "cartao traz rotulo de tempo, estado colorido e um slot por item do custo com o que falta" {
        val state = IslandResearch(UUID.randomUUID(), delivered = mutableMapOf("rede" to mutableMapOf("planks" to 3)))

        val card = ResearchEntries.card(def("rede"), state, NOW)

        card["rotulo"] shouldBe "Leva 20s"
        card["status"] shouldBe "Entregue os itens"
        card["status_estilo"] shouldBe "good"
        card["botao"] shouldBe "Entregar"
        card["c0_item"] shouldBe "oak_planks"
        card["c0_qtd"] shouldBe "7"
        card["c0_nome"] shouldBe "Tábuas"
        card["c0_tem"] shouldBe "<gray>Entregue 3/10"
        card["c0_falta"] shouldBe "true"
        card["c1_item"] shouldBe "string"
        card["c1_qtd"] shouldBe "6"
        card["c2_item"] shouldBe ""
        card["c3_item"] shouldBe ""
    }

    "cartao pesquisando, concluido e bloqueado mudam a linha de estado" {
        val running = IslandResearch(UUID.randomUUID(), running = mutableListOf(RunningResearch("rede", NOW - 10_000, 90_000)))
        val done = IslandResearch(UUID.randomUUID(), completed = mutableSetOf("rede"))

        ResearchEntries.card(def("rede"), running, NOW).let {
            it["status"] shouldBe "Pronta em 1m20s"
            it["status_estilo"] shouldBe "muted"
            it["botao"] shouldBe "Pesquisando"
            it["c0_falta"] shouldBe "false"
            it["c0_qtd"] shouldBe "10"
        }
        ResearchEntries.card(def("rede"), done, NOW).let {
            it["status"] shouldBe "✔ Concluída"
            it["status_estilo"] shouldBe "good"
        }
        ResearchEntries.card(def("peneira_diamante"), done, NOW).let {
            it["status"] shouldBe "Bloqueada"
            it["botao"] shouldBe "Bloqueada"
            it["status_estilo"] shouldBe "bad"
        }
    }

    "icones dos custos cobrem todos os itens da arvore" {
        ResearchEntries.costIcons().toSet() shouldBe setOf(
            "oak_planks", "string", "iron_ingot", "copper_ingot", "gold_ingot", "clay_ball", "diamond", "redstone",
        )
    }

    "entrega parcial aparece no custo" {
        val state = IslandResearch(UUID.randomUUID(), delivered = mutableMapOf("rede" to mutableMapOf("planks" to 3)))

        ResearchEntries.card(def("rede"), state, NOW)["custo_lista"]!! shouldContain "∙ 3/10 Tábuas"
    }

    "pesquisando mostra o tempo que falta" {
        val state = IslandResearch(UUID.randomUUID(), running = mutableListOf(RunningResearch("rede", NOW - 10_000, 90_000)))

        val card = ResearchEntries.card(def("rede"), state, NOW)
        card["acao"] shouldBe "faltam 1m20s"
        card["estado"] shouldBe "Pesquisando"
        card["bloqueado"] shouldBe "true"
    }

    "concluida e bloqueada" {
        val done = IslandResearch(UUID.randomUUID(), completed = mutableSetOf("rede"))

        ResearchEntries.card(def("rede"), done, NOW)["acao"] shouldBe "✔ Concluída"
        ResearchEntries.card(def("peneira_diamante"), done, NOW)["acao"] shouldBe "⌀ Requer: Peneira de ferro"
    }

    "fila mostra o que roda e o que espera" {
        val state = IslandResearch(
            UUID.randomUUID(),
            queue = mutableListOf("peneira_ferro"),
            running = mutableListOf(RunningResearch("rede", NOW, 20_000)),
        )

        ResearchEntries.queue(state, NOW) shouldBe listOf(
            mapOf("nome" to "Rede de Detritos", "estado" to "Pesquisando", "tempo" to "faltam 20s"),
            mapOf("nome" to "Peneira de ferro", "estado" to "Na fila", "tempo" to "leva 1min"),
        )
        ResearchEntries.slotsText(1, 2) shouldBe "Vagas: 1/2"
    }

    "categorias marcam a ativa" {
        val categories = ResearchEntries.categories(ResearchCategory.FUNDICAO)

        categories.map { it["nome"] } shouldBe listOf("Coleta", "Fundição", "Raft")
        categories.map { it["ativa"] } shouldBe listOf("false", "true", "false")
    }

    "duracao curta, mista e em minutos" {
        ResearchEntries.duration(45_000) shouldBe "45s"
        ResearchEntries.duration(80_000) shouldBe "1m20s"
        ResearchEntries.duration(300_000) shouldBe "5min"
        ResearchEntries.duration(400) shouldBe "1s"
    }
})
