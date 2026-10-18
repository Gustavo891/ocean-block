# Avanço de fase — re-implementação

Data: 2026-07-27
Status: implementado

## Estado atual

`Island.advancePhase()` troca o `currentPhaseId`, zera o `grid` e manda um
`"<green>Nova fase desbloqueada!"` hardcoded para os membros online. Nada além
disso.

Dois arquivos escritos para esse fluxo nunca são chamados de lugar nenhum:

- `PhaseAnimation` (126 linhas — title, som, bossbar, fogos, partículas).
- `PhaseRewarder` (54 linhas — recompensas Message, Item e Command).

Ou seja, os blocos `completion.rewards` dos 14 YAMLs de fase nunca foram entregues
a ninguém, e nenhum efeito visual jamais disparou. O toast de fase, integrado em
`PhaseAnimation`, está pendurado nesse mesmo caminho morto.

Defeitos adicionais no `advancePhase` atual:

- `grid = 0` executa duas vezes, nas linhas 99 e 111.
- A segunda execução roda mesmo quando não há próxima fase, então na última fase o
  contador zera em loop para sempre e nada acontece.
- `MiniMessage.miniMessage()` em vez de `Mini.mm`, tanto em `Island` quanto em
  `PhaseRewarder` — tags do Textura saem como texto literal.

## Decisões

- Recompensa é do **dono da ilha**, não de cada membro.
- Dono offline: a recompensa fica pendente e é entregue no login dele.
- Chat, toast, título, som e animação vão para **todos os membros online**.
- Sem anúncio global: o resto do servidor não fica sabendo.
- O texto do chat é configurado **por fase**, no YAML da própria fase.
- A animação congela o bloco por ~2s (opção escolhida sobre `BlockDisplay` e sobre
  apenas melhorar os fogos existentes).

## Arquitetura

Orquestração num `object PhaseAdvancement` novo, em `phase/`. `Island.advancePhase()`
vira uma linha delegando. `PhaseAnimation` e `PhaseRewarder` ressuscitam como peças
chamadas por ele.

A alternativa de empilhar tudo dentro de `Island` foi descartada: é um `data class`
que já carrega blacklist, membros, upgrades e `processBlockBreak`, e nada ali fica
testável isolado. A alternativa de evento Bukkit com listeners também foi
descartada: a sequência tem ordem obrigatória — congelar antes de animar,
descongelar depois — e ordem entre listeners é frágil de garantir.

### Congelamento

Um set de UUIDs de ilha em memória, dentro do `PhaseAdvancement`.
`processBlockBreak` consulta e sai cedo enquanto a ilha estiver no set. Não é
persistido de propósito: um restart no meio da animação destrava, que é o
comportamento correto.

### Fluxo de `advance(island)`

1. Resolve a próxima fase por `priority`. Se não existe, satura o `grid` em
   `blocksToNextPhase`, avisa os membros online de que a ilha está completa e
   retorna. Esse texto é fixo no código, não vem de YAML: é evento único do jogo
   inteiro, não de uma fase específica.
2. Congela a ilha.
3. Resolve o destinatário: dono online recebe na hora; dono offline, o id da fase
   entra na fila de pendentes.
4. `PhaseAnimation` roda a coreografia de ~2s no bloco.
5. Membros online recebem chat, título, som e toast.
6. Fim da janela: `currentPhaseId` vira a próxima, `grid` zera uma vez só, a ilha
   descongela e o bloco assume o material novo.

A troca de fase acontece no passo 6, não no 1. Durante os 2 segundos o bloco está
congelado, então ninguém minera contra o estado antigo. O preço é HUD e placeholder
exibirem a fase anterior nesses 2s.

### Última fase

A correção sai da saturação em vez do zeramento. `processBlockBreak` só incrementa
enquanto `grid < blocksToNextPhase`, então o limiar é cruzado exatamente uma vez e o
aviso de ilha completa sai uma vez. Sem campo novo e sem flag de "já avisei".

### Recuperação de falha

Como a fase só troca no passo 6, um crash antes disso deixa o `grid` saturado no
alvo com a fase antiga ainda ativa. A saturação sozinha travaria a ilha para sempre,
porque o limiar nunca mais seria cruzado — então `processBlockBreak` re-dispara o
avanço sempre que o grid está completo **e ainda existe próxima fase**, não apenas
no instante em que cruza.

Isso cobre os dois casos com uma regra só: na última fase não existe próxima, então
o aviso de ilha completa continua saindo uma vez; num avanço interrompido existe, e
a próxima quebra retoma do zero. O bloco de vidro deixado pela animação é
sobrescrito no `reveal`.

## Dados

### YAML da fase

Bloco opcional dentro de `completion`:

```yaml
completion:
  announce:
    - "<gold>A ilha concluiu a fase <yellow>Profundezas Rasas<gold>!"
  rewards:
    - type: Command
      value: "coins add %player% 2500"
```

O `announce` descreve a conclusão da fase em que está declarado, não a chegada na
seguinte. Fase sem `announce` não manda chat — e por isso os 14 YAMLs existentes
recebem a linha padrão acima na implementação, senão o chat sumiria em vez de
melhorar.

### Island

Um campo novo: `pendingRewardPhases: MutableList<String>`, com os ids das fases cuja
recompensa não foi entregue.

Guardar o id em vez do `Reward` inteiro mantém o documento pequeno e deixa a config
como fonte da verdade. O efeito colateral é que editar o YAML depois muda o que o
dono vai receber — comportamento desejado, não acidente.

No login do dono, o listener de jogador existente drena a fila, entrega e informa de
qual fase veio cada item.

## Erros

- Fase sem `announce`: não manda chat, sem log.
- Fase na fila de pendentes que sumiu do YAML: descartada com log, não estoura.
- Dono que nunca mais loga: a fila fica parada no documento indefinidamente.
  Aceitável — são strings curtas.
- Inventário cheio na entrega: o excedente é dropado aos pés, como `PhaseRewarder`
  já faz hoje.

## Regras de contagem

As três regras do grid ficam num object puro, `PhaseProgress` (`isComplete`,
`increment`, `crossedThreshold`), consumido por `processBlockBreak`. É o que mantém
a decisão de avançar legível, em vez de espalhada em comparações soltas dentro do
`Island`.

Verificação é manual, em jogo.

## Verificação manual

1. `./gradlew :plugins:oneblock:plugin:localDeploy` com o servidor parado.
2. Quebrar blocos até virar a fase; conferir congelamento, animação, toast, chat,
   título e som.
3. Deslogar o dono, avançar a fase com um membro, relogar o dono e conferir a
   entrega pendente.
4. Chegar na última fase e confirmar que o aviso sai uma vez só e o grid não zera.
