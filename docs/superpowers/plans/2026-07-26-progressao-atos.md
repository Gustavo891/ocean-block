# Plano de Progressão — Atos I a V, Rebalanceamento do Minerador e Catálogo de Itens

Data: 2026-07-26
Spec de origem: [2026-07-26-progressao-oneblock-design.md](../specs/2026-07-26-progressao-oneblock-design.md)

Este documento detalha (1) cada ato da progressão com gates, curvas e checklist de implementação, (2) o rebalanceamento do Minion Minerador, e (3) o catálogo de itens novos — sem repetir Vacuum Hopper nem Minion Minerador.

---

## Parte 1 — Os cinco atos, em detalhe

### Convenções usadas

- **Gate de entrada**: o que o jogador precisa ter para o ato começar a fazer sentido.
- **Gate de saída**: o que ele obrigatoriamente construiu para sair.
- **Relógio**: o que consome o tempo dele nesse ato.
- **Parede**: o ponto onde a maioria desiste se o balanceamento estiver errado.
- Tempos-alvo são de jogador solo médio. Ilha com 4 membros: ~40% do tempo.

---

## ATO I — "O primeiro bloco"

**Fases 1–3 (Origem → Profundezas de Ferro) · 2.100 blocos · alvo 1h30–2h30**

### Objetivo de design
Ensinar o loop `quebrar → vender → comprar → automatizar` sem uma linha de tutorial escrita. Todo sistema aberto aqui é sistema que o jogador vai usar por 200 horas.

### Fluxo minuto a minuto

| Momento | O que acontece | Sistema |
|---|---|---|
| 0–2 min | Ilha criada, starter kit, primeira quebra | `islands`, `starter-kit` |
| 2–10 min | Madeira/terra acumulam, primeira bancada, primeira picareta | vanilla |
| 10–15 min | **Fase 2** (300 blocos): pedra e carvão aparecem | `islands/phase` |
| 15–25 min | Primeira venda no `/sell` — primeiras coins | `sell` |
| 25–40 min | Missão diária do passe: "quebre 500 blocos", "venda 1.000 coins" | `battle-pass` |
| 40–60 min | **Fase 3** (900 acumulados): ferro | `islands/phase` |
| 1h–1h30 | Compra o **Ímã** na loja (item mais barato do jogo) | `custom-items` |
| 1h30–2h | Junta 16 Peças Mecânicas → **primeiro minion** | `craft`, `minions` |
| 2h–2h30 | **Fase 4** (2.100 acumulados) e primeira borda | `islands/upgrade` |

### Gates

- **Entrada**: nenhum (é o começo).
- **Saída**: ter 1 minion colocado + fase 4 desbloqueada.

### Gates a implementar

```
Fase 2 → desbloqueia loja de blocos e /sell
Fase 3 → desbloqueia Forge (/craft) e receita de Peça Mecânica
Fase 4 → desbloqueia receita do Minion Minerador e upgrade de borda nível 1
```

Gate por fase, não por preço. Preço o jogador contorna com sorte; fase ele tem que jogar.

### Curva econômica

| Item | Preço alvo | Tempo de grind |
|---|---|---|
| Ímã | 2.500 coins | ~15 min |
| Borda nível 1 | 25.000 coins | ~40 min |
| Minion Minerador | craft (16 peças) | ~30 min |
| Peça Mecânica | 1.500 coins ou drop de fase | — |

Correção necessária: `IslandUpgrade.Members` e `Size` custam 50.000 hoje, com 1 nível só. Para o Ato I, borda nível 1 precisa custar ~25.000 e existirem 8 níveis.

### Parede
"Quebrei 300 blocos e não ganhei nada." Recompensa de fase 1 hoje dá 5 diamantes + 1.000 coins — ok. Manter recompensa **visível** (título na tela, som, fogos) em toda fase; a sensação importa mais que o valor.

