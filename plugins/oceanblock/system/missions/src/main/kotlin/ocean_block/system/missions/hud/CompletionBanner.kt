package ocean_block.system.missions.hud

class CompletionBanner(private val durationTicks: Long) {

    private data class Entry(val text: String, val start: Long, val end: Long)

    private var entries: List<Entry> = emptyList()

    fun push(texts: List<String>, now: Long) {
        val live = entries.filter { it.end > now }
        var start = maxOf(now, live.lastOrNull()?.end ?: now)
        val added = texts.map { text -> Entry(text, start, start + durationTicks).also { start = it.end } }
        entries = live + added
    }

    fun current(now: Long): String? = entries.firstOrNull { now >= it.start && now < it.end }?.text

    fun ends(): List<Long> = entries.map { it.end }
}
