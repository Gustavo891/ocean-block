# OceanBlock — Peneira, Fundição e Mesa de Pesquisa

Data: 2026-10-21. Parte do subprojeto 4 (tecnologia) da spec `docs/superpowers/specs/2026-10-18-oceanblock-design.md`; as fases do mar (subprojeto 3) ainda não existem, então nada aqui depende delas.

## 1. Intenção

No mar não há terra nem mina. Hoje o metal só aparece em pepitas e cobre bruto da `sucata` e, raramente, um lingote em `barril`. O jogador precisa de uma **primeira fonte de minério cedo e simples** (peneira + fundição) e de um **motivo para progredir** (Mesa de Pesquisa, estilo Raft) que libere versões melhores e automação. A Mesa também vira o lugar onde as próximas estações (draga, gerador, covo, aquário) serão liberadas.

Sucesso: na primeira meia hora o jogador puxa areia/cascalho da deriva, peneira, funde o primeiro lingote, monta a Mesa e libera a Rede de Detritos; depois disso tem uma árvore curta de melhorias com esperas de segundos a poucos minutos.

## 2. Objetivos, não-objetivos, anti-objetivos

**Objetivos**
- Peneira em três níveis (madeira, ferro, diamante) + peneira automática, operada por cliques.
- Fundição como **única** forma de transformar pedaço de minério em lingote; versão melhorada e versão com funil.
- Mesa de Pesquisa com entrega parcial de itens, tempo de pesquisa, fila de uma pesquisa por vez com vagas extras liberadas pela própria árvore, progresso por time do raft.
- Receitas ligadas a pesquisa aparecem bloqueadas no dialog de Receitas até o time pesquisar.
- Areia, cascalho e terra passam a chegar pela deriva e pela Rede.

**Não-objetivos (v1)**
- Gating por fase do mar (fica para o subprojeto 3: a definição de pesquisa ganha um campo de fase depois).
- Draga, gerador de pedra, mergulho, covo, aquário, isca, tripulantes, preços de economia.
- Cancelar pesquisa ou devolver itens entregues.
- Editar pesquisas pelo Textura Studio.

**Anti-objetivos** (seria falha mesmo funcionando)
- Espera longa: nenhuma pesquisa da v1 passa de 5 minutos.
- Travar o início: peneira básica, fundição básica, Mesa e Gancho são craftáveis desde o primeiro minuto, só com o que a deriva dá.
- Ciclo impossível: nenhuma receita inicial pode depender de algo que só sai de outra estação ainda bloqueada.
- Perder progresso de pesquisa num restart ou crash além da janela normal de flush.

## 3. Decisões do usuário (fechadas)

| Tema | Decisão |
|---|---|
| Peneira básica | Liberada desde o começo |
| Pedaço → metal | **Só a Fundição custom** (pedaço não derrete em fornalha vanilla) |
| Uso da peneira | Clicar várias vezes (estilo Ex Nihilo); automação por pesquisa |
| Custo da pesquisa | Entregar itens; progresso do time do raft |
| Entrega | Aos poucos, qualquer membro completa |
| Receitas existentes | Gancho continua livre; **Rede de Detritos vira a primeira pesquisa** |
| Tempo | A pesquisa leva tempo depois de tudo entregue, cresce por nível, de segundos a alguns minutos; conta offline |
| Paralelismo | Uma pesquisa por vez; a árvore libera mais vagas |

## 4. Peneira

### 4.1 Bloco e níveis
- `CustomBlock` no framework de `custom-items`, um bloco por nível: `peneira` (madeira), `peneira_ferro`, `peneira_diamante`, `peneira_automatica`. `limitPerIsland` 6 cada *(minha decisão)*.
- Material base `COMPOSTER` *(minha decisão)*: o nível visível do compostor (0–8) mostra a camada baixando. A interação vanilla é sempre negada, como a `NetListener` faz com o tear.
- Receitas no `craft` (categoria nova `PROCESSAMENTO`, ícone do tema `stone`, ex.: `ui:sparkle`):
  - `peneira`: 4 Tábuas + 2 Linhas + 2 Gravetos — livre.
  - `peneira_ferro`: 1 Peneira + 4 Lingotes de ferro — pesquisa `peneira_ferro`.
  - `peneira_diamante`: 1 Peneira de ferro + 2 Diamantes + 4 Lingotes de ouro — pesquisa `peneira_diamante`.
  - `peneira_automatica`: 1 Peneira de ferro + 4 Lingotes de ferro + 4 Redstone + 1 Funil — pesquisa `peneira_automatica`.
  (Ingrediente "peneira" casa pelo id custom do item, não pelo material.)

