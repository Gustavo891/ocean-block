package ocean_block.system.research.screen

import ocean_block.libs.instance.Instance
import ocean_block.system.craft.screen.RecipeEntries
import ocean_block.system.custom_items.CustomIds
import ocean_block.system.research.ResearchTimer
import ocean_block.system.research.data.ResearchManager
import ocean_block.system.research.model.ResearchCategory
import ocean_block.system.research.model.ResearchCost
import ocean_block.system.research.model.ResearchDefinition
import ocean_block.system.research.model.ResearchStatus
import ocean_block.system.research.rules.ResearchRules
import ocean_block.system.research.tree.ResearchTree
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ResearchScreen {

    const val SCREEN_ID = "pesquisa"

    private data class View(val category: ResearchCategory, val page: Int)

    private val views = ConcurrentHashMap<UUID, View>()
    private val warned = ConcurrentHashMap.newKeySet<String>()

    fun open(player: Player, islandUuid: UUID) {
        ResearchTimer.completeDue(islandUuid)
        when (DialogApi.openResult(player, SCREEN_ID) { it.bind(player, islandUuid) }) {
            DialogOpenResult.Shown, DialogOpenResult.NeedsPack -> Unit
            DialogOpenResult.MissingScreen, DialogOpenResult.Failed -> {
                player.sendRichMessage("<red>A tela de pesquisa não está disponível.")
                if (warned.add(SCREEN_ID)) Instance.logger.warning("[pesquisa] a tela '$SCREEN_ID' do Textura não abriu; confira configs/ui/$SCREEN_ID.json")
            }
        }
    }

    fun forget(id: UUID) {
        views.remove(id)
    }

    private fun DialogBinds.bind(player: Player, islandUuid: UUID) {
        val id = player.uniqueId
        text("titulo") { view(id).category.displayName }
        text("titulo_icone") { view(id).category.icon }
        text("vagas") {
            ResearchManager.read(islandUuid) { ResearchEntries.slotsText(it.running.size, ResearchRules.slots(it, ResearchTree.byId)) }
        }
        list("categorias") { ResearchEntries.categories(view(id).category) }
        list("fila") { ResearchManager.read(islandUuid) { ResearchEntries.queue(it, System.currentTimeMillis()) } }
        list("pesquisas") {
            val now = System.currentTimeMillis()
            ResearchManager.read(islandUuid) { state -> definitions(id).map { ResearchEntries.card(it, state, now) } }
        }
        listPage("pesquisas") { page(id) }
        list("paginacao") { RecipeEntries.pagination(page(id), definitions(id).size) }
        listButton("categoria") { _, index -> ResearchCategory.entries.getOrNull(index)?.let { views[id] = View(it, 0) } }
        listButton("entregar") { _, index -> definitions(id).getOrNull(index)?.let { deliver(player, islandUuid, it) } }
        button("anterior") { turn(id, -1) }
        button("proxima") { turn(id, 1) }
        button("atualizar") { ResearchTimer.completeDue(islandUuid) }
    }

    private fun view(id: UUID): View = views[id] ?: View(ResearchCategory.COLETA, 0)

    private fun definitions(id: UUID): List<ResearchDefinition> = ResearchTree.all.filter { it.category == view(id).category }

    private fun page(id: UUID): Int = RecipeEntries.clampPage(view(id).page, definitions(id).size)

    private fun turn(id: UUID, delta: Int) {
        val current = view(id)
        views[id] = current.copy(page = RecipeEntries.clampPage(page(id) + delta, definitions(id).size))
    }

    private fun deliver(player: Player, islandUuid: UUID, def: ResearchDefinition) {
        val contents = player.inventory.storageContents
        val available = def.cost.associate { cost -> cost.key to contents.filter { matches(cost, it) }.sumOf { it!!.amount } }
        val now = System.currentTimeMillis()
        val (taken, status) = ResearchManager.edit(islandUuid) { state ->
            ResearchRules.deliver(def, state, available, now, ResearchTree.byId) to ResearchRules.status(def, state, now)
        }
        if (taken.isEmpty()) {
            player.sendRichMessage("<gray>Você não tem nenhum item que falta para ${def.name}.")
            return
        }
        def.cost.forEach { cost -> remove(player, cost, taken[cost.key] ?: 0) }
        ResearchManager.saveNow(islandUuid)
        val delivered = def.cost.mapNotNull { cost -> taken[cost.key]?.let { "$it ${cost.labelFor(it)}" } }.joinToString(" ∙ ")
        player.sendRichMessage("<green>Entregue: <white>$delivered</white>.")
        when (status) {
            is ResearchStatus.Running -> player.sendRichMessage("<green>Pesquisa iniciada: <white>${def.name}</white>.")
            ResearchStatus.Queued -> player.sendRichMessage("<yellow>${def.name} entrou na fila.")
            else -> Unit
        }
    }

    private fun matches(cost: ResearchCost, stack: ItemStack?): Boolean {
        if (stack == null || stack.type.isAir || CustomIds.of(stack) != null) return false
        return if (cost.key == PLANKS) Tag.PLANKS.isTagged(stack.type) else stack.type == Material.matchMaterial(cost.key)
    }

    private fun remove(player: Player, cost: ResearchCost, amount: Int) {
        var left = amount
        val contents = player.inventory.storageContents
        contents.forEachIndexed { slot, stack ->
            if (left == 0 || !matches(cost, stack)) return@forEachIndexed
            val item = stack!!
            val used = minOf(left, item.amount)
            item.amount -= used
            left -= used
            contents[slot] = item.takeIf { it.amount > 0 }
        }
        player.inventory.storageContents = contents
    }

    private const val PLANKS = "planks"
}
