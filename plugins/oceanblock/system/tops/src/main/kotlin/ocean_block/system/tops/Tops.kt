package ocean_block.system.tops

import ocean_block.system.tops.data.TopData

object Tops {

    private val tops = mutableMapOf<String, TopData>()

    fun register(data: TopData) {
        tops[data.id] = data
    }

    fun tops() = tops.values.toList()
}
