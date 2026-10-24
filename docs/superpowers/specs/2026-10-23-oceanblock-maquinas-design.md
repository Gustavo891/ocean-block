# OceanBlock — Máquinas, energia e Picareta fundidora

Data: 2026-10-23
Pedido do usuário: "Eu quero criar receitas custom, picareta que derrete minerios (tente fazer a textura), uma fornalha nova onde derrete mais itens ao mesmo tempo e mais rapido. Sistema de maquinas, por exemplo: Gerador de energia; Fornalha nova citada acima que funciona com a energia; Miner (drill que quebra o bloco abaixo sozinho); etc".

## 1. Intenção

A ilha hoje produz minério pelo gerador de pedregulho (upgrade Gerador 0–4) e o jogador derrete na Fundição (1 item por vez, combustível sólido). O próximo degrau de progressão é **automação**: máquinas que trabalham sozinhas e consomem energia, com uma ferramenta intermediária (picareta que já derrete) para quem ainda minera na mão. Tudo é desbloqueado por pesquisa e craftado em `/receitas`, como o resto do modo.

## 2. Decisões do usuário

| Tema | Decisão |
|---|---|
| "Receitas custom" | Receitas próprias das coisas novas no `/receitas` existente (sem arquivo yml de receitas) |
| Transporte de energia | **Raio do gerador**: máquina só funciona se houver fonte com energia a até **8 blocos** |
| Armazenamento | A fonte guarda energia (bateria interna); Gerador até **10.000 E** |
| Combustível do Gerador | Combustível sólido, os mesmos da Fundição (`Fuel`) |
| Outras máquinas | **Painel solar** (gera pouco, de graça, de dia) |
| Fornalha elétrica | **4 slots** em paralelo, **4× mais rápida** que a Fundição (50 ticks por item), aceita minério bruto e os itens comuns de fornalha; abre menu de baú |
| Miner | Quebra o bloco **abaixo**, **1 bloco a cada 2 s**, só **pedra, pedregulho e minérios**; **guarda** em 27 slots internos e **empurra** para baú/funil encostado |
| Picareta | **Auto-fundição**: minério de ferro/cobre/ouro sai em lingote (respeita Fortuna), pedregulho sai como pedra; durabilidade normal; textura própria |
| Obtenção | **Pesquisa + receita**: nova categoria **Máquinas** em `/pesquisa` e em `/receitas` |
| Aparência | **Textura custom nos blocos** (blocos `CUSTOM` do Textura sobre note_block) |

## 3. Objetivos, não-objetivos, anti-objetivos

Objetivos: as quatro máquinas e a picareta jogáveis no servidor, com textura; energia por raio; tudo pesquisável e craftável; estado das máquinas sobrevive a restart e a quebrar/recolocar o bloco.

Não-objetivos (ficam para depois): bateria separada, cabos, triturador, funis na Fornalha elétrica, upgrades de máquina, aviso de "Minerador cheio", receitas em arquivo, máquinas trabalhando com o chunk descarregado.

Anti-objetivos: duplicar itens (dois jogadores no mesmo menu, quebra no meio de uma ação); máquinas quebrando a ilha ou blocos custom; lag com muitas máquinas (nada de varrer todos os blocos por tick); textura que some ou fica roxa/preta no cliente.

## 4. Unidade e números

Energia em **E**. Toda máquina roda no tick do framework a cada **10 ticks** (`tickInterval = 10`); as regras recebem a quantidade de ticks decorridos, como `SmelterRules.tick`.

| Máquina | Papel | Números |
|---|---|---|
| Gerador de energia | fonte | queima 1 tick de combustível por tick e gera **2 E/tick** (40 E/s); carvão (1600 ticks) = 3.200 E; bateria 10.000 E; combustível guardado até 64 de um tipo |
| Painel solar | fonte | **3 E a cada 10 ticks** (6 E/s) de dia (`world.time` 0–12.000) com céu aberto (bloco acima com `lightFromSky == 15`); bateria 1.000 E |
| Fornalha elétrica | consumidor | **1 E/tick por slot ativo** (20 E/s; 4 slots = 80 E/s, dois geradores cheios); 50 ticks por item = 50 E por item |
| Minerador | consumidor | 1 bloco a cada **40 ticks**, **40 E por bloco** (20 E/s) |