### Checklist
- [ ] Gates de fase nos sistemas (loja, forge, minions, upgrades)
- [ ] Borda com 8 níveis e preço progressivo
- [ ] Ímã na loja por 2.500
- [ ] Missões de tutorial no passe (5 primeiras, sequenciais)
- [ ] Animação/som de conclusão de fase

---

## ATO II — "Automação inicial"

**Fases 4–6 (Caverna Dourada → Picos Congelados) · 11.000 blocos · alvo 4–10h**

### Objetivo de design
Trocar o clique pelo sistema. O jogador precisa perceber que **parar de clicar e investir rende mais**. É a primeira decisão de build.

### O que abre

| Desbloqueio | Fase | Papel |
|---|---|---|
| Vacuum Hopper | 4 | Coleta automática, fim do inventário cheio |
| Spawners básicos (porco, galinha, zumbi) | 4 | Segunda fonte de renda |
| Segundo e terceiro minion (Fazendeiro, Exterminador) | 5 | Especialização inicial |
| **Skills** (novo) | 4 | Progressão pessoal que persiste entre fases |
| **Coleções** (novo) | 5 | 1.000 cobble → Compactor; 500 ferro → Super Compactor |
| Varinha de Venda | 5 | QoL de venda |
| **Banco da ilha** (novo) | 6 | Poupança coletiva; upgrades passam a sair daqui |
| Picareta de Explosão | 6 | Quebra 3×3 — dobra o ritmo manual |

### Skills — desenho mínimo do Ato II

| Skill | XP por | Recompensa a cada 5 níveis |
|---|---|---|
| Mineração | bloco quebrado | +1% velocidade de quebra, nível 10 = +1 slot de minion |
| Agricultura | crop colhido | +1% crop growth, nível 10 = +10% drop |
| Combate | mob morto | +1% dano, nível 10 = +5% drop de mob |
| Coleta | log quebrado | +1% drop de madeira |

Curva: `xp(n) = 100 * n^1.5`. Nível máximo 50 no Ato II, 100 no fim do jogo.

### Gates

- **Entrada**: 1 minion + fase 4.
- **Saída**: fase 7 exige, além dos 13.100 blocos: **3 minions colocados OU 5 spawners OU Mineração nível 15**. Três caminhos, um requisito — respeita a build do jogador.

### Balanceamento crítico
Fase 6 (13.100 blocos acumulados) tem que ser **inviável na mão**. Se um jogador dedicado chega lá em 2h clicando, os minions estão mal precificados e o Ato III inteiro perde sentido. Alvo: 60% dos blocos do Ato II vindos de automação.

### Parede
"Tenho minion mas ele para toda hora." Energia e armazenamento têm que ser gerenciáveis, não punitivos: 1 tanque de energia deve durar uma sessão inteira (~3h), e o armazenamento deve encher em ~40 min sem upgrade — o suficiente pra doer, não pra travar.

### Checklist
- [ ] Módulo `system/skills` (XP, níveis, stats passivos, GUI, PlaceholderAPI)
- [ ] Módulo `system/collections` (contador vitalício, desbloqueio de receita na Forge)
- [ ] Banco da ilha em `islands` + `currencies`
- [ ] Gate de tipo de spawner por fase
- [ ] Varinha de Venda e Picareta de Explosão em `custom-items`
- [ ] Requisito multi-caminho na transição fase 6 → 7

---

## ATO III — "Escala"

**Fases 7–10 (Ermos Glaciais → Areias de Almas) · 54.000 blocos · alvo 10–50h**

### Objetivo de design
Produção industrial e **mercado**. É aqui que o servidor vive ou morre: se todo mundo faz a mesma coisa, o AH morre e o servidor esvazia.

### As três builds

| Build | Núcleo | Renda | Fraqueza |
|---|---|---|---|
| **Minerador** | Minions + geradores | Constante, offline | Teto baixo, precisa de muito capital |
| **Criador** | Spawners empilhados + arena | Alta com o jogador online | Zero offline, exige gear |
| **Fazendeiro** | Plantação + crop growth + Fazendeiro | Média, muito barata de escalar | Depende de preço de venda |

