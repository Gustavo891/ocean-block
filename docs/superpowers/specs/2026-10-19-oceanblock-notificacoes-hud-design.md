# OceanBlock — Notificações na bossbar ancoradas por shader

Data: 2026-10-19
Pedido do usuário: tirar o HUD "Ilha • 30x30" e criar um sistema de notificação na bossbar, no estilo do Raft (lista à direita quando coleta recurso), que sirva para várias coisas e aceite várias ao mesmo tempo em lugares diferentes. Ex.: coleta de item à direita abaixo da scoreboard e, ao mesmo tempo, no topo esquerdo "sua rede de detritos ficou cheia". Posição pela tela do jogador (GUI scale, resolução) usando shader.

Base validada: spike `8b16b80` (`/textura hudtest`) — o shader `core/text.vsh` move texto marcado por cor para os cantos reais em qualquer escala; textos sem marca ficam iguais.

## 1. Decisões

| Tema | Decisão |
|---|---|
| HUD da ilha | Removido (`IslandHud` e o uso em `SpawnHudListener`). |
| Onde vive | Motor no Textura (`one_block.textura.notify`, API pública sem tipos do Kotlin); uso no ocean-block. |
| Desenho | **Uma bossbar por jogador** (`textura:notificacoes`) que compõe todas as regiões; sempre a primeira barra do jogador (criada antes das outras; se outra barra aparecer antes, a de notificações é reexibida para voltar ao topo). |
| Regiões | Configuráveis em `server/plugins/Textura/configs/hud/notificacoes.yml`: id, âncora (`TOPO_ESQUERDA`, `TOPO_DIREITA`, `MEIO_ESQUERDA`, `MEIO_DIREITA`, `BASE_ESQUERDA`, `BASE_DIREITA`), deslocamento `x`/`y` em px de GUI, máximo de linhas, espaçamento, se tem fundo, direção (`baixo` ou `cima`). Padrões: `coleta` = `MEIO_DIREITA`, y +70 (abaixo da scoreboard), 6 linhas, para baixo, com fundo; `avisos` = `TOPO_ESQUERDA`, x +4, y +4, 3 linhas, para baixo, com fundo. |
| Linha | Texto MiniMessage (com `<lang:...>`), ícone opcional (`item:<material>` ou `glyph:<id>`), duração em ticks, chave opcional. Push com a mesma chave na mesma região **substitui** a linha e reinicia o tempo (o chamador acumula, ex.: `+3 Tábua`). |
| Excesso | Região cheia descarta a linha mais antiga. |
| Fundo | Placa escura semitransparente por linha (glyphs de fundo do HUD), na largura do conteúdo, alinhada ao lado da âncora (direita alinha pela direita). |
| Ícones de item | Novo gerador no `/textura gerar`: assa a textura vanilla do item (mesmo fallback item → bloco da `VanillaTextures`) como glyph na fonte do HUD de notificação; materiais registrados por código (`NotificationApi.registerItemIcons`) antes do build. |
| Shader | Evolução do spike: marca `R=254`, âncora no `G`, paleta no `B` (nibble alto) e **índice de linha no nibble baixo do `B`** para empilhar para cima nas âncoras de base (o shader sobe `linha × altura`); sombra com `shadow_color` explícita `R=253`. Detecção de sombra por 1/4 continua desligada. |
| Atualização | A barra só é reenviada quando o conteúdo muda; um task de 5 ticks expira linhas vencidas. Sem jogador com notificações, a barra é escondida. |
| Compatibilidade | `/textura hudtest` continua (debug). Outras barras (battle-pass) continuam funcionando. Sem o pack/shader, as notificações aparecem empilhadas no topo (mesma degradação do spike). |

## 2. Uso no ocean-block
- **Coleta (deriva/gancho/toque):** troca o action bar `+N item` por notificação na região `coleta`: ícone do item + `<green>+N</green> <white><lang:...></white>`, chave `coleta:<material>`, 60 ticks; o `drift` acumula a quantidade enquanto a linha da mesma chave estiver viva.
- **Rede cheia:** quando uma Rede de Detritos fica cheia (transição para cheia, usando o `notifiedFull` do `PlacedBlock` para não repetir até esvaziar), aviso na região `avisos` para o dono e membros online da ilha: ícone `item:loom` + `<yellow>Sua Rede de Detritos ficou cheia!</yellow>`, 100 ticks.
- `Channel.Drift` deixa de ser usado; o `lib:actionbar` continua para outros usos.

## 3. Testes
- Kotest no Textura: composição de regiões (ordem, limite, substituição por chave, expiração), cálculo de layout (âncora, deslocamentos, linha para cima/baixo, largura com e sem fundo, alinhamento à direita), codificação da cor marcadora (âncora, paleta, linha) e parser do `notificacoes.yml` (padrões, avisos de valor inválido). Guarda de API sem tipos do Kotlin cobre o novo pacote.
- Kotest no ocean-block: acumulação de quantidade por chave na coleta (puro).
- Aceite manual: duas regiões ao mesmo tempo, empilhamento, substituição `+1 → +2`, expiração, várias escalas de GUI, rede cheia avisa uma vez.

## 4. Fora de escopo
- Animação de entrada/saída; sons; notificações para outros sistemas (o motor fica pronto para eles).
