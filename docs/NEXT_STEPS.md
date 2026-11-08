# Próximos passos

## Estado atual

0.8.0-dev adiciona sidebar real por viewer: objetivos/linhas privados, layouts e
condições, atualização incremental, título único compartilhado com TAB, animações,
preferência persistida de visibilidade e detecção conservadora de sidebar externo.
Schema SQLite permanece5. Infraestrutura, perfil, títulos, clãs, chat, estilos e TAB
preservados. Uso e limites em SCOREBOARD.md; evidências atuais em PROJECT_STATUS.md.

## Próxima implementação

1. Auditar versões/artefatos/APIs reais de Cobblemon1.8.3, Impactor, Shirokai Dex,
   Fabric Placeholder API e LuckPerms Fabric. Implementar adaptadores opcionais com
   dados reais, sem confundir impactor:dollars e impactor:tokens. Não declarar a API
   nativa de placeholders como integração Fabric PAPI.
2. Integrar StyleEngine com segurança a cabeçalhos de chat/menus/títulos/clãs, incluindo
   permissões, menções e preservação da personalização após expiração de VIP.
3. Completar ferramentas administrativas/editor textual de clãs, convites enviados,
   estilos independentes para nome/sigla e demais requisitos completos.
4. Avaliar recursos adicionais de TAB: listed/vanish por viewer, nametags, objetivos
   abaixo do nome e bossbar, sem substituir controle de outros mods.
5. Validar com cliente real TAB/sidebar/GUI/gradientes, contexto LuckPerms, autenticação,
   reconexão, mudanças de mundo, espectador, carga/multiclientes e mods de terceiros.

## Limitações preservadas

A fase7 tem implementação e testes de pacotes, mas renderização/autenticação e
compatibilidade universal não foram validadas. Sidebar cede a SIDEBAR/TEAM_* externo;
colisão do objetivo exato suspende nessa conexão até reconectar. Mods que contornam
ServerCommonNetworkHandler.send podem escapar do observador: escolha um dono.

Teams do TAB são opt-in e podem afetar mecânicas de equipe. Nunca adotar/remover
team externo. Preservar accessor/mixins/refmap e a versão exata de Minecraft.
Não inventar dados de saldo, Pokédex ou ginásios: fallback explícito até adapters reais.
Smokes usam conexões/jogadores sintéticos, não substituem cliente real nem teste de carga.
Não desabilitar online-mode para mascarar falhas de autenticação ou rede.

Salvar progresso está autorizado em istwi/Shirokai-tab/main, sem reescrever histórico.
Não publicar ambiente nem release automaticamente. Não reaplicar setup já publicado.
