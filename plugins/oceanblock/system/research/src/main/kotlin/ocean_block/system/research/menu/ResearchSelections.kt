package ocean_block.system.research.menu

import ocean_block.system.research.model.ResearchCategory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ResearchSelections {

    private val categories = ConcurrentHashMap<UUID, ResearchCategory>()

    fun get(id: UUID): ResearchCategory = categories[id] ?: ResearchCategory.entries.first()

    fun set(id: UUID, category: ResearchCategory) {
        categories[id] = category
    }

    fun forget(id: UUID) {
        categories.remove(id)
    }
}
