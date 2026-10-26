package ocean_block.system.islands.dialog

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandMember
import ocean_block.system.islands.island.IslandSetting
import ocean_block.system.islands.island.IslandUpgrade
import ocean_block.system.islands.position.Position
import java.util.UUID

private val OWNER = UUID.randomUUID()

private fun island() = Island(
    uuid = UUID.randomUUID(),
    ownerUuid = OWNER,
    ownerName = "Dono",
    center = SimpleLocation(0.0, 63.0, 0.0),
    position = Position.fromIndex(0),
    spawn = SimpleLocation(0.5, 64.0, 0.5),
    isPublic = false,
)

class IslandRowsTest : StringSpec({

    "membros: dono primeiro, depois membros em ordem, com cargo e status" {
        val island = island().apply {
            addMember("zeca", IslandMember("Zeca", mutableSetOf()))
            addMember("ana", IslandMember("Ana", mutableSetOf()))
        }

        val rows = IslandRows.members(island, viewerIsOwner = true) { it == "Ana" }

        rows.map { it["nome"] } shouldContainExactly listOf("DONO", "ANA", "ZECA")
        rows.map { it["cargo"] } shouldContainExactly listOf("DONO", "MEMBRO", "MEMBRO")
        rows[0]["cargo_estilo"] shouldBe "selected"
        rows[1]["status"] shouldBe "ONLINE"
        rows[1]["status_estilo"] shouldBe "good"
        rows[2]["status"] shouldBe "OFFLINE"
        rows[2]["status_estilo"] shouldBe "muted"
    }

    "membros: só o dono remove, e nunca a si mesmo" {
        val island = island().apply { addMember("ana", IslandMember("Ana", mutableSetOf())) }

        IslandRows.members(island, viewerIsOwner = true) { false }.map { it["fixo"] } shouldContainExactly listOf("true", "false")
        IslandRows.members(island, viewerIsOwner = false) { false }.map { it["fixo"] } shouldContainExactly listOf("true", "true")
        IslandRows.memberName(island, 1) shouldBe "Ana"
        IslandRows.memberName(island, 0) shouldBe null
    }

    "melhorias: só as que têm nível, com preço, nível e botão" {
        val island = island().apply { setUpgradeLevel(IslandUpgrade.Members, 1) }

        val rows = IslandRows.upgrades(island, { it <= 30_000.0 }) { "$" + it.toLong() }

        rows.map { it["nome"] } shouldContainExactly listOf("MEMBROS", "BORDA", "GERADOR")
        rows[0]["nivel"] shouldBe "NÍVEL 1/1"
        rows[0]["preco"] shouldBe "NÍVEL MÁXIMO"
        rows[0]["preco_estilo"] shouldBe "muted"
        rows[0]["botao"] shouldBe "MÁXIMO"
        rows[0]["bloqueado"] shouldBe "true"
        rows[2]["nivel"] shouldBe "NÍVEL 0/4"
        rows[2]["preco"] shouldBe "$25000"
        rows[2]["preco_estilo"] shouldBe "good"
        rows[2]["botao"] shouldBe "MELHORAR"
        rows[2]["bloqueado"] shouldBe "false"
        IslandRows.upgradeAt(2) shouldBe IslandUpgrade.Generator
    }

    "melhorias: sem moedas o preço fica vermelho e o botão desliga" {
        val rows = IslandRows.upgrades(island(), { false }) { "$" + it.toLong() }

        rows[2]["preco_estilo"] shouldBe "bad"
        rows[2]["bloqueado"] shouldBe "true"
    }

    "configurações: uma linha por ajuste com estado e descrição do estado atual" {
        val island = island()

        val rows = IslandRows.settings(island)

        rows.size shouldBe IslandSetting.entries.size
        val fire = rows[IslandSetting.FireSpread.ordinal]
        fire["nome"] shouldBe "PROPAGAÇÃO DE FOGO"
        fire["ligado"] shouldBe "false"
        fire["descricao"] shouldBe IslandSetting.FireSpread.disabledLore
    }

    "visitas: bloqueados em ordem e só o dono desbloqueia" {
        val island = island().apply { blackList = mutableSetOf("zeca", "ana") }

        IslandRows.blocked(island, viewerIsOwner = true).map { it["nome"] } shouldContainExactly listOf("ANA", "ZECA")
        IslandRows.blocked(island, viewerIsOwner = false).map { it["fixo"] } shouldContainExactly listOf("true", "true")
        IslandRows.blockedName(island, 1) shouldBe "zeca"
    }
})
