package one_block.system.minions.enums

import one_block.system.minions.behavior.FarmerBehavior
import one_block.system.minions.behavior.MinerBehavior
import one_block.system.minions.behavior.MinionBehavior
import one_block.system.minions.behavior.TerminatorBehavior
import one_block.system.minions.placed_minion.PlacedMinion
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import java.util.UUID

enum class MinionType(
    val prettyName: String,
    val headValue: String,
    val desc: List<String> = emptyList(),
    private val behavior: MinionBehavior,
    val itemModel: NamespacedKey? = null,
    val producedLabel: String = "Ações",
) {

    Miner(
        prettyName = "Minerador",
        headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWExYjUzODk3MTdhNjFhMGU5NDIxYzk3OTg4NDA1ZDVhMzM2YTVhYzFlZThlMGYyZjM1Njc4ZDFkNjFmNDU1MCJ9fX0=",
        desc = listOf("Minera o bloco abaixo dele", "e dropa os itens no chão."),
        behavior = MinerBehavior(),
        itemModel = NamespacedKey("oneblock", "miner"),
        producedLabel = "Blocos quetttttbrados",
    ),

    Farmer(
        prettyName = "Fazendeiro",
        headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2E4YWIyMjY3YWU0NDNlODNiMDVlM2E4ZDY3YjhiNzYwYWRmYWFkMzM1YzkwNDczMzhjMGUxNzc0YTY1YzM3MiJ9fX0=",
        behavior = FarmerBehavior(),
        itemModel = NamespacedKey("oneblock", "farmer"),
        producedLabel = "Plantações colhidas",
    ),

    Terminator(
        prettyName = "Exterminador",
        headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGQyYmQ5ZGQwYWIwNWRmM2Q4MjIyODJhNjRmZjZmMGMyZTJhMGExYTNhMWY2YzUzZmZmMDBhOTU5MzEzZTFjOCJ9fX0=",
        behavior = TerminatorBehavior(),
        itemModel = NamespacedKey("oneblock", "knight"),
        producedLabel = "Mobs mortos",
    );

    fun spawn(placedMinion: PlacedMinion, location: Location) = behavior.spawn(placedMinion, location)
    fun placement(clickedBlock: Block, face: BlockFace) = behavior.placement(clickedBlock, face)
    fun tick(placedMinion: PlacedMinion): Boolean = behavior.tick(placedMinion)
    fun onUnregister(uuid: UUID) = behavior.onUnregister(uuid)
    fun updateDisplay(placedMinion: PlacedMinion) = behavior.updateDisplay(placedMinion)

}
