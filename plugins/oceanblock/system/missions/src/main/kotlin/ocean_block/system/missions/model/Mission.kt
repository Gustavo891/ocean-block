package ocean_block.system.missions.model

data class Mission(
    val id: String,
    val text: String,
    val objective: MissionObjective,
    val xp: Int,
)