Limite por ilha (minha escolha): Gerador 4, Painel solar 8, Fornalha elétrica 4, Minerador 4.

O Gerador só queima quando a bateria não está cheia: com a bateria cheia ele pausa no meio da queima e não perde combustível.

## 5. Arquitetura

Novo módulo `plugins/oceanblock/system/machines` (pacote `ocean_block.system.machines`), auto-incluído pelo plugin. `MachinesSystem.init(plugin, viewFrame)` roda depois de `StationsSystem` e antes de `ResearchSystem`; registra os blocos em `CustomBlockRegistry`, a picareta em `CustomItemRegistry`, as receitas em `Recipes` e os listeners. Dependências `compileOnly`: `custom-items`, `craft`, `stations` (reusa `Fuel`), `islands`, `textura`, `lib:*`. Testes Kotest no próprio módulo.

Toda regra de jogo é pura e testada (`EnergyRules`, `GeneratorRules`, `SolarRules`, `FurnaceRules`, `MinerRules`, `AutoSmelt`); o lado Bukkit só lê o mundo, chama a regra e grava. Estado de cada máquina mora nos `counters` do `PlacedBlock` (como a Fundição), então quebrar e recolocar mantém tudo, e o flush do `PlacedBlockManager` persiste.

### 5.1 Energia por raio

- Fontes (Gerador, Painel) guardam a energia no counter `energia`.
- `EnergyIndex`: ilha → fontes (uuid, centro do bloco), montado a partir de `PlacedBlockManager.all()` só com chunks carregados; refeito quando uma fonte é colocada ou quebrada e a cada 100 ticks. Nada varre todos os blocos por tick.
- Um consumidor que precisa de N E no ciclo chama `EnergyNetwork.draw(consumidor, N)`: fontes da mesma ilha e mundo com distância euclidiana entre centros ≤ 8,0, ordenadas da mais perto (empate por uuid). A regra pura `EnergyRules.plan(needed, fontes)` devolve quanto tirar de cada uma; **tudo ou nada**: se a soma disponível for menor que N, não tira nada e a máquina não trabalha naquele ciclo (status "Sem energia").
- Várias fontes no raio somam; um consumidor pode puxar de várias.

### 5.2 Blocos com textura (Textura)

- `server/plugins/Textura/configs/blocks/maquinas.yml` declara `gerador_energia`, `painel_solar`, `fornalha_eletrica`, `minerador` como `type: CUSTOM`, `item_material: note_block`, `textures` com `top`/`bottom`/`side` (modelo `cube_bottom_top`) e `block_default_drop: false`.
- A classe base `MachineBlock : CustomBlock` usa `material = NOTE_BLOCK`, `itemModel = oneblock:<id>`, e o item ganha a marca de bloco do Textura para que o `CustomBlockListener` do Textura aplique a variante de note_block ao colocar. Se a API pública do Textura não expuser como marcar um item, adiciona-se `BlockApi.markItem(item, id)` (tipos Java/Bukkit apenas, sem default).
- Quebra: o listener do custom-items dropa o item com o estado; o do Textura (MONITOR) não dropa nada porque `block_default_drop: false`. O `NoteBlockGuard` do Textura já impede tocar nota e mudar a variante.
- As texturas são PNG 16×16 em `resource/assets/oneblock/textures/block/maquinas/`, geradas por um script descartável que parte das texturas vanilla do `generated/vanilla/client-26.3.jar` (fornalha, alto-forno, observador, sensor de luz) com recolor e sobreposições: Gerador com bobina de cobre e raio, Fornalha elétrica com brilho ciano, Minerador com broca embaixo, Painel solar com células azuis em cima.
- Como a face frontal não gira (a variante de note_block é fixa), as quatro laterais são iguais.

