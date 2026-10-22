package one_block.textura.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.json.ScreenParser
import one_block.textura.dialog.kit.BaseUiKit
import one_block.textura.dialog.kit.SurfaceHeight
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

class ReceitasScreenTest : StringSpec({

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
        return ScreenParser(logger).parse(File(ui, "receitas.json")) to warnings.messages.toList()
    }

    fun receita(nome: String, falta: Boolean) = mapOf(
        "item" to "fishing_rod",
        "qtd" to "1",
        "nome" to nome,
        "descricao" to "Lance na água e recolha para puxar detritos.",
        "custo" to "3 Tábuas ∙ 2 Linhas",
        "custo_lista" to "∙ 3 Tábuas\n∙ 2 Linhas",
        "falta" to falta.toString(),
        "rende" to "1 unidade",
        "acao" to if (falta) "⌀ Falta: 1 Linha" else "✔ Pronto! Clique para craftar.",
        "status" to if (falta) "Faltam itens" else "Pronto para craftar",
        "status_icone" to if (falta) "ui:dot-red" else "ui:check",
        "status_estilo" to if (falta) "bad" else "good",
        "i0_item" to "oak_planks",
        "i0_qtd" to "3",
        "i0_nome" to "Tábuas",
        "i0_tem" to "<green>Você tem 12",
        "i0_falta" to "false",
        "i1_item" to "string",
        "i1_qtd" to "2",
        "i1_nome" to "Linhas",
        "i1_tem" to if (falta) "<red>Você tem 1, falta 1" else "<green>Você tem 2",
        "i1_falta" to falta.toString(),
    ) + (2 until 5).flatMap { i -> listOf("i${i}_item", "i${i}_qtd", "i${i}_nome", "i${i}_tem", "i${i}_falta").map { it to "" } }

    fun binds(receitas: List<Map<String, String>>, page: Int, paginacao: Boolean) = DialogBinds().apply {
        list("categorias") {
            listOf(
                mapOf("nome" to "Ferramentas", "icone" to "ui:pickaxe", "ativa" to "true", "descricao" to "x", "acao" to "✔ Aberta agora!"),
                mapOf("nome" to "Coleta", "icone" to "ui:chest", "ativa" to "false", "descricao" to "y", "acao" to "Clique para ver!"),
            )
        }
        list("receitas") { receitas }
        listPage("receitas") { page }
        list("paginacao") {
            if (paginacao) listOf(mapOf("pagina" to "${page + 1}", "paginas" to "2", "sem_anterior" to "${page == 0}", "sem_proxima" to "${page == 1}")) else emptyList()
        }
        listButton("categoria") { _, _ -> }
        listButton("craftar") { _, _ -> }
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

    "receitas.json lê sem avisos e declara os binds da tela" {
        val (definition, warnings) = parse()

        warnings.shouldBeEmpty()
        definition.shouldNotBeNull().theme shouldBe "stone"
        definition.keys shouldContainAll listOf(
            "categorias", "receitas", "paginacao", "categoria", "craftar", "anterior", "proxima",
        )
    }

    "a tela compila com o kit real, sem avisos e dentro da largura do dialog" {
        val run = render(binds(listOf(receita("Gancho", false), receita("Rede de Detritos", true)), 0, paginacao = false))

        run.warnings.shouldBeEmpty()
        run.unknownBinds.shouldBeEmpty()
        run.laid.width shouldBeLessThanOrEqual ScreenCompiler.MAX_WIDTH
        run.laid.texts.map { it.text } shouldContainAll listOf(
            "Categorias", "Ferramentas", "Coleta", "Gancho", "Rede de Detritos", "Pronto para craftar", "Faltam itens", "1 unidade",
        )
        run.laid.texts.map { it.text } shouldNotContain "Os itens vão para o seu inventário."
        run.laid.images.map { it.id } shouldContain "ui:stone/icons/pickaxe"
        run.laid.images.count { it.id == "ui:stone/slot" } shouldBe 3
        run.laid.images.count { it.id == "ui:stone/slot_missing" } shouldBe 1
    }

    "card sem material fica sem ação e o pronto tem ação" {
        val run = render(binds(listOf(receita("Gancho", false), receita("Rede de Detritos", true)), 0, paginacao = false))
        val craftButtons = run.laid.hotspots.filter { it.tooltip?.asHoverEvent()?.value().toString().contains("Custo") }

        craftButtons.size shouldBe 2
        craftButtons.count { it.action != null } shouldBe 1
    }

    "segunda página mostra a sétima receita e a linha de páginas" {
        val receitas = (1..7).map { receita("Receita $it", false) }
        val texts = render(binds(receitas, 1, paginacao = true)).laid.texts.map { it.text }

        texts shouldContainAll listOf("Receita 7", "2/2", "«", "»")
        texts shouldNotContain "Receita 1"
    }

    "sem página extra a linha de páginas some" {
        val texts = render(binds(listOf(receita("Gancho", false)), 0, paginacao = false)).laid.texts.map { it.text }

        texts shouldNotContain "«"
    }

    "o gerar cobre todas as combinações sem aviso e assa as alturas da folha de receitas" {
        val definition = parse().first.shouldNotBeNull()
        val warnings = mutableListOf<String>()
        val usage = UiUsage.collect(listOf(definition), themes, kit, setOf("fishing_rod", "loom")) { warnings += it }

        warnings.shouldBeEmpty()
        val sheetHeights = usage.surfaces.filter { it.id == "ui:stone/sheet" }.map(SurfaceHeight::height).toSet()
        val withOne = render(binds(listOf(receita("Gancho", false)), 0, paginacao = false)).laid
        val withSeven = render(binds((1..7).map { receita("R$it", false) }, 0, paginacao = true)).laid
        (withOne.images + withSeven.images).filter { it.id == "ui:stone/sheet" }.forEach { sheetHeights shouldContain it.height }
    }
})
