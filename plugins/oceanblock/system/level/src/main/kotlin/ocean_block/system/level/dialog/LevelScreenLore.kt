package ocean_block.system.level.dialog

import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.level.curve.LevelCurve
import ocean_block.system.level.levelup.LevelUpRewards
import java.text.NumberFormat
import java.util.Locale

object LevelScreenLore {

    const val PER_ROW = 5
    const val ROWS = 5
    const val PER_PAGE = PER_ROW * ROWS

    private val LOCALE: Locale = Locale.forLanguageTag("pt-BR")

    private const val ITEM = "item:"
    private const val FALLBACK_RECIPE_ICON = "crafting_table"
    private const val COINS_ICON = "gold_nugget"

    private val RECIPE_ICONS = mapOf(
        "gancho" to "fishing_rod",
        "rede" to "cobweb",
        "picareta_fundidora" to "iron_pickaxe",
        "gerador_energia" to "blast_furnace",
        "painel_solar" to "daylight_detector",
        "fornalha_eletrica" to "furnace",
        "minerador" to "piston",
        "broca_ouro" to "golden_pickaxe",
        "broca_diamante" to "diamond_pickaxe",
        "broca_obsidiana" to "obsidian",
        "broca_netherite" to "netherite_pickaxe",
    )

    val ICONS: List<String> = (RECIPE_ICONS.values + listOf(FALLBACK_RECIPE_ICON, COINS_ICON)).distinct()

    fun pages(): Int = (LevelCurve.MAX_LEVEL + PER_PAGE - 1) / PER_PAGE

    fun pageOf(level: Int): Int = (level.coerceIn(1, LevelCurve.MAX_LEVEL) - 1) / PER_PAGE

    fun order(page: Int): List<List<Int>> = (0 until ROWS).map { row ->
        val first = page * PER_PAGE + row * PER_ROW + 1
        val levels = (first until first + PER_ROW).filter { it <= LevelCurve.MAX_LEVEL }
        if (row % 2 == 1) levels.reversed() else levels
    }

    fun row(page: Int, row: Int, xp: Long, recipes: Map<Int, List<OceanRecipe>>): List<Map<String, String>> {
        val levels = order(page).getOrNull(row) ?: return emptyList()
        val current = LevelCurve.levelOf(xp)
        return levels.mapIndexed { position, level ->
            val link = levels.getOrNull(position + 1)?.let { if (maxOf(level, it) <= current) "ui:liga_verde" else "ui:liga_cinza" }
            node(level, xp, recipes[level].orEmpty()) + ("liga" to link.orEmpty())
        }
    }

    fun drop(page: Int, row: Int, level: Int): Map<String, String> {
        val below = page * PER_PAGE + (row + 1) * PER_ROW + 1
        return mapOf("liga" to if (below <= level) "ui:desce_verde" else "ui:desce_cinza")
    }

    fun header(xp: Long): Map<String, String> {
        val level = LevelCurve.levelOf(xp)
        val base = mapOf("nivel" to level.toString(), "nivel_de" to "NÍVEL $level DE ${LevelCurve.MAX_LEVEL}")
        if (level >= LevelCurve.MAX_LEVEL) {
            return base + mapOf("xp" to "MÁXIMO", "barra" to "100", "faltam" to "VOCÊ CHEGOU AO TOPO")
        }
        val inLevel = xp - LevelCurve.totalFor(level)
        val needed = LevelCurve.toNext(level)
        return base + mapOf(
            "xp" to "${number(inLevel)}/${number(needed)}",
            "barra" to (inLevel * 100 / needed).coerceIn(0, 100).toString(),
            "faltam" to "FALTAM ${number(needed - inLevel)} XP PARA O NÍVEL ${level + 1}",
        )
    }

    fun next(xp: Long, recipes: Map<Int, List<OceanRecipe>>): List<Map<String, String>> {
        val level = LevelCurve.levelOf(xp)
        if (level >= LevelCurve.MAX_LEVEL) return emptyList()
        val target = level + 1
        return listOf(
            mapOf(
                "numero" to target.toString(),
                "titulo" to "NÍVEL $target",
                "faltam" to "FALTAM ${number(LevelCurve.totalFor(target) - xp)} XP",
                "coins" to "+${number(LevelUpRewards.coins(level, target).toLong())}",
                "coins_icone" to ITEM + COINS_ICON,
                "libera" to recipes[target].orEmpty().joinToString(", ") { it.name },
            ),
        )
    }

    fun nextRecipe(level: Int, recipes: Map<Int, List<OceanRecipe>>): List<Map<String, String>> {
        val target = recipes.keys.filter { it > level }.minOrNull() ?: return emptyList()
        val recipe = recipes[target].orEmpty().firstOrNull() ?: return emptyList()
        return listOf(
            mapOf(
                "icone" to ITEM + (RECIPE_ICONS[recipe.id] ?: FALLBACK_RECIPE_ICON),
                "nivel" to "NÍVEL $target",
                "nome" to recipe.name,
            ),
        )
    }

    fun pager(page: Int): Map<String, String> {
        val first = page * PER_PAGE + 1
        val last = minOf(first + PER_PAGE - 1, LevelCurve.MAX_LEVEL)
        return mapOf(
            "faixa" to "NÍVEIS $first A $last",
            "pagina1_estilo" to if (page == 0) "branco" else "muted",
            "pagina2_estilo" to if (page == 1) "branco" else "muted",
        )
    }

    fun number(value: Long): String = NumberFormat.getIntegerInstance(LOCALE).format(value)

    private fun node(level: Int, xp: Long, recipes: List<OceanRecipe>): Map<String, String> {
        val current = LevelCurve.levelOf(xp)
        val icon = recipes.firstNotNullOfOrNull { RECIPE_ICONS[it.id] }?.let { ITEM + it }.orEmpty()
        return mapOf(
            "icone" to icon,
            "numero" to if (icon.isEmpty()) level.toString() else "",
            "canto" to if (icon.isEmpty()) "" else level.toString(),
            "estado" to when {
                level < current -> ""
                level == current -> "ui:no_atual"
                else -> "ui:no_trancada"
            },
            "selo" to if (level < current) "ui:no_feita" else "",
            "dica" to tooltip(level, xp, recipes),
        )
    }

    private fun tooltip(level: Int, xp: Long, recipes: List<OceanRecipe>): String {
        val current = LevelCurve.levelOf(xp)
        val status = when {
            level < current -> "✔ Alcançado"
            level == current -> "Você está aqui"
            else -> "✖ Faltam ${number(LevelCurve.totalFor(level) - xp)} XP"
        }
        return (listOf("Nível $level") + recipes.map { "∙ ${it.name}" } + listOf("", status)).joinToString("\n")
    }
}
