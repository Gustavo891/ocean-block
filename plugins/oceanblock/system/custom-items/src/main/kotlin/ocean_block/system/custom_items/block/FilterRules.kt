package ocean_block.system.custom_items.block

object FilterRules {

    fun accepts(mode: FilterMode, filter: Set<String>, materialName: String): Boolean {
        if (filter.isEmpty()) return true
        val listed = filter.contains(materialName)
        return when (mode) {
            FilterMode.WHITELIST -> listed
            FilterMode.BLACKLIST -> !listed
        }
    }
}
