package one_block.system.repair.enums

enum class Repair(
    val amount: Double
) {
    I(0.30),
    II(0.50),
    III(1.0);

    fun getPercent(): Int {
        return (amount * 100).toInt()
    }
}