Regra de balanceamento: as três precisam ficar dentro de **±20% de coins/hora** no mesmo nível de investimento. Se uma vence, o AH vira monocultura.

### O que abre

| Desbloqueio | Fase | Papel |
|---|---|---|
| Island Level / Island Value (novo) | 7 | Métrica de competição, base do top |
| Upgrades de ilha completos (novo) | 7 | Limite de minion/spawner, spawn rate, growth |
| Custom Enchants — Forja Rúnica (novo) | 8 | Primeiro grande sumidouro |
| Boosters (novo) | 8 | Sumidouro recorrente |
| Gerador de Minério (bloco novo) | 8 | Loop de gerador que hoje falta |
| Silo + Coletor (blocos novos) | 9 | Logística de ilha grande |
| Minions tier III–V | 9 | Escala da renda passiva |
| Mochila, Coração da Ilha, Cristal de Crescimento | 9–10 | QoL de alto nível |
| Bazaar ou AH aquecido | 10 | Especialização vira lucro |

### Island Value — tabela base

```yaml
COBBLESTONE: 1
STONE: 1
IRON_BLOCK: 40
GOLD_BLOCK: 80
DIAMOND_BLOCK: 200
EMERALD_BLOCK: 250
NETHERITE_BLOCK: 1000
BEACON: 2000
# custom
GERADOR_DE_MINERIO: 500 * tier
SPAWNER: 300 * quantidade_empilhada
MINION: 400 * tier
```

Island Level = `floor(sqrt(valor / 100))`. Valor 1.000.000 → nível 100.

Cálculo: assíncrono por chunk com cache; spawners e minions vêm dos managers em memória (`PlacedSpawnerManager`, `PlacedMinionManager`), sem varrer chunk.

### Gates

- **Entrada**: 3 eixos ativos (minion + spawner/skill + banco com saldo).
- **Saída**: Island Level 40 + fase 11.

### Parede
Tédio. 54.000 blocos é muito. O antídoto é **conteúdo lateral**: eventos globais, top semanal, missões de coleção, primeiro boss. Se o único objetivo é "quebrar mais", o jogador sai na fase 9.

### Checklist
- [ ] Island Value/Level + `island-values.yml` + integração no `tops`
- [ ] Upgrades de ilha (9 tipos, tabela da spec §4.2)
- [ ] Forja Rúnica (custom enchants) + livros com raridade e chance de falha
- [ ] Boosters (temporários, de ilha e pessoais)
- [ ] Blocos: Gerador de Minério, Silo, Coletor
- [ ] Minions tier III–V + produção offline
- [ ] Taxa de 3% no Auction House (sumidouro)

---

## ATO IV — "Especialização"

**Fases 11–13 (Fortaleza Vulcânica → Cidades do End) · 113.000 blocos · alvo 50–150h**

### Objetivo de design
O jogador para de progredir *quebrando* e passa a progredir *jogando conteúdo*. A ilha vira a base, não o jogo.

### O que abre

| Sistema | Descrição | Recompensa |
|---|---|---|
| **Bosses de ilha** | Invocados com item craftado no Altar; escalam com nº de membros online | Fragmentos de Alma, materiais exclusivos |
| **Slayer / Caçadas** | Quest: N mobs → mini-boss → boss. 5 tipos, 9 níveis cada | Armas, stats permanentes |
| **Dungeon** | Instância coop, 5 andares, chave como entrada, ranking por tempo | Melhor gear do jogo |
| **Acessórios / Reforge** | Bag de talismãs com poder agregado, reforge com rolagem aleatória | Stats + sumidouro infinito |
| **Minions tier VI–X** | Super Compactor, Auto-Sell, Expander | Renda passiva de end-game |

### Estrutura de um boss de ilha