### 4.2 Uso
- Clique direito com **areia, cascalho ou terra** na mão principal numa peneira vazia: consome 1 bloco e "carrega" a peneira (compostor no nível 7).
- Cada clique direito seguinte (mão vazia ou qualquer item) avança uma etapa: 6 etapas na madeira, 5 no ferro, 4 no diamante *(minha decisão)*. Cada etapa: partícula de bloco do material, som de areia/cascalho, compostor baixa um nível.
- Na última etapa: rola a tabela do material e do nível e **entrega pelo mesmo caminho da deriva** (inventário, sobra no chão, notificação na região `coleta` com ícone e `+N item`), volta para vazia.
- Shift+clique direito com material na mão carrega e já faz a primeira etapa (só conforto) *(minha decisão)*.
- Quem pode usar: mesma regra do `CollectorRule` (dono, membros, confiados).
- Cooldown mínimo de 4 ticks entre etapas por jogador, para segurar autoclick *(minha decisão)*.

### 4.3 Peneira automática
- Recebe material por funil vindo de cima, faz uma etapa a cada 20 ticks e solta o resultado num contêiner diretamente abaixo (funil/baú). Sem contêiner embaixo ou contêiner cheio: para. Funciona com o chunk carregado, sem forçar chunk.
- Tabela igual à da peneira de ferro.

### 4.4 Tabelas (config `server/plugins/ocean-block/peneira.yml`, mesmo formato `material/min/max/chance` do `drift.yml`, rolagem independente como o `DropRoller`)
Valores iniciais *(minha decisão, para balancear em jogo)*:

| Material | Saída | Madeira | Ferro | Diamante |
|---|---|---|---|---|
| Cascalho | Pedaço de ferro (1–2) | 30% | 40% | 50% |
| | Pedaço de cobre (1–3) | 25% | 35% | 45% |
| | Carvão (1) | 15% | 20% | 25% |
| | Pederneira (1) | 20% | 20% | 20% |
| | Redstone (1–2) | — | 4% | 10% |
| | Diamante (1) | — | 0,5% | 2% |
| Areia | Pedaço de ouro (1) | 12% | 20% | 30% |
| | Argila (bola, 1–2) | 35% | 40% | 45% |
| | Cristal de prismarinho (1) | 6% | 10% | 15% |
| | Lápis-lazúli (1–2) | — | 4% | 10% |
| Terra | Sementes de trigo | 25% | 25% | 25% |
| | Cana-de-açúcar | 10% | 12% | 15% |
| | Osso | 12% | 15% | 18% |
| | Muda de carvalho | 5% | 6% | 8% |

Nenhuma rolagem acerta → nada sai (diferente da deriva, que dá Linha). Material inválido ou chance fora de 0..1 no yml: aviso e ignora a linha, como o `DriftConfigParser`.

### 4.5 Pedaços de minério
- Três itens custom (`CustomItem`): `pedaco_ferro`, `pedaco_cobre`, `pedaco_ouro`. Material base `RAW_IRON`/`RAW_COPPER`/`RAW_GOLD` com nome, lore e id custom; sem receita vanilla e **sem receita de fornalha** — a fornalha vanilla recusa (o evento de fundição vanilla é cancelado para itens com id custom). Modelo/textura próprios entram depois pelo Textura (`configs/items`).
- Como o material base é minério bruto vanilla, os usos vanilla precisam ser barrados para itens com id custom: fundir em fornalha/alto-forno/defumador (eventos de início e fim de fundição) e as receitas de bancada que usam o material (bloco de minério bruto). O ícone da notificação usa o material base (`item:raw_iron` etc.), registrado em `NotificationApi.registerItemIcons` junto com os outros drops da peneira.
- Os minérios brutos vanilla que já vêm da deriva (`RAW_COPPER`) também entram na Fundição, 1 bruto → 1 lingote *(minha decisão: sem pedra no começo, a fornalha vanilla não existe, e sem isso o cobre da `sucata` ficaria inútil)*. As pepitas (`IRON_NUGGET`) continuam com a receita vanilla de 9 → 1 lingote.

## 5. Fundição

### 5.1 Bloco e níveis
- `CustomBlock` `fundicao`, `fundicao_melhorada`, `fundicao_funil`. Material base `BLAST_FURNACE` *(minha decisão)*, interação vanilla sempre negada, estado `lit` ligado enquanto queima. `limitPerIsland` 4.
- Receitas (categoria `PROCESSAMENTO`):
  - `fundicao`: 8 Argila + 4 Tábuas — livre. A argila sai da peneira de areia, sem fornalha.
  - `fundicao_melhorada`: 1 Fundição + 6 Lingotes de ferro + 4 Lingotes de cobre — pesquisa `fundicao_melhorada`.
  - `fundicao_funil`: 1 Fundição melhorada + 2 Funis + 2 Redstone — pesquisa `fundicao_funil`.

