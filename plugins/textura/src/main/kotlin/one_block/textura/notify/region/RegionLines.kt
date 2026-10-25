package one_block.textura.notify.region

import one_block.textura.notify.NotificationLine

internal data class ActiveLine(val line: NotificationLine, val expiresAt: Long, val startedAt: Long)

internal class RegionLines(private val maxLines: Int) {

    private val active = ArrayList<ActiveLine>()

    fun lines(): List<ActiveLine> = active.toList()

    fun isEmpty(): Boolean = active.isEmpty()

    fun push(line: NotificationLine, now: Long) {
        val entry = ActiveLine(line, now + line.durationTicks, now)
        val index = line.key?.let { key -> active.indexOfFirst { it.line.key == key } } ?: -1
        if (index >= 0) {
            active[index] = entry
            return
        }
        active.add(entry)
        while (active.size > maxLines) active.removeAt(0)
    }

    fun expire(now: Long): Boolean = active.removeIf { it.expiresAt <= now }

    fun clear(): Boolean {
        if (active.isEmpty()) return false
        active.clear()
        return true
    }
}
