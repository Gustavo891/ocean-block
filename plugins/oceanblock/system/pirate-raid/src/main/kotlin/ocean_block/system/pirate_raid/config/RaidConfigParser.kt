package ocean_block.system.pirate_raid.config

import ocean_block.system.pirate_raid.rules.Band
import ocean_block.system.pirate_raid.rules.LootEntry
import ocean_block.system.pirate_raid.rules.LootRoller
import org.bukkit.configuration.ConfigurationSection

object RaidConfigParser {

    private const val TAG = "[piratas]"

    fun parse(config: ConfigurationSection, isMaterial: (String) -> Boolean, warn: (String) -> Unit): RaidConfig {
        val reader = Reader(config, warn)
        val d = RaidConfig.DEFAULT
        return RaidConfig(
            chance = reader.double("sorteio.chance", d.chance, 0.0, 1.0),
            intervalTicks = reader.int("sorteio.intervalo-ticks", d.intervalTicks.toInt(), 20).toLong(),
            minLevel = reader.int("sorteio.nivel-minimo", d.minLevel, 1),
            durationSeconds = reader.int("raid.duracao-segundos", d.durationSeconds, 10),
            emptySeconds = reader.int("raid.ilha-vazia-segundos", d.emptySeconds, 1),
            leashDistance = reader.double("raid.limite", d.leashDistance, 8.0, 500.0),
            boatDistance = reader.double("barcos.distancia", d.boatDistance, 8.0, 200.0),
            boatMinDistance = reader.double("barcos.distancia-minima", d.boatMinDistance, 4.0, 200.0),
            boatSpeed = reader.double("barcos.velocidade", d.boatSpeed, 0.01, 2.0),
            landingDistance = reader.double("barcos.desembarque", d.landingDistance, 1.0, 100.0),
            coinMin = reader.int("moeda.min", d.coinMin, 1),
            coinMax = reader.int("moeda.max", d.coinMax, 1),
            levelFactor = reader.double("moeda.fator-nivel", d.levelFactor, 0.0, 10.0),
            lootDespawnSeconds = reader.int("moeda.some-segundos", d.lootDespawnSeconds, 5),
            victoryCoins = reader.int("vitoria.coins", d.victoryCoins, 0),
            victoryXpFraction = reader.double("vitoria.xp-fracao", d.victoryXpFraction, 0.0, 10.0),
            bruteRolls = reader.int("brutamontes-rolagens", d.bruteRolls, 1),
            superPirate = SuperPirateConfig(
                health = reader.double("super-pirata.vida", d.superPirate.health, 1.0, 1024.0),
                rolls = reader.int("super-pirata.rolagens", d.superPirate.rolls, 1),
                coins = reader.int("super-pirata.moedas", d.superPirate.coins, 1),
            ),
            bands = bands(config, warn).ifEmpty { d.bands },
            drops = drops(config, isMaterial, warn) ?: d.drops,
        ).let { parsed ->
            if (parsed.coinMax >= parsed.coinMin) parsed
            else parsed.copy(coinMin = d.coinMin, coinMax = d.coinMax).also {
                warn("$TAG moeda.min maior que moeda.max; usando ${d.coinMin}-${d.coinMax}")
            }
        }
    }

    private fun bands(config: ConfigurationSection, warn: (String) -> Unit): List<Band> {
        val section = config.getConfigurationSection("faixas") ?: return emptyList()
        return section.getKeys(false).mapNotNull { key ->
            val level = key.toIntOrNull()
            val waves = section.getList("$key.ondas").orEmpty().map { (it as? Number)?.toInt() ?: 0 }
            val share = section.getDouble("$key.brutamontes", 0.0)
            when {
                level == null || level < 1 -> null.also { warn("$TAG faixa '$key' precisa ser um nível maior que 0; ignorada") }
                waves.isEmpty() || waves.any { it <= 0 } -> null.also { warn("$TAG faixa '$key' precisa de ondas maiores que 0; ignorada") }
                share !in 0.0..1.0 -> null.also { warn("$TAG faixa '$key' tem brutamontes fora de 0..1; ignorada") }
                else -> Band(level, waves, share)
            }
        }.sortedBy { it.minLevel }
    }

    private fun drops(config: ConfigurationSection, isMaterial: (String) -> Boolean, warn: (String) -> Unit): List<LootEntry>? {
        val section = config.getConfigurationSection("drops") ?: return null
        return section.getKeys(false).mapNotNull { key ->
            val chance = section.getDouble("$key.chance", -1.0)
            val min = section.getInt("$key.min", 1)
            val max = section.getInt("$key.max", min)
            val special = key == LootRoller.COIN || key == LootRoller.PEARL
            when {
                !special && !isMaterial(key) -> null.also { warn("$TAG drop '$key' não é um item válido; ignorado") }
                chance !in 0.0..1.0 -> null.also { warn("$TAG drop '$key' precisa de chance entre 0 e 1; ignorado") }
                min < 1 || max < min -> null.also { warn("$TAG drop '$key' tem min/max inválidos; ignorado") }
                else -> LootEntry(key, chance, min, max)
            }
        }
    }

    private class Reader(private val config: ConfigurationSection, private val warn: (String) -> Unit) {

        fun int(path: String, default: Int, min: Int): Int {
            if (!config.contains(path)) return default
            val value = (config.get(path) as? Number)?.toInt()
            if (value != null && value >= min) return value
            warn("$TAG '$path' precisa ser um número inteiro de pelo menos $min; usando $default")
            return default
        }

        fun double(path: String, default: Double, min: Double, max: Double): Double {
            if (!config.contains(path)) return default
            val value = (config.get(path) as? Number)?.toDouble()
            if (value != null && value in min..max) return value
            warn("$TAG '$path' precisa estar entre $min e $max; usando $default")
            return default
        }
    }
}
