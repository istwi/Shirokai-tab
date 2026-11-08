# Prompt para continuar Shirokai Core

Continue no checkout existente de istwi/Shirokai-tab, branch main mais recente.
Confira status/branch/remoto/commits antes de sincronizar. Preserve alterações locais
e histórico; não faça reset destrutivo, force push ou worktree sem solicitação.
Commits e push para main estão autorizados. Não publique releases nem configuração
de ambiente; não reaplique o setup já publicado.

Leia integralmente AGENTS.md, REQUIREMENTS.md, PROJECT_STATUS.md e NEXT_STEPS.md
(os três últimos em docs/). Antes de mudar cada área, leia CUSTOMIZATION.md,
PRESENTATION.md, STYLES.md, TITLES.md, CLANS.md, CHAT.md, TAB.md, SCOREBOARD.md,
INTEGRATIONS.md, CONFIGURATION.md, PERMISSIONS.md, API_COMPATIBILITY.md,
FEATURE_MATRIX.md e TEST_PLAN.md em docs/.

## Estado atual

0.11.0-dev, schema SQLite6. Presets estáticos por ID com permissões e seleções
persistidas de títulos pessoais, nome e sigla de clã independentes. Fallback sem
apagar ID em permissão perdida/preset removido; recupera ao voltar. Reusa apresentação,
TitleService e clãs existentes. Presets não concedem títulos/grupos. Menu/commands
revalidam catálogo, CAS, associação e líder do UUID de clã exibido. Sem SQL por render.
Editor textual anvil: digitar e clicar no papel esquerdo salva; Esc cancela; não usar
output de forja/XP/material e limpar ícones antes de onClosed. Sent invites permitem
consulta/cancelamento autorizado. Administração list/inspect/rename/tag tem nós
separados, CAS/auditoria e preserva cooldowns/membros. Contratos em CUSTOMIZATION/CLANS.

Core/Fabric/Java21 e integrações anteriores preservados. Dependências fixadas:
Gradle8.12.1/Loom1.10.5/Yarn1.21.1+build.3/Fabric API0.116.17+1.21.1/Kotlin2.2.20.
Loader mínimo0.16.14; perfil Cobblemon/Dex0.17.2. APIs/artefatos exatos em INTEGRATIONS.
Resultado atual, SHA e limitações estão em PROJECT_STATUS; reexecute após mudanças.

## Continue

NEXT_STEPS.md prioriza extras de TAB (listed/vanish, nametags, BELOW_NAME, bossbar),
administração relacional restante e ampliação dos presets. Não reaplique escolhas
persistidas/editor/convites já entregues. Auditoria javap confirma símbolos1.21.1,
não compatibilidade universal nem implementação concluída. Recursos de ownership
precisam de opt-in e preservar mods externos.

Preserve: título único TAB/sidebar; até duas identificações ROLE/SPECIAL/CLAN;
clan_ representa associação real; clãs gratuitos, limites não expulsam membros,
transferência aceita/exclusão confirmada, menus antigos não agem sobre outro clã.
Permissão visual perdida nunca apaga preset. Thread servidor para APIs de jogo,
SQLite em executor próprio, migrações transacionais com backup/rollback/restart.
Dados dinâmicos nunca viram templates, Text compartilhado é read-only, rich slots
removem ações. Corpo/assinatura/máscara do chat continuam intactos; menções somente
após roteamento e sem revelar conteúdo filtrado; não mudar staff/clã para global.
Sidebar cede a SIDEBAR/TEAM_* externo; colisão exata suspende até reconectar. Teams
só próprios/opt-in; liberar antes do save. Preservar /scoreboard e mixins/refmap.
Moedas e fontes de Pokédex são explícitas; não ler arquivos privados dos providers,
inventar dados ou presumir transação distribuída. Core inicia em SERVER_STARTED.

## Checks e entrega

Na nuvem use python3 tools/gradle_cloud.py build smokeClasses --max-workers=2,
python3 tools/server_smoke.py --accept-eula, python3 tools/prepare_integration_mods.py
e python3 tools/integration_smoke.py --accept-eula. EULA autorizada para testes
isolados. JDK21 já está em /workspace/toolchains/jdk-21.0.13+9; helper prepara proxy/CA.
Não mudar TLS/checksums/JARs ou online-mode para passar. Código smoke fica fora do JAR.

Smokes são sintéticos, não cliente autenticado/assinatura válida/renderização real.
Authlib registrou UnknownHostException para publickeys; resolver acesso legítimo e
cliente/session antes de afirmar login/contextos reais. Cliente/carga pendentes não
podem ser apresentados como testes aprovados.

Atualize AGENTS e docs de estado/config/permissões/compatibilidade/testes/changelog
conforme escopo, incluindo versão/SHA do artefato. Revise diff, faça commit/push sem
reescrever histórico e confirme SHA local/remoto. Worlds/DB/caches/segredos/build não
vão ao Git. Informe em português entrega, testes executados, limites, commit e JAR.
Não declare o mod inteiro concluído antes dos critérios completos.