### 5.3 Menus

Baú vanilla padrão sem arte (preferência do usuário), título em texto cinza-escuro, `cancelInteractions`, atualização a cada 10 ticks. Todo clique relê o `PlacedBlock` e verifica que ele ainda está registrado e que o jogador ainda pode gerenciar a ilha, então dois jogadores no mesmo menu ou a máquina trabalhando no meio não duplicam itens. Retiradas usam `saveNow`; o trabalho da máquina usa `markDirty`. Clicar no próprio inventário (parte de baixo) **coloca** o item na máquina quando ele é aceito.

- **Gerador**: item de status (energia `x / 10.000 E`, queimando ou parado, combustível restante) e o slot de combustível (clicar retira; clicar num combustível do inventário coloca).
- **Fornalha elétrica**: 4 colunas, cada uma com entrada (linha 1), progresso (linha 2: "Derretendo 40%", "Sem energia", "Saída cheia", "Vazio") e saída (linha 3); status de energia à esquerda. Clicar na entrada devolve; clicar na saída coleta; clicar num item do inventário coloca no primeiro slot com o mesmo item e espaço, senão no primeiro vazio.
- **Minerador**: 27 slots guardados (só saída), botão "Coletar tudo" e item de status (energia no raio, bloco abaixo, "Armazenamento cheio").
- **Painel solar**: sem menu; clique direito manda no chat `Painel solar: <x>/1.000 E, gerando 6 E/s` ou `parado (noite ou sem céu aberto)`.

### 5.4 Fornalha elétrica

- Receitas: as de fornalha vanilla do servidor (`FurnaceRecipe` em `Bukkit.recipeIterator()`, só `MaterialChoice`), lidas uma vez no `init` num mapa material → resultado; item custom (com id do custom-items) nunca entra. A regra pura recebe esse mapa.
- Cada slot guarda um material (até 64) e sua saída (até 64 do resultado). Um slot trabalha se tem entrada, a saída é vazia ou do mesmo resultado e cabe mais um, e o ciclo conseguiu a energia. Progresso por slot; 50 ticks completam 1 item. Sem lingote bônus.
- Counters: `forno_in/<slot>/<MATERIAL>`, `forno_out/<slot>/<MATERIAL>`, `forno_progress/<slot>`.
- `VanillaGuard` não se aplica (note_block não tem inventário).

### 5.5 Minerador

- A cada 40 ticks: se o bloco logo abaixo está na lista (pedregulho, pedra e os minérios de carvão, cobre, ferro, ouro, redstone, lápis, esmeralda e diamante, mais as versões deepslate), pertence à mesma ilha, não é bloco custom, os drops cabem e o ciclo conseguiu 40 E, então quebra: drops de `block.getDrops(picareta de ferro)`, efeito e som de quebra, bloco vira ar com física (a água/lava do gerador forma o próximo bloco).
- Saída: primeiro tenta entregar num contêiner vanilla encostado (acima ou nas 4 laterais; nunca um bloco custom), depois guarda no armazenamento interno de 27 slots (`storageCapacity = 27`). A cada ciclo também esvazia o armazenamento no contêiner encostado. Armazenamento cheio pausa sem gastar energia.

### 5.6 Picareta fundidora

- `CustomItem` `picareta_fundidora`, `IRON_PICKAXE`, `itemModel = oneblock:picareta_fundidora`, item flat em `configs/items/maquinas.yml` com textura em `resource/assets/oneblock/textures/item/maquinas/picareta_fundidora.png` (picareta de ferro vanilla com a cabeça recolorida em laranja/brasa).
- Listener de `BlockDropItemEvent`: com ela na mão principal, troca cada drop pelo mapa puro `AutoSmelt` (`RAW_IRON`→`IRON_INGOT`, `RAW_COPPER`→`COPPER_INGOT`, `RAW_GOLD`→`GOLD_INGOT`, `COBBLESTONE`→`STONE`), mantendo a quantidade (Fortuna funciona porque multiplica o bruto). Quando algo foi fundido: partícula de chama e som curto. Durabilidade e encantamentos vanilla.

