package ocean_block.system.playtime_rewards.model

data class Milestone(
    val id: String,
    val minutes: Int,
    val coins: Int,
    val pearls: Int,
    val xp: Int,
    val icon: String = "clock",
    val items: List<ItemPrize> = emptyList(),
    val commands: List<String> = emptyList(),
) {
    val seconds: Long get() = minutes * 60L
}

data class ItemPrize(val material: String, val amount: Int)
