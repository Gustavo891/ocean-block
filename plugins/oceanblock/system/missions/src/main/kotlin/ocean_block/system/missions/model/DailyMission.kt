package ocean_block.system.missions.model

data class DailyMission(
    val id: String,
    val progress: Long,
    val xp: Int,
    val done: Boolean,
)
