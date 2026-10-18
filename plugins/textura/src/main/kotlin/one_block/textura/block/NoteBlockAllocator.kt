package one_block.textura.block

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Level
import java.util.logging.Logger

class NoteBlockAllocator(
    private val file: File,
    private val logger: Logger,
) : BlockStateAllocator {

    private class State(
        val reserved: Map<String, Int>,
        val active: Set<String>,
        val next: Int,
    ) {
        val byIndex: Array<String?> = arrayOfNulls<String>(NoteBlockStates.CAPACITY).also { slots ->
            reserved.forEach { (id, index) -> if (id in active) slots[index] = id }
        }
    }

    override val baseName = "note_block"
    override val capacity = NoteBlockStates.CAPACITY - NoteBlockStates.FIRST_ALLOCATABLE

    @Volatile
    private var state = State(emptyMap(), emptySet(), NoteBlockStates.FIRST_ALLOCATABLE)

    @Volatile
    private var readOnly = false

    private var dirty = false

    private val backup = File(file.parentFile, file.name + ".bak")

    private var backupTrusted = true

    init {
        loadFromDisk()
    }

    private fun parse(source: File): YamlConfiguration? {
        if (!source.isFile) return null
        val config = YamlConfiguration()
        val failure = runCatching { config.load(source) }.exceptionOrNull()
        if (failure != null) {
            logger.log(Level.SEVERE, "Nao foi possivel ler ${source.path}", failure)
            return null
        }
        return config.takeIf { it.contains("reserved") || it.contains("next") }
    }

    private fun loadFromDisk() {
        if (!file.isFile) return

        val main = parse(file)
        val config = main ?: parse(backup)?.also {
            backupTrusted = false
            dirty = true
            logger.warning("${file.name} ilegivel ou vazio; alocacoes restauradas de ${backup.name}.")
        }
        if (config == null) {
            readOnly = true
            logger.severe(
                "Alocacoes de bloco em ${file.path} ilegiveis e sem backup valido. Ficam somente leitura ate o arquivo " +
                    "ser corrigido: nenhum bloco novo recebe variante e o arquivo nao sera sobrescrito."
            )
            return
        }

        val reserved = mutableMapOf<String, Int>()
        config.getConfigurationSection("reserved")?.let { section ->
            for (id in section.getKeys(false)) {
                val index = section.getInt(id, -1)
                if (index < NoteBlockStates.FIRST_ALLOCATABLE || index >= NoteBlockStates.CAPACITY) {
                    logger.warning("Alocacao de bloco invalida ignorada: $id -> $index")
                    continue
                }
                reserved[id] = index
            }
        }

        val saved = config.getInt("next", NoteBlockStates.FIRST_ALLOCATABLE)
        val minimum = (reserved.values.maxOrNull() ?: (NoteBlockStates.FIRST_ALLOCATABLE - 1)) + 1
        state = State(reserved, emptySet(), maxOf(saved, minimum, NoteBlockStates.FIRST_ALLOCATABLE))
    }

    @Synchronized
    override fun sync(ids: Collection<String>) {
        val current = state
        val reserved = current.reserved.toMutableMap()
        val active = mutableSetOf<String>()
        var next = current.next

        for (id in ids) {
            if (reserved[id] != null) {
                active.add(id)
                continue
            }
            if (readOnly) {
                logger.warning("Bloco '$id' ficou sem variante: alocacoes em modo somente leitura.")
                continue
            }
            if (next >= NoteBlockStates.CAPACITY) {
                logger.severe(
                    "Capacidade de blockstates de note_block esgotada ($capacity slots). " +
                        "Bloco '$id' ficou sem variante e nao aparecera no pack."
                )
                continue
            }

            reserved[id] = next
            active.add(id)
            next++
            dirty = true
        }

        state = State(reserved, active, next)
    }

    override fun propertiesOf(id: String): String? {
        val current = state
        if (id !in current.active) return null
        val index = current.reserved[id] ?: return null
        return NoteBlockStates.propertiesOf(index)
    }

    override fun idByProperties(properties: String): String? {
        val index = NoteBlockStates.indexOf(properties) ?: return null
        return state.byIndex[index]
    }

    override fun activeAllocations(): List<Pair<String, String>> {
        val current = state
        return current.reserved.entries
            .filter { it.key in current.active }
            .sortedBy { it.value }
            .map { it.key to NoteBlockStates.propertiesOf(it.value) }
    }

    fun indexOf(id: String): Int? = state.reserved[id]

    @Synchronized
    override fun save() {
        if (readOnly) return
        if (!dirty && file.isFile) return

        val current = state
        val config = YamlConfiguration()
        config.set("next", current.next)
        for ((id, index) in current.reserved.entries.sortedBy { it.value }) {
            config.set("reserved.$id", index)
        }

        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, file.name + ".tmp")
            Files.writeString(temp.toPath(), config.saveToString())
            if (file.isFile && backupTrusted) {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            backupTrusted = true
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            dirty = false
        }.onFailure {
            logger.log(Level.SEVERE, "Falha ao salvar as alocacoes de bloco em ${file.path}", it)
        }
    }
}
