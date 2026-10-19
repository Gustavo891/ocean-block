# OceanBlock — Aceite manual (fork base + ilha)

Verificações que precisam de jogador no jogo; os agentes não conseguiram rodar. Servidor: `server/`, porta 25566. Suba com `java -jar paper.jar --nogui` direto (o `start.bat` reinicia em loop).

## 1. Preparação
- [ ] Conferir que `server/plugins/ocean-block/templates/island.schem` existe (vem versionado no repositório).

## 2. Mundo
- [ ] Voar ~2000 blocos em duas direções: só água, nenhuma terra.
- [ ] Mergulhar em (0, 0): fundo de areia/cascalho por volta de Y 41–44.
- [ ] Em X=5000, Z=5000: construir na superfície e cavar o fundo, selecionar 100×100 da altura mínima à máxima e `//regen`: água e fundo voltam.

## 3. Ilha
- [ ] Jogador novo entra (ou `/ilha`, ou o portal do spawn): a ilha do schematic aparece no centro do lote e o jogador é levado para cima dela só depois da colagem, no centro, com os pés em Y 64 (chão em Y 63), sem cair na água nem ficar preso em bloco.
- [ ] Morrer e renascer: volta em cima da ilha no centro, não dentro do tronco da árvore.
- [ ] Renomear `island.schem` temporariamente e criar ilha com outro jogador novo: 3×3 de grama sobre terra (grama em Y 63, terra em Y 62), jogador em pé na grama + log `[ilha]` severe. Voltar o nome.
- [ ] Quebrar o bloco central: dropa normalmente, não regenera, nenhum mob nasce.
- [ ] HUD mostra `Ilha • 30x30`; depois de um upgrade de borda mostra `Ilha • 50x50` na hora.
- [ ] Scoreboard com título `OCEAN BLOCK` e nenhum `%placeholder%` cru.
- [ ] `/ilha`, `/island` e `/is` abrem o comando; `/raft`, `/balsa`, `/is phases` e `/is setphase` não existem mais.
- [ ] Mensagens, menus, HUD e scoreboard dizem "ilha" com concordância certa ("sua ilha", "Sua ilha:").

## 4. Construção e membros
- [ ] Construir para fora a partir da borda da ilha com terra, pedregulho e tábua; fora da borda é bloqueado.
- [ ] `/ilha member <outro>`: o outro constrói; sem membro/trust, não.
- [ ] `/ilha sethome` fora da própria ilha é recusado; dentro funciona e sobrevive a reinício.

## 5. Reset
- [ ] Construir para fora, cavar o fundo, soltar um item, spawnar um mob, definir home em outro lugar; `/ilha reset terrain <dono> -confirm`: mar e fundo restaurados, ilha recolada, item e mob removidos, jogador volta em cima da ilha (pés em Y 64) e a home volta para o centro.
- [ ] Bloco custom colocado no lote antes do reset deixa de ser rastreado depois.

## 6. Geral
- [ ] Sair e entrar de novo: volta para a ilha.
- [ ] `/locate structure minecraft:shipwreck` não encontra nada.
- [ ] `/oceanblock reload`, `/shop`, `/sell`, `/tops` respondem.

## 7. Deriva e gancho
Pré-requisito: ilha criada e o servidor com o plugin novo. A config fica em `server/plugins/ocean-block/drift.yml`.

### Config e itens
- [ ] Na primeira subida, `server/plugins/ocean-block/drift.yml` foi criado com os 4 tipos (`tabua`, `folhas`, `sucata`, `barril`).
- [ ] Jogador novo (nunca entrou antes) recebe o gancho ao entrar; jogador antigo não recebe de novo.
- [ ] `/citem give gancho` entrega o gancho.
- [ ] A bancada não faz mais o gancho (`..P / .PS / P.S` com tábua e linha não dá resultado); ele só sai pela tela de receitas.