## 6. Pesquisa e receitas

Nova `ResearchCategory.MAQUINAS("Máquinas", "observer")` e `RecipeCategory.MAQUINAS("Máquinas", "observer", "Máquinas que trabalham sozinhas com energia.")`. A picareta fica em Ferramentas nas receitas e em Fundição na pesquisa.

Os menus de receitas e pesquisa passam a ter **4 vagas de categoria** (coluna 1, linhas 1–4; `CATEGORY_SLOTS = 4` nos dois `Lore`). Na pesquisa o relógio de slots/fila desce para a linha 5, coluna 1. Se a arte de fundo (`receitas_menu.png`, `pesquisa_menu.png`) desenhar molduras só nas 3 vagas, a arte ganha a moldura da 4ª vaga e a do relógio no novo lugar.

| Pesquisa (id) | Categoria | Custo | Tempo | Requer |
|---|---|---|---|---|
| `picareta_fundidora` | Fundição | 8 ferro, 4 ouro, 16 carvão | 120 s | `fundicao_melhorada` |
| `gerador_energia` | Máquinas | 12 ferro, 8 cobre, 8 redstone | 180 s | `fundicao_melhorada` |
| `painel_solar` | Máquinas | 12 cobre, 8 lápis, 6 redstone | 180 s | `gerador_energia` |
| `fornalha_eletrica` | Máquinas | 16 ferro, 8 ouro, 12 redstone | 300 s | `gerador_energia` |
| `minerador` | Máquinas | 20 ferro, 6 ouro, 12 redstone | 300 s | `gerador_energia` |

| Receita (id = bloco/item) | Ingredientes |
|---|---|
| `picareta_fundidora` | 4 ferro, 2 ouro, 8 carvão |
| `gerador_energia` | 1 Fundição, 8 ferro, 6 cobre, 4 redstone |
| `painel_solar` | 6 cobre, 4 lápis, 2 redstone, 2 ferro |
| `fornalha_eletrica` | 1 Fundição rápida, 10 ferro, 4 ouro, 8 redstone |
| `minerador` | 14 ferro, 4 ouro, 8 redstone |

Cada receita tem `research = <id>`. Custos, tempos e ingredientes são minha escolha, pensando no gerador da ilha: redstone a partir do nível 2, lápis a partir do 3; diamante fica de fora para o Minerador não depender do nível 4.

## 7. Testes e verificação

- Kotest: `EnergyRules.plan` (ordem por distância, soma de várias fontes, tudo ou nada, raio), `GeneratorRules` (queima, pausa com bateria cheia, troca de combustível, carga), `SolarRules`, `FurnaceRules` (colocar, retirar, 4 slots em paralelo, saída cheia, sem energia, mapa de receitas injetado), `MinerRules` (lista de blocos, custo, intervalo), `AutoSmelt`, ids/limites dos blocos; atualizar `ResearchTreeTest`, `RecipeLoreTest` e `ResearchLoreTest` para 4 categorias.
- O lado Bukkit (colocar/quebrar, textura no cliente, menus, contêineres, drops da picareta) vai para uma nova seção do manual de aceite (`docs/superpowers/plans/2026-10-18-oceanblock-aceite-manual.md`).
- Pronto: testes verdes, `localDeploy` dos dois plugins com o servidor parado, servidor reiniciado com "Done (" e "Hash:" sem erros novos, CLAUDE.md e manual atualizados, commits na `main`.

## 8. Deixado para quem constrói

Layout exato dos slots nos menus, ícones dos itens de status, textos de lore, partículas e sons, arte final das texturas (dentro do que a seção 5.2 descreve) e nomes internos de classes.