```
Item de invocação (craft na Forge, gate por fase)
  → colocado no Altar de Invocação
  → boss spawna com HP = base * (1 + 0.4 * membros_online)
  → 3 fases de combate, mecânica simples (adds, área, enrage aos 25%)
  → loot table própria: material exclusivo garantido + rolagem de raro
  → cooldown de 6h por ilha
```

Cooldown por ilha, não por jogador — senão a ilha de 8 membros invoca 8×.

### Slayer — desenho

| Tier | Mobs a matar | Boss | Desbloqueia |
|---|---|---|---|
| I | 50 zumbis | Zumbi Podre | Espada de Osso |
| II | 150 | Zumbi Ancião | +5% dano vs undead |
| III | 400 | Ceifador | Armadura de Alma |
| IV | 1.000 | Rei da Podridão | Arma de tier 4 |
| V | 2.500 | Devorador | Stat permanente |

Repetir a estrutura para Aranha, Enderman, Blaze, Warden.

### Gates
- **Entrada**: Island Level 40.
- **Saída**: 1 boss derrotado + 1 dungeon completa + fase 14.

### Parede
Dificuldade de combate num modo que até aqui era de mineração. O gear precisa vir do próprio conteúdo (drop do boss tier 1 permite boss tier 2), nunca da economia — senão quem tem dinheiro pula tudo.

### Checklist
- [ ] Módulo `system/bosses` + Altar de Invocação (CustomBlock)
- [ ] Módulo `system/slayer` (quest, progresso, boss, níveis)
- [ ] Módulo `system/dungeon` (instância, andares, chave, ranking)
- [ ] Módulo `system/accessories` (bag, poder agregado, reforge)
- [ ] Minions tier VI–X + upgrades de end-game

---

## ATO V — "End-game"

**Fase 14 + pós-fase · 100.000 blocos · 150h+**

### Objetivo de design
Dar motivo para continuar depois de "terminar". Sem isso, o jogador que zera a fase 14 sai e não volta.

### Escuridão Profunda
Última fase, Warden no pool, 100.000 blocos. Deve ser uma parede reconhecida como tal — é o troféu.

### Prestígio / Renascimento

```
Requisito: concluir fase 14
Ação: reseta fase para 1, mantém ilha, itens e skills
Ganho permanente por prestígio:
  +10% drop de bloco
  +5% velocidade de minion
  +1 slot de minion
  +N Núcleos de Prestígio (moeda)
Loja de prestígio: cosméticos, upgrades exclusivos, tags
Prestígio 2+: exige também Island Level mínimo crescente
```

Curva de fase no prestígio: −20% de blocos exigidos por prestígio, piso de 40% do original. Segunda volta é rápida, décima não é instantânea.

### Seasons

- Duração: 3 meses.
- Reseta: top, passe, ranking de dungeon.
- Persiste: ilha, itens, skills, prestígio.
- Premia: tag permanente, cosmético exclusivo, entrada no hall of fame.

### Eventos globais

| Evento | Frequência | Efeito |
|---|---|---|
| Chuva de Meteoros | 2×/dia | Blocos raros caem nas ilhas por 15 min |
| Invasão | 1×/dia | Ondas de mobs na ilha; recompensa por onda |
| Rush | 3×/semana | Bloco central vale 2× por 30 min |
| Mercador Raro | 1×/dia | `trader` com estoque limitado por servidor |

Escassez com estoque global é o que faz o jogador voltar em horário marcado.

### Checklist
- [ ] Sistema de prestígio + moeda + loja
- [ ] Seasons (reset de top/passe, hall of fame)
- [ ] Módulo `system/events` (agendador, anúncio, efeito)
- [ ] Expandir `trader` para evento global com estoque de servidor
- [ ] Guildas/alianças (opcional, se a base justificar)

---

## Parte 2 — Rebalanceamento do Minion Minerador

### Diagnóstico do estado atual

Lendo `MinerBehavior`, `PlacedMinionManager` e `MinionsSystem`:

