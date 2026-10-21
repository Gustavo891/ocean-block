package ocean_block.system.research.model

enum class ResearchCategory(val displayName: String, val icon: String) {
    COLETA("Coleta", "ui:chest"),
    FUNDICAO("Fundição", "ui:pickaxe"),
    RAFT("Raft", "ui:sparkle"),
}

data class ResearchCost(val key: String, val label: String, val plural: String, val amount: Int) {
    fun labelFor(count: Int): String = if (count == 1) label else plural
}

data class ResearchDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: ResearchCategory,
    val icon: String,
    val cost: List<ResearchCost>,
    val durationSeconds: Int,
    val requires: List<String>,
    val unlocksRecipes: List<String>,
    val extraSlots: Int = 0,
) {
    val durationMs: Long get() = durationSeconds * 1000L
}
