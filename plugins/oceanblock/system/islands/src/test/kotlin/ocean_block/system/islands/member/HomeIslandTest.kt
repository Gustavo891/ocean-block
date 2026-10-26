package ocean_block.system.islands.member

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandMember
import ocean_block.system.islands.position.Position
import java.util.UUID

private fun home(owner: UUID = UUID.randomUUID(), index: Int = 0) = Island(
    uuid = UUID.randomUUID(),
    ownerUuid = owner,
    ownerName = "Dono$index",
    center = SimpleLocation(0.0, 63.0, 0.0),
    position = Position.fromIndex(index),
    spawn = SimpleLocation(0.5, 64.0, 0.5),
    isPublic = false,
)

class HomeIslandTest : StringSpec({

    "a ilha propria vem antes da ilha onde e membro" {
        val owned = home()
        val other = home(index = 1).apply { addMember("alvo", IslandMember("Alvo", mutableSetOf())) }

        MemberRules.homeIsland(owned, listOf(other), "Alvo") shouldBe owned
    }

    "sem ilha propria vai para a ilha onde e membro" {
        val other = home(index = 1).apply { addMember("alvo", IslandMember("Alvo", mutableSetOf())) }

        MemberRules.homeIsland(null, listOf(home(index = 2), other), "Alvo") shouldBe other
    }

    "sem ilha propria e sem ser membro nao tem casa" {
        MemberRules.homeIsland(null, listOf(home(index = 1)), "Alvo") shouldBe null
    }
})
