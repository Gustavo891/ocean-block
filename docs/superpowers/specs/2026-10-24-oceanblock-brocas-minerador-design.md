# Brocas do Minerador

Data: 2026-10-24. Módulo: `plugins/oceanblock/system/machines` (pacote `miner/`).

## Intenção

O Minerador hoje quebra um bloco a cada 2 s, sempre igual. O usuário quer uma progressão de brocas: começa em ferro e sobe para ouro, diamante, obsidiana e netherite. A broca melhor deixa o Minerador mais rápido, e o jogador vê a broca que gira mudar de cor.

## Decisões do usuário

- Só a **velocidade** muda por nível. Blocos que dá para quebrar, drops (sem fortuna) e alcance (só o bloco de baixo) ficam como estão.
- 5 níveis, parando em netherite:

  | Nível | Tempo por bloco | E/s com 40 E por bloco |
  |---|---|---|
  | Ferro | 2 s | 20 |
  | Ouro | 1,5 s | ~27 |
  | Diamante | 1 s | 40 |
  | Obsidiana | 0,75 s | ~53 |
  | Netherite | 0,5 s | 80 |

- **Gasto por bloco igual (40 E)**: broca rápida puxa mais energia por segundo.
- A broca de **ferro vem embutida**: slot vazio = ferro, não existe item de ferro. Mineradores já colocados ficam em ferro.
- Cada outra broca é um **item encaixado** num slot do menu do Minerador. Não desgasta.
- A broca que gira troca de **modelo e cor** por nível (mesmo cone, cor do material). Arte própria; Create e Better 3D Blocks são só referência visual.
- Liberação **só por receita** em `/receitas`, categoria Máquinas, sem pesquisa.
- A Broca de netherite pede **lingote de netherite** mesmo sem fonte hoje (Nether desligado); a receita aparece e só fica possível quando o netherite entrar no jogo.

## Regras

- `DrillTier` (puro): `IRON`, `GOLD`, `DIAMOND`, `OBSIDIAN`, `NETHERITE`, com nível 0..4, nome em pt-BR, tempo por bloco em ticks (40, 30, 20, 15, 10), id do item (`broca_ouro`, `broca_diamante`, `broca_obsidiana`, `broca_netherite`; ferro não tem) e modelo do display (`oneblock:minerador_broca` para ferro, `oneblock:minerador_broca_<material>` para os outros).
- O nível fica no counter `minerador_broca` do `PlacedBlock`; ausente ou inválido = ferro. Como os counters vão no item ao quebrar, quebrar o Minerador guarda a broca encaixada (*escolha do builder*).
- Progresso em unidades: um bloco = 120. A cada ciclo de 10 ticks a broca soma `10 × 120 / ticksPorBloco` (ferro 30, ouro 40, diamante 60, obsidiana 80, netherite 120). Ao quebrar, o progresso perde 120 e guarda o resto, para a obsidiana fazer 2 blocos a cada 3 ciclos. Sem bloco para quebrar, o progresso para em `120 + ganho - 1`.
- Energia por ciclo = `ceil(40 × ganho / 120)`: 10, 14, 20, 27, 40 E. O Minerador ligado puxa isso todo ciclo, como hoje.
- As rachaduras usam o progresso em unidades e o ganho do ciclo para interpolar entre ciclos pagos.

## Item e menu

- `DrillItem` é um `CustomItem` por nível (4 itens), material `PAPER`, `itemModel` igual ao modelo do display, então o inventário mostra a broca 3D. Lore: tempo por bloco, gasto e "Encaixe no menu do Minerador".
- Menu do Minerador, linha 4: alavanca, **slot da broca**, coletar tudo, status. O slot mostra a broca atual (ferro com o ícone de ferro quando vazio) com tempo por bloco e E/s.
  - Clicar uma broca no próprio inventário encaixa e devolve a anterior (se não for ferro) ao inventário; sobra cai nos pés.
  - Clicar no slot com uma broca encaixada devolve a broca e volta para ferro; com inventário cheio não tira.
  - Cada clique re-checa acesso (`MachineMenus.requireAccess`).
- Os textos "20 E/s" e "a cada 2 s" no menu e na lore do Minerador passam a vir da broca.

## Visual

- `MinerDisplay` guarda o nível de cada display e troca o item do display da broca quando o nível muda (encaixe, retirada ou ciclo seguinte).
- Modelos `models/block/minerador_broca_<material>.json` com parent `oneblock:block/minerador_broca`, trocando só a textura `bit`; definições `items/minerador_broca_<material>.json`.
- Texturas 16×16 `textures/block/maquinas/minerador_broca_<material>.png`, desenhadas por script descartável a partir da paleta de cada material: ouro dourado, diamante ciano, obsidiana roxo escuro, netherite cinza-preto com friso dourado.

## Receitas (`MachineRecipes`, `research = null`)

| Broca | Ingredientes |
|---|---|
| Ouro | 2 blocos de ouro, 4 lingotes de ferro, 4 redstone |
| Diamante | Broca de ouro, 6 diamantes, 4 redstone |
| Obsidiana | Broca de diamante, 8 obsidianas, 2 diamantes |
| Netherite | Broca de obsidiana, 2 lingotes de netherite |

## Fora do escopo

Fortuna, alcance maior, filtro de blocos por nível, desgaste, pesquisa das brocas e níveis acima de netherite.

## Testes

Kotest em `DrillTier`/`MinerRules`/`MinerCrackRules`: nível a partir do counter, ganho e energia por nível, carry da obsidiana (2 blocos em 3 ciclos), teto sem bloco, ids e modelos. O lado Bukkit (menu, display, receitas) vai para a seção 12 do manual de aceite.
