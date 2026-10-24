package ocean_block.system.research.menu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ocean_block.system.research.model.IslandResearch
import ocean_block.system.research.model.ResearchCategory
import ocean_block.system.research.model.RunningResearch
import ocean_block.system.research.tree.ResearchTree
import java.util.UUID

private const val NOW = 5_000_000L
private fun def(id: String) = ResearchTree.byId.getValue(id)

class ResearchLoreTest : StringSpec({

    "pesquisa disponivel mostra o custo entregue e chama para entregar" {
        val state = IslandResearch(UUID.randomUUID(), delivered = mutableMapOf("rede" to mutableMapOf("planks" to 3)))

        ResearchLore.lore(def("rede"), state, NOW) shouldContainExactly listOf(
            "<gray>Coleta sozinha o que passa perto do raft.",
            "",
            "<white>Custo:",
            "<red>✖ <gray>3/10 Tábuas",
            "<red>✖ <gray>0/6 Linhas",
            "",
            "<gray>Duração: <white>20s",
            "",
            "<yellow>Clique para entregar os itens!",
        )
    }

    "pesquisando mostra o custo pago e o tempo que falta" {
        val state = IslandResearch(UUID.randomUUID(), running = mutableListOf(RunningResearch("rede", NOW - 10_000, 90_000)))

        val lore = ResearchLore.lore(def("rede"), state, NOW)

        lore[3] shouldBe "<green>✔ <gray>10/10 Tábuas"
        lore.last() shouldBe "<aqua>Pesquisando: faltam 1m20s"
    }

    "concluida, na fila e bloqueada" {
        val done = IslandResearch(UUID.randomUUID(), completed = mutableSetOf("rede"))
        val queued = IslandResearch(UUID.randomUUID(), queue = mutableListOf("rede"))

        ResearchLore.lore(def("rede"), done, NOW).last() shouldBe "<green>✔ Concluída"
        ResearchLore.lore(def("rede"), queued, NOW).last() shouldBe "<gold>Na fila."
        ResearchLore.lore(def("fundicao_funil"), done, NOW).last() shouldBe "<red>Requer: Fundição rápida"
    }

    "nome fica verde quando concluida" {
        val done = IslandResearch(UUID.randomUUID(), completed = mutableSetOf("rede"))

        ResearchLore.name(def("rede"), done, NOW) shouldBe "<green>Rede de Detritos"
        ResearchLore.name(def("rede"), IslandResearch(UUID.randomUUID()), NOW) shouldBe "<white>Rede de Detritos"
    }

    "vagas e fila no item de informacao" {
        val state = IslandResearch(
            UUID.randomUUID(),
            queue = mutableListOf("fundicao_melhorada"),
            running = mutableListOf(RunningResearch("rede", NOW, 20_000)),
        )

        ResearchLore.slotsName(1, 2) shouldBe "<white>Vagas: 1/2"
        ResearchLore.queueLore(state, NOW) shouldContainExactly listOf(
            "<aqua>Rede de Detritos <gray>faltam 20s",
            "<gold>Fundição rápida <gray>na fila, leva 2min",
        )
        ResearchLore.queueLore(IslandResearch(UUID.randomUUID()), NOW) shouldContainExactly listOf("<gray>Nenhuma pesquisa em andamento.")
    }

    "categoria aberta e fechada" {
        ResearchLore.categoryName(ResearchCategory.FUNDICAO, active = true) shouldBe "<green>Fundição"
        ResearchLore.categoryName(ResearchCategory.FUNDICAO, active = false) shouldBe "<white>Fundição"
        ResearchLore.categoryLore(active = true) shouldContainExactly listOf("<green>Aberta agora.")
        ResearchLore.categoryLore(active = false) shouldContainExactly listOf("<yellow>Clique para ver!")
    }

    "duracao curta, mista e em minutos" {
        ResearchLore.duration(45_000) shouldBe "45s"
        ResearchLore.duration(80_000) shouldBe "1m20s"
        ResearchLore.duration(300_000) shouldBe "5min"
        ResearchLore.duration(400) shouldBe "1s"
    }

    "o menu comporta todas as categorias" {
        (ResearchCategory.entries.size <= ResearchLore.CATEGORY_SLOTS) shouldBe true
    }

    "a quarta vaga e a categoria Maquinas" {
        ResearchLore.CATEGORY_SLOTS shouldBe 4
        ResearchCategory.entries.last() shouldBe ResearchCategory.MAQUINAS
        ResearchCategory.MAQUINAS.displayName shouldBe "Máquinas"
    }
})
