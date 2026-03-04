package one_block.libs.formatter.ext

import one_block.libs.formatter.NumberFormatter

val Double.format: String get() = NumberFormatter.format(this)

