package ocean_block.system.islands.island

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.system.islands.position.Position
import java.util.UUID

private fun island(size: Int = 50) = Island(
    uuid = UUID.randomUUID(),
    ownerUuid = UUID.randomUUID(),
    ownerName = "Dono",
    center = SimpleLocation(0.0, 63.0, 0.0),
    position = Position.fromIndex(0),
    spawn = SimpleLocation(0.5, 64.0, 0.5),
    size = size,
    isPublic = false,
)

class IslandBorderTest : StringSpec({

    "bordas por nivel vao de 50 a 150" {
        Island.BORDER_SIZE shouldBe listOf(50, 70, 100, 150)
    }

    "ilha nova comeca com borda 50" {
        island().borderSize() shouldBe 50
    }

    "upgrade de borda sobe para 70" {
        val island = island()
        island.setUpgradeLevel(IslandUpgrade.Size, 1)

        island.borderSize() shouldBe 70
    }

    "ilha antiga salva com 30 usa a borda do nivel" {
        island(size = 30).borderSize() shouldBe 50
    }
})
