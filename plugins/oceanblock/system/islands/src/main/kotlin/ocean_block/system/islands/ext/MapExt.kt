package ocean_block.system.islands.ext

fun <T> Map<T, Int>.getRandomByWeight(): T? {
    val totalWeight = this.values.sum()
    if (totalWeight <= 0) return null

    var randomNum = (1..totalWeight).random()
    for ((item, weight) in this) {
        if (randomNum <= weight) return item
        randomNum -= weight
    }
    return this.keys.first()
}
