# OceanBlock — Aceite manual (fork base + raft)

Verificações que precisam de jogador no jogo; os agentes não conseguiram rodar. Servidor: `server/`, porta 25566. Suba com `java -jar paper.jar --nogui` direto (o `start.bat` reinicia em loop).

## 1. Preparação
- [ ] Salvar o schematic do raft: em qualquer ponto do oceano, 3×3 de `OAK_PLANKS` em Y 62 e um baú em cima da tábua do canto (+1, 63, +1). `//pos1` (-1, 62, -1) e `//pos2` (+1, 63, +1) relativos ao centro; em pé em cima da tábua central (pés em Y 63), `//copy` e `//schem save raft`. Copiar `plugins/FastAsyncWorldEdit/schematics/raft.schem` para `server/plugins/ocean-block/templates/raft.schem`.

## 2. Mundo
- [ ] Voar ~2000 blocos em duas direções: só água, nenhuma terra.
- [ ] Mergulhar em (0, 0): fundo de areia/cascalho por volta de Y 41–44.
- [ ] Em X=5000, Z=5000: construir na superfície e cavar o fundo, selecionar 100×100 da altura mínima à máxima e `//regen`: água e fundo voltam.

## 3. Raft
- [ ] Jogador novo entra (ou `/raft`): aparece em cima do raft, rente à água, com baú (ou o 3×3 de fallback sem schematic).
- [ ] Renomear `raft.schem` temporariamente e criar raft com outro jogador novo: 3×3 de tábuas + log `[raft]` severe. Voltar o nome.
- [ ] Quebrar a tábua central: dropa `OAK_PLANKS`, não regenera, nenhum mob nasce.
- [ ] HUD mostra `Raft • 30x30`; depois de um upgrade de borda mostra `Raft • 50x50` na hora.
- [ ] Scoreboard com título `OCEAN BLOCK` e nenhum `%placeholder%` cru.
- [ ] `/raft`, `/balsa`, `/ilha`, `/island` e `/is` abrem o comando; `/is phases` e `/is setphase` não existem mais.
- [ ] Mensagens e menus dizem "raft" com concordância certa ("seu raft").

## 4. Construção e membros
- [ ] Construir para fora a partir da borda do raft com terra, pedregulho e tábua; fora da borda é bloqueado.
- [ ] `/raft member <outro>`: o outro constrói; sem membro/trust, não.
- [ ] `/raft sethome` fora do próprio raft é recusado; dentro funciona e sobrevive a reinício.

## 5. Reset
- [ ] Construir para fora, cavar o fundo, soltar um item, spawnar um mob, definir home em outro lugar; `/raft reset terrain <dono> -confirm`: mar e fundo restaurados, raft recolado, item e mob removidos, jogador volta em cima do raft e a home volta para o centro.
- [ ] Bloco custom colocado no lote antes do reset deixa de ser rastreado depois.

## 6. Geral
- [ ] Sair e entrar de novo: volta para o raft.
- [ ] `/locate structure minecraft:shipwreck` não encontra nada.
- [ ] `/oceanblock reload`, `/shop`, `/sell`, `/tops` respondem.

## 7. Deriva e gancho
Pré-requisito: raft criado e o servidor com o plugin novo. A config fica em `server/plugins/ocean-block/drift.yml`.

### Config e itens
- [ ] Na primeira subida, `server/plugins/ocean-block/drift.yml` foi criado com os 4 tipos (`tabua`, `folhas`, `sucata`, `barril`).
- [ ] Jogador novo (nunca entrou antes) recebe o gancho ao entrar; jogador antigo não recebe de novo.
- [ ] `/citem give gancho` entrega o gancho.
- [ ] A receita do gancho funciona na bancada (`..P / .PS / P.S`, P = qualquer tábua, S = linha) e o resultado é o gancho custom, não uma vara comum.

### Detritos
- [ ] Em pé no raft, detritos aparecem a montante e passam boiando perto do raft, rente à água (Y ~62,6), movendo suave, na direção da corrente (leste por padrão).
- [ ] Em ~2 minutos já passam tábuas suficientes para crescer o raft; nunca mais de 8 detritos ao mesmo tempo no lote.
- [ ] Detrito que passa do fim da trilha (40 blocos) some sem sobrar entidade.
- [ ] Detrito que encosta na borda do raft com 1 bloco de profundidade (tábua, laje, cerca na altura do raft) afunda devagar, passa por baixo sem atravessar blocos, volta à superfície do outro lado e segue a corrente; submerso, solta bolhas em vez de respingo e balança menos.
- [ ] Parede sólida da superfície até o fundo (ou mais funda que 3 blocos): o detrito para encostado e, após ~20 s, afunda meio bloco e encolhe até sumir (~1 s), sem sobrar entidade.
- [ ] Detrito preso na parede pode ser pego por toque e puxado pelo gancho; submerso (mergulhando sob o raft) também. Só o detrito sumindo não é coletado.
- [ ] Alga ou grama-marinha que chega à superfície NÃO faz o detrito mergulhar; tábua, laje e cerca fazem.
- [ ] Tirar a água de uma área na frente da trilha com balde ou esponja: o detrito entra no buraco e cai (acelerando, sem andar para os lados). Se cair na água de baixo, volta a boiar e sobe até a superfície dela. Se cair num bloco, fica no chão, parado e sem girar.
- [ ] Tirar a água logo embaixo de um detrito boiando faz ele cair na hora.
- [ ] Mergulho não passa por ar: com um bolsão de ar sob a borda do raft, o detrito não mergulha por ele (cai ou fica preso).
- [ ] Detrito no chão ou preso pode ser pego nadando ou andando até ele e com o gancho; se ninguém pegar, some em ~20 s.
- [ ] Devolver a água sobre um detrito no chão faz ele voltar a boiar.
- [ ] Detrito que cai num buraco muito fundo (mais de 64 blocos abaixo do raft ou fora do mundo) é removido sem erro no log.
- [ ] Detritos nunca forçam o carregamento de chunks: com a distância de renderização baixa (ex.: `view-distance=2`), podem nascer menos detritos.

### Coleta
- [ ] Lançar a linha do gancho perto de um detrito e recolher: o detrito desliza até o jogador em ~0,5 s, com som, respingo e action bar `+N item`.
- [ ] Nadar até um detrito (a menos de 1,5 bloco) coleta na hora; na borda do raft também.
- [ ] Barril dá loot variado (pão, linha, alga, muda, bacalhau etc.) e nunca vem vazio (pior caso: 1 linha).
- [ ] Com inventário cheio, o que não cabe cai aos pés do jogador.
- [ ] Pesca vanilla com o gancho continua dando peixe.
- [ ] Vara de pesca comum não puxa detrito.
- [ ] Visitante sem membro/trust perto do detrito não coleta (nem por toque, nem com gancho) e não recebe mensagem; membro e trust coletam.
- [ ] Espectador (modo espectador) perto de um detrito: anotar o que acontece (coleta, ignora ou erro no log). O comportamento ainda não foi decidido.

### Limpeza
- [ ] Sair do lote (ou desconectar) apaga os detritos do lote, sem ficar nenhum flutuando.
- [ ] `/raft reset terrain <dono> -confirm` não deixa detrito fantasma (somem no tick seguinte).
- [ ] Reiniciar o servidor com detritos no mundo: depois de subir não sobra nenhum `ItemDisplay` de detrito.
- [ ] Sem erros `[deriva]` no log durante toda a sessão.
