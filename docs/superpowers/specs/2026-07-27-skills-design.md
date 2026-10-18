# Sistema de Skills — Design Detalhado

Data: 2026-07-27
Módulo: `plugins/oneblock/system/skills`
Relacionado: [progressão dos atos](../plans/2026-07-26-progressao-atos.md) · [mapa de progressão](../plans/2026-07-26-mapa-progressao.md) · [spec de progressão](2026-07-26-progressao-oneblock-design.md)

Skills são o **Eixo B** da progressão: o relógio de semanas. Diferente da fase (que reseta no prestígio) e do dinheiro (que é gasto), o nível de skill é permanente e acompanha o jogador para sempre. É o que ele mostra pros outros.

---

## 1. Estado atual do módulo

Já implementado:

| Arquivo | O que faz |
|---|---|
| [Skill.kt](../../../plugins/oneblock/system/skills/src/main/kotlin/one_block/system/skills/skill/Skill.kt) | Enum das 4 skills, curva de XP, `bonusAt(level)` |
| [SkillData.kt](../../../plugins/oneblock/system/skills/src/main/kotlin/one_block/system/skills/skill/SkillData.kt) | POJO Mongo com `xp: MutableMap<String, Long>` |
| [SkillManager.kt](../../../plugins/oneblock/system/skills/src/main/kotlin/one_block/system/skills/skill/SkillManager.kt) | Cache em memória, `dirtyIds`, flush 30s |
| [Skills.kt](../../../plugins/oneblock/system/skills/src/main/kotlin/one_block/system/skills/Skills.kt) | API: `level`, `xp`, `bonus`, `hasLevel`, `addXp` |
| [SkillListener.kt](../../../plugins/oneblock/system/skills/src/main/kotlin/one_block/system/skills/listener/SkillListener.kt) | XP fixo: bloco 1, crop 2, log 2, mob 5 |
| [SkillsView.kt](../../../plugins/oneblock/system/skills/src/main/kotlin/one_block/system/skills/view/SkillsView.kt) | GUI com barra de 20 segmentos |
| `SkillCommand` · `SkillExpansion` | `/skills`, `/skills xp\|set` · placeholders |

Falta (é o que este documento especifica):

1. **Nenhum bônus é consumido** — `Skills.bonus()` existe e ninguém chama.
2. **XP é fixo por categoria** — pedregulho vale o mesmo que diamante.
3. **Não há milestones** — só bônus linear, nada desbloqueia.
4. **Barra de progresso só na GUI** — sem feedback ao ganhar XP.
5. **Sem level-up visível** — só mensagem de chat.

---

## 2. Curva de XP

Fórmula em uso (`Skill.xpForLevel`):

```
custo(nível n) = floor(100 × n^1.5)
```

| Nível | Custo do nível | XP acumulado |
|---|---|---|
| 1 | 100 | 100 |
| 2 | 282 | 382 |
| 3 | 519 | 901 |
| 4 | 800 | 1.701 |
| 5 | 1.118 | 2.819 |
| 10 | 3.162 | 14.264 |
| 15 | 5.809 | 37.802 |
| 20 | 8.944 | 76.072 |
| 25 | 12.500 | 131.300 |
| 30 | 16.431 | 205.449 |
| 40 | 25.298 | 417.481 |
| 50 | 35.355 | 724.849 |
| 60 | 46.475 | 1.138.725 |
| 75 | 64.951 | 1.981.105 |
| 90 | 85.381 | 3.116.502 |
| 100 | 100.000 | 4.050.079 |

Formato da curva: cresce ~2,5× a cada dobra de nível. Nível 50 é metade do caminho em número, **18% do caminho em XP**. Isso é intencional — os últimos 20 níveis são o troféu.

### Calibração contra os atos

