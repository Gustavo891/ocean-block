# Tipo de ilha — design

Data: 2026-10-28. Pedido do usuário: "Quero fazer uma tela dessa quando o jogador entrar no servidor, porém vai ser pra escolher o tipo de ilha: Default (atual) / Deserto - tema de deserto / Gelo - tema de gelo / Raft - apenas um raft de madeira (Hardcore)." Referência visual: o seletor de modos do servidor Hoplite (lista vertical de cards largos, arte à esquerda, painel escuro com título, descrição, status verde no canto e "Clique para jogar"; botão de sair embaixo; cenário desfocado no fundo). Só referência: nenhuma arte do Hoplite é usada.

## Intenção

O jogador novo escolhe com que cara começa: a ilha de sempre, uma versão de deserto, uma de gelo ou só uma jangada de madeira. É uma escolha de estilo, de uma vez só; a progressão é a mesma para todos.

## Decisões do usuário

- Quatro tipos: **Padrão** (a ilha atual, `templates/island.schem`), **Deserto**, **Gelo** e **Jangada** (o "Raft").
- **Jangada "hardcore" é só a ilha sem recursos**: uma jangada de madeira simples. Não muda morte, inventário, drops nem recompensas.
- **Deserto e Gelo mudam só o visual e o bioma do lote.** Detritos (`drift.yml`), gerador de pedra, XP, missões e máquinas são iguais aos do Padrão.
- **No Gelo a água não congela.**
- **A escolha é permanente.** Só um admin muda, com `/ilha resetplayer <nick>`, que **apaga a ilha do jogador e reabre a escolha** (na hora se ele estiver online, senão no próximo login).
- Os schematics de Deserto, Gelo e Jangada são **gerados por código** a partir da ilha atual (troca de blocos) e da jangada.
- Enquanto escolhe, o jogador fica como **câmera parada sobre o mar** (espectador); o dialog desfoca o fundo, como no print.
- **Fechar a tela reabre** a tela; o botão do rodapé é **"Sair do servidor"** e desconecta.
- Layout: **4 cards em lista**, como no print.
- Clicar em escolher abre uma **tela de confirmação** antes de criar.
- O canto verde do card mostra o **número de ilhas daquele tipo** no servidor.

## Escolhas minhas (o usuário pode trocar)

- Ids dos tipos: `padrao`, `deserto`, `gelo`, `jangada`. Nome na tela: PADRÃO, DESERTO, GELO, JANGADA (texto em pt-BR; "Raft" vira "Jangada").
- Descrições: Padrão "A ilha de sempre: grama, árvore e lava."; Deserto "Areia, arenito e cactos sob o sol."; Gelo "Neve, abetos e gelo que não derrete."; Jangada "Só tábuas no mar. Para quem quer desafio.".
- Contagem: `1 ilha`, `N ilhas`, `nenhuma ilha` para zero. Conta as ilhas carregadas em `IslandManager` por tipo.
- Membro convidado não escolhe nada: entra na ilha do dono, do tipo do dono. Quem já é membro de alguma ilha (`getHomeIsland` não nulo) nunca vê a tela.
- O jogador sem ilha e sem participação vê a tela no login e no `/ilha` (no lugar da criação automática).

## Tipos de ilha

### Modelo

`Island` ganha `var tipo: String` (id do tipo). `IslandRepository.migrateMissingFields` preenche `tipo = "padrao"` nas ilhas antigas (o codec ignora defaults). O enum puro `IslandType` (`PADRAO`, `DESERTO`, `GELO`, `JANGADA`) tem `id`, nome, descrição, bioma (`null` no Padrão e na Jangada, `DESERT` no Deserto, `SNOWY_PLAINS` no Gelo) e `fromId(id)` que devolve `PADRAO` para id desconhecido.

### Terreno

`StarterIsland.generate(island, onReady)` passa a olhar `island.tipo`:

1. Se existe `templates/<id>.schem` (por exemplo um `deserto.schem` que o usuário montou depois), cola esse arquivo como hoje.
2. Senão, Deserto e Gelo leem `island.schem`, trocam cada bloco do clipboard pela tabela pura `IslandPalette.swap(tipo, blockString)` e colam no mesmo ponto (`PASTE_Y` 64, centro do lote, `ignoreAirBlocks`).
3. Jangada não usa schematic: coloca 3×3 de `OAK_PLANKS` em Y 62 (a superfície do mar, no lugar da água) centrado no centro do lote, no main thread. O spawn continua o mesmo (`IslandPlacement.spawn` + 2); o jogador cai 3 blocos na jangada.

A troca mantém as propriedades do estado que existirem no bloco novo (eixo do tronco, `distance`/`persistent` das folhas, `facing`/`half`/`shape`/`waterlogged` da escada) e descarta as outras. Tabelas (paleta atual de `island.schem`):