| Ponto | Estado atual | Problema |
|---|---|---|
| **Bloco central** | `island.processBlockBreak(blockBelow)` — o minion avança a fase | **Crítico.** Um Minerador em cima do bloco central farma o eixo A inteiro AFK. Toda a progressão de fase vira "deixe o jogo ligado" |
| **Velocidade** | `processTick` roda a cada 20 ticks; cooldown = `(10 - speed - efficiency).coerceAtLeast(1)` — em **segundos** | Base 10s/bloco, chega a **1s/bloco** com Velocidade 5 + Efficiency 5. 10× de ganho, e o Efficiency é praticamente grátis |
| **Efficiency** | −1s por nível, sem custo no sistema | Encantamento vanilla barato dá metade do ganho total de velocidade |
| **Energia** | `consumeEnergy(1)` por bloco, `MAX_ENERGY = 10_000` | 10.000 blocos por tanque, independente da velocidade. Velocidade não custa nada |
| **Armazenamento** | 9 slots base, +9 por nível, máx. 4 níveis = 45 slots (2.880 itens) | Ok, mas destrava rápido demais |
| **Tiers** | Não existem | Sem eixo de progressão de longo prazo no minion |
| **Receitas** | `minerRecipe` e `farmerRecipe` são declaradas e **nunca registradas** (código morto). Só `craft_minion` é registrado: 9 netherite + Heavy Core + 8 fragmentos | O primeiro minion do jogo custa netherite — inalcançável no Ato I/II. Na prática o minion não existe até o end-game, ou é dado por comando |

Resumo: o minion é ao mesmo tempo **inacessível cedo demais** (receita de netherite) e **forte demais quando existe** (1s/bloco, avança fase AFK).

### Proposta de rebalanceamento

#### A. Bloco central — a mudança mais importante

**Opção recomendada (dura):** o Minerador comum **não trabalha no bloco central**. Se o bloco abaixo for `island.center`, o tick retorna sem minerar e o display mostra `<red>⛏ Não opera no bloco central`. Ele trabalha em blocos colocados pelo jogador (cobble de gerador, blocos comprados, plantações de pedra).

Isso muda o papel do minion de "farma sua progressão" para "processa sua produção" — que é o papel certo.

**Opção alternativa (suave):** trabalha no central, mas:
- conta apenas **25%** do progresso de fase (1 a cada 4 quebras),
- consome **10× energia** por quebra no central,
- exige o upgrade **Perfuração** (só disponível no Ato III).

**Perfuratriz** (minion novo, Ato III): único que opera no central com 100% de progresso. Custo alto, combustível especial (Núcleo de Magma), consumo de 15 energia/bloco. É o "cheat de AFK" transformado em objetivo de long-term.

#### B. Curva de velocidade

`processTick` roda a cada 20 ticks, então o cooldown é em segundos. Nova tabela:

| Velocidade | Atual (s/bloco) | Proposto (s/bloco) | Blocos/h |
|---|---|---|---|
| 0 | 10 | **20** | 180 |
| 1 | 9 | **17** | 212 |
| 2 | 8 | **15** | 240 |
| 3 | 7 | **13** | 277 |
| 4 | 6 | **11** | 327 |
| 5 | 5 | **9** | 400 |

- Ganho total do upgrade: de 2× (atual, sem contar Efficiency) para 2,2× — parecido, mas partindo de uma base **2× mais lenta**.
- **Efficiency**: deixa de dar −1s/nível. Passa a dar **−0,2s por nível** (teto −1s), ou é removido e vira upgrade pago. Recomendado: remover a leitura de `Enchantment.EFFICIENCY` do cooldown e mover o bônus para o tier do minion.
- **Piso absoluto**: 7s/bloco, mesmo com tudo somado.
- Preço do upgrade de Velocidade: `50.000 * 2.5^nivel` coins, pago do banco da ilha.

#### C. Energia proporcional à velocidade