| Ato | Blocos da fase | XP médio/bloco alvo | XP total | Nível alcançado |
|---|---|---|---|---|
| I (fases 1–3) | 2.100 | 1,2 | ~2.500 | Mineração 4 |
| II (fases 4–6) | 11.000 | 3,0 | ~33.000 | Mineração 14 |
| III (fases 7–10) | 54.000 | 6,0 | ~324.000 | Mineração 35 |
| IV (fases 11–13) | 113.000 | 10,0 | ~1.130.000 | Mineração 60 |
| V (fase 14+) | 100.000+ | 14,0 | ~1.400.000 | Mineração 75+ |

O gate "Mineração 15" do Ato II bate em ~37.800 XP — alcançável no fim do Ato II com um pouco de esforço extra, não de graça. **É por isso que o XP não pode continuar fixo em 1 por bloco**: com 1 XP/bloco o gate exigiria 37.800 blocos, três vezes o ato inteiro.

---

## 3. Fontes de XP

Substitui os quatro valores fixos do `SkillListener` por tabelas por material.

### 3.1 Mineração

| Material | XP |
|---|---|
| Terra, areia, cascalho, sculk | 1 |
| Pedra, pedregulho, deepslate, netherrack | 1 |
| Minério de carvão | 3 |
| Minério de cobre / ferro | 5 |
| Minério de ouro / lápis / redstone | 8 |
| Minério de diamante | 20 |
| Minério de esmeralda | 25 |
| Ancient debris | 60 |
| Bloco central da ilha | 5 (independente do material) |
| Bloco colocado por jogador | 0 |

**Regra crítica:** bloco colocado pelo próprio jogador dá **zero XP**. Sem isso, o loop `colocar cobble → quebrar → repetir` fabrica nível infinito. Precisa de um marcador de bloco colocado — o `PlacedBlockManager` do `custom-items` já resolve blocos custom, mas para blocos vanilla é preciso um `Set` por chunk ou reaproveitar o registro de blocos da ilha.

### 3.2 Agricultura

| Material | XP |
|---|---|
| Trigo, cenoura, batata, beterraba (maduros) | 3 |
| Cana, cacto, bambu | 2 |
| Melancia, abóbora | 4 |
| Cogumelo, cacau | 4 |
| Nether wart | 5 |
| Colheita com plantio automático (nível 10+) | mesmo valor |

Crop imaturo dá 0 — evita farmar plantando e quebrando na hora.

### 3.3 Combate

| Mob | XP |
|---|---|
| Passivo (galinha, porco, vaca, ovelha) | 1 |
| Hostil comum (zumbi, esqueleto, aranha, creeper) | 5 |
| Hostil de fase alta (blaze, enderman, piglin) | 12 |
| Wither skeleton, guardião | 20 |
| Warden | 250 |
| Boss de ilha | 500 × tier |
| Mob de spawner | 40% do valor base |

**Mob de spawner vale 40%** porque é renda automatizada. Sem essa penalidade, Combate vira "quem tem mais spawner", não "quem luta".

### 3.4 Coleta

| Material | XP |
|---|---|
| Log (qualquer madeira) | 2 |
| Folhas | 0 |
| Cogumelo gigante | 3 |
| Árvore derrubada inteira (nível 10+) | 2 por log |

---

## 4. Barra de progresso

Quatro superfícies, cada uma com um papel.

### 4.1 GUI de skills (`/skills`)

Já existe, mas com 20 segmentos `|`. Proposta: 10 segmentos com cor da skill e percentual.

```
┌─────────────────────────────────────────────┐
│  ⛏ Mineração                                │
│  ▸ Nível: 14/100                            │
│  ▸ Bônus: +14% de velocidade de quebra      │
│                                             │
│  ████████░░  72%                            │
│  4.230 / 5.809 XP                           │
│                                             │
│  Próximo desbloqueio (nível 15):            │
│  ▸ +5% de chance de drop duplo em minério   │
└─────────────────────────────────────────────┘
```

A linha "Próximo desbloqueio" é o que faz a GUI virar objetivo em vez de placar.

