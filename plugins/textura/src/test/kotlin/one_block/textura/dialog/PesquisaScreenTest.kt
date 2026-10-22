package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.json.ScreenParser
import one_block.textura.dialog.kit.BaseUiKit
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.kit.UiTextFontLoader
import one_block.textura.dialog.kit.UiUsage
import one_block.textura.dialog.layout.LaidOut
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.theme.UiTheme
import one_block.textura.registry.UiThemeRegistry
import java.io.File
import java.util.logging.Logger

class PesquisaScreenTest : StringSpec({

    val data = File(File(System.getProperty("user.dir")).parentFile.parentFile, "server/plugins/Textura")
    val ui = File(data, "configs/ui")
    val quiet = Logger.getAnonymousLogger().apply { useParentHandlers = false }
    val themes: Map<String, UiTheme> = UiThemeRegistry(quiet).also { it.load(ui) }.themes
    val kit: UiKit = BaseUiKit(
        UiArtCatalog(File(data, "resource/assets/oneblock/textures/ui"), { null }, quiet),
        UiTextFontLoader.load(File(data, "configs/fonts/custom.json"), File(data, "resource"), quiet),
    )

    fun parse(): Pair<ScreenDefinition?, List<String>> {
        val warnings = DialogFixtures.Warnings()
        val logger = Logger.getAnonymousLogger().apply {
            useParentHandlers = false
            addHandler(warnings)
        }
        return ScreenParser(logger).parse(File(ui, "pesquisa.json")) to warnings.messages.toList()
    }

    fun pesquisa(nome: String, acao: String, bloqueado: Boolean) = mapOf(
        "icone" to "item:loom",
        "nome" to nome,
        "descricao" to "Coleta sozinha o que passa perto do raft.",
        "custo_lista" to "∙ 3/10 Tábuas\n∙ 0/6 Linhas",
        "estado" to if (bloqueado) "Pesquisando" else "Disponível",
        "tempo" to "leva 20s",
        "pode_entregar" to (!bloqueado).toString(),
        "bloqueado" to bloqueado.toString(),
        "acao" to acao,
        "botao" to acao,
        "rotulo" to "Leva 20s",
        "status" to if (bloqueado) "Pronta em 40s" else "Entregue os itens",
        "status_estilo" to if (bloqueado) "muted" else "good",
        "c0_item" to "oak_planks", "c0_qtd" to "7", "c0_nome" to "Tábuas", "c0_tem" to "<gray>Entregue 3/10", "c0_falta" to (!bloqueado).toString(),
        "c1_item" to "string", "c1_qtd" to "6", "c1_nome" to "Linhas", "c1_tem" to "<gray>Entregue 0/6", "c1_falta" to (!bloqueado).toString(),
    ) + (2 until 4).flatMap { i -> listOf("c${i}_item", "c${i}_qtd", "c${i}_nome", "c${i}_tem", "c${i}_falta").map { it to "" } }

    fun binds(pesquisas: List<Map<String, String>>) = DialogBinds().apply {
        text("vagas") { "Vagas: 1/1" }
        list("categorias") {
            listOf(
                mapOf("nome" to "Coleta", "icone" to "ui:chest", "ativa" to "true", "acao" to "✔ Aberta agora!"),
                mapOf("nome" to "Fundição", "icone" to "ui:pickaxe", "ativa" to "false", "acao" to "Clique para ver!"),
                mapOf("nome" to "Raft", "icone" to "ui:sparkle", "ativa" to "false", "acao" to "Clique para ver!"),
            )
        }
        list("fila") { listOf(mapOf("nome" to "Rede de Detritos", "estado" to "Pesquisando", "tempo" to "faltam 12s")) }
        list("pesquisas") { pesquisas }
        listPage("pesquisas") { 0 }
        list("paginacao") { emptyList() }
        listButton("categoria") { _, _ -> }
        listButton("entregar") { _, _ -> }
        button("anterior") { }
        button("proxima") { }
    }

    class Render(val laid: LaidOut, val warnings: List<String>, val unknownBinds: Set<String>)

    fun render(binds: DialogBinds): Render {
        val definition = parse().first.shouldNotBeNull()
        val warnings = mutableListOf<String>()
        val session = DialogSession(definition.id, binds, player = DialogFixtures.player())
        val screen = ScreenCompiler(kit) { warnings += it }.compile(definition, themes.getValue(definition.theme), session)
        return Render(UiLayout(kit).layout(screen), warnings, binds.keys - definition.keys)
    }

    "pesquisa.json lê sem avisos e declara os binds da tela" {
        val (definition, warnings) = parse()

        warnings.shouldBeEmpty()
        definition.shouldNotBeNull().theme shouldBe "stone"
        definition.keys shouldContainAll listOf("categorias", "fila", "pesquisas", "paginacao", "categoria", "entregar", "vagas")
    }

    "a tela compila com o kit real, sem avisos e dentro da largura do dialog" {
        val run = render(binds(listOf(pesquisa("Rede de Detritos", "Entregar", false), pesquisa("Peneira de ferro", "faltam 40s", true))))

        run.warnings.shouldBeEmpty()
        run.unknownBinds.shouldBeEmpty()
        run.laid.width shouldBeLessThanOrEqual ScreenCompiler.MAX_WIDTH
        run.laid.texts.map { it.text } shouldContainAll listOf("Categorias", "Coleta", "Vagas: 1/1", "Rede de Detritos", "faltam 12s", "Entregar", "faltam 40s", "Leva 20s", "Entregue os itens")
    }

    "cartao em andamento fica sem acao e o disponivel tem acao" {
        val run = render(binds(listOf(pesquisa("Rede de Detritos", "Entregar", false), pesquisa("Peneira de ferro", "faltam 40s", true))))
        val buttons = run.laid.hotspots.filter { it.tooltip?.asHoverEvent()?.value().toString().contains("Custo") }

        buttons.size shouldBe 2
        buttons.count { it.action != null } shouldBe 1
    }

    "o gerar cobre todas as combinações sem aviso" {
        val definition = parse().first.shouldNotBeNull()
        val warnings = mutableListOf<String>()
        UiUsage.collect(listOf(definition), themes, kit, setOf("loom", "composter", "blast_furnace", "hopper", "cartography_table")) { warnings += it }

        warnings.shouldBeEmpty()
    }
})
