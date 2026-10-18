package one_block.system.scoreboard.config

data class ScoreLine(
    val score: Int,
    val text: String,
)

data class BoardDefinition(
    val id: String,
    val titles: List<String>,
    val scores: List<ScoreLine>,
    val hideNumber: Boolean,
    val renderEvery: Long,
    val conditions: List<BoardCondition>,
)
