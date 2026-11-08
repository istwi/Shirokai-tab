# Matriz de funcionalidades

| Área | Estado | Limitação |
|---|---|---|
| Bootstrap Fabric | Servidor chegou a Done | Login de jogadores não testado |
| Lifecycle modular | Implementado e testado | Dependências em ordem de registro |
| core.json | Defaults, validação e reload testados | Outros JSONs ainda pendentes |
| SQLite | Schema5, upgrade com backup, perfis/concessões/clãs, CAS e auditoria | Schema3 clãs presente; restauração automática pendente |
| Permissões | Adaptador LuckPerms API5.4 e fallback admin | Runtime LuckPerms Fabric não testado |
| Placeholders | Online/nome/UUID/título/tags, fallback literal | Adaptador PAPI e demais dados pendentes |
| Administração | Status/version/modules/permissions/reload/debug/backup/placeholders | Gestão de clãs/títulos pendente |
| Títulos e duas identificações | Implementados e testados | ROLE/SPECIAL; integração com renderizadores e clãs pendente |
| Clãs nativos | Comandos, menus, matriz de ações e identificação CLAN testados | Estilos, editor textual GUI e administração avançada pendentes |
| Chat e moderação | Canais, privado, ignore, mute e gates implementados/testados | Cliente real, estilos por grupo e menções avançadas pendentes |
| TAB | Header/footer, nomes, título e teams opt-in implementados/testados | Cliente real, listed/vanish, nametags, bossbar e objetivos abaixo do nome pendentes |
| Scoreboard visual | Sidebar privado por viewer, layouts/linhas condicionais, diffs e visibilidade persistida | Cliente real e compatibilidade universal não validados; cede a sidebar externo |
| /perfil e menus de clãs | Handlers testados com jogadores sintéticos | Aparência/pacotes de cliente real não testados |
| Estilos e animações | Cores/HEX/gradientes/frames com cache usados no TAB/scoreboard | Integração a chat/menus/títulos/clãs e transições pendentes |
| Cobblemon/Impactor/Dex/PAPI | Pendente | Auditoria de versões e APIs |
| CI | Workflow de build criado | Execução no GitHub não realizada |
| Release estável | Não disponível | Critérios completos não cumpridos |

Scoreboard funcional implementada em0.8; a validação de pacotes sintéticos não equivale à aparência com cliente real.
