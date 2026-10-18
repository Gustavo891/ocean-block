# Escolha de template da ilha — design

**Data:** 2026-08-06
**Módulo:** `plugins/oneblock/system/islands`
**Status:** aprovado, pronto para plano de implementação

## Objetivo

Hoje o jogador que entra sem ilha recebe uma automaticamente, em silêncio: `PlayerListener.onPlayerSpawnLocation` chama `IslandManager.createIsland`, que gera um bedrock com um dirt em cima (`generateStarterIsland`). O jogador não escolhe nada e a primeira impressão do servidor é um bloco de terra no vazio.

Substituir isso por uma escolha: quem entra sem ilha é levado a um showcase, vê cinco builds prontas, escolhe uma, e a ilha nasce com aquela build e o bioma do tema.

O template define **a plataforma inicial e o tema visual**. Não altera progressão: as fases de `phases/*.yml` continuam iguais para todos.

## Restrição que guiou o design

O fluxo roda no login, e vários jogadores entram ao mesmo tempo. Nada no caminho pode custar por jogador:

- As builds são **pré-construídas** num mundo dedicado, carregadas uma vez. Nenhum bloco é escrito e nenhum pacote de bloco em massa é enviado para mostrar um template.
- As câmeras são **cinco entidades compartilhadas** por todo o servidor, não uma por jogador.
- Trocar de template é trocar o `spectatorTarget`. Não gera teleporte nem alteração de mundo.
- O único trabalho pesado — o paste da schematic — acontece **uma vez por jogador, na confirmação**, assíncrono via FAWE.

## Fluxo do jogador

1. Jogador entra sem ilha. `PlayerSpawnLocationEvent` desvia o spawn para o mundo `templates`. Nenhuma ilha é criada.
2. Gamemode spectator, `spectatorTarget` apontado para a câmera do primeiro template. A câmera é uma entidade invisível, sem IA e sem gravidade, parada no enquadramento que o builder montou.
3. Cena limpa. `HudApi` no topo mostra nome e descrição do template atual; a instrução `SHIFT abrir menu` fica no rodapé, via `ActionBar`.
4. SHIFT abre `TemplateSelectView`: cinco ícones, um por template, nome e descrição na lore, com destaque no que ele está vendo no momento.
5. Clicar num ícone abre `TemplateDetailView`, com duas opções:
   - **Visualizar** — fecha o menu, troca o `spectatorTarget` para a câmera daquele template, volta para a cena limpa.
   - **Selecionar** — confirma e cria a ilha.
6. Fechar o menu com ESC volta para a cena limpa sem trocar nada. SHIFT reabre.
7. Ao selecionar: a sessão trava (uma segunda confirmação é no-op), a tela escurece, a ilha é criada no próximo grid livre, FAWE cola o `.schem` e aplica o bioma de forma assíncrona. Ao terminar, o jogador volta para survival, é teleportado para o spawn da ilha e a HUD é limpa.
8. Sair sem escolher não grava nada — sem ilha, sem documento, sem grid ocupado. No próximo login ele cai no showcase de novo.

### Por que spectator preso à câmera

Só existiam dois caminhos viáveis, e o servidor não consegue esconder a hotbar de um jogador — isso é desenho do cliente, e só um resource pack a esconde, o que seria global para o servidor inteiro.

Spectator esconde a hotbar como efeito colateral do gamemode, e prender a câmera numa entidade garante o enquadramento que o builder montou: o jogador não voa, não anda e não sai da cena. Em troca, perde-se item na mão, clique direito e tecla F — daí o menu ser aberto por SHIFT, a única tecla que ainda gera evento nesse estado (`PlayerToggleSneakEvent`).

O SHIFT normalmente solta o jogador da entidade. `PlayerStopSpectatingEntityEvent` (Paper) é cancelável e é cancelado enquanto a sessão estiver ativa, então ele fica preso.

O GUI só aparece quando o jogador pede. Isso importa porque, do 1.20.2 em diante, o cliente aplica blur e escurecimento em tela cheia atrás de qualquer inventário — um menu permanentemente aberto deixaria a build borrada o tempo todo, que é justamente o que o showcase existe para mostrar.

### Guardas durante a sessão

Enquanto a sessão de seleção existir para o jogador: cancela dano, perda de fome, drop de item, e qualquer teleporte que não venha do próprio fluxo. Comandos ficam restritos a uma whitelist.

## Arquitetura

Código novo em `plugins/oneblock/system/islands/src/main/kotlin/one_block/system/islands/template/`.

Fica dentro de `islands` de propósito: o fluxo termina criando uma ilha, e um módulo separado precisaria de `IslandManager`, invertendo a direção de dependência que o projeto mantém (sistemas dependem de `islands`, nunca o contrário).

