package ocean_block.libs.formatter.ext

import ocean_block.libs.formatter.NumberFormatter

val Double.format: String get() = NumberFormatter.format(this)