### Detritos
- [ ] Em pé na ilha, detritos aparecem a montante e passam boiando ao lado da ilha, rente à água (Y ~62,6), movendo suave, na direção da corrente (leste por padrão). Passam dos dois lados, entre `largura-minima` (8) e `largura` (13) blocos do centro, ou seja, 2 a 7 blocos fora da costa (a parte sólida da ilha vai até ~6 blocos do centro); nenhum vem pelo meio nem bate de frente na ilha.
- [ ] Em ~2 minutos já passam tábuas suficientes para crescer a ilha; nunca mais de 8 detritos ao mesmo tempo no lote.
- [ ] Detrito que passa do fim da trilha (40 blocos) some sem sobrar entidade.
- [ ] Detrito que encosta numa borda construída na trilha com 1 bloco de profundidade (tábua, laje, cerca na altura da água) afunda devagar, passa por baixo sem atravessar blocos, volta à superfície do outro lado e segue a corrente; submerso, solta bolhas em vez de respingo e balança menos.
- [ ] Parede sólida da superfície até o fundo (ou mais funda que 3 blocos): o detrito para encostado e, após ~20 s, afunda meio bloco e encolhe até sumir (~1 s), sem sobrar entidade.
- [ ] Detrito preso na parede pode ser pego por toque e puxado pelo gancho; submerso (mergulhando sob uma borda) também. Só o detrito sumindo não é coletado.
- [ ] Alga ou grama-marinha que chega à superfície NÃO faz o detrito mergulhar; tábua, laje e cerca fazem.
- [ ] Tirar a água de uma área na frente da trilha com balde ou esponja: o detrito entra no buraco e cai (acelerando, sem andar para os lados). Se cair na água de baixo, volta a boiar e sobe até a superfície dela. Se cair num bloco, fica no chão, parado e sem girar.
- [ ] Tirar a água logo embaixo de um detrito boiando faz ele cair na hora.
- [ ] Mergulho não passa por ar: com um bolsão de ar sob uma borda construída, o detrito não mergulha por ele (cai ou fica preso).
- [ ] Detrito no chão ou preso pode ser pego nadando ou andando até ele e com o gancho; se ninguém pegar, some em ~20 s.
- [ ] Devolver a água sobre um detrito no chão faz ele voltar a boiar.
- [ ] Detrito que cai num buraco muito fundo (mais de 64 blocos abaixo da superfície do mar ou fora do mundo) é removido sem erro no log.
- [ ] Detritos nunca forçam o carregamento de chunks: com a distância de renderização baixa (ex.: `view-distance=2`), podem nascer menos detritos.

### Coleta
- [ ] Lançar a linha do gancho perto de um detrito e recolher: o detrito desliza até o jogador em ~0,5 s, com som, respingo e action bar `+N item`.
- [ ] Nadar até um detrito (a menos de 1,5 bloco) coleta na hora; na beira da ilha também.
- [ ] Barril dá loot variado (pão, linha, alga, muda, bacalhau etc.) e nunca vem vazio (pior caso: 1 linha).
- [ ] Com inventário cheio, o que não cabe cai aos pés do jogador.
- [ ] Pesca vanilla com o gancho continua dando peixe.
- [ ] Vara de pesca comum não puxa detrito.
- [ ] Visitante sem membro/trust perto do detrito não coleta (nem por toque, nem com gancho) e não recebe mensagem; membro e trust coletam.
- [ ] Espectador (modo espectador) perto de um detrito: anotar o que acontece (coleta, ignora ou erro no log). O comportamento ainda não foi decidido.

### Rede de detritos
- [ ] A bancada não faz mais a rede (`PSP / SSS / PSP` com tábua e linha não dá resultado); ela só sai pela tela de receitas. `/cblock give rede` também entrega.
- [ ] Colocar a rede na borda da ilha: detritos que passam a até 3 blocos dela (distância 3D até o centro do bloco) deslizam até a rede em ~0,5 s e somem com respingo no bloco, sem action bar para ninguém.
- [ ] Detrito mais perto do jogador (a menos de 1,5 bloco) vai para o jogador, não para a rede; o gancho não puxa um detrito que já está indo para a rede.
- [ ] Clique direito na rede (dono, membro ou trust) abre `Rede de Detritos` com 27 slots; clicar num item passa ele para o inventário, `Coletar tudo` passa tudo; não dá para colocar itens nem arrastar; o tear vanilla nunca abre (nem agachado).
- [ ] Com inventário cheio, clicar num item avisa `Seu inventário está cheio.` e o item fica na rede.
- [ ] Duas pessoas com a tela aberta ao mesmo tempo: o item que uma tirou não sai de novo para a outra (o clique da segunda só atualiza a tela).
- [ ] Visitante sem membro/trust clica na rede: nada abre e nenhuma mensagem aparece.
- [ ] Rede cheia (os 27 slots ocupados, mesmo com pilhas incompletas): os detritos passam direto por ela e seguem a corrente. Encher a rede durante um puxão faz o detrito sumir sem entregar e sem dropar nada no chão; o que não cabe de uma entrega é descartado.
- [ ] Quebrar a rede devolve o item com os itens guardados (`Itens guardados: N` na lore); recolocar mantém os itens. Quebrar durante um puxão faz o detrito sumir sem entregar.
- [ ] Limite de 4 redes por ilha: a 5ª recebe `Limite de 4 por ilha atingido.`
- [ ] Com duas redes no alcance, o detrito vai para a mais próxima que não está cheia.
- [ ] Rede em chunk não carregado não puxa nada e nada força o carregamento do chunk dela.

