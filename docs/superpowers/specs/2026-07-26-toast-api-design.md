# Toast API — design

Data: 2026-07-26
Status: implementado

## Problema

Não existe forma de exibir um popup de conquista no one-block. A conclusão de fase
hoje só se anuncia por title, bossbar, som e fogos (`PhaseAnimation.play`), o que se
perde no meio de outros efeitos e não deixa nada visualmente marcante.

Queremos um toast no estilo do advancement vanilla: ícone do bloco da fase, texto
superior fixo por tipo ("Fase concluída") e texto inferior variável (nome da fase
concluída).

## Escopo

Entra:

- Módulo `lib:toast` com `ToastApi`, consumível por qualquer system.
- Um tipo, `PHASE`, mapeado para a moldura `task` do advancement vanilla.
- Override do texto superior via arquivo de lang no resource pack do Textura.
- Integração em `PhaseAnimation`.
- `/oneblock toast <tipo> <material> <texto>`, para conferir o visual sem precisar
  concluir uma fase de verdade.

Não entra:

- Persistência ou histórico de toasts.
- Aba de Conquistas (tecla L) — o toast é invisível para ela por construção.
- Tipos 2 e 3. Nascem quando a feature que os usa existir.

## Mecanismo

Toast 100% por packet, via PacketEvents. Nada é registrado no servidor, nada é
gravado em `advancements/<uuid>.json`, e só o jogador alvo recebe.

Fluxo por envio:

1. Gera um id único, `oneblock:toast/<contador>`.
2. Monta `AdvancementDisplay` com `showToast = true` e `hidden = true`, ícone
   convertido do `ItemStack` do Bukkit, e a moldura do tipo.
3. Monta `Advancement` com um critério único (`toast`) e `sendsTelemetryData = false`.
4. Envia `WrapperPlayServerUpdateAdvancements` adicionando o advancement já com o
   critério marcado como concluído — é isso que faz o cliente exibir o toast.
5. Cinco ticks depois, envia um segundo packet com o id no conjunto de removidos,
   limpando o advancement fake do cliente.

### Alternativas descartadas

- `Bukkit.getUnsafe().loadAdvancement()` — ainda existe no paper-api 1.21.11, mas
  registra o advancement no servidor de verdade: aparece na aba de Conquistas,
  persiste em disco por jogador e exige reload para sincronizar.
- ProtocolLib — já instalado no servidor, mas nenhum código do repo o usa, e montar
  `ClientboundUpdateAdvancementsPacket` pela API de reflexão dele é bem pior do que
  pelo wrapper tipado do PacketEvents.
- Toast desenhado com glyphs/HUD — controle total do visual, complexidade
  desproporcional ao ganho.

## Texto superior

O toast vanilla não aceita cabeçalho livre. Ele vem de três chaves de tradução
fixas, uma por moldura: `advancements.toast.task`, `.goal` e `.challenge`.

A escolha foi sobrescrever a chave no resource pack e manter o título do
advancement em uma linha só. O `ResourcePackBuilder` do Textura copia tudo que está
em `resource/` verbatim para dentro do pack, então isso é arquivo de configuração,
não código:

```
server/plugins/Textura/resource/assets/minecraft/lang/pt_br.json
server/plugins/Textura/resource/assets/minecraft/lang/en_us.json
```

Os dois arquivos já existiam, com as três chaves de toast preenchidas. Só a de
`task` muda, para `"Fase concluída"`; `goal` e `challenge` ficam como estão,
reservadas para os dois tipos futuros. Um arquivo de lang no pack só cobre o idioma
que o cliente está usando, daí os dois.

Consequência aceita: no máximo três cabeçalhos distintos, um por moldura, para
sempre.

### Alternativa descartada

Título de duas linhas (`header` + `\n` + `mensagem`), que faria o cliente exibir as
duas linhas do título no lugar do cabeçalho nativo. Permitiria cabeçalhos
ilimitados, mas o `AdvancementToast` vanilla mostra o texto nativo da moldura
durante os primeiros ~1.5s e só depois troca pelas linhas do título — todo toast
começaria com "Desafio concluído!" antes de virar o texto certo.

## Decisão pendente: colisão com advancements vanilla

`advancements.toast.task` é global. Com os advancements vanilla ligados — que é o
estado atual do servidor de dev, sem nada em `server.properties` e sem datapack
desabilitando — completar "Getting Wood" também vai exibir "Fase concluída" no topo.

Opções:

- Cancelar `PlayerAdvancementCriterionGrantEvent` num listener. Nenhum advancement
  vanilla completa, nenhum toast vanilla aparece. Efeito colateral: o livro de
  receitas para de desbloquear sozinho, porque desbloqueio de receita é advancement
  por baixo.
- Conviver com a colisão.

Fica fora desta implementação: é política de servidor, não da API de toast, e a
troca envolvida é do dono do servidor.

## Módulo e API

`lib/toast`, package `one_block.libs.toast`, convenção `paper-common`, shadado no
jar do one-block como qualquer outra lib.

```kotlin
object ToastApi {
    fun send(player: Player, type: ToastType, icon: Material, text: String)
    fun send(player: Player, type: ToastType, icon: ItemStack, text: Component)
    fun send(players: Collection<Player>, type: ToastType, icon: Material, text: String)
}

enum class ToastType { PHASE }
```

Decisões de superfície:

- O overload de `Collection` existe porque conclusão de fase dispara para a ilha
  inteira, não para um jogador.
- O overload de `String` passa por `Mini.mm`, então `<glyph:>` e `<shift:>`
  funcionam no texto do toast, como no resto do repo.
- `ToastType` não expõe `AdvancementType`. O mapeamento tipo → moldura é interno;
  expor obrigaria todo módulo consumidor a ter PacketEvents no classpath só para
  ler o enum.

Dependências do módulo: `packetevents-spigot` 2.13.0 (`compileOnly`, do repositório
maven do CodeMC), `lib:formatter` (`Mini`), `lib:instance` (scheduler do delay de
limpeza). O PacketEvents entra como `softDepend` no `plugin.yml`, não `depend`: com
`depend`, um servidor sem o jar deixaria de carregar o one-block inteiro, enquanto o
no-op já cobre a ausência dele.

`ToastService` é `object` sem estado além de um contador atômico, então não entra na
lista de `onDisable()` do `OneBlockPlugin`.

## Integração

`PhaseAnimation.play()` já recebe `completed: Phase` e a lista de jogadores, e
`Phase` já carrega `iconMaterial`. A chamada entra direto:

```kotlin
ToastApi.send(players, ToastType.PHASE, completed.iconMaterial, completed.prettyName)
```

`system/islands` ganha `compileOnly(projects.lib.toast)`.

Melhoria pontual no mesmo arquivo: `PhaseAnimation` usa `MiniMessage.miniMessage()`,
o que o CLAUDE.md proíbe — tags do Textura sairiam como texto literal. Passa a usar
`Mini.mm`.

## Erros

- PacketEvents ausente ou não inicializado: `send` vira no-op, com um `warning` uma
  única vez, não por toast. Mesmo padrão do `HudApi`, que no-opa enquanto o manager
  é nulo.
- Jogador que deslogou entre o envio e a limpeza: ignorado.
- Nada aqui persiste nem tem estado a perder num desligamento.

## Verificação manual

1. Baixar o jar do PacketEvents 2.13.0 para `server/plugins/`.
2. `./gradlew :plugins:oneblock:plugin:localDeploy` com o servidor parado.
3. Subir o servidor, rodar `/textura gerar`, reentrar para pegar o pack novo.
4. `/oneblock toast <material> <texto>` e conferir ícone, cabeçalho e texto.