### 4.2 Action bar (ao ganhar XP)

Aparece a cada ganho, com throttle de 1 segundo por skill:

```
⛏ Mineração 14   ▌▌▌▌▌▌▌░░░   4.230/5.809   (+20)
```

Throttle é obrigatório: sem ele, um jogador com Picareta de Explosão dispara 9 atualizações por clique.

### 4.3 Level-up

Usa o `lib:toast` que já existe (`ToastApi.send`), mais som e mensagem:

```
╔══════════════════════════════╗
║   ⛏  MINERAÇÃO NÍVEL 15      ║   ← toast (advancement popup)
╚══════════════════════════════╝

[chat]
▸ Mineração subiu para o nível 15!
  +15% de velocidade de quebra
  Desbloqueado: +5% de chance de drop duplo em minério

[som] ENTITY_PLAYER_LEVELUP
```

Milestone (múltiplo de 5) usa `ToastType` de frame diferente do nível comum — o jogador aprende a diferença visualmente.

### 4.4 Boss bar (opcional, sessão de grind)

Enquanto o jogador ganha XP continuamente, uma boss bar mostra o progresso e some após 5s de inatividade. Bom para quem fica 40 minutos minerando.

### 4.5 Placeholders (HUD do Textura)

Já existem em `SkillExpansion`:

| Placeholder | Retorno |
|---|---|
| `%skill_mining_level%` | `14` |
| `%skill_mining_xp%` | `41,2k` |
| `%skill_mining_progress%` | `4,2k` |
| `%skill_mining_next%` | `5,8k` |
| `%skill_mining_bonus%` | `14%` |

Falta adicionar: `%skill_mining_bar%` devolvendo a barra pronta em glyphs do Textura, para a HUD da ilha.

---

## 5. Desbloqueios por skill

Modelo: **bônus passivo a cada nível** (linear, já implementado) + **milestone a cada 5 níveis** (novo).

### 5.1 Mineração — bônus: +1% velocidade de quebra por nível

| Nível | Desbloqueio |
|---|---|
| 5 | Receita de **Peça Mecânica** na Forja |
| 10 | **+1 slot de minion** na ilha |
| 15 | +5% de chance de **drop duplo** em minério |
| 20 | Receita da **Picareta de Explosão** |
| 25 | **Haste I** permanente enquanto na ilha |
| 30 | **+1 slot de minion** |
| 35 | Minérios dão **+50% XP** |
| 40 | Receita do **Perfurador de Veia** |
| 45 | +10% de chance de drop duplo (total 15%) |
| 50 | **+1 slot de minion** · **Haste II** na ilha |
| 60 | Desbloqueia **Gerador de Minério tier 4 e 5** |
| 70 | Fortune conta **+1 nível efetivo** |
| 80 | **+1 slot de minion** |
| 90 | Receita da **Broca Portátil** |
| 100 | Título **Escavador** · +100% velocidade total · drop duplo 25% |

### 5.2 Agricultura — bônus: +1% velocidade de crescimento por nível

| Nível | Desbloqueio |
|---|---|
| 5 | Receita do **Minion Fazendeiro** |
| 10 | **Replantio automático** ao colher |
| 15 | +5% de drop de plantação |
| 20 | Receita do **Cristal de Crescimento** |
| 25 | **Colheita 3×3** com enxada |
| 30 | **+1 slot de minion Fazendeiro** |
| 35 | Plantações nunca consomem semente |
| 40 | Receita do **Silo** |
| 50 | +10% no preço de venda de itens agrícolas |
| 60 | Bone meal sempre funciona (100%) |
| 70 | **Colheita 5×5** |
| 80 | Plantações crescem **2× mais rápido** na ilha |
| 90 | Colheita atravessa a borda da ilha inteira com Cristal ativo |
| 100 | Título **Ceifeiro** · +100% growth · +25% venda |

### 5.3 Combate — bônus: +1% de dano por nível

