# Próximos passos

## Estado atual

0.11.0-dev entrega presets persistidos por jogador/clã, nome/sigla independentes,
menus/comandos, fallback que preserva IDs, migração6/CAS/auditoria, editor textual,
convites enviados e administração inicial de identidade. Não reaplicar fases anteriores.
CUSTOMIZATION.md define os escopos; CLANS.md explica editor/convites/administração.
Resultados executados e SHA atual em PROJECT_STATUS.md.

## Próxima etapa de implementação

1. Recursos adicionais de TAB, auditando versão exata e ownership antes de implementar:
   listed/vanish por viewer, nametags, objetivo BELOW_NAME e bossbar. Javap1.21.1
   confirma UPDATE_LISTED, ScoreboardDisplaySlot.BELOW_NAME e ServerBossBar, mas isso
   não equivale a implementação. Não inventar list-order; teams têm efeitos globais.
   Escolher opt-in por recurso e preservar proprietário/sistemas externos.
2. Completar administração/configurações de clã conforme REQUIREMENTS.md: diagnósticos
   e reparos relacionais devem ser explícitos/transacionais, sem exclusão/transferência
   forçada que contorne confirmação. Comandos0.11 já corrigem nome/sigla com auditoria.
3. Ampliar presets de forma segura quando viável: animação individual/clã, campos
   pessoais separados além dos títulos, controles por superfície e respectivas permissões.
   Não animar histórico de chat nem confundir animações de layout com preset animado.
4. Cliente real: editor anvil (digitar/clique no papel à esquerda/Esc), menus de estilo,
   TAB/sidebar/gradientes/escala/actionbar, autenticação e assinatura, contexto LP,
   reconexão/mudança de mundo, multijogador/desempenho e conflitos de mods.
5. Novas versões de providers: confirmar contratos públicos e artefatos exatos antes
   de ampliar suporte. Pokédex deve manter provider/coleção explícitos e moedas distintas.

## Regras de continuidade

ID de preset é dado persistente, não markup. Permissão perdida/preset removido nunca
apaga escolha de estilo; entitlement do título tem regras próprias. Preservar CAS,
catálogo/UUID de clã da tela e liderança fresca. Nome/sigla têm cooldowns independentes.
Renderização usa cache e permissões atuais; APIs de jogo só na thread do servidor.
Nenhuma transação distribuída é presumida entre SQLite e LuckPerms/providers externos.

Testes Fabric usam jogadores sintéticos. A falha UnknownHostException na busca de
publickeys do authlib não autoriza desativar online-mode. Não declarar cliente real,
assinatura criptográfica, contextos autenticados ou carga aprovados sem evidências.
Sidebar/teams seguem ownership anterior e mixins/refmap fixados em1.21.1.

Salvar progresso autorizado em istwi/Shirokai-tab/main sem reescrever histórico.
Não publicar ambiente ou releases automaticamente; não reaplicar setup publicado.