```
custo_energia_por_bloco = 1 + nivel_velocidade
```

| Velocidade | Energia/bloco | Blocos por tanque (10.000) | Autonomia |
|---|---|---|---|
| 0 | 1 | 10.000 | 55h |
| 3 | 4 | 2.500 | 9h |
| 5 | 6 | 1.666 | 4h10 |

Velocidade passa a ter custo real: o minion rápido produz mais por hora mas queima combustível 6× mais rápido. Combustível vira demanda constante no AH — bom para a economia.

Fuel deve ser **craftado ou comprado**, nunca dado por comando em produção (`FuelCommand` só admin).

#### D. Tiers I–V

Substituem o upgrade solto e dão eixo de longo prazo:

| Tier | Requisito | Teto de Velocidade | Teto de Armazenamento | Ato |
|---|---|---|---|---|
| I | 16 Peças Mecânicas + picareta de ferro + Fase 4 | 1 | 1 (18 slots) | II |
| II | 32 Peças + 16 ferro + Fase 6 | 2 | 2 (27) | II |
| III | 64 Peças + 8 diamante + Cristal Abissal × 4 + Fase 8 | 3 | 3 (36) | III |
| IV | 128 Peças + Essência Exótica × 8 + Fase 10 | 4 | 4 (45) | III |
| V | 256 Peças + Coração de Magma × 4 + Fase 12 | 5 | 5 (54) | IV |

Tiers VI–X (Ato IV): destravam Compactor, Super Compactor, Auto-Sell, Expander.

#### E. Ferramenta

- Aceita picareta de **ferro para cima** (remove madeira/pedra do `acceptedToolMaterials` — não faz sentido um minion de fase 4 com picareta de madeira).
- Durabilidade continua consumindo 1 por bloco: picareta de diamante = 1.561 blocos ≈ 4h no tier baixo. É um bom sumidouro; **manter**.
- **Mending não funciona** no minion (bloquear na hora de equipar, com mensagem).
- Fortune continua funcionando — é o incentivo pra investir em ferramenta boa.

#### F. Limite por ilha

Minions colocados limitados pelo upgrade de ilha **Limite de Minions**: começa em **2**, vai a 25. Hoje não há limite — uma ilha com 40 minions é um problema de performance e de economia ao mesmo tempo.

#### G. Correções de código necessárias

- [ ] `MinionsSystem.setupRecipes()`: `minerRecipe` e `farmerRecipe` são criados mas nunca passados a `Crafts.register` — código morto. Registrar (com os custos novos) ou remover.
- [ ] `craft_minion` (9 netherite + Heavy Core) deve virar a receita da **Perfuratriz**, não do Minerador básico.
- [ ] `MinionUpgrade` não tem campo de preço — os preços estão só na view. Mover para o enum, como em `IslandUpgrade.getPrice`.
- [ ] `MinerBehavior.mine()` precisa do ramo novo de bloco central (recusar ou penalizar).
- [ ] Custo de energia deixa de ser fixo em `PlacedMinionManager.processTick` (`consumeEnergy(1)`) — passa a vir do behavior, que conhece o nível de velocidade.

### Resumo do impacto

| Métrica | Antes | Depois (tier V, velocidade 5) |
|---|---|---|
| Blocos/hora | 3.600 | 400 |
| Progresso de fase AFK | 100% | 0% (ou 25% com Perfuratriz) |
| Blocos por tanque | 10.000 | 1.666 |
| Custo de aquisição | 9 netherite (inviável) | 16 peças + picareta de ferro (Ato II) |
| Minions por ilha | ilimitado | 2 → 25 (upgrade) |

Fica **9× mais lento** por unidade, mas **acessível 30 horas mais cedo** e escalável por quantidade. O jogador constrói uma fábrica em vez de plantar um cheat.

---

## Parte 3 — Catálogo de itens novos

Todos implementados via `system/custom-items` (`CustomItem` para itens, `CustomBlock` para blocos colocáveis). Nenhum usa listener próprio.