| Nível | Desbloqueio |
|---|---|
| 5 | Receita do **Minion Exterminador** |
| 10 | **+2 de vida máxima** (1 coração) |
| 15 | +5% de drop de mob |
| 20 | Desbloqueia **spawners de tier 2** na loja |
| 25 | **+4 de vida máxima** (total 3 corações extras) |
| 30 | +10% de dano crítico |
| 35 | Mobs de spawner passam a dar **60% do XP** em vez de 40% |
| 40 | Receita da **Espada de Almas** |
| 50 | Desbloqueia **Slayer tier 3** |
| 60 | Desbloqueia **Boss de ilha tier 2** |
| 70 | **+8 de vida máxima** |
| 80 | Mobs de spawner dropam **loot dobrado** |
| 90 | Desbloqueia **Dungeon andar 5** |
| 100 | Título **Carrasco** · +100% dano · +50% drop de mob |

### 5.4 Coleta — bônus: +1% de drop de madeira por nível

| Nível | Desbloqueio |
|---|---|
| 5 | Receita de **Machado reforçado** |
| 10 | **Derruba a árvore inteira** de uma vez |
| 15 | +5% de chance de sapling extra |
| 20 | Receita do **Minion Lenhador** |
| 25 | Madeira dá **+50% XP** |
| 30 | **+1 slot de minion Lenhador** |
| 35 | Log vira 6 tábuas em vez de 4 |
| 40 | Árvores crescem instantaneamente com bone meal |
| 50 | +10% no preço de venda de madeira |
| 60 | Folhas dropam sapling 100% |
| 70 | Derruba árvores num raio de 5 blocos |
| 80 | **+1 slot de minion** |
| 90 | Madeira comprimida (bloco denso) na Forja |
| 100 | Título **Guardião da Floresta** · +100% drop |

### 5.5 Resumo dos slots de minion

Distribuídos entre as skills para forçar o jogador a não ignorar nenhuma:

| Origem | Slots |
|---|---|
| Base | 2 |
| Upgrade de ilha | +8 (2 → 10) |
| Mineração 10/30/50/80 | +4 |
| Agricultura 30 | +1 |
| Coleta 30/80 | +2 |
| Prestígio | +1 por prestígio |

Teto prático no end-game: ~25 minions.

---

## 6. Modelo de dados dos milestones

Estrutura sugerida, sem quebrar o que existe:

```kotlin
sealed interface SkillReward {
    data class Stat(val id: String, val value: Double) : SkillReward
    data class Unlock(val id: String) : SkillReward
    data class MinionSlot(val amount: Int) : SkillReward
    data class Title(val id: String) : SkillReward
}

data class SkillMilestone(
    val level: Int,
    val description: String,
    val rewards: List<SkillReward>,
)
```

`Skill` ganha `val milestones: List<SkillMilestone>` e dois helpers:

```kotlin
fun milestoneAt(level: Int): SkillMilestone?
fun nextMilestone(level: Int): SkillMilestone?
```

`Skills.addXp` já detecta a subida de nível (`data.addXp` devolve quantos níveis subiu); basta iterar os níveis atravessados e aplicar cada milestone. Um `SkillMilestoneEvent` (Bukkit) permite que outros módulos reajam sem que `skills` dependa deles — mesma abordagem do `CollectionTierUnlockEvent`.

**Desbloqueios são consultados, não armazenados.** `Skills.hasUnlock(uuid, "explosive_pickaxe")` deriva do nível atual. Nada de flag persistida que pode dessincronizar.

---

## 7. Onde cada bônus é consumido

O bônus não faz nada sozinho. Cada um precisa de um ponto de consumo:

| Bônus | Onde aplicar | Como |
|---|---|---|
| Mineração: velocidade | join + troca de mundo | Atributo de block break speed, ou efeito Haste escalado |
| Mineração: drop duplo | `BlockDropItemEvent` | Rolagem por evento, duplica o stack |
| Agricultura: growth | `BlockGrowEvent` na ilha | Chance extra de tick, empilha com `IslandUpgrade.CropGrowth` |
| Agricultura: replantio | `BlockBreakEvent` | Recoloca o crop no estado 0 e consome 1 da semente dropada |
| Combate: dano | `EntityDamageByEntityEvent` | `event.damage = event.damage * (1 + bonus)` |
| Combate: vida máxima | join | Modificador de atributo com `NamespacedKey` próprio, removido no logout |
| Combate: drop de mob | `EntityDeathEvent` | Rolagem sobre `event.drops` |
| Coleta: drop de madeira | `BlockDropItemEvent` | Mesma rolagem da mineração |
| Slots de minion | `MinionsSystem` no placement | `limiteBase + upgradeIlha + Skills.minionSlots(uuid)` |
| Receitas na Forja | `ForgeRequirement` | Requisito novo do tipo `SkillRequirement(skill, level)` |

O item mais importante da tabela é o último: **a Forja é o consumidor principal**. Sem `SkillRequirement`, os desbloqueios de receita não existem.

---

## 8. Anti-abuso

| Exploit | Defesa |
|---|---|
| Colocar e quebrar o mesmo bloco | XP zero em bloco colocado por jogador |
| Plantar e quebrar imaturo | XP só em crop maduro |
| Farm de spawner para Combate | Mob de spawner vale 40% |
| Minion farmando skill do dono | Minion **não gera XP de skill** — XP é do jogador, não da máquina |
| Matar mob de outro jogador | XP vai para `entity.killer`, já é o comportamento atual |
| Picareta de Explosão inflando XP | XP por bloco quebrado é somado normalmente, mas o custo de durabilidade e o cooldown limitam; se virar problema, aplicar 50% no bloco secundário |

O terceiro e o quarto são os que mais importam: se minion desse XP de skill, o Eixo B viraria cópia do Eixo C (dinheiro) e o jogador não teria motivo para jogar.

---

## 9. Checklist de implementação

**Fase 1 — tornar o sistema real**
- [ ] Tabela de XP por material (`SkillXpTable`), substituindo os 4 valores fixos
- [ ] Marcação de bloco colocado por jogador (anti-farm)
- [ ] Action bar com throttle de 1s por skill
- [ ] Toast de level-up via `ToastApi`, com frame diferente em milestone

**Fase 2 — milestones**
- [ ] `SkillReward` / `SkillMilestone` + `milestones` no enum `Skill`
- [ ] `SkillMilestoneEvent`
- [ ] `Skills.hasUnlock(uuid, id)` e `Skills.minionSlots(uuid)`
- [ ] GUI mostrando o próximo desbloqueio

**Fase 3 — consumo dos bônus**
- [ ] `SkillBonusListener`: dano, drop de mob, drop de bloco, growth
- [ ] Atributos aplicados no join (vida, velocidade de quebra)
- [ ] `SkillRequirement` na Forja
- [ ] Limite de minion lendo `Skills.minionSlots`

**Fase 4 — apresentação**
- [ ] `%skill_<id>_bar%` com glyphs do Textura
- [ ] Boss bar de sessão de grind
- [ ] Títulos de nível 100 integrados ao sistema de tags

---

## 10. Métricas de validação

| Métrica | Alvo |
|---|---|
| Mineração no fim do Ato II | 13–16 |
| Mineração no fim do Ato III | 32–38 |
| Skill mais alta ≠ Mineração em pelo menos | 25% dos jogadores |
| Jogadores que passam de nível 50 em alguma skill | 15% |
| Tempo mediano para o primeiro milestone (nível 5) | < 30 min |
| Razão XP de spawner / XP de combate manual | < 1,5 |

A segunda métrica é a mais importante: se 95% dos jogadores têm Mineração como skill mais alta, as outras três são decoração e o sistema falhou.