### Limpeza
- [ ] Sair do lote (ou desconectar) apaga os detritos do lote, sem ficar nenhum flutuando.
- [ ] `/ilha reset terrain <dono> -confirm` não deixa detrito fantasma (somem no tick seguinte).
- [ ] Reiniciar o servidor com detritos no mundo: depois de subir não sobra nenhum `ItemDisplay` de detrito.
- [ ] Sem erros `[deriva]` no log durante toda a sessão.

## 8. Receitas

### Preparação
- [ ] Depois de instalar o jar com a tela, `/textura gerar` termina sem aviso `[ui:receitas]` no console e o pack novo chega aos jogadores (o hash muda).
- [ ] Antes do `/textura gerar`, o card mostra o slot vazio (sem o ícone do item); depois dele o slot mostra a vara de pesca (Gancho) e o tear (Rede).

### Abrir
- [ ] `/receitas`, `/craft` e `/receita` abrem a mesma tela, com título `Receitas`, tema de pedra e o rodapé `Os itens vão para o seu inventário.`
- [ ] Clicar no NPC "RECEITAS" do spawn abre a tela.
- [ ] Sem a textura aceita, aparece o aviso `Textura necessária` do Textura em vez da tela (o chat também mostra `A tela de receitas não está disponível.`, porque o `DialogApi.open` devolve false nesse caso).
- [ ] Renomear `configs/ui/receitas.json`, `/textura reload` e rodar `/receitas`: chat `A tela de receitas não está disponível.` e um aviso `[receitas]` no console. Voltar o nome e `/textura reload`.

### Categorias e chips
- [ ] A coluna da esquerda lista `Ferramentas` (picareta) e `Coleta` (baú); a aberta fica destacada e o tooltip diz `✔ Aberta agora!`, a outra `Clique para ver!`.
- [ ] Clicar em `Coleta` troca o cabeçalho para `Coleta` com o ícone do baú e mostra só a Rede; voltar para `Ferramentas` mostra só o Gancho.
- [ ] Os chips do topo mostram quanto o jogador tem dos ingredientes da categoria aberta (ex.: `12 Tábuas`, `1 Linha`, singular com 1); pegar ou largar linha e reabrir atualiza o número.
- [ ] Fechar a tela em `Coleta` e abrir de novo: continua em `Coleta`. Sair do servidor e entrar de novo: volta para `Ferramentas`.

### Cards
- [ ] Cada card mostra o item no slot (tooltip com nome e descrição), o nome, a linha de custo (`3 Tábuas ∙ 2 Linhas` no Gancho, `4 Tábuas ∙ 5 Linhas` na Rede) e o botão `Craftar` com o carrinho.
- [ ] Sem material, o botão `Craftar` aparece desabilitado, não reage ao clique e o tooltip termina com `⌀ Falta: ...` listando só o que falta (ex.: `⌀ Falta: 1 Tábua ∙ 2 Linhas`).
- [ ] Com material, o tooltip termina com `✔ Pronto! Clique para craftar.`
- [ ] Com 6 receitas ou menos numa categoria, a linha de páginas (`«  1/1  »`) não aparece.

### Craftar
- [ ] Com 3 tábuas e 2 linhas, `Craftar` no Gancho: consome exatamente isso, entrega um gancho que funciona (puxa detrito), mostra `Você craftou 1x Gancho.` e a tela se redesenha com os chips e os botões atualizados.
- [ ] Com 4 tábuas e 5 linhas, `Craftar` na Rede: entrega a rede, que pode ser colocada e coleta detritos.
- [ ] Tábuas divididas em pilhas de madeiras diferentes (ex.: 1 de carvalho + 2 de abeto, em slots separados) contam juntas e são consumidas certo; o que sobra fica no inventário.
- [ ] Inventário cheio (sem slot livre): o craft consome os ingredientes e o item cai aos pés do jogador.
- [ ] Craftar duas redes avança o desafio `CRAFT_REDE` do battle pass.
- [ ] Largar os ingredientes com a tela aberta e clicar `Craftar` (botão ainda ativo): nada é consumido, aparece `⌀ Falta: ...` em vermelho e a tela se redesenha com o botão desabilitado.

### Bancada
- [ ] A bancada de trabalho não faz mais o gancho nem a rede com tábua e linha em nenhum formato.
- [ ] Sem erros `[receitas]` ou `[ui:receitas]` no log durante toda a sessão.
