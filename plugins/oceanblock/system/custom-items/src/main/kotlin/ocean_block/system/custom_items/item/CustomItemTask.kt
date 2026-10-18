package ocean_block.system.custom_items.item

data class CustomItemTask(
    val intervalTicks: Long,
    val async: Boolean = false,
    val run: () -> Unit,
)
