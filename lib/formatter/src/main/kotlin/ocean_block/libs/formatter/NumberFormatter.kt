package ocean_block.libs.formatter

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.regex.Pattern
import kotlin.math.ln

object NumberFormatter {

    private val PATTERN: Pattern = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)([A-Z]+)?")

    private val FORMATS = arrayOf(
        arrayOf(
            "", "K", "M", "B", "T", "Q", "QQ", "S", "SS", "O", "N",
            "D", "UN", "DD", "TR", "QT", "QN", "SD", "SPD", "OD",
            "ND", "VG", "UVG", "DVG", "TVG", "QTV", "QNV", "SEV",
            "SPV", "OVG", "NVG", "ITG"
        ),
        arrayOf(
            1.0, 1000.0, 1000000.0, 1.0E9, 1.0E12,
            1.0E15, 1.0E18, 1.0E21, 1.0E24, 1.0E27, 1.0E30,
            1.0E33, 1.0E36, 1.0E39, 1.0E42, 1.0E45, 1.0E48,
            1.0E51, 1.0E54, 1.0E57, 1.0E60, 1.0E63, 1.0E66,
            1.0E69, 1.0E72, 1.0E75, 1.0E78, 1.0E81, 1.0E84,
            1.0E87, 1.0E90, 1.0E93
        )
    )


    private val decimalFormatSmall = DecimalFormat("#,##0.##", DecimalFormatSymbols().apply {
        decimalSeparator = '.'
        groupingSeparator = ','
    })

    private val decimalFormatBig = DecimalFormat("#.##", DecimalFormatSymbols().apply {
        decimalSeparator = '.'
        groupingSeparator = ','
    })

    private val log: Double = 6.907755278982137


    fun formatNumber(number: Double): String {
        if (number < 1_000_000) {
            return decimalFormatSmall.format(number)
        }

        return try {
            val index = (ln(number) / log).toInt()
            val value = number / (FORMATS[1][index] as Double)
            decimalFormatBig.format(value) + FORMATS[0][index]
        } catch (e: Exception) {
            "?"
        }
    }

    fun format(number: Double): String {
        return formatNumber(number)
    }

    fun deform(s: String): Double {
        val string = s.uppercase()
        val matcher = PATTERN.matcher(string)

        if (matcher.matches()) {
            val suffix = matcher.group(2) ?: ""

            val valueString = matcher.group(1)
            val value = if (suffix.isEmpty()) {
                valueString.replace(".", "").toDouble()
            } else {
                valueString.toDouble()
            }

            val index = FORMATS[0].indexOf(suffix)
            val multiplier = if (index >= 0) FORMATS[1][index] as Double else 1.0

            return value * multiplier
        }

        return s.replace(".", "").toDoubleOrNull() ?: 0.0
    }



}
