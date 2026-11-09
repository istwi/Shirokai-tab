> Continuidade0.16: clãs têm histórico GUI/CLI, diagnose e repair com prévia/token,
> TTL120s, CAS/auditoria. Nunca mover líder ou excluir membros. Presets possuem
> frames/frameTicks e personalization.json por área. Escolhas continuam IDs SQL6.
> Menu clã padrão true só em arquivo novo; preservar false existente.
> Integração real Drex Vanish1.5.9+1.21.1 opcional: VANISH.md. Sem provider não há
> vanish de entidades. Os itens históricos pendentes abaixo sobre essas três
> etapas foram atendidos; cliente real/carga/compatibilidade continuam pendentes.

> 0.15.2: teams efêmeros próprios filtrados do save via mixin NBT auditado;
> não apagar/adotar teams por nome e não remover membros durante autosave. RANKS.md.

> 0.15.1 acrescenta cashCurrency/%shirokai:cash%; não misturar moedas nem criar
> contas para exibir saldos. Compatibilidade de JSON em INTEGRATIONS.md.

> 0.15.0: menus.json configura perfil; menu/editor de clã pausados por padrão.
> Não reativar sem pedido. Leia MENUS.md e o estado atual antes de continuar.

> 0.14.1 corrige colisão de teams antigos com namespace livre por instância.
> Veja RANKS.md/PROJECT_STATUS.md; não adotar nem excluir teams por nome.

> 0.14.0: cargo principal LuckPerms é fixo em TAB/sidebar/chat/nametag; ROLE não
> é selecionável. Eventos/clã apenas no chat. RANKS.md substitui a política antiga.

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
FEATURE_MATRIX.md, TAB_EXTRAS.md, CHAT_APPEARANCE.md e TEST_PLAN.md em docs/.

## Estado atual

Correção0.13.1: somente nick branco em todos os canais e avisos; componentes de
remetente/alvo separados para preservar indicador/tag/corpo. Não reaplicar layout.


0.13 ajusta o chat ao pedido visual: [G] cinza/[L] amarelo, Nick: mensagem, /g e /l,
[+] verde/[-] vermelho em avisos vanilla. chat-appearance.json e CHAT_APPEARANCE.md
controlam cores. A interpretação limitada de &0..f/&r foi solicitada explicitamente:
só para códigos autorizados/máscara pass-through/não filtrados adiciona unsignedContent
visual, sem mudar signedBody/assinatura/máscara. Cliente pode marcar modificação.
Não permitir markup/ações, bypass de filtro por código ou decoração para filtrados.
Cores padrão usam chat_type sincronizado sem unsignedContent. Novos mixins locais de
join/quit trocam o Text do broadcast existente; liberar bridge ao parar o chat.


0.13.1-dev, SQLite6, versões externas preservadas (INTEGRATIONS.md).
0.11 já entregou presets estáticos persistidos, fallback que preserva ID, editor
textual/convites enviados e administração inicial de clã. CUSTOMIZATION/CLANS definem
contratos, não reaplique. 0.12 acrescenta listagem por viewer, nametags, BELOW_NAME
privado e bossbar pessoal opt-in; tab-extras.json/ownership em TAB_EXTRAS.md.
UPDATE_LISTED não é vanish de entidades nem privacidade de chat/contagem online.

Todos os enabled de extras exigem reinício; templates/condições/numéricos validam
no reload atômico. Teams de nametag são globais e modificam ordenação/mecânicas;
ceder a teams externos/mutados. Objetivos privados cedem a slots externos/colisões.
Bossbar tem UUID por viewer e preserva outros UUIDs/replacements. SidebarPackets
agora usa retain/release para não apagar evidência de outro consumidor. Hooks de
send só observam, com chaves fracas/limites; runtime na thread servidor e sem SQL por frame.

Resultado/SHA atuais em PROJECT_STATUS; reexecute após mudanças. NEXT_STEPS prioriza
administração relacional/diagnóstico/histórico de clãs, ampliação dos presets e
integração de um mod real de vanish se necessário. Cliente real/renderização/carga
continuam pendentes. APIs1.21.1 foram auditadas, não extrapolar para outra versão.

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