### 5.2 Uso
- Clique direito com **pedaços** (ou minério bruto vanilla) na mão: coloca todos daquele tipo, até 32 na entrada (um tipo por vez; outro tipo com entrada ocupada é recusado com mensagem curta).
- Clique direito com **combustível**: adiciona tudo da mão até 16 itens de combustível guardados. Combustíveis e tempos *(minha decisão)*: tábua 15 s, tronco 60 s, graveto 5 s, carvão/carvão vegetal 80 s, bloco de alga seca 200 s.
- Clique direito com a mão vazia: retira os lingotes prontos. Shift+mão vazia: retira também a entrada e o combustível guardados.
- Cada **4 pedaços → 1 lingote** do metal. Tempo por lingote: 10 s (básica), 5 s (melhorada, e 25% de chance de lingote extra), igual à melhorada na de funil. A saída guarda até 16 lingotes; cheia, para.
- Um `ItemDisplay` em cima mostra o pedaço que está derretendo; partículas de chama enquanto queima.
- Processa no `tick` do bloco, só com chunk carregado. Estado persistido no `PlacedBlock`: entrada e saída na `storage` existente; combustível restante, progresso em ticks e combustível guardado precisam de campos novos (o `PlacedBlock` hoje só tem `upgrades` e `storage`): o construtor adiciona um mapa de contadores numéricos persistido pelo `PlacedBlockCodec`.
- Fundição com funil: puxa pedaços e combustível de funil acima/lado e solta lingotes no contêiner abaixo, como uma fornalha vanilla.
- Quebrar uma fundição ou peneira devolve no item (como a Rede faz com a `storage`) a entrada, o combustível e a saída guardados; uma peneira carregada perde só o material da etapa atual *(minha decisão)*.
- Custo realista de ferro: 1 lingote = 4 pedaços ≈ 10 cascalhos na peneira de madeira. A primeira pesquisa com ferro (`peneira_ferro`) mais a receita pedem 8 lingotes, cerca de 80 cascalhos — é o primeiro "objetivo" de meio de jogo. Ajustar no balanceamento em jogo, não no código.

## 6. Origem de areia, cascalho e terra
- Três tipos novos em `detritos` do `drift.yml` *(minha decisão)*: `torrao_areia` (drop `SAND` 1–3), `torrao_cascalho` (`GRAVEL` 1–3), `torrao_lodo` (`DIRT` 1–2), com `peso` que somado dê por volta de 2–4 torrões por minuto de corrente ativa. O `barril` ganha `SAND` e `GRAVEL` com chance média.
- A Rede de Detritos coleta esses tipos como qualquer outro detrito. Os ícones novos entram em `NotificationApi.registerItemIcons` como os outros drops.

## 7. Mesa de Pesquisa

### 7.1 Bloco
- `CustomBlock` `mesa_pesquisa`, material base `CARTOGRAPHY_TABLE` *(minha decisão)*, interação vanilla negada, `limitPerIsland` 2. Receita livre: 6 Tábuas + 3 Linhas + 2 Pepitas de ferro (vem da `sucata`) *(minha decisão)*.
- Clique direito (quem passa no `CollectorRule`) abre o dialog `server/plugins/Textura/configs/ui/pesquisa.json`.

### 7.2 Modelo
- `ResearchDefinition(id, nome, descricao, categoria, icone, custo: List<Ingredient + quantidade>, duracao, requer: List<id>, efeito)`, registradas em código num livro global como o `Recipes`. Efeito: libera receitas (ids) e/ou soma vagas de pesquisa.
- Estado por raft (`islandUuid`): itens entregues por pesquisa e ingrediente, pesquisas concluídas, fila, e as pesquisas rodando com instante de início e duração (relógio de parede, para contar offline).
- Persistência no padrão Manager + Repository do projeto (coleção Mongo própria, flush dos ids sujos a cada 30 s, `onDisable` chamado no `OceanBlockPlugin.onDisable`). Entrega de itens salva na hora (`saveNow`), como a retirada da Rede, para nunca perder item consumido.

### 7.3 Fluxo
1. **Bloqueada**: falta pré-requisito. Cartão mostra o que falta.
2. **Disponível**: botão "Entregar" puxa do inventário de quem clicou o que tiver de cada ingrediente, até o que falta. Progresso aparece como `3/10 Tábuas`.
3. Tudo entregue → **Na fila**. Se houver vaga livre, vira **Pesquisando** na hora.
4. **Pesquisando**: termina em `início + duração`. Um timer global de 1 s confere as pesquisas dos rafts carregados; ao abrir a Mesa ou carregar o raft, as vencidas concluem na hora (offline incluso).
5. **Concluída**: aplica o efeito, notifica os membros online na região `avisos` (`<green>Pesquisa concluída:</green> <nome>`, ícone do resultado) com som, e a próxima da fila começa.
- Vagas: começa com 1. `bancada_auxiliar` → 2, `bancada_auxiliar_2` → 3.
- Sem cancelar, sem devolver (v1).

