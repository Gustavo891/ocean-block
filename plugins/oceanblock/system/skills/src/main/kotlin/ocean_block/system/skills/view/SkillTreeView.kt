package ocean_block.system.skills.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.skills.skill.Skill
import ocean_block.system.skills.skill.SkillData
import ocean_block.system.skills.skill.SkillLevelReward
import ocean_block.system.skills.skill.SkillManager
import ocean_block.system.skills.skill.SkillPerk
import ocean_block.system.skills.skill.SkillRewards
import ocean_block.system.skills.skill.SkillUnlockInfo
import ocean_block.system.skills.skill.SkillUnlocks
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class SkillTreeView : View() {

    private companion object {

        const val NAMESPACE = "oneblock"

        const val ROWS = 6
        const val COLUMNS = 9
        const val TRACK_ROWS = ROWS - 2
        const val SEGMENTS = (COLUMNS + 1) / 2
        const val SEGMENT_PERIOD = TRACK_ROWS + 1
        const val LEVELS_PER_PAGE = SEGMENTS * SEGMENT_PERIOD - 1

        const val MENU_ROWS = TRACK_ROWS + 1
        const val NAV_ROW = MENU_ROWS - 1

        val NAV_SLOTS = setOf(
            NAV_ROW * COLUMNS + 3,
            NAV_ROW * COLUMNS + 4,
            NAV_ROW * COLUMNS + 5,
        )

        val LAST_PAGE = (Skill.MAX_LEVEL - 1) / LEVELS_PER_PAGE

        val BACKGROUND = Mini.mm.deserialize("<white><shift:-8><glyph:skill_tree>")
    }

    private enum class BranchArrow {
        Left,
        Right,
        Up,
        Down;

        val id: String get() = name.lowercase()

        val opposite: BranchArrow
            get() = when (this) {
                Left -> Right
                Right -> Left
                Up -> Down
                Down -> Up
            }
    }

    private data class BranchPlacement(val level: Int, val arrow: BranchArrow)

    private data class TrackLayout(
        val nodeBySlot: Map<Int, Int>,
        val branchBySlot: Map<Int, BranchPlacement>,
    ) {

        fun branchSideOf(level: Int): BranchArrow? =
            branchBySlot.values.firstOrNull { it.level == level }?.arrow?.opposite
    }

    private val skillState = initialState<Skill>()
    private val pageState = mutableState(0)
    private val layoutCache = mutableMapOf<Pair<Skill, Int>, TrackLayout>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(BACKGROUND)
        config.layout(
            "         ",
            "         ",
            "         ",
            "         ",
            "   PBN   "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        for (slot in 0 until MENU_ROWS * COLUMNS) {
            if (slot in NAV_SLOTS) continue
            render.slot(slot).renderWith { trackItem(render, slot) }
        }

        render.layoutSlot('P')
            .renderWith { pageArrowItem(render, -1) }
            .onClick { click -> turnPage(click, -1) }

        render.layoutSlot('N')
            .renderWith { pageArrowItem(render, 1) }
            .onClick { click -> turnPage(click, 1) }

        render.layoutSlot('B')
            .renderWith { blankItem() }
            .onClick { click -> click.openForPlayer(SkillsView::class.java) }
    }

    private fun slotOf(offset: Int): Int {
        val segment = offset / SEGMENT_PERIOD
        val within = offset % SEGMENT_PERIOD
        val goingDown = segment % 2 == 0

        val isTurn = within == TRACK_ROWS

        val column = if (isTurn) segment * 2 + 1 else segment * 2
        val row = when {
            isTurn -> if (goingDown) TRACK_ROWS - 1 else 0
            goingDown -> within
            else -> TRACK_ROWS - 1 - within
        }

        return row * COLUMNS + column
    }

    private fun turnPage(click: SlotClickContext, delta: Int) {
        val target = pageState.get(click) + delta
        if (target !in 0..LAST_PAGE) return

        pageState.set(target, click)
        click.player.playSound(click.player, Sound.UI_BUTTON_CLICK, 1f, 1f)
        click.update()
    }

    private fun levelOf(page: Int, offset: Int): Int? {
        val level = page * LEVELS_PER_PAGE + offset + 1
        return if (level > Skill.MAX_LEVEL) null else level
    }

    private fun currentLevel(ctx: Context): Int =
        SkillManager.get(ctx.player.uniqueId)?.levelOf(skillState.get(ctx)) ?: 0

    private fun layoutFor(skill: Skill, page: Int): TrackLayout = layoutCache.getOrPut(skill to page) {
        val pathSlots = (0 until LEVELS_PER_PAGE).map { slotOf(it) }.toSet()
        val nodeBySlot = mutableMapOf<Int, Int>()

        for (offset in 0 until LEVELS_PER_PAGE) {
            levelOf(page, offset) ?: continue
            nodeBySlot[slotOf(offset)] = offset
        }

        val branchBySlot = mutableMapOf<Int, BranchPlacement>()
        val taken = mutableSetOf<Int>()

        for (offset in 0 until LEVELS_PER_PAGE) {
            val level = levelOf(page, offset) ?: continue
            if (!hasBranch(skill, level)) continue

            val slot = slotOf(offset)
            val free = neighboursOf(slot / COLUMNS, slot % COLUMNS)
                .firstOrNull { (candidate, _) -> candidate !in pathSlots && candidate !in taken }
                ?: continue

            branchBySlot[free.first] = BranchPlacement(level, free.second)
            taken += free.first
        }

        TrackLayout(nodeBySlot, branchBySlot)
    }

    private fun neighboursOf(row: Int, column: Int): List<Pair<Int, BranchArrow>> = listOf(
        Triple(row, column + 1, BranchArrow.Left),
        Triple(row + 1, column, BranchArrow.Up),
        Triple(row - 1, column, BranchArrow.Down),
        Triple(row, column - 1, BranchArrow.Right),
    )
        .filter { (r, c, _) -> r in 0 until MENU_ROWS && c in 0 until COLUMNS }
        .map { (r, c, arrow) -> r * COLUMNS + c to arrow }
        .filterNot { (slot, _) -> slot in NAV_SLOTS }

    private fun hasBranch(skill: Skill, level: Int): Boolean =
        SkillPerk.unlockedAt(skill, level).isNotEmpty() ||
            SkillRewards.at(skill, level) != null ||
            SkillUnlocks.at(skill, level).isNotEmpty()

    private fun trackItem(ctx: Context, slot: Int): ItemStack {
        val page = pageState.get(ctx)
        val layout = layoutFor(skillState.get(ctx), page)

        layout.nodeBySlot[slot]?.let { offset ->
            val level = levelOf(page, offset) ?: return ItemStack(Material.AIR)
            return nodeItem(ctx, level, layout.branchSideOf(level))
        }

        layout.branchBySlot[slot]?.let { return branchItem(ctx, it.level) }

        return ItemStack(Material.AIR)
    }

    private fun nodeItem(ctx: Context, level: Int, branchSide: BranchArrow?): ItemStack {
        val skill = skillState.get(ctx)
        val current = currentLevel(ctx)

        val reached = level <= current
        val isNext = level == current + 1

        return itemStack(Material.PAPER) {
            meta<ItemMeta> {
                itemModel = stateModel(reached, isNext, branchSide)

                name("${stateColor(reached, isNext)}Nível $level")

                addLore {
                    if (branchSide == null) {
                        SkillPerk.unlockedAt(skill, level).firstOrNull()?.let { unlock ->
                            +"<gold>Perk: <white>${unlock.perk.prettyName} ${unlock.tier.roman} <gray>(${unlock.tier.percent}%)"
                            +"<gray>${unlock.perk.description}"
                            +""
                        }

                        SkillRewards.at(skill, level)?.let { reward ->
                            +"<gold>Recompensa:"
                            +rewardLines(reward)
                            +""
                        }

                        unlockLines(skill, level).takeIf { it.isNotEmpty() }?.let { lines ->
                            +"<gold>Desbloqueia:"
                            +lines
                            +""
                        }
                    }

                    +"<dark_gray>▸ <white>XP necessário: <aqua>${costText(ctx, level, isNext)}"
                    +""
                    when {
                        reached -> +"<green>✔ Alcançado."
                        isNext -> +"<gold>➜ Próximo nível."
                        else -> +"<red>✖ Bloqueado."
                    }
                }
            }
        }
    }

    private fun branchItem(ctx: Context, level: Int): ItemStack {
        val skill = skillState.get(ctx)
        val current = currentLevel(ctx)

        val reached = level <= current
        val isNext = level == current + 1

        val perk = SkillPerk.unlockedAt(skill, level).firstOrNull()
        val reward = SkillRewards.at(skill, level)
        val unlocks = SkillUnlocks.at(skill, level)

        return itemStack(Material.PAPER) {
            meta<ItemMeta> {
                itemModel = branchModel(perk?.perk, unlocks)

                val label = when {
                    perk != null -> "Perk: ${perk.perk.prettyName} ${perk.tier.roman}"
                    unlocks.size == 1 -> unlocks.first().prettyName
                    unlocks.isNotEmpty() -> "Desbloqueios"
                    else -> "Recompensa"
                }
                name("${stateColor(reached, isNext)}$label <dark_gray>• nível $level")

                addLore {
                    if (perk != null) {
                        +"<gray>${perk.perk.description}"
                        +"<dark_gray>▸ <white>Chance: <aqua>${perk.tier.percent}%"
                        +""
                    }

                    if (reward != null) {
                        +"<gold>Recompensa:"
                        +rewardLines(reward)
                        +""
                    }

                    unlockLines(skill, level).takeIf { it.isNotEmpty() }?.let { lines ->
                        +"<gold>Desbloqueia:"
                        +lines
                        +""
                    }

                    when {
                        reached -> +"<green>✔ Liberado."
                        isNext -> +"<gold>➜ No próximo nível."
                        else -> +"<red>✖ Requer nível $level <dark_gray>($current)"
                    }
                }
            }
        }
    }

    private fun branchModel(perk: SkillPerk?, unlocks: List<SkillUnlockInfo>): NamespacedKey {
        val model = when {
            perk != null -> perk.itemModel ?: "skill"
            unlocks.any { it.craft } -> "craft"
            unlocks.isNotEmpty() -> "skill"
            else -> "reward"
        }

        return NamespacedKey(NAMESPACE, model)
    }

    private fun infoItem(ctx: Context): ItemStack {
        val skill = skillState.get(ctx)
        val data = SkillManager.get(ctx.player.uniqueId)

        return itemStack(skill.icon) {
            meta<ItemMeta> {
                name("<green>${skill.prettyName}")

                addLore {
                    if (data == null) {
                        +"<red>Dados não carregados."
                        return@addLore
                    }

                    val level = data.levelOf(skill)
                    val percent = (skill.bonusAt(level) * 100).toInt()
                    val unlockedPerks = skill.perks.count { level >= it.minLevel }

                    +"<dark_gray>▸ <white>Nível: <aqua>$level<dark_gray>/${Skill.MAX_LEVEL}"
                    +"<dark_gray>▸ <white>Bônus: <aqua>+$percent%<white> de ${skill.bonusDescription}"
                    +"<dark_gray>▸ <white>Perks: <aqua>$unlockedPerks<dark_gray>/${skill.perks.size}"
                    +"<dark_gray>▸ <white>XP por ação: <aqua>${skill.xpPerAction}"
                    +""
                    +SkillProgress.line(data, skill)
                    +""
                    +nextBranchLine(skill, data)
                }
            }
        }
    }

    private fun nextBranchLine(skill: Skill, data: SkillData): String {
        val level = data.levelOf(skill)

        val nextPerk = skill.perks.mapNotNull { it.nextTier(level)?.level }.minOrNull()
        val nextReward = SkillRewards.nextAfter(skill, level)?.level
        val nextUnlock = SkillUnlocks.nextAfter(skill, level)
        val next = listOfNotNull(nextPerk, nextReward, nextUnlock).minOrNull()
            ?: return "<gold>Árvore concluída."

        return "<dark_gray>Próximo galho: <white>nível $next"
    }

    private fun unlockLines(skill: Skill, level: Int): List<String> =
        SkillUnlocks.at(skill, level).map { "<dark_gray>▪ <gray>${it.prettyName}" }

    private fun rewardLines(reward: SkillLevelReward): List<String> = buildList {
        if (reward.coins > 0.0) add("<dark_gray>▪ <yellow>${reward.coins.toLong()} coins")

        reward.rewards.forEach { item ->
            val material = item.material ?: return@forEach
            val prettyName = material.name.lowercase().replace('_', ' ')
            add("<dark_gray>▪ <gray>${item.amount ?: 1}x $prettyName")
        }
    }

    private fun stateModel(reached: Boolean, isNext: Boolean, branchSide: BranchArrow?): NamespacedKey {
        if (branchSide == null) {
            return NamespacedKey(
                NAMESPACE,
                when {
                    reached -> "desbloqueado"
                    isNext -> "andamento"
                    else -> "bloqueado"
                },
            )
        }

        val state = when {
            reached -> "sucess"
            isNext -> "current"
            else -> "bloq"
        }

        return NamespacedKey(NAMESPACE, "$state-${branchSide.id}")
    }

    private fun stateColor(reached: Boolean, isNext: Boolean): String = when {
        reached -> "<green>"
        isNext -> "<gold>"
        else -> "<red>"
    }

    private fun costText(ctx: Context, level: Int, isNext: Boolean): String {
        val cost = Skill.xpForLevel(level)
        val costText = NumberFormatter.format(cost.toDouble())

        if (!isNext) return costText

        val data = SkillManager.get(ctx.player.uniqueId) ?: return costText
        val progress = NumberFormatter.format(data.progressOf(skillState.get(ctx)).toDouble())

        return "$progress<dark_gray>/<aqua>$costText"
    }

    private fun pageArrowItem(ctx: Context, step: Int) = itemStack(Material.PAPER) {
        val target = pageState.get(ctx) + step
        val available = target in 0..LAST_PAGE

        meta<ItemMeta> {
            if (!available) {
                isHideTooltip = true
                itemModel = NamespacedKey(NAMESPACE, "blank")
                name("<dark_gray>")
                return@meta
            }

            itemModel = NamespacedKey(NAMESPACE, if (step > 0) "forward" else "backward")
            name(if (step > 0) "<yellow>Próxima página" else "<yellow>Página anterior")

            addLore {
                val from = target * LEVELS_PER_PAGE + 1
                val to = ((target + 1) * LEVELS_PER_PAGE).coerceAtMost(Skill.MAX_LEVEL)

                +"<gray>Página <white>${target + 1}<gray>/<white>${LAST_PAGE + 1}"
                +"<gray>Níveis <white>$from<gray>–<white>$to"
            }
        }
    }

    private fun blankItem() = itemStack(Material.PAPER) {
        meta<ItemMeta> {
            itemModel = NamespacedKey(NAMESPACE, "blank")
            isHideTooltip = true
        }
    }
}