### 3.1 Ferramentas de mineração

#### Picareta de Explosão
- **Ato**: II · **Material**: `DIAMOND_PICKAXE` · **Cooldown**: nenhum
- Quebra 3×3 no plano perpendicular ao olhar. Consome 3× durabilidade.
- Tiers: 3×3 → 5×5 → 5×5+profundidade 2 (upgrade na Forja).
- Não funciona no bloco central (senão vira 9× progresso de fase por clique).

#### Perfurador de Veia
- **Ato**: III · **Material**: `NETHERITE_PICKAXE` · **Cooldown**: 3s
- Quebra toda a veia conectada do mesmo material, até 64 blocos. Clique direito alterna ligado/desligado.
- Consome energia própria (recarrega com Núcleo de Magma).

#### Martelo Sísmico
- **Ato**: IV · **Material**: `MACE` · **Cooldown**: 30s
- Clique direito: onda que quebra todos os blocos num raio de 7 e empurra mobs. Uso pesado, cooldown longo.

#### Broca Portátil
- **Ato**: III · **Material**: `CROSSBOW` · **heldTickInterval**: 10
- Enquanto segurada, mina o bloco encarado continuamente sem clicar. Consome combustível por bloco.

### 3.2 Economia e logística

#### Varinha de Venda
- **Ato**: II · **Material**: `BLAZE_ROD` · **Cooldown**: 2s
- Clique direito num baú → vende todo o conteúdo pelo `SellManager`. Tem N usos (durabilidade), recarregável.
- Variante **Varinha Infinita** (Ato IV): sem limite, vende direto pro banco da ilha.

#### Mochila
- **Ato**: III · **Material**: `BUNDLE`
- Armazenamento portátil, tiers de 9 a 54 slots. Clique direito abre GUI. Conteúdo salvo em Base64 no NBT (via `ItemSerializer`, como o auction house).
- Variante **Mochila de Minério**: só aceita minérios, mas com auto-smelt.

#### Fundidor de Bolso
- **Ato**: II · **Material**: `FURNACE_MINECART` · **Cooldown**: 1s
- Clique direito: converte todo o minério do inventário em lingote. Consome carvão do inventário.

#### Comprimidor
- **Ato**: III · **Material**: `PISTON`
- Clique direito: comprime tudo que der (9 ferro → 1 bloco). Clique esquerdo: descomprime.

#### Cheque Bancário
- **Ato**: III · **Material**: `PAPER`
- Item com valor em coins, transferível e negociável no AH. Resolve troca de dinheiro entre jogadores sem comando.

### 3.3 Ilha e utilidade

#### Bússola de Fase
- **Ato**: I · **Material**: `RECOVERY_COMPASS`
- Segurada, mostra na action bar: fase atual, progresso, próxima fase e o que ela desbloqueia. Item de onboarding puro.

#### Coração da Ilha
- **Ato**: III · **Material**: `HEART_OF_THE_SEA` (bloco colocável)
- Enquanto colocado, dá buff à ilha inteira: +10% drop, +5% crop growth. Um por ilha. Upgradeável em 5 níveis.

#### Cristal de Crescimento
- **Ato**: III · **Material**: `AMETHYST_CLUSTER` (bloco)
- Acelera crescimento de plantação num raio de 8. Consome combustível. Empilha com o upgrade de ilha, mas com retorno decrescente.

#### Tocha de Spawn
- **Ato**: III · **Material**: `SOUL_TORCH` (bloco)
- Bloqueia spawn hostil natural num raio de 32. Resolve o problema de mob de fase spawnando em cima da fazenda.

#### Foguete de Voo
- **Ato**: III · **Material**: `FIREWORK_ROCKET`
- Consome tempo do `fly-timer` em vez de dar voo permanente. Integra o sistema existente ao consumível.

#### Teleportador de Ilha
- **Ato**: II · **Material**: `ENDER_PEARL`
- Clique direito: volta pra ilha. Cooldown 60s, bloqueado em combate (`combat-log`).

