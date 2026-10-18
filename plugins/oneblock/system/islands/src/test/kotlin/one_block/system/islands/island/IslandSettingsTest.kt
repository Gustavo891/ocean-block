package one_block.system.islands.island

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import one_block.system.islands.listener.IslandSettingListener
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason

class IslandSettingsTest : StringSpec({

    "mapa nulo usa o default da setting" {
        IslandSetting.entries.forEach { setting ->
            IslandSettings.isEnabled(null, setting) shouldBe setting.defaultEnabled
        }
    }

    "chave ausente usa o default da setting" {
        val settings = mapOf(IslandSetting.Redstone.name to false)

        IslandSettings.isEnabled(settings, IslandSetting.LeafDecay) shouldBe
            IslandSetting.LeafDecay.defaultEnabled
    }

    "valor explicito tem prioridade sobre o default" {
        val settings = mapOf(
            IslandSetting.Redstone.name to false,
            IslandSetting.LeafDecay.name to true,
        )

        IslandSettings.isEnabled(settings, IslandSetting.Redstone) shouldBe false
        IslandSettings.isEnabled(settings, IslandSetting.LeafDecay) shouldBe true
    }

    "toda setting comeca ligada para nao mudar o comportamento de ilhas existentes" {
        IslandSetting.entries.forEach { setting ->
            setting.defaultEnabled shouldBe true
        }
    }

    "spawn de fase e minion nao conta como spawn natural" {
        (SpawnReason.CUSTOM in IslandSettingListener.NATURAL_SPAWN_REASONS) shouldBe false
        (SpawnReason.SPAWNER in IslandSettingListener.NATURAL_SPAWN_REASONS) shouldBe false
        (SpawnReason.NATURAL in IslandSettingListener.NATURAL_SPAWN_REASONS) shouldBe true
    }

    "acender fogo na mao nao conta como propagacao" {
        val causes = IslandSettingListener.FIRE_SPREAD_CAUSES

        (org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL in causes) shouldBe false
        (org.bukkit.event.block.BlockIgniteEvent.IgniteCause.SPREAD in causes) shouldBe true
        (org.bukkit.event.block.BlockIgniteEvent.IgniteCause.LAVA in causes) shouldBe true
    }
})
