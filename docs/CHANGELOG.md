# 0.9.0-dev — integrações opcionais reais

Cobblemon1.8.3: registro/Pokédex oficial e eventos; Impactor5.3.5: moedas distintas e
contas existentes assíncronas; PAPI2.4.2: importação com argumentos e exportação literal;
Shirokai Dex: delegação aos seus handlers públicos; ginásios: objetivo vanilla existente.
LuckPerms Fabric5.4.140 exercitado; runtime passa a SERVER_STARTED para respeitar enable.
Configuração integrations.json/reload atômico, snapshots e diagnóstico adicionados.
Sem novas migrações SQLite ou mods opcionais embutidos. Manifesto de JARs verificáveis
e teste em servidor real adicionados; cliente autenticado/visual/carga continuam pendentes.

# 0.8.0-dev — scoreboard visual

Sidebar privado por viewer com layouts/condições, linhas ordenadas e repetidas,
estilos/animações, título compartilhado com TAB, diffs e visibilidade persistida.
Ownership conservador observa pacotes sem os cancelar, preserva sidebars externos
e libera somente recursos próprios. Comandos /sidebar e /shirokai:sidebar, diagnóstico
/shirokaicore scoreboard, reload atômico e testes
JUnit/Fabric adicionados. Schema SQLite permanece5. Cliente real e integrações
externas continuam pendentes. Evidências e SHA em PROJECT_STATUS.md.

# Histórico

## 0.7.0-dev — estilos e TAB

- Motor de templates com cores/HEX/gradientes Unicode, estilos e animações de frames.
- Placeholders literais, tags allowlisted, cache e limites de expansão.
- Header/footer e nomes com título escolhido, layouts/regras por grupo/perm/dimensão.
- Pacotes de nomes incrementais por viewer, sem alterar ping/roster/game mode.
- Teams de ordenação opt-in, isolamento de ownership e limpeza antes do save.
- Metadata/peso de grupo LuckPerms por API e diagnóstico real de atualizações.
- Schema5/preferência de animação, migração com backup e comando pessoal.
- Mixin accessor/refmapMC1.21.1 e testes de componentes no servidor Fabric.

## 0.6.0-dev — chat e moderação

- Canais global/local/clã/staff com destinatários, permissões e distância/dimensão.
- Privados/reply/ignore, duas identificações escolhidas e corpo literal.
- Roteamento de chat normal preservando SignedMessage e filtro, com decoração do remetente.
- Gates compartilhados de mute/spam/cooldown/duplicação/tamanho/filtro e bypasses explícitos.
- Schema4 com canal/ignore/mute persistidos, backup de migração e auditoria transacional.
- Ownership de /msg vanilla por configuração e aliases preservados em reload de datapacks.
- Administração de mute temporário/permanente e logs de rejeição limitados.
- Regressão: ações de mute/ignore não trocam canal após mudar default no JSON.

## 0.5.0-dev — menus e identificação de clãs

- Menus nativos para clã, membros/cargos, convite/aceitação e confirmações.
- Cancelamento revoga confirmação de exclusão no banco.
- Matriz de ações por cargo configurável, com invariantes de liderança/hierarquia.
- Ações de menus vinculadas ao ID do clã exibido, validadas na transação.
- Título CLAN dinâmico e clã atual no /perfil, respeitando duas identificações totais.
- Leave/kick/disband removem seleção CLAN atomicamente; save bloqueia falsificação.
- Cache online atualizado por eventos do serviço sem SQL por tick.
- Testes de integração de regras e menus em servidor real com jogadores sintéticos.

## 0.4.0-dev — núcleo de clãs e GitHub

- Código0.3 salvo na branchmain do GitHub (89117d0), conforme pedido do usuário.
- Schema3 com backups e integridade de liderança/associação.
- Criação e alterações gratuitas, nome/sigla independentes, convites persistentes.
- Cargos, limites por permissão, perda de VIP sem expulsão e aceitação transacional.
- Transferência aceita e exclusão confirmada, prazos e auditoria atômica.
- clans.json validado/reload e comandos sem sobrescrever comandos de terceiros.
- Testes de concorrência, expiração, permissões, rollback, migração e componente no servidor real.

## 0.3.0-dev — títulos e perfil

- titles.json validado sem sobrescrever customizações; reload junto com core.json.
- Concessões permanentes/temporárias, expiração, permissões e seleção visual única.
- Duas identificações totais com ordem por categoria/prioridade e bloqueio da terceira.
- Schema2 com backup pré-migração, CAS por revisão e auditoria transacional.
- /perfil nativo com paginação, escolha, remoção, refresh e retorno a /menu.
- Proteções de inventário, ações revalidadas e limpeza de cache no disconnect.
- Administração de títulos por UUID e placeholders de título/identificações.
- Testes de regras, persistência, concorrência, migração e GUI no servidor com jogadores sintéticos.

## 0.2.0-dev — infraestrutura e servidor

- EULA aceita por solicitação explícita e boot real Fabric concluído.
- Configuração core.json validada, defaults preservados e reload transacional em memória.
- SQLite assíncrono com schema versionado, rollback, limites de fila, preferências,
  auditoria, backup consistente e drenagem no shutdown.
- Dependências de módulos e isolamento de falhas.
- Adaptador LuckPerms API5.4 e fallback administrativo nível4 quando ausente.
- Placeholders nativos literais com fallback; comandos administrativos com permissões.
- Testes unitários e smoke em servidor real, com reload, restart e falhas de storage.

## 0.1.0-dev — início

- Auditoria de checkout vazio e preservação do prompt integral.
- Bootstrap Fabric/Gradle/Java21, lifecycle inicial, CI e documentação de continuidade.