### 3.4 Combate

#### Espada de Almas
- **Ato**: IV · **Material**: `NETHERITE_SWORD`
- Acumula almas por kill; a cada 100 almas, +1 de dano permanente até o teto. Reset ao morrer (opcional, cria tensão).

#### Totem de Invocação
- **Ato**: IV · **Material**: `TOTEM_OF_UNDYING`
- Item de invocação de boss, colocado no Altar. Um por tipo de boss, craftado na Forge.

#### Chave de Masmorra
- **Ato**: IV · **Material**: `TRIAL_KEY`
- Entrada na dungeon. Tiers por andar. Sumidouro recorrente.

#### Poção de Concentração
- **Ato**: III · **Material**: `POTION`
- Booster pessoal em item: +50% drop por 15 min. Negociável no AH — transforma booster em economia, não em compra de loja.

#### Fragmento de Alma
- **Ato**: IV · **Material**: `ECHO_SHARD`
- Moeda de drop de boss. Usada na Forja para gear de tier alto. Não comprável — só dropável.

### 3.5 Blocos de produção

#### Gerador de Minério
- **Ato**: III · **Bloco**
- Gera minério em intervalo fixo no espaço acima. 5 tiers: carvão → ferro → ouro → diamante → netherite (com chance). É o loop de gerador clássico que hoje não existe no servidor.
- Combina com o Minerador: gerador produz, minion processa. **É a razão de o minion não precisar do bloco central.**

#### Fornalha Industrial
- **Ato**: III · **Bloco**
- Fila de 64 stacks, funde tudo sem supervisão, 4 tiers de velocidade. Combustível próprio.

#### Silo
- **Ato**: III · **Bloco**
- Armazena um único material até 1.000.000 unidades. Conecta ao Auto-Sell. Mostra holograma com a quantidade.

#### Coletor
- **Ato**: III · **Bloco**
- Recolhe o output de todos os minions num raio de 16 e joga no Silo/baú. Elimina o "andar de minion em minion".

#### Altar de Invocação
- **Ato**: IV · **Bloco**
- Onde o Totem é usado. Um por ilha. Define a área de arena do boss.

### 3.6 Resumo por ato

| Ato | Itens |
|---|---|
| I | Bússola de Fase, Ímã (existe) |
| II | Varinha de Venda, Picareta de Explosão, Fundidor de Bolso, Teleportador, Vacuum Hopper (existe) |
| III | Perfurador de Veia, Broca, Mochila, Comprimidor, Cheque, Coração da Ilha, Cristal de Crescimento, Tocha de Spawn, Foguete, Poção de Concentração, Gerador de Minério, Fornalha Industrial, Silo, Coletor |
| IV | Martelo Sísmico, Varinha Infinita, Espada de Almas, Totem, Chave de Masmorra, Fragmento de Alma, Altar |
| V | Núcleo de Prestígio, cosméticos |

---

## Parte 4 — Ordem de execução

| Sprint | Entrega | Por quê primeiro |
|---|---|---|
| 1 | Gates de fase + rebalanceamento do Minerador | Sem isso, todo o resto é construído em cima de uma progressão quebrada |
| 2 | Island Value/Level + banco da ilha + upgrades reais | Desbloqueia top, coop e sumidouro |
| 3 | Skills + Coleções | Progressão pessoal que persiste |
| 4 | Gerador de Minério + Silo + Coletor + tiers de minion | Fecha o loop de produção do Ato III |
| 5 | Forja Rúnica (enchants) + boosters + itens do Ato III | Sumidouro econômico |
| 6 | Bosses + slayer + dungeon | Conteúdo do Ato IV |
| 7 | Prestígio + seasons + eventos | End-game |

O Sprint 1 é o mais urgente e o mais barato: é rebalanceamento de números e um `if` no `MinerBehavior.mine()`, não sistema novo.
