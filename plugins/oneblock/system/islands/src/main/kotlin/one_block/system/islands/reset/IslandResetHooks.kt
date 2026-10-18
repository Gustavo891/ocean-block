package one_block.system.islands.reset

import one_block.libs.instance.Instance
import one_block.system.islands.island.Island

object IslandResetHooks {

    private val hooks = mutableListOf<IslandResetHook>()

    fun register(hook: IslandResetHook) {
        hooks.add(hook)
    }

    internal fun fireTerrainReset(island: Island, area: IslandArea) {
        hooks.forEach { hook ->
            runCatching { hook.onTerrainReset(island, area) }
                .onFailure {
                    Instance.logger.warning("[islands] hook de reset falhou: ${it.message}")
                }
        }
    }

    internal fun describeAll(island: Island, area: IslandArea): List<String> =
        hooks.mapNotNull { hook ->
            runCatching { hook.describeTerrainReset(island, area) }
                .onFailure {
                    Instance.logger.warning("[islands] preview de reset falhou: ${it.message}")
                }
                .getOrNull()
        }
}
