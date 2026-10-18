package ocean_block.system.islands.island

object IslandSettings {

    fun isEnabled(settings: Map<String, Boolean>?, setting: IslandSetting): Boolean =
        settings?.get(setting.name) ?: setting.defaultEnabled
}
