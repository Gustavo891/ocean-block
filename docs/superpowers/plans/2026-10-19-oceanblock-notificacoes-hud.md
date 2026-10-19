# OceanBlock — Notificações na bossbar — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Requirements are the referenced spec sections; values there are binding.

**Goal:** Notificações em várias regiões da tela ao mesmo tempo, numa bossbar ancorada pelo shader do Textura; coleta de detritos e rede cheia usam o sistema; HUD da ilha sai.

**Spec:** `docs/superpowers/specs/2026-10-19-oceanblock-notificacoes-hud-design.md` · **Base:** spike `8b16b80` (`plugins/textura/.../hud/anchor/`, `resource/assets/minecraft/shaders/core/text.vsh`, relatório `.superpowers/sdd/hud-shader-spike.md`).

## Global Constraints
- Sem comentários em código/Gradle/YAML/JSON/GLSL gerado novo (o shader pode manter os comentários do vanilla copiados). Strings ao jogador em português.
- API pública do Textura sem tipos do Kotlin (`PublicApiKotlinTypesTest` deve cobrir o pacote novo).
- Commits `type(scope): description` + trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, comando separado. Sem arquivos de `server/` fora dos listados.
- Builds: `./gradlew :plugins:textura:localJar :plugins:oceanblock:plugin:localJar`; testes `:plugins:textura:localTest`, módulos do ocean-block com `-Penv=local`. Não parar o servidor nem fazer deploy.

## Task 1: Motor de notificações no Textura
Spec §1 inteiro e §3 (testes do Textura). Pacote `one_block.textura.notify` (+ evolução de `hud/anchor/` e do shader para o índice de linha), `configs/hud/notificacoes.yml` (commit, com as duas regiões padrão), gerador de ícones de item para a fonte do HUD, `NotificationApi` (`push(player, region, NotificationLine)`, `clear(player, region)`, `registerItemIcons(materials)`), README do Textura (seção nova). Produces: `NotificationApi`, `NotificationLine` (texto, ícone opcional, duração, chave opcional; construtores/fábricas sem defaults do Kotlin).

## Task 2: Integração no ocean-block
Spec §1 (HUD da ilha removido) e §2. Files: `islands/hud/IslandHud.kt` e chamadores, `spawn/listener/SpawnHudListener.kt`, `drift` (entrega de coleta e aviso de rede cheia, registro de ícones de item de todos os drops e do `loom`), CLAUDE.md, roteiro de aceite (seção nova "9. Notificações"). Consumes: Task 1.
