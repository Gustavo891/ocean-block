# Progressão OneBlock — Pesquisa, Desenho de Progressão e Backlog de Sistemas

Data: 2026-07-26
Escopo: pesquisa de mercado (OneBlock/Skyblock 1.21+), desenho da curva de progressão do início ao end-game, e catálogo de funcionalidades (existentes + novas).

---

## 1. Pesquisa — o que os servidores de referência usam

### 1.1 Hypixel Skyblock (referência de profundidade)

Eixos de progressão **paralelos**, não uma linha única. É o motivo do jogo sustentar meses:

| Eixo | Como funciona | O que trava |
|---|---|---|
| Skills (9) | XP por atividade (Mining, Farming, Combat, Foraging, Fishing, Enchanting, Alchemy, Taming, Carpentry) | Stats passivos, receitas, slots |
| Collections | Contador vitalício por material; tiers desbloqueiam receitas e **minions** | Craft de itens e minions |
| Minions | Produção offline, tiers I–XI, combustível, upgrades (compactor, super compactor, minion expander) | Renda passiva / economia |
| Slayers | Quest de matar mob-tipo, mini-boss, boss; XP só pro dono do boss | Armas e drops raros |
| Dungeons (Catacombs) | Andares, classes, secrets, tempo | Melhor gear do jogo |
| Accessories / Talismãs | Magical Power somado em bag, reforges | Stats agregados |
| Skyblock Level | Métrica agregada; a comunidade usa como "estágio": ~22 = early, ~87 = mid, ~154 = late | Status/gate social |
| Bazaar / AH | Bazaar (ordens instantâneas, itens a granel) + Auction House (itens únicos) | Economia inteira |

Lição principal: **progressão é uma malha, não uma escada.** Sempre existem 3–4 coisas que o jogador pode estar grindando ao mesmo tempo, com ritmos diferentes (minutos, horas, semanas).

### 1.2 OneBlock (formato do modo)

- Bloco único que regenera; **fases** por bioma (Floresta, Caverna, Neve, Deserto, Nether, End), cada uma com pool de blocos + pool de mobs próprios.
- Fases servem como o relógio-mestre da progressão: material novo = receita nova = renda nova.
- Servidores customizados por cima: quests, skills, jobs, custom enchants, island upgrades, eventos, loop econômico completo.
- Minions "worker" (chop, harvest+replant, mine) com upgrades — padrão de mercado, não diferencial.

### 1.3 Cores de Skyblock (SuperiorSkyblock2, IridiumSkyblock, BentoBox)

Funcionalidades que o mercado trata como **obrigatórias**:

- **Island Bank** (saldo da ilha, separado do pessoal) + moeda premium (Crystals).
- **Island Value / Island Level**: cada bloco vale pontos (cobble = 1, bloco de diamante = 200). É a métrica do top e o principal driver de competição.
- **Upgrades de ilha**: tamanho de borda, limites de bloco/entidade, geradores, crescimento de plantação, spawn rate.
- **Boosters** temporários compráveis (farm, spawner, XP, drop).
- **Missões** diárias rotativas + missões one-time.
- **Co-op**: membros, roles, permissões granulares por membro, trust.
- **Geradores** de cobble/basalto com tiers (upgrade = mais chance de minério raro / raio maior).
- **Stacking** de mobs e blocos (performance + contagem de missão/valor).
- **Biomas** trocáveis por custo.

### 1.4 Custom enchants

- **EcoEnchants** (250+ encantamentos, integra enchanting table, aldeão, bigorna, grindstone, loot natural) e **ExcellentEnchants** (80+, "vanilla-like", sem mexer em loot table/trades).
- Servidores OneBlock usam uma bancada própria ("Bone Forge") com raridades e grind de semanas — encantamento é **sumidouro de economia de longo prazo**, não conteúdo de dia 1.

### Fontes

