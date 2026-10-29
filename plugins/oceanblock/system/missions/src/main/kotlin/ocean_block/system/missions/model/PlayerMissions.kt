package ocean_block.system.missions.model

data class PlayerMissions(
    val uuid: String,
    val trailIndex: Int,
    val trailProgress: Long,
    val dailyDate: String,
    val dailies: List<DailyMission>,
    val activeDaily: String? = null,
)