### 7.4 Árvore v1 *(custos e tempos minha decisão)*

| Pesquisa | Requer | Custo | Tempo | Libera |
|---|---|---|---|---|
| `rede` Rede de Detritos | — | 10 Tábuas, 6 Linhas | 20 s | receita `rede` |
| `peneira_ferro` | — | 4 Lingotes de ferro, 8 Linhas | 1 min | `peneira_ferro` |
| `fundicao_melhorada` | — | 6 Lingotes de ferro, 4 de cobre, 8 Argila | 2 min | `fundicao_melhorada` |
| `bancada_auxiliar` | `rede` | 12 Tábuas, 6 Lingotes de ferro | 90 s | +1 vaga |
| `peneira_diamante` | `peneira_ferro` | 2 Diamantes, 8 Lingotes de ferro | 4 min | `peneira_diamante` |
| `peneira_automatica` | `peneira_ferro` | 10 Lingotes de ferro, 4 Redstone | 5 min | `peneira_automatica` |
| `fundicao_funil` | `fundicao_melhorada` | 10 Lingotes de ferro, 4 de cobre, 2 Redstone | 5 min | `fundicao_funil` |
| `bancada_auxiliar_2` | `bancada_auxiliar`, `peneira_diamante` | 12 Lingotes de ferro, 6 de ouro, 2 Diamantes | 5 min | +1 vaga |

Viabilidade: redstone e diamante só saem da peneira de ferro em diante, e a peneira de ferro só pede ferro e linha — não há ciclo.

### 7.5 Tela (dialog do Textura, feito no Studio)
- Navegação à esquerda: categorias (Coleta, Fundição, Raft) e o painel "Fila" (pesquisando com tempo restante, na fila, vagas livres/total).
- Grade de cartões: ícone, nome, descrição curta, custo com progresso, estado e botão (Entregar / Na fila / `faltam 1m20s` / Concluída / Bloqueada).
- Botão "Atualizar" redesenha (o dialog é estático; o tempo restante é calculado ao desenhar).
- Entradas montadas por uma função pura (como `RecipeEntries`) para ter teste.

### 7.6 Receitas bloqueadas
- `OceanRecipe` ganha um campo opcional `pesquisa` (id). O `craft` não depende do módulo de pesquisa: ele expõe uma porta "esta receita está liberada para este jogador?" (padrão: sim), e o sistema de pesquisa a preenche no `init` resolvendo jogador → raft → pesquisas concluídas.
- No dialog de Receitas, receita bloqueada aparece com cadeado, custo normal e o texto `Pesquise na Mesa: <nome>`; o botão Craftar fica desativado. Craftar por outro caminho também recusa.

## 8. Onde fica no código *(minha decisão)*
- Módulo novo `system/stations`: peneira, fundição, pedaços, tabelas (`peneira.yml`), receitas dessas estações.
- Módulo novo `system/research`: Mesa, definições, estado, manager/repository, tela, porta de receitas.
- `craft`: campo `pesquisa` no `OceanRecipe`, porta de liberação, cadeado na tela; `DriftRecipes.rede()` passa a ter `pesquisa = "rede"`.
- `drift`: tipos novos no `drift.yml` bundled.
- Ambos os módulos novos entram no `settings.gradle.kts`, no plugin e no `CLAUDE.md`.

## 9. Testes
- Kotest nas regras puras: etapas e rolagem da peneira (por nível), parser do `peneira.yml`, matemática da fundição (combustível, progresso, 4→1, extra de 25% com semente), máquina de estados da pesquisa (entrega parcial, fila, vagas, conclusão por relógio injetado, offline), montagem das entradas do dialog de pesquisa e do cadeado nas receitas.
- `ReceitasScreenTest`-like para `pesquisa.json` no `plugins/textura` (compila a tela real).
- Checklist manual novo no plano: carregar/peneirar, fundir, travar fornalha vanilla com pedaço, pesquisar a Rede do zero, fila com 2 vagas, concluir offline, restart no meio de uma pesquisa.

## 10. Para o construtor decidir
- Partículas, sons e textos exatos; nomes das categorias no enum.
- Forma do mapa de contadores no `PlacedBlock` e migração (campo ausente = vazio).
- Como o timer de 1 s encontra os rafts com pesquisa rodando (índice em memória).
- Layout final do `pesquisa.json` dentro do tema `stone`.
