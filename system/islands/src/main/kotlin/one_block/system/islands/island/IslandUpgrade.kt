package one_block.system.islands.island

enum class IslandUpgrade(
    val maxLevel: Int
) {

    Size(3),
    CropGrowth(3);

    fun getPrice(level: Int): Double {
        return when(this) {
            Size -> when(level) {
                0 -> 10_000.0
                1 -> 20_000.0
                2 -> 30_000.0
                else -> Double.MAX_VALUE
            }

            CropGrowth -> when(level) {
                0 -> 100.0
                1 -> 200.0
                2 -> 300.0
                else -> Double.MAX_VALUE
            }
        }
    }

    fun apply(island: Island, level: Int) {
        when(this) {
            IslandUpgrade.Size -> {
                val sizes = listOf(50, 70, 100)
                island.size = sizes[level]
                island.refreshWorldBorder()
            }

            IslandUpgrade.CropGrowth -> {

            }
        }
    }
}