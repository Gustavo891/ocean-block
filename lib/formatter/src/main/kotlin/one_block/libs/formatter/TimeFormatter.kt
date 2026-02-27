package one_block.libs.formatter

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object TimeFormatter {

    private val DURATION_PATTERN = Pattern.compile("(\\d+)\\s*([dhms])")

    fun parse(input: String): Long {
        val matcher = DURATION_PATTERN.matcher(input.lowercase())
        var millis = 0L

        while (matcher.find()) {
            val value = matcher.group(1).toLong()
            val unit = matcher.group(2)

            when (unit) {
                "d" -> millis += TimeUnit.DAYS.toMillis(value)
                "h" -> millis += TimeUnit.HOURS.toMillis(value)
                "m" -> millis += TimeUnit.MINUTES.toMillis(value)
                "s" -> millis += TimeUnit.SECONDS.toMillis(value)
            }
        }

        return millis
    }

    fun formatReduced(time: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(time).toInt()
        val hours = (TimeUnit.MILLISECONDS.toHours(time) % 24)
        val minutes = (TimeUnit.MILLISECONDS.toMinutes(time) % 60)
        val seconds = (TimeUnit.MILLISECONDS.toSeconds(time) % 60)

        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0) append("${seconds}s")
            if (isEmpty()) append("${time / 100}s")
        }.trim()
    }
}