package ocean_block.system.islands.dialog

import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandSetting
import ocean_block.system.islands.island.IslandUpgrade

object IslandRows {

    private val UPGRADES = IslandUpgrade.entries.filter { it.maxLevel > 0 }

    fun members(island: Island, viewerIsOwner: Boolean, online: (String) -> Boolean): List<Map<String, String>> =
        names(island).mapIndexed { index, name ->
            val owner = index == 0
            mapOf(
                "nome" to name.uppercase(),
                "cargo" to if (owner) "DONO" else "MEMBRO",
                "cargo_estilo" to if (owner) "selected" else "muted",
                "status" to if (online(name)) "ONLINE" else "OFFLINE",
                "status_estilo" to if (online(name)) "good" else "muted",
                "fixo" to (owner || !viewerIsOwner).toString(),
            )
        }

    fun memberName(island: Island, index: Int): String? = names(island).getOrNull(index)?.takeIf { index > 0 }

    fun upgrades(island: Island, affordable: (Double) -> Boolean, price: (Double) -> String): List<Map<String, String>> =
        UPGRADES.map { upgrade ->
            val level = island.getUpgradeLevel(upgrade)
            val atMax = level >= upgrade.maxLevel
            val cost = upgrade.getPrice(level)
            val canPay = !atMax && affordable(cost)
            mapOf(
                "nome" to upgrade.prettyName.uppercase(),
                "descricao" to summary(upgrade),
                "nivel" to "NÍVEL $level/${upgrade.maxLevel}",
                "preco" to if (atMax) "NÍVEL MÁXIMO" else price(cost),
                "preco_estilo" to when {
                    atMax -> "muted"
                    canPay -> "good"
                    else -> "bad"
                },
                "botao" to if (atMax) "MÁXIMO" else "MELHORAR",
                "bloqueado" to (!canPay).toString(),
            )
        }

    private fun summary(upgrade: IslandUpgrade): String = when (upgrade) {
        IslandUpgrade.Members -> "MAIS VAGAS PARA MEMBROS NA ILHA."
        IslandUpgrade.Size -> "BORDA MAIOR PARA CONSTRUIR."
        IslandUpgrade.CropGrowth -> "PLANTAÇÕES CRESCEM MAIS RÁPIDO."
        IslandUpgrade.Generator -> "O GERADOR FORMA MINÉRIOS MELHORES."
    }

    fun upgradeAt(index: Int): IslandUpgrade? = UPGRADES.getOrNull(index)

    fun settings(island: Island): List<Map<String, String>> =
        IslandSetting.entries.map { setting ->
            val enabled = island.isSettingEnabled(setting)
            mapOf(
                "nome" to setting.displayName.uppercase(),
                "descricao" to if (enabled) setting.enabledLore else setting.disabledLore,
                "estado" to if (enabled) "LIGADO" else "DESLIGADO",
                "ligado" to enabled.toString(),
            )
        }

    fun blocked(island: Island, viewerIsOwner: Boolean): List<Map<String, String>> =
        blockedNames(island).map { mapOf("nome" to it.uppercase(), "fixo" to (!viewerIsOwner).toString()) }

    fun blockedName(island: Island, index: Int): String? = blockedNames(island).getOrNull(index)

    private fun names(island: Island): List<String> =
        listOf(island.ownerName) + island.members.values.map { it.name }.sortedBy { it.lowercase() }

    private fun blockedNames(island: Island): List<String> = island.blackListNames().sorted()
}
