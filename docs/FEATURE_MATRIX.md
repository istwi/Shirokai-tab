> 0.15.0: /perfil configurável em menus.json; menu/editor de clã pausados por
> padrão, com comandos/dados preservados. Veja MENUS.md.

> 0.14.0: ROLE é cargo principal automático e fixo (TAB/sidebar/chat/nametag);
> eventos/clã são opcionais apenas no chat. Nametags padrão schema2. RANKS.md.

# Matriz de funcionalidades

| Área | Estado | Limitação |
|---|---|---|
| Bootstrap Fabric | Servidor chegou a Done | Login de jogadores não testado |
| Lifecycle modular | Implementado e testado | Dependências em ordem de registro |
| core.json | Defaults, validação e reload testados | Presets/presentation e configs dos módulos documentados |
| SQLite | Schema6, presets persistidos, upgrade com backup, perfis/concessões/clãs, CAS e auditoria | Schema3 clãs presente; restauração automática pendente |
| Permissões | Adaptador API5.4 e runtime Fabric5.4.140: grant/revoke/prefixo reais | Contextos com cliente autenticado e outras versões pendentes |
| Placeholders | Nativos e ponte PAPI2.4.2 real, argumentos e exportação literal | Depende de handlers instalados; conflitos preservados |
| Administração | Status/version/modules/permissions/reload/debug/backup/placeholders | Títulos e consulta/correção de identidade de clãs; reparos relacionais avançados pendentes |
| Títulos e duas identificações | Implementados e testados | ROLE/SPECIAL/CLAN integrados; cliente autenticado pendente |
| Clãs nativos | Comandos, menus, matriz de ações e identificação CLAN testados | Presets, editor textual e convites enviados implementados; administração relacional avançada pendente |
| Chat e moderação | Canais [G]/[L], /g e /l, cores limitadas &, avisos [+]/[-], privado/ignore/mute/gates testados | Cliente real e assinaturas/login reais pendentes; presets das identificações implementados |
| TAB | Header/footer, nomes, título, listed por viewer, nametags, BELOW_NAME e bossbar; ownership opt-in/diffs testados | Cliente real, vanish de entidades e compatibilidade universal pendentes; efeitos globais de teams explícitos |
| Scoreboard visual | Sidebar privado por viewer, layouts/linhas condicionais, diffs e visibilidade persistida | Cliente real e compatibilidade universal não validados; cede a sidebar externo |
| /perfil e menus de clãs | Handlers testados com jogadores sintéticos | Aparência/pacotes de cliente real não testados |
| Estilos e animações | Cores/HEX/gradientes/frames com cache usados no TAB/sidebar e apresentação de chat/menus/títulos/clãs | Presets pessoais/clã e editor textual implementados; animação de presets/menus e transições pendentes |
| Cobblemon/Impactor/Dex/PAPI | Adaptadores opcionais implementados: Pokédex oficial, moedas separadas e Dex via PAPI | Versões/semântica em INTEGRATIONS.md; cliente real e versões futuras não certificados |
| CI | Workflow de build criado | Execução no GitHub não realizada |
| Release estável | Não disponível | Critérios completos não cumpridos |

Scoreboard funcional implementada em0.8; a validação de pacotes sintéticos não equivale à aparência com cliente real.
