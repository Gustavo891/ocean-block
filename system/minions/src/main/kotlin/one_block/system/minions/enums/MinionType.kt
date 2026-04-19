package one_block.system.minions.enums

import one_block.system.minions.behavior.FarmerBehavior
import one_block.system.minions.behavior.MinerBehavior
import one_block.system.minions.behavior.MinionBehavior
import one_block.system.minions.behavior.TerminatorBehavior
import one_block.system.minions.placed_minion.PlacedMinion
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import java.util.UUID

enum class MinionType(
    val prettyName: String,
    val headValue: String,
    val desc: List<String> = emptyList(),
    val acceptedToolMaterials: Set<Material>,
    val defaultTool: Material,
    val toolName: String,
    private val behavior: MinionBehavior,
) {

    Miner(
        prettyName = "Minerador",
        headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWExYjUzODk3MTdhNjFhMGU5NDIxYzk3OTg4NDA1ZDVhMzM2YTVhYzFlZThlMGYyZjM1Njc4ZDFkNjFmNDU1MCJ9fX0=",
        desc = listOf("Minera o bloco abaixo dele", "e dropa os itens no chão."),
        acceptedToolMaterials = setOf(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
        ),
        defaultTool = Material.DIAMOND_PICKAXE,
        toolName = "Picareta",
        behavior = MinerBehavior(),
    ),

    Farmer(
        prettyName = "Fazendeiro",
        headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2E4YWIyMjY3YWU0NDNlODNiMDVlM2E4ZDY3YjhiNzYwYWRmYWFkMzM1YzkwNDczMzhjMGUxNzc0YTY1YzM3MiJ9fX0=",
        acceptedToolMaterials = setOf(
            Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
            Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,
        ),
        defaultTool = Material.DIAMOND_HOE,
        toolName = "Enxada",
        behavior = FarmerBehavior(),
    ),

    Terminator(
        prettyName = "Exterminador",
        headValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGQyYmQ5ZGQwYWIwNWRmM2Q4MjIyODJhNjRmZjZmMGMyZTJhMGExYTNhMWY2YzUzZmZmMDBhOTU5MzEzZTFjOCJ9fX0=",
        acceptedToolMaterials = setOf(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
        ),
        defaultTool = Material.DIAMOND_SWORD,
        toolName = "Espada",
        behavior = TerminatorBehavior(),
    );

    fun spawn(placedMinion: PlacedMinion, location: Location) = behavior.spawn(placedMinion, location)
    fun tick(placedMinion: PlacedMinion): Boolean = behavior.tick(placedMinion)
    fun onUnregister(uuid: UUID) = behavior.onUnregister(uuid)
    fun updateDisplay(placedMinion: PlacedMinion) = behavior.updateDisplay(placedMinion)

}
