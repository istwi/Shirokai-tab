# Próximos passos

## Estado atual

0.9.0-dev adiciona adapters opcionais reais de Cobblemon1.8.3, Impactor5.3.5,
Text Placeholder API2.4.2 e Shirokai Dex via seus handlers públicos; LuckPerms
Fabric5.4.140 foi exercitado. Ginásios usam scores vanilla existentes. Configuração,
contratos e limites em INTEGRATIONS.md. TAB/sidebar recebem essas métricas pelo
serviço nativo. Schema SQLite permanece5 e as funcionalidades anteriores continuam.

## Próxima implementação

1. Integrar StyleEngine com segurança a cabeçalhos de chat/menus/títulos/clãs, incluindo
   permissões, menções e preservação da personalização após expiração de VIP. Nunca
   interpretar o corpo escrito pelo jogador nem quebrar assinatura/filtro do chat.
2. Completar ferramentas administrativas/editor textual de clãs, convites enviados,
   estilos independentes para nome/sigla e demais requisitos completos.
3. Avaliar recursos adicionais de TAB: listed/vanish por viewer, nametags, objetivos
   abaixo do nome e bossbar, sem substituir controle de outros mods.
4. Validar com cliente real TAB/sidebar/GUI/gradientes, contexto LuckPerms, autenticação,
   reconexão, mudanças de mundo, espectador, carga/multiclientes e mods de terceiros.
5. Acompanhar novas versões do Shirokai Dex: o Core só usa o contrato público PAPI.
   Certificar novas versões externas antes de ampliar a lista suportada. Dados mais
   amplos de treinador, economias alternativas e integrações futuras exigem APIs reais.

## Limitações preservadas

Os adapters estão implementados para as versões auditadas e exercitados com seus JARs,
mas login/renderização/carga e contextos de jogadores autenticados não foram validados.
O authlib no ambiente pode falhar ao buscar chaves Yggdrasil por DNS; não desabilitar
online-mode para contornar isso. Testes usam jogadores/conexões sintéticos em mundo isolado.

Fonte da Pokédex é explícita. Não misturar métricas de coleções/providers diferentes;
sem handler/conta/moeda/objetivo, preservar fallback. Economia não tem fallback primário,
transação distribuída ou leitura de arquivos internos. Impactor account é fetch-or-create;
checagem de presença não é atômica contra exclusão concorrente de outro mod.

Sidebar cede a SIDEBAR/TEAM_* externo; colisão do objetivo exato suspende nessa conexão
até reconectar. Mods que contornam ServerCommonNetworkHandler.send podem escapar do
observador: escolha um dono. Teams TAB são opt-in e podem afetar mecânicas de equipe;
nunca adotar/remover team externo. Preservar mixins/refmap e versão exata de Minecraft.

Salvar progresso está autorizado em istwi/Shirokai-tab/main, sem reescrever histórico.
Não publicar ambiente nem release automaticamente. Não reaplicar setup já publicado.
