package one_block.system.minions.enums

import one_block.system.minions.enums.MinionType.*
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

enum class MinionUpgrade(
    val id: String,
    val prettyName: String,
    val maxLevel: Int,
    val lore: List<String>,
    val applicableTo: Set<MinionType>,
) {

    MinerSpeed(
        id = "miner_speed",
        prettyName = "Velocidade",
        maxLevel = 5,
        lore = listOf(
            "Diminui o tempo entre",
            "cada bloco minerado."
        ),
        applicableTo = setOf(Miner)
    ) {
        override fun getStatLine(value: Int, maxValue: Int): String {
            val percent = ((value.toDouble() / maxValue) * 100).toInt()
            return "<yellow>+${percent}%"
        }
    };


    companion object {
        fun valuesFor(type: MinionType): List<MinionUpgrade> {
            return entries.filter { type in it.applicableTo }
        }
    }

    abstract fun getStatLine(value: Int, maxValue: Int): String


}