| Bloco original | Deserto | Gelo |
|---|---|---|
| `grass_block` | `sand` | `snow_block` |
| `dirt` | `sandstone` | `dirt` |
| `moss_block` | `smooth_sandstone` | `snow_block` |
| `sand` | `sand` | `snow_block` |
| `gravel` | `gravel` | `gravel` |
| `cobblestone` | `sandstone` | `cobblestone` |
| `mossy_cobblestone` | `cut_sandstone` | `cobblestone` |
| `mossy_cobblestone_stairs` | `sandstone_stairs` | `cobblestone_stairs` |
| `short_grass`, `dandelion`, `poppy` | `dead_bush` | `snow` (1 camada) |
| `tall_grass` (metade de baixo) | `dead_bush` | `snow` (2 camadas) |
| `tall_grass` (metade de cima) | `air` | `air` |
| `oak_log` | `stripped_oak_log` (árvore morta) | `spruce_log` |
| `oak_leaves` | `air` | `spruce_leaves` |
| `cactus` | `cactus` | `air` |
| `lava`, `iron_trapdoor`, `air` e o resto | iguais | iguais |

A lava e a tampa de ferro ficam, então o gerador de pedra funciona nos dois temas como no Padrão. `air` na tabela vira ar mesmo colando com `ignoreAirBlocks` porque a troca acontece antes; o ar resultante simplesmente não é colado (o lote foi regenerado como mar, não há o que apagar).

### Bioma

Depois de colar, Deserto e Gelo pintam o bioma do tipo em toda a `IslandArea` (150×150, altura toda) e reenviam os chunks aos jogadores (`World.refreshChunk`). Padrão e Jangada não pintam: ficam com o bioma do gerador do mundo, restaurado pelo regen do `resetplayer`.

### Gelo sem congelar

Um listener cancela `BlockFormEvent` cujo estado novo é `ICE` dentro de um lote de ilha `gelo`. Neve caindo e camadas de neve continuam.

## A tela de escolha

### Quando abre

- **Login sem ilha**: `PlayerListener.onPlayerSpawnLocation` deixa de criar a ilha. Quem não tem ilha nem participação nasce no ponto da câmera: mundo das ilhas, (0.5, 80, 0.5), olhando para o horizonte (yaw 0, pitch 10); no join vira espectador e recebe a marca PDC `oceanblock:escolhendo_ilha`. A tela abre quando o pack carrega (`PlayerResourcePackStatusEvent` `SUCCESSFULLY_LOADED`) ou 40 ticks depois do join, o que vier primeiro, se ainda estiver escolhendo.
- **Saiu no meio da escolha**: no próximo login, se ainda não tem ilha, volta à câmera e à tela; se tem a marca mas já tem ilha (um admin criou, virou membro), volta a `SURVIVAL`, perde a marca e vai para a ilha. Admins em espectador sem a marca nunca são tocados.
- **`/ilha` sem ilha** (por exemplo um ex-membro): vai para a câmera e vê a mesma tela do login. O rodapé da tela é um só (`Sair do servidor`), então os dois fluxos se comportam igual.
- **`/ilha resetplayer <nick>`** (permissão `screen.dev`, com prévia e `-confirm` como os outros resets): apaga a ilha do dono (regen do mar da `IslandArea`, `IslandResetHooks.fireTerrainReset`, entidades removidas, membros expulsos como no `resetProgress`, ilha removida do `IslandManager` e do Mongo). Se o dono está online, vai para a câmera e a tela abre. Se o alvo não é dono de ilha, a mensagem diz isso e nada muda.

### Tela `configs/ui/ilha_tipo.json` (tema `abismo`)

```
ESCOLHA SUA ILHA
+------+------------------------------------+------------+
|[arte]| PADRÃO                    12 ILHAS | [ESCOLHER] |
| 32px | A ILHA DE SEMPRE: GRAMA...         |            |
+------+------------------------------------+------------+
| ... DESERTO, GELO, JANGADA ...                         |
+--------------------------------------------------------+
            [ Sair do servidor ]   (rodapé vanilla)
```

- Moldura `moldura`, `w` 340, sem título vanilla. Título `ESCOLHA SUA ILHA` no estilo `titulo`.
- Quatro cards fixos no JSON (um por tipo, sem `repeat`: um ícone por placeholder exigiria alias no tema `abismo`), surface `painel`: à esquerda o ícone `ui:ilha_tipo/<id>` de 32 px; no meio o nome (`heading`) com a contagem (`good`) à direita e a descrição (`muted`, uma linha) embaixo; à direita o botão `ESCOLHER` (look `acao`, bind `escolher_<id>`), no lugar do "Clique para jogar" do print. O botão é a área clicável; o card inteiro não é um botão porque botão do dialog tem altura fixa. Textos em maiúsculas (a fonte `pequena` só tem maiúsculas); binds `<id>_nome`, `<id>_descricao`, `<id>_contagem`.
- Corpo de 187 px: cabe na área visível do dialog também na escala de GUI 4 (1080p).
- Rodapé: `close` = `Sair do servidor`, `canCloseWithEscape: false`. Clicar desconecta o jogador com `Até a próxima!`.

