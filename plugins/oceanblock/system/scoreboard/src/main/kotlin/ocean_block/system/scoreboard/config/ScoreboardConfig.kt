package ocean_block.system.scoreboard.config

import ocean_block.libs.instance.Instance
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

object ScoreboardConfig {

    private const val ROOT = "boards"

    private lateinit var file: File
    private lateinit var config: FileConfiguration

    @Volatile
    var enabled: Boolean = true
        private set

    @Volatile
    private var boards: List<BoardDefinition> = emptyList()

    fun load() {
        file = File(Instance.dataFolder, "scoreboard.yml")
        if (!file.exists()) {
            Instance.dataFolder.mkdirs()
            writeDefaults()
        }

        reload()
    }

    fun reload() {
        config = YamlConfiguration.loadConfiguration(file)

        enabled = config.getBoolean("enabled", true)
        boards = readBoards()
    }

    fun boardFor(player: Player): BoardDefinition? {
        val matched = boards.firstOrNull { board ->
            board.conditions.isNotEmpty() && board.conditions.all { it.test(player) }
        }

        return matched ?: boards.firstOrNull { it.conditions.isEmpty() }
    }

    private fun readBoards(): List<BoardDefinition> {
        val root = config.getConfigurationSection(ROOT) ?: return emptyList()

        return root.getKeys(false).mapNotNull { id ->
            val section = root.getConfigurationSection(id) ?: return@mapNotNull null
            read(id, section)
        }
    }

    private fun read(id: String, section: ConfigurationSection): BoardDefinition? {
        val titles = readTitles(section)
        val scores = readScores(id, section)

        if (titles.isEmpty() && scores.isEmpty()) {
            Instance.logger.warning("[scoreboard] quadro '$id' está vazio; ignorado.")
            return null
        }

        return BoardDefinition(
            id = id,
            titles = titles.ifEmpty { listOf(" ") },
            scores = scores,
            hideNumber = section.getBoolean("defaultHideNumber", true),
            renderEvery = section.getLong("defaultRenderEvery", 20L).coerceAtLeast(1L),
            conditions = section.getStringList("conditions").map(BoardCondition::parse),
        )
    }

    private fun readTitles(section: ConfigurationSection): List<String> {
        val list = section.getStringList("titles")
        if (list.isNotEmpty()) return list

        return listOfNotNull(section.getString("titles"))
    }

    private fun readScores(id: String, section: ConfigurationSection): List<ScoreLine> {
        val scores = section.getConfigurationSection("scores") ?: return emptyList()

        return scores.getKeys(false).mapNotNull { key ->
            val score = key.toIntOrNull()
            if (score == null) {
                Instance.logger.warning("[scoreboard] score '$key' do quadro '$id' não é número; ignorado.")
                return@mapNotNull null
            }

            ScoreLine(score, scores.getString(key).orEmpty())
        }.sortedByDescending { it.score }
    }

    private fun writeDefaults() {
        val defaults = YamlConfiguration()

        defaults.options().setHeader(
            listOf(
                "Sidebar do one-block.",
                "",
                "Cada seção de 'boards' é um quadro. O primeiro cujas 'conditions' passam",
                "todas é o que o jogador vê; um quadro sem condição nenhuma é o padrão.",
                "",
                "Condições: spawn | world <nome> | permission <node> | placeholder %x% <valor>",
                "",
                "titles: string única ou lista (a lista vira animação, um quadro por ciclo).",
                "scores: a chave é o score (maior em cima), o valor é o texto MiniMessage.",
                "defaultHideNumber: esconde o número vermelho da direita (1.20.3+).",
                "defaultRenderEvery: intervalo de redesenho em ticks.",
            )
        )

        defaults.set("enabled", true)

        defaults.set("$ROOT.spawn.titles", TITLE)
        defaults.set("$ROOT.spawn.scores.15", "<gray>%date% <dark_gray>%serverid%")
        defaults.set("$ROOT.spawn.scores.14", "")
        defaults.set("$ROOT.spawn.scores.13", "<white>Localização")
        defaults.set("$ROOT.spawn.scores.12", "<aqua> Spawn")
        defaults.set("$ROOT.spawn.scores.11", "")
        defaults.set("$ROOT.spawn.scores.10", "<white>Coins: <gold>%walletcoins%")
        defaults.set("$ROOT.spawn.scores.9", "")
        defaults.set("$ROOT.spawn.scores.8", "<white>Tempo online")
        defaults.set("$ROOT.spawn.scores.7", "<aqua> %playtime%")
        defaults.set("$ROOT.spawn.scores.6", "")
        defaults.set("$ROOT.spawn.scores.0", "<yellow>www.redescreen.com")
        defaults.set("$ROOT.spawn.defaultHideNumber", true)
        defaults.set("$ROOT.spawn.defaultRenderEvery", 20)
        defaults.set("$ROOT.spawn.conditions", listOf("spawn"))

        defaults.set("$ROOT.default.titles", TITLE)
        defaults.set("$ROOT.default.scores.16", "")
        defaults.set("$ROOT.default.scores.15", "<white> Coins: <dark_green>$<white>%walletcoins%")
        defaults.set("$ROOT.default.scores.14", "")
        defaults.set("$ROOT.default.scores.13", "<white> <glyph:floating_islands> <color:#FFD54F>Sua ilha:")
        defaults.set("$ROOT.default.scores.12", "<white> Fase: <gray>%island_phasename%")
        defaults.set("$ROOT.default.scores.10", "")
        defaults.set("$ROOT.default.scores.9", "<white> Tempo online: <aqua>%playtime%")
        defaults.set("$ROOT.default.scores.7", "")
        defaults.set("$ROOT.default.scores.0", "  <gray>redescreen.com ")
        defaults.set("$ROOT.default.defaultHideNumber", true)
        defaults.set("$ROOT.default.defaultRenderEvery", 20)
        defaults.set("$ROOT.default.conditions", emptyList<String>())

        defaults.save(file)
    }

    private const val TITLE = "<glyph:scoreboard_logo>"
}
