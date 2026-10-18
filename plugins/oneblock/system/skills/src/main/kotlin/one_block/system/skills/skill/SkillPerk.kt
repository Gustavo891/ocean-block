package one_block.system.skills.skill

private val ROMAN = listOf("I", "II", "III", "IV", "V")

data class PerkTier(val tier: Int, val level: Int, val value: Double) {

    val roman: String get() = ROMAN.getOrElse(tier - 1) { tier.toString() }

    val percent: Int get() = (value * 100).toInt()
}

data class PerkUnlock(val perk: SkillPerk, val tier: PerkTier)

private fun tiers(vararg levels: Int): List<PerkTier> = levels.mapIndexed { index, level ->
    PerkTier(index + 1, level, (index + 1) / levels.size.toDouble())
}

enum class SkillPerk(
    val skill: Skill,
    val id: String,
    val prettyName: String,
    val description: String,
    val tiers: List<PerkTier>,
    val itemModel: String? = null,
) {

    Smelting(
        skill = Skill.Mining,
        id = "smelting",
        prettyName = "Fundição",
        description = "Chance do minério quebrado já dropar derretido.",
        tiers = tiers(10, 30, 50, 70),
        itemModel = "smeltery",
    ),

    ExtraFortune(
        skill = Skill.Mining,
        id = "extra_fortune",
        prettyName = "Fortuna Extra",
        description = "Chance do drop de minério vir com +1 unidade.",
        tiers = tiers(20, 40, 60, 80),
        itemModel = "extra_fortune",
    ),

    Replant(
        skill = Skill.Farming,
        id = "replant",
        prettyName = "Replantio",
        description = "Chance da plantação ser replantada ao colher.",
        tiers = tiers(10, 30, 50, 70),
    ),

    AreaHarvest(
        skill = Skill.Farming,
        id = "area_harvest",
        prettyName = "Colheita em Área",
        description = "Chance de colher 3x3 de plantações maduras de uma vez.",
        tiers = tiers(20, 40, 60, 80),
    ),

    ExtraLooting(
        skill = Skill.Combat,
        id = "extra_looting",
        prettyName = "Pilhagem Extra",
        description = "Chance do loot de mob vir com +1 unidade.",
        tiers = tiers(10, 30, 50, 70),
    ),

    Massacre(
        skill = Skill.Combat,
        id = "massacre",
        prettyName = "Massacre",
        description = "Ao matar, causa dano em área nos mobs próximos.",
        tiers = tiers(20, 40, 60, 80),
    ),

    FastBait(
        skill = Skill.Fishing,
        id = "fast_bait",
        prettyName = "Isca Rápida",
        description = "Reduz o tempo de espera até a fisgada.",
        tiers = tiers(10, 30, 50, 70),
    ),

    Net(
        skill = Skill.Fishing,
        id = "net",
        prettyName = "Rede",
        description = "Chance de puxar dois itens de uma vez.",
        tiers = tiers(20, 40, 60, 80),
    );

    val minLevel: Int get() = tiers.first().level

    fun tierAt(level: Int): PerkTier? = tiers.lastOrNull { level >= it.level }

    fun valueAt(level: Int): Double = tierAt(level)?.value ?: 0.0

    fun nextTier(level: Int): PerkTier? = tiers.firstOrNull { level < it.level }

    companion object {

        private val bySkill: Map<Skill, List<SkillPerk>> =
            entries.groupBy { it.skill }.mapValues { (_, perks) -> perks.sortedBy { it.minLevel } }

        fun of(skill: Skill): List<SkillPerk> = bySkill[skill] ?: emptyList()

        fun byId(id: String): SkillPerk? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }

        fun unlockedAt(skill: Skill, level: Int): List<PerkUnlock> = of(skill).mapNotNull { perk ->
            perk.tiers.firstOrNull { it.level == level }?.let { PerkUnlock(perk, it) }
        }
    }
}