### Confirmação `configs/ui/ilha_tipo_confirmar.json` (tema `abismo`)

Mesma moldura, `w` 260: o ícone do tipo (32 px, `states` pelo bind `tipo`), `CRIAR A ILHA <NOME>?`, a linha `Não dá para trocar depois.` (`bad`) e dois botões: `CONFIRMAR` (`acao`) e `VOLTAR` (`neutro`, reabre a lista). Mesmo rodapé e mesmo Esc da lista.

### Ao confirmar

`IslandManager.createIsland(player, tipo) { ready -> ... }` cria e gera a ilha do tipo; no `onReady` o jogador sai do espectador para `SURVIVAL` (só se tinha a marca), perde a marca e é teleportado para `spawn + (0, 2, 0)`, como o primeiro login de hoje. Se a criação falha (ilhas não carregadas, mundo ausente), a tela fecha, o jogador recebe a mensagem vermelha `Não foi possível criar sua ilha agora. Tente de novo em instantes.` e a tela reabre em 100 ticks. Clique duplo: quem já tem ilha ao confirmar só é levado a ela.

### Artes dos cards

Arte original: um render isométrico de cada ilha (gerado offline do próprio schematic de cada tipo, como o `ui:gerador_item`) sobre um fundo de mar, 160×160 px (4× de 40), em `resource/assets/oneblock/textures/ui/ilha_tipo/<id>.png`. A borda do ícone usa a cor do selo de cada tipo: Padrão verde-grama, Deserto areia, Gelo azul-gelo, Jangada madeira, seguindo o padrão aprovado nos toasts (corpo escuro, selo colorido).

### Se a tela não abre

`MissingScreen`/`Failed` (ou pack recusado) cai num baú vanilla simples de 3 linhas, título `Escolha sua ilha`, um item por tipo (grama, areia, neve, tábua) com nome, descrição e contagem na lore; clicar abre um segundo baú de confirmação (lã verde `Confirmar`, lã vermelha `Voltar`). Fechar o baú no fluxo de login reabre em 20 ticks.

## Textura

`DialogBinds` ganha `closed(Runnable)`: chamado quando o jogador clica no botão de fechar do rodapé (o payload `close`). Hoje esse clique só fecha a tela e o código de quem abriu não fica sabendo; a tela de escolha precisa disso para desconectar. Tipo Java, sem default, coberto pelo `PublicApiKotlinTypesTest`.

## Regras puras e testes (Kotest)

- `IslandType.fromId` (desconhecido vira Padrão) e os campos de cada tipo.
- `IslandPalette.swap`: cada linha da tabela, propriedades mantidas e descartadas, blocos fora da tabela intactos, Padrão e Jangada nunca trocam.
- `IslandChoice` (o fluxo): quem vê a tela (sem ilha própria e sem participação), texto da contagem (`nenhuma ilha`, `1 ilha`, `N ilhas`), linhas da lista (`tipos`) a partir das contagens.
- `ScreenListsPagedTest` já cobre o `perPage` das duas telas novas.

O lado Bukkit (paste, bioma, espectador, câmera, kick, `resetplayer`) fica no checklist manual do plano.

## Fora do escopo

- Progressão diferente por tema (detritos, gerador, missões).
- Trocar de tipo sem admin.
- Schematics feitos à mão (o código aceita `templates/<id>.schem` quando o usuário quiser).
- Mudar a ilha Padrão.

## Revisão de 2026-10-28 (depois do primeiro teste no jogo)

O usuário pediu: arte da ilha sem a água, tela mais no estilo do Hoplite, sidebar desligada e dia durante a escolha.

- Tema próprio `escolha` (cópia do `abismo` com as looks dos cards). Sem moldura: o logo do OceanBlock no topo e quatro cards largos.
- Cada card é um botão sem rótulo com arte de 36 px (degradê da cor do tipo para o corpo escuro translúcido), então o card inteiro é clicável; por cima ficam a ilha, o nome grande, a descrição, a contagem verde e `CLIQUE PARA ESCOLHER`, como o "Click to play" do print.
- Artes das ilhas sem água, fundo transparente, 144 px.
- Durante a escolha o jogador vê meio-dia e tempo limpo (tempo e clima por jogador) e não vê a sidebar; tudo volta ao sair da câmera.