| Arquivo | Responsabilidade |
|---|---|
| `IslandTemplate` | Modelo imutável de um template: `id`, `displayName`, `description`, `biome`, nome do arquivo `.schem`, `SimpleLocation` da câmera, e o material/`itemModel` do ícone. |
| `TemplateRegistry` | Carrega `templates.yml`. Mutável e recarregável, no mesmo contrato do `PhaseManager`: template inválido (bioma inexistente, `.schem` ausente) é logado e pulado, não derruba o carregamento. Expõe a lista **ordenada** — a ordem do YAML é a ordem do menu. |
| `TemplateCameras` | Cria ou reaproveita as cinco entidades-câmera no mundo `templates` no boot, e remove órfãs de execuções anteriores. Marca cada uma com uma `NamespacedKey` no PDC para reconhecê-las. |
| `SelectionSession` | Estado por jogador: template atual, flag de confirmação em andamento, gamemode e localização de origem para restaurar. |
| `TemplateSelectionService` | Operações da sessão: `enter`, `preview`, `confirm`, `exit`. Mantém o mapa `UUID -> SelectionSession`. Único ponto que muda `spectatorTarget` e gamemode. |
| `TemplateListener` | `PlayerSpawnLocationEvent`, `PlayerToggleSneakEvent`, `PlayerStopSpectatingEntityEvent`, `PlayerQuitEvent` e as guardas. Não contém lógica: delega ao service. |
| `TemplateSelectView` | Lista os cinco templates. |
| `TemplateDetailView` | Visualizar / Selecionar para um template. Mesmo par que `PhaseView` → `PhaseDetailView` já usa. |
| `TemplatePaster` | Paste da schematic via FAWE assíncrono, seguido de `setBiome` na área da ilha. Callback de conclusão no main thread. |
| `TemplateCommand` | `setpoint <id>` grava a posição e rotação atuais do admin no `templates.yml`; `reload` recarrega o registry. Permissão `screen.dev`. |
| `TemplateWorld` | Garante que o mundo `templates` esteja carregado no boot; loga severe e desativa o fluxo se não estiver. |

### Quem entra no fluxo

Só quem não é dono de ilha **e** não é membro nem trusted de nenhuma. Membro da ilha de um amigo entra normalmente no jogo.

### Registro

`IslandsSystem.init` ganha: `TemplateRegistry.load()`, `TemplateWorld.ensure()`, `TemplateCameras.ensure()` e as duas views no `viewFrame.with(...)`. `TemplateListener` é registrado em `setupListeners()` e `TemplateCommand` em `setupCommands()`, ambos no `OneBlockPlugin`.

## Mudanças no código existente

- **`PlayerListener.onPlayerSpawnLocation`** — deixa de criar ilha. Se o jogador não tem nem participa de nenhuma, o spawn é desviado para o mundo `templates` e `TemplateSelectionService.enter` assume. A criação silenciosa some.
- **`IslandManager.createIsland`** — passa a receber o `IslandTemplate` escolhido (nullable) e grava o `templateId` na ilha. Com template, quem monta a ilha é o `TemplatePaster`; sem template, `generateStarterIsland` continua existindo como fallback dos casos de erro abaixo.
- **`IslandResetService`** — o reset de terreno recola o template salvo na ilha em vez de gerar bedrock+dirt. Ilha sem `templateId` (documento antigo) cai no comportamento atual.

## Dados

### Modelo

`Island` ganha `templateId: String?`.

**Nullable de propósito:** documentos de ilha já existentes no Mongo não têm o campo, e um primitivo com default quebra a decodificação deles. `null` significa "ilha anterior ao sistema de templates" e é tratado explicitamente no reset.

### Configuração

```
server/plugins/one-block/templates/
├── templates.yml
├── lush.schem
├── nether.schem
└── ...
```

`templates.yml` guarda a lista ordenada. Cada entrada: `id`, `display-name`, `description`, `biome`, `schematic`, `icon` (material + `itemModel` opcional da Textura) e `camera` (world, x, y, z, yaw, pitch). O bloco `camera` é escrito pelo `/is template setpoint <id>`.

## Texto na tela

Duas superfícies, e cada uma tem um dono no projeto:

- **Topo — `HudApi` da Textura.** Bossbar desenhada com glyph, slot próprio (`template`), mostrando nome e descrição do template que está na tela. Atualiza a cada `preview`, some no `exit`.
- **Rodapé — `ActionBar` do `lib:actionbar`.** A instrução (`SHIFT abrir menu`). A HUD secundária não serve aqui: ela ocupa a mesma linha da bossbar, no topo, e não tem como descer.

`Channel` ganha uma entrada `TemplateSelect` com prioridade acima das existentes (`FlyTimer` 0, `Combat` 10, `SkillXp` 20) — durante a seleção nada mais deve roubar o rodapé. O canal é republicado enquanto a sessão viver e limpo com `ActionBar.clear` no `exit`.

`islands` passa a depender de `lib:actionbar` (`compileOnly`, como os outros `lib:*`).

## Dependências

- **FAWE** — `compileOnly` no módulo `islands` (`worldedit-bukkit` via Maven), e `depend` no `plugin.yml`. É o único caminho do paste; sem ele o fluxo não tem como completar, então é dependência dura e não `softDepend`.
- **Textura** — já é `compileOnly` de `islands`. Usada para o painel (`HudApi`) e para os ícones (`itemModel`).

## Erros e casos de borda

| Situação | Comportamento |
|---|---|
| Mundo `templates` ausente | `TemplateWorld.ensure` loga severe e desativa o fluxo. O jogador sem ilha cai no comportamento antigo (ilha criada com plataforma padrão), para ninguém ficar preso. |
| `templates.yml` vazio ou todos inválidos | Mesmo fallback acima. |
| `.schem` de um template ausente | Template é pulado no carregamento e não aparece no menu. |
| Paste falha no meio | Jogador volta para survival na ilha criada com a plataforma padrão, recebe mensagem de erro, e a falha é logada com o id do template. Ilha não fica em estado inacessível. |
| Confirmação dupla (clique rápido) | Flag na sessão torna a segunda no-op. |
| Deslogar durante o paste | O paste termina; o jogador entra na ilha pronta no próximo login, pelo fluxo normal. |
| Grid cheio / `createIsland` retorna `null` | Mensagem de erro, jogador permanece no showcase, severe no log. |

## Fora de escopo

- Trocar de template depois da ilha criada. O reset recola o mesmo template.
- Template alterar progressão, drops ou fases.
- Câmera com movimento/órbita. Câmeras são estáticas nesta versão.
