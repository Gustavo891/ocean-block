package ocean_block.system.islands.island.type

object IslandPalette {

    private const val NAMESPACE = "minecraft:"

    private val LOG = setOf("axis")
    private val LEAVES = setOf("distance", "persistent", "waterlogged")
    private val STAIRS = setOf("facing", "half", "shape", "waterlogged")

    private data class Rule(
        val target: String,
        val keep: Set<String> = emptySet(),
        val fixed: String = "",
        val condition: Pair<String, String>? = null,
    )

    private val DESERT = mapOf(
        "grass_block" to listOf(Rule("sand")),
        "dirt" to listOf(Rule("sandstone")),
        "moss_block" to listOf(Rule("smooth_sandstone")),
        "cobblestone" to listOf(Rule("sandstone")),
        "mossy_cobblestone" to listOf(Rule("cut_sandstone")),
        "mossy_cobblestone_stairs" to listOf(Rule("sandstone_stairs", STAIRS)),
        "short_grass" to listOf(Rule("dead_bush")),
        "dandelion" to listOf(Rule("dead_bush")),
        "poppy" to listOf(Rule("dead_bush")),
        "tall_grass" to listOf(Rule("air", condition = "half" to "upper"), Rule("dead_bush")),
        "oak_log" to listOf(Rule("stripped_oak_log", LOG)),
        "oak_leaves" to listOf(Rule("air")),
    )

    private val ICE = mapOf(
        "grass_block" to listOf(Rule("snow_block")),
        "moss_block" to listOf(Rule("snow_block")),
        "sand" to listOf(Rule("snow_block")),
        "mossy_cobblestone" to listOf(Rule("cobblestone")),
        "mossy_cobblestone_stairs" to listOf(Rule("cobblestone_stairs", STAIRS)),
        "short_grass" to listOf(Rule("snow", fixed = "layers=1")),
        "dandelion" to listOf(Rule("snow", fixed = "layers=1")),
        "poppy" to listOf(Rule("snow", fixed = "layers=1")),
        "tall_grass" to listOf(Rule("air", condition = "half" to "upper"), Rule("snow", fixed = "layers=2")),
        "oak_log" to listOf(Rule("spruce_log", LOG)),
        "oak_leaves" to listOf(Rule("spruce_leaves", LEAVES)),
        "cactus" to listOf(Rule("air")),
    )

    private fun table(type: IslandType): Map<String, List<Rule>> = when (type) {
        IslandType.DESERTO -> DESERT
        IslandType.GELO -> ICE
        IslandType.PADRAO, IslandType.JANGADA -> emptyMap()
    }

    fun swap(type: IslandType, block: String): String {
        val open = block.indexOf('[')
        val id = (if (open < 0) block else block.substring(0, open)).removePrefix(NAMESPACE)
        val properties = if (open < 0) emptyList() else parse(block.substring(open + 1, block.length - 1))

        val rule = table(type)[id]?.firstOrNull { rule ->
            rule.condition == null || properties.contains(rule.condition)
        } ?: return block

        val kept = properties.filter { it.first in rule.keep }.map { "${it.first}=${it.second}" }
        val all = if (rule.fixed.isEmpty()) kept else kept + rule.fixed

        return if (all.isEmpty()) "$NAMESPACE${rule.target}" else "$NAMESPACE${rule.target}[${all.joinToString(",")}]"
    }

    private fun parse(raw: String): List<Pair<String, String>> =
        raw.split(',').filter { '=' in it }.map { it.substringBefore('=') to it.substringAfter('=') }
}