- [Hypixel Skyblock — Slayer](https://wiki.hypixel.net/Slayer), [Beginner's Guide to Skyblock](https://www.beginnersguidetoskyblock.org/overview), [Skyblock Guides](https://skyblock-guides.com/)
- [Hypixel Forums — All Skyblock progression](https://hypixel.net/threads/all-skyblock-progression.5134859/), [Estágios do jogo por nível](https://hypixel.net/threads/what-stage-of-the-game-you-are-according-to-hypixel-aka-the-skyblock-guide.6030939/)
- [OneBlock MC Wiki — Phases](https://oneblock.fandom.com/wiki/Phases), [OneBlock Reborn (CurseForge)](https://www.curseforge.com/minecraft/worlds/oneblock-reborn)
- [IridiumSkyblock — Features](https://iridium-development.gitbook.io/iridiumskyblock/general/features), [SuperiorSkyblock2](https://www.spigotmc.org/resources/%E2%9A%A1%EF%B8%8F-superiorskyblock2-%E2%9A%A1%EF%B8%8F-the-best-core-on-market-%E2%9A%A1%EF%B8%8F-1-21-10-support.87411/)
- [EcoEnchants](https://plugins.auxilor.io/ecoenchants), [ExcellentEnchants](https://modrinth.com/plugin/excellentenchants)
- [Como montar servidor Skyblock — design de economia](https://teamvisionary.net/everything-to-know-about-making-a-skyblock-server/)

---

## 2. Estado atual do projeto

### 2.1 Sistemas implementados (`plugins/oneblock/system/`)

| Módulo | Papel na progressão |
|---|---|
| `islands` | Ilha, membros, permissões, settings, **fases**, upgrades, monstros, HUD |
| `minions` | Minerador, Fazendeiro, Exterminador + upgrades (velocidade, armazenamento), fuel, fragmentos |
| `spawners` | Spawners empilháveis, loot table própria, shop, regras de placement/stack |
| `custom-items` | Framework de item/bloco custom (Magnet, Vacuum Hopper) — base para todo item novo |
| `special-items` | Legado (sell beacon) |
| `craft` | Forge com receitas e requisitos (`ForgeRequirement`) |
| `currencies` | Multi-moeda: `coins`, `token` |
| `shop` | Loja por categorias (block, farming, minerals, redstone, misc) |
| `sell` | Venda de inventário/itens com cooldown |
| `auction-house` | Mercado jogador↔jogador |
| `trader` | NPC rotativo com itens por raridade |
| `battle-pass` | Tiers + challenges/missões com bossbar de progresso |
| `tops` | Rankings |
| `mob-drops` | Drops customizados por mob |
| `arena`, `coinflip`, `combat-log` | PvP / apostas |
| `fly-timer`, `repair`, `xp-bottle`, `starter-kit`, `offhand-pets`, `playtime`, `chats`, `worlds` | QoL / suporte |

### 2.2 Curva de fases atual (14 fases)

| # | Fase | Blocos p/ próxima | Acumulado |
|---|---|---|---|
| 1 | Origem | 300 | 300 |
| 2 | Profundezas Rasas | 600 | 900 |
| 3 | Profundezas de Ferro | 1.200 | 2.100 |
| 4 | Caverna Dourada | 2.000 | 4.100 |
| 5 | Caverna Abissal | 3.500 | 7.600 |
| 6 | Picos Congelados | 5.500 | 13.100 |
| 7 | Ermos Glaciais | 8.000 | 21.100 |
| 8 | Terras Exóticas | 11.000 | 32.100 |
| 9 | Desolação Carmesim | 15.000 | 47.100 |
| 10 | Areias de Almas Torcidas | 20.000 | 67.100 |
| 11 | Fortaleza Vulcânica | 27.000 | 94.100 |
| 12 | Ecos do Vazio | 36.000 | 130.100 |
| 13 | Cidades do End | 50.000 | 180.100 |
| 14 | Escuridão Profunda | 100.000 | 280.100 |

Curva ~×1,4 por fase. Saudável. **Problema: a fase é hoje o único eixo real de progressão.** Terminou a 14, acabou o jogo.

### 2.3 Lacunas frente ao mercado

1. **Sem Island Level / Island Value** — não existe métrica agregada de "quão longe eu estou", nem base decente pro top.
2. **Sem skills/coleções** — não há progressão pessoal que persista entre fases.
3. **Sem banco da ilha** — economia é 100% pessoal, coop fica fraco.
4. **Upgrades de ilha quase vazios** — 3 upgrades, 2 com 1 nível só, `CropGrowth` sem implementação.
5. **Sem geradores** — o modo não usa o loop cobble/basalt clássico, mas também não tem substituto de "gerador upgradeável".
6. **Minions rasos** — 3 tipos, 2 upgrades, sem tiers, sem produção offline explícita, sem compactor.
7. **Sem custom enchants / reforge** — nada pra gastar dinheiro de late game.
8. **Sem conteúdo de combate estruturado** — sem boss de ilha, sem dungeon, sem slayer.
9. **Sem prestígio/rebirth** — nada depois da fase 14.
10. **Sem eventos temporizados** — nada que traga o jogador de volta em horário marcado.

---

## 3. Progressão desenhada — do início ao end-game

Princípio: **4 eixos paralelos** com relógios diferentes.

```
EIXO A — Fase do bloco     (relógio: dias)      → desbloqueia materiais
EIXO B — Skills/Coleções   (relógio: semanas)   → desbloqueia receitas e stats
EIXO C — Ilha              (relógio: dinheiro)  → desbloqueia espaço e automação
EIXO D — Equipamento       (relógio: sorte+grind)→ desbloqueia velocidade de tudo
```

O jogador deve, a qualquer momento, ter tarefa aberta em pelo menos 3 dos 4.

### ATO I — "O primeiro bloco" (Fases 1–3, ~0–2h)

**Objetivo do jogador:** sair do nada e ter um loop de renda.

| Passo | O que faz | Sistema |
|---|---|---|
| 1 | Recebe starter kit, quebra o bloco | `starter-kit`, `islands` |
| 2 | Primeiras 300 quebras → Fase 2 | `islands/phase` |
| 3 | Vende drops → primeiras `coins` | `sell`, `shop` |
| 4 | Compra **primeiro minion Minerador** | `minions` |
| 5 | Missões diárias do passe (tutorial disfarçado) | `battle-pass` |
| 6 | Primeiro upgrade de borda | `islands/upgrade` |

**Gates a adicionar:** minion Minerador só compra a partir da Fase 2; borda nível 2 exige Fase 3. Gate por fase é melhor que gate por preço — ensina o loop.

**Sensação alvo:** "toda vez que quebro, algo acontece".

### ATO II — "Automação inicial" (Fases 4–6, ~2–10h)

**Objetivo:** substituir clique por automação; primeira decisão de build.

- **Vacuum Hopper** vira essencial (já existe) — gate na Fase 4.
- **Primeiros spawners** (porco/galinha/zumbi) via `spawners` — comprados com coins, colocados na ilha.
- **Segundo e terceiro minion**, escolha entre Minerador/Fazendeiro/Exterminador.
- **Skills começam a importar**: Mining 10 dá +1 slot de minion; Farming 10 dá +10% de crop growth.
- **Coleções**: 1.000 cobble → receita de Compactor; 500 ferro → receita de Super Compactor.
- **Sell beacon / sell wand** para vender sem voltar ao spawn.

**Gate de saída:** Fase 6 exige 13.100 blocos acumulados — o jogador precisa de minions rodando pra chegar em tempo razoável. Se dá pra fazer na mão em 2h, os minions estão mal-precificados.

### ATO III — "Escala" (Fases 7–10, ~10–50h)

**Objetivo:** produção industrial. É aqui que o servidor ganha ou perde o jogador.

| Sistema | Papel |
|---|---|
| Spawners empilhados | Renda principal de combate |
| Minions Tier III–V | Renda passiva offline |
| **Banco da ilha** (novo) | Poupança coletiva, coop começa a valer |
| **Upgrades de ilha reais** (novo) | Limite de spawner, limite de minion, spawn rate, growth |
| **Custom enchants** (novo) | Primeiro grande sumidouro de dinheiro |
| **Island Level** (novo) | Métrica de competição no top |
| Auction House | Especialização: jogador vende o que produz melhor |

**Decisão estratégica que o jogador toma aqui:** "sou de farm, de spawner ou de minion?". O servidor precisa deixar as três viáveis com rendas parecidas e caminhos diferentes. Isso é o que gera mercado no AH.

**Gate de saída:** Fase 10 (67.100 blocos) deve exigir ter 3+ eixos desenvolvidos, não só clicar mais.

### ATO IV — "Especialização" (Fases 11–13, ~50–150h)

**Objetivo:** conteúdo que não é mais sobre quebrar o bloco.

- **Bosses de ilha** (novo): invocados com item craftado na Forge, dropam materiais de tier alto. Escalam com nº de membros.
- **Slayer / Caçadas** (novo): quest de matar N mobs → mini-boss → boss. XP de slayer desbloqueia armas.
- **Dungeon / Masmorra do servidor** (novo): instância coop no `arena` ou mundo dedicado, com andares e recompensa por tempo.
- **Reforge / Acessórios** (novo): stats agregados, sumidouro contínuo.
- **Minions Tier VI–X** + upgrades caros (Super Compactor, Auto-Sell, Expander).

**Sensação alvo:** "minha ilha roda sozinha, eu jogo o conteúdo".

### ATO V — "End-game" (Fase 14 + pós-fase, 150h+)

**Objetivo:** competição e prestígio, não acumulação.

- **Escuridão Profunda** com Warden e 100.000 blocos: última parede.
- **Prestígio / Renascimento** (novo): reseta fase e mantém um multiplicador permanente + moeda de prestígio. Sem isso, o jogo acaba na fase 14.
- **Top de Ilha** com temporada (season) e reset periódico com recompensas.
- **Eventos globais temporizados** (novo): Chuva de Meteoros, Invasão, Mercador Raro (já existe `trader` — expandir).
- **Cosméticos**: pets, partículas, tags, skins de minion — sumidouro puro de moeda premium.

### Resumo dos gates por ato

| Ato | Fases | Métrica de saída | Sistema-chave |
|---|---|---|---|
| I | 1–3 | 1º minion + 2.100 blocos | starter, sell, minions |
| II | 4–6 | Hopper + 2 spawners + 13.100 blocos | spawners, custom-items, skills |
| III | 7–10 | Island Level ~X + banco + enchants | ilha, economia, AH |
| IV | 11–13 | Boss/dungeon clear + minion T6 | combate, reforge |
| V | 14+ | Prestígio 1 | prestígio, seasons, eventos |

---

## 4. Funcionalidades — existentes a aprofundar

### 4.1 `islands` — Island Level / Island Value **(prioridade máxima)**

Cada bloco na ilha vale pontos; soma = valor da ilha. Base do `tops`, das missões e dos gates.

- Tabela `island-values.yml`: `COBBLESTONE: 1`, `IRON_BLOCK: 40`, `DIAMOND_BLOCK: 200`, `NETHERITE_BLOCK: 1000`, blocos custom com valor próprio.
- Cálculo assíncrono por chunk, com cache; recalcula sob demanda + a cada X minutos.
- Contar spawners e minions colocados no valor (usa `PlacedSpawnerManager` e `PlacedMinionManager`, já existem em memória — evita varrer chunk).
- Island Level derivado do valor por curva (ex.: `level = floor(sqrt(value / 100))`).

### 4.2 `islands/upgrade` — upgrades reais

`IslandUpgrade` hoje tem 3 entradas, 2 com `maxLevel = 1`, uma sem implementação. Expandir para:

| Upgrade | Níveis | Efeito |
|---|---|---|
| Borda | 8 | 50 → 250 blocos |
| Membros | 6 | 1 → 8 |
| Plantação | 5 | +20% growth por nível (implementar de fato) |
| Limite de Minions | 8 | 3 → 25 |
| Limite de Spawners | 8 | 5 → 50 |
| Spawn Rate | 5 | −10% delay por nível |
| Mob Drops | 5 | +10% drop por nível |
| Experiência | 5 | +10% XP por nível |
| Geração do Bloco | 5 | +chance de minério raro no `ore-pool` da fase |

Preço exponencial (`base * 2.2^nivel`), pago pelo **banco da ilha**, não pela carteira pessoal.

### 4.3 `minions` — tiers e profundidade

- **Tiers I–XI** por fragmentos (`FragmentCommand` já existe): cada tier reduz o intervalo e aumenta o armazenamento.
- **Produção offline**: acumula até o cap de armazenamento enquanto o dono está off (é o principal apelo de minion).
- **Upgrades novos**: Compactor (9→1 automático), Super Compactor (bloco→item comprimido), Auto-Sell (vende direto pro banco), Diamond Spreading, Expander (raio), Fuel Slot duplo.
- **Tipos novos**: Lenhador, Pescador, Coletor de XP, Mineiro de Minério (só ore-pool), Nether/End specialist.
- **Skin de minion** via `textura` — cosmético vendável.

### 4.4 `spawners` — integrar com progressão

- Gate de tipo por fase (Blaze só na Fortaleza Vulcânica, Enderman só nos Ecos do Vazio).
- Upgrade de spawner individual (velocidade, drop múltiplo) além do stack.
- Contar no Island Value.

### 4.5 `craft`/Forge — a bancada de progressão

Já tem `ForgeRequirement`. Usar como **o** ponto de desbloqueio: receita exige `fase >= N`, `skill >= N`, `coleção >= N`. Toda receita nova de item custom entra aqui, nunca em receita vanilla.

### 4.6 `battle-pass` — temporadas

- Season com data de início/fim, reset de tiers, passe premium.
- Missões semanais além das diárias.
- Recompensas exclusivas por season (cosmético, skin de minion, título).

### 4.7 `tops` — competição por temporada

Top de: Island Level, blocos quebrados, coins, playtime, kills. Reset sazonal com premiação e um "hall of fame" permanente.

### 4.8 `trader` — expandir para eventos

Já é um NPC rotativo. Virar **evento global** anunciado no chat, com estoque limitado por servidor (escassez gera corrida).

---

## 5. Funcionalidades novas propostas

Ordenadas por (impacto na retenção) ÷ (custo de implementação).

### Onda 1 — fundação (maior impacto)

| # | Sistema | Descrição | Depende de |
|---|---|---|---|
| 1 | **Island Level / Value** | Métrica agregada + top | `islands`, `tops` |
| 2 | **Banco da ilha** | Saldo coletivo, extrato, permissão de saque por role | `currencies`, `islands` |
| 3 | **Upgrades de ilha completos** | Tabela da seção 4.2 | banco |
| 4 | **Skills** | Mining, Farming, Combat, Foraging, Fishing, Enchanting, Crafting. XP por ação, stats passivos, desbloqueios | novo módulo `system/skills` |
| 5 | **Coleções** | Contador vitalício por material; tier desbloqueia receita na Forge | `craft`, `skills` |

### Onda 2 — profundidade econômica

| # | Sistema | Descrição |
|---|---|---|
| 6 | **Custom Enchants** | Bancada própria ("Forja Rúnica"), livros com raridade, chance de falha, sumidouro pesado. Encantamentos: Fortuna Sombria, Explosão, Telecinese, Auto-Smelt, Veia (veinminer), Perfurar, Vampirismo |
| 7 | **Boosters** | Temporários compráveis: +XP, +drop, +venda, +velocidade de minion. Booster de ilha vale pra todos os membros |
| 8 | **Quests / Jobs** | Cadeia de quests por fase (narrativa) + jobs por atividade (pagamento por ação) |
| 9 | **Bazaar** (opcional) | Ordens de compra/venda a granel, separado do AH de itens únicos. Só vale com base de jogadores grande |
| 10 | **Mercado negro / Loja rotativa** | Estoque diário limitado, preços variáveis |

### Onda 3 — conteúdo de combate

| # | Sistema | Descrição |
|---|---|---|
| 11 | **Bosses de ilha** | Item de invocação craftado; boss escala com membros; drop de material de tier alto exclusivo |
| 12 | **Slayer / Caçadas** | Quest: N mobs → mini-boss → boss. Nível de slayer desbloqueia armas e stats |
| 13 | **Dungeon / Masmorra** | Instância coop com andares, chaves como entrada, ranking por tempo |
| 14 | **Arena de ondas** | Reaproveita `arena`: ondas crescentes, moeda própria, loja de arena |
| 15 | **Acessórios / Reforge** | Bag de acessórios com poder agregado; reforge com custo por rolagem |

### Onda 4 — retenção de longo prazo

| # | Sistema | Descrição |
|---|---|---|
| 16 | **Prestígio / Renascimento** | Reseta fase, mantém multiplicador permanente + moeda de prestígio + loja exclusiva |
| 17 | **Eventos globais temporizados** | Chuva de Meteoros (blocos raros caem na ilha), Invasão (ondas na ilha), Rush (bloco vale 2× por 30min) |
| 18 | **Seasons** | Reset sazonal do top + passe + recompensas; ilhas persistem |
| 19 | **Guildas / Alianças** | Acima da ilha: chat, banco, top de guilda, guerra |
| 20 | **Cosméticos** | Pets (`offhand-pets` já existe), partículas, tags, skins de minion/ferramenta via `textura` |

---

## 6. Itens, blocos e minérios novos

### 6.1 Itens custom (via `system/custom-items`)

| Item | Efeito | Ato |
|---|---|---|
| **Ímã** (existe) | Puxa drops | I |
| **Vacuum Hopper** (existe) | Coleta + filtro + venda | II |
| **Varinha de Venda** | Clique num baú → vende tudo | II |
| **Picareta de Explosão** | Quebra 3×3 | II |
| **Bússola de Fase** | Mostra progresso e próxima fase | I |
| **Cristal de Crescimento** | Acelera plantação num raio | III |
| **Coração da Ilha** | Bloco colocável que dá buff de ilha enquanto ativo | III |
| **Mochila / Sacola** | Armazenamento portátil com tiers | III |
| **Foguete de Voo** | Consome `fly-timer` | III |
| **Tocha de Spawn** | Bloqueia spawn hostil num raio | III |
| **Fragmento de Alma** | Moeda de drop de boss, usada na Forja | IV |
| **Chave de Masmorra** | Entrada em dungeon | IV |
| **Núcleo de Prestígio** | Item de prestígio | V |
| **Talismãs** | Acessórios de stat passivo | IV |

### 6.2 Blocos custom (via `CustomBlock`)

| Bloco | Função |
|---|---|
| **Gerador de Minério** | Gera minério com tier upgradeável — traz o loop clássico de gerador que hoje falta |
| **Fornalha Industrial** | Auto-smelt de fila grande, upgradeável |
| **Compactador** | Comprime blocos em item denso |
| **Baú da Ilha** | Armazenamento compartilhado entre membros |
| **Altar de Invocação** | Onde o boss de ilha é invocado |
| **Coletor** | Recolhe output de minions num raio (evita andar de minion em minion) |
| **Silo** | Armazena um único material em quantidade absurda; alimenta auto-sell |

### 6.3 Minérios e materiais novos (pool das fases)

Materiais exclusivos por fase criam demanda no AH e dão identidade à fase:

| Fase | Material exclusivo | Uso |
|---|---|---|
| 5 Caverna Abissal | **Cristal Abissal** | Upgrade de minion T3+ |
| 7 Ermos Glaciais | **Núcleo Congelado** | Encantamento de lentidão / booster |
| 8 Terras Exóticas | **Essência Exótica** | Reforge |
| 9 Desolação Carmesim | **Cinza Carmesim** | Forja de armas |
| 11 Fortaleza Vulcânica | **Coração de Magma** | Boss de fogo |
| 12 Ecos do Vazio | **Eco do Vazio** | Acessórios de end-game |
| 13 Cidades do End | **Fragmento Élitro** | Voo permanente na ilha |
| 14 Escuridão Profunda | **Estilhaço de Sculk** | Prestígio |

Implementação: adicionar ao `ore-pool` das fases como bloco custom (já suportado por `CustomBlockRegistry`) com drop próprio.

---

## 7. Economia — fontes e sumidouros

Um servidor morre por inflação. Mapa explícito:

**Fontes (entra dinheiro):**
- Venda de drops (`sell`) — controlada por `sell.yml`
- Recompensas de fase (`completion.rewards`) — hoje 1.000 na fase 1, 750.000 na 14
- Missões / passe
- Spawners e minions (auto-sell)

**Sumidouros (sai dinheiro) — hoje quase inexistentes:**
- Upgrades de ilha (exponencial, sem teto prático)
- Tiers de minion
- Custom enchants com chance de falha
- Reforge (rolagem aleatória, jogador re-rola)
- Boosters temporários
- Taxa do Auction House (2–5%)
- Cosméticos
- Chaves de dungeon

Regra: **todo sistema novo que gera renda precisa vir com um sumidouro do mesmo ato.** Enchants e reforge são os melhores porque consomem indefinidamente.

---

## 8. Roadmap sugerido

```
Sprint 1  Island Value/Level + banco da ilha + upgrades reais    ← desbloqueia top e coop
Sprint 2  Skills + Coleções (novo módulo, integra com Forge)     ← progressão pessoal
Sprint 3  Minions: tiers, offline, compactor, auto-sell          ← retenção passiva
Sprint 4  Custom enchants (Forja Rúnica) + boosters              ← sumidouro econômico
Sprint 5  Bosses de ilha + slayer                                 ← conteúdo de combate
Sprint 6  Prestígio + seasons + eventos globais                   ← end-game
```

Sprint 1 e 2 são os que mudam o servidor de "OneBlock com módulos" para "OneBlock com progressão".

---

## 9. Métricas para validar

- Tempo mediano por fase (alvo: fase 1–3 em <2h; fase 14 em >40h).
- % de jogadores que colocam o 1º minion (alvo >80%) e o 3º (alvo >40%).
- Distribuição de renda por fonte (nenhuma fonte >50% do total).
- Retenção D1/D7/D30 por ato em que o jogador parou — mostra onde a curva quebra.
- Inflação: coins totais em circulação / semana.
