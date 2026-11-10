## 0.16.9-dev

- Dinheiro/tokens/cash exibem0 para jogadores sem conta em moeda válida do Impactor.
- Não cria conta nem modifica saldo; moedas ausentes e erros continuam N/D.
- Regressão com Impactor real para ausência de conta nas três moedas.

## 0.16.8-dev

- Deixa vazio o slot34 do painel principal do clã, onde ficava a barreira antiga.

## 0.16.7-dev

- Cabeças de perfil/clã/estilos exibem a skin do jogador que abriu o menu.
- Exclusão do clã movida para19; não líderes veem vidro nessa posição.
- Seta28 volta ao /menu externo; cabeça10 atualiza o painel do clã.
- Ícones de estrela do clã substituídos por papel.

## 0.16.6-dev

- Corrige migração do perfil antigo com título/textos/ícones personalizados.
- Reconhece geometria antiga pelos slots e move controles sem apagar a aparência.
- Mantém backup, clanMenuEnabled e layouts com posições personalizadas.

## 0.16.5-dev

- /perfil e seleção de estilos compartilham o layout de cinco linhas da referência.
- Cabeça informativa10, quadro19, seta28 e opções à direita; paginação contextual.
- Migração do perfil padrão antigo com backup, preservando layouts personalizados.
- Mantém seleção por ID, permissões, revisões, segurança de itens e animações.

## 0.16.4-dev

- Menu de clã disponível somente para membros de um clã, incluindo quem entrou por convite.
- Sem clã, orienta criação/convites no chat; fecha telas após perda da associação.
- Preserva comandos de criação, convites e aceitação.

## 0.16.3-dev

- Remove somente os vidros dos seis espaços indicados no painel principal de clã:24,25,30,31,32,33.

## 0.16.2-dev

- Menu principal de clã segue a referência: cinco linhas, vidro cinza, cabeça
  informativa, bússola para atualizar, bloco de redstone para fechar e barreira
  para excluir. Oito estrelas para as funções, com estilos reunidos em submenu.
- Listas adaptadas a27 entradas por página e navegação em cinco linhas.
- Preservadas permissões, escopo do clã e confirmações de transferência/exclusão.

## 0.16.1-dev

- Menu de clã com moldura escura/ciano, estrela central e ações espaçadas por função.
- Ícones e descrições revisados; exclusão isolada em vermelho; fechar distinto de voltar.
- Paginação contextual, total de entradas e estado vazio; decoração sem ações.
- Mantém confirmações, autorização SQL e personalização de nome/sigla.

## 0.16.0-dev

- Clãs: histórico paginado por GUI/comando, diagnóstico SQLite/relacional e reparo
  explícito transacional com prévia, expiração, CAS e auditoria.
- Menu/editor de clã habilitados em configurações novas; false existente preservado.
- Presets pessoais/clã com quadros, permissões e políticas por área; previews vivos.
- Integração opcional auditada Drex Vanish1.5.9+1.21.1: visibilidade em chat, TAB,
  contadores e seletores. Ocultação de entidades pertence ao provider externo.
- Sem alteração de schema SQL6 ou reescrita de JSONs existentes.

## 0.15.2-dev

- Evita persistir teams temporários de TAB/nametag durante saves do mundo.
  Mantém a aparência dos jogadores online e permite recriar vínculos após restart.
- Ownership validado por instância, aparência completa e membros conhecidos;
  teams externos, modificados ou mistos continuam persistidos. Sem adoção por nome.
- Regressão com serialização/restauração de NBT e troca OWNER -> ADMIN após restart.
- Vínculos antigos já salvos exigem liberação administrativa uma vez; JSONs preservados.

## 0.15.1-dev

- Terceira moeda independente: cashCurrency (padrão impactor:cash) e placeholder
  %shirokai:cash%, também exportado para Fabric PAPI.
- JSON antigo preservado; alias cash desativado se impactor:cash já era usado em
  outro alias legado. Null desativa cash; mapeamentos explícitos devem ser distintos.
- Reload limpa saldos antigos, sem fallback entre moedas ou criação de contas.

## 0.15.0-dev

- menus.json configura título, linhas, slots de tags, ícones, nomes e lore dos
  botões/entradas do perfil, com cores e placeholders seguros.
- Reload valida layout e IDs de itens antes de publicar; telas abertas conservam
  seu layout e arquivos existentes não são sobrescritos. Ações/permissões são fixas.
- Menu e editor de clã desativados por padrão; comandos e dados preservados.

## 0.14.1-dev

- Corrige colisão com teams antigos vazios/salvos após liberar jogador: namespace
  de nametag livre por instância permite recuperar prefixo e ordenação.
- Preserva teams antigos/externos e requer liberação administrativa explícita de
  membros presos a teams anteriores. Não remove teams por prefixo.
- Status TAB informa se nametags estão habilitadas.
- Regressão Fabric reproduz bloqueio antigo e testa recuperação, pesos e reinício.

## 0.14.0-dev

- Cargo fixo pelo grupo principal LuckPerms, aplicado sem equipar e atualizado
  dinamicamente no chat/TAB/sidebar/nametag; rótulo automático e ROLE configurável.
- ROLE fora do perfil/sugestões; eventos e clãs somente no chat. Limpar perfil
  preserva cargo e estilos pessoais não alteram ROLE.
- Nametags habilitadas por padrão schema2; migração em memória do padrão schema1,
  opt-out explícito e preservação dos arquivos/teams externos.
- Seleções antigas normalizadas online sem alterar concessões ou schema SQLite6.

# 0.13.1-dev

- Somente cor do nick: branco explícito no chat global/local/clã/staff/privado e
  avisos de conexão, sem alterar cores de indicadores, tags, corpo, códigos & ou gates.
- Caminhos com apresentação e fallback literal cobertos pelo smoke de cores existente.

# 0.13.0-dev

- Chat compacto [G] cinza/[L] amarelo, formato Nick: mensagem sem < > via tipos de
  chat sincronizados e corpo padrão por canal preservando conteúdo assinado.
- /g e /l com os mesmos gates e seleção/envio opcional de /global e /local.
- Cores &0..f/&r com permissão configurável, escape && e filtro/duplicação canônicos;
  decoração visual opcional só sem filtragem, com original/assinatura/máscara intactos.
- Avisos vanilla [+] verde/[-] vermelho sem duplicação, com configuração para desativar.
- chat-appearance.json com reload atômico; formatos antigos exatos promovidos em
  memória sem sobrescrever arquivos. SQLite6 e dependências preservados.
- Testes de parser/moderação, registries/cores/assinatura/filtro e callbacks dos mixins.

# 0.12.0-dev

- Extras opt-in em tab-extras.json: UPDATE_LISTED por viewer respeitando baseline
  externo, nametags de prefixo/sufixo/visibilidade, BELOW_NAME privado e bossbar pessoal.
- Diffs por componente, permissões/condições dinâmicas, dados numéricos estritos e
  ownership com cedência a sistemas externos/colisões. Sem vanish de entidades.
- Teams individuais têm rank estável por prioridade/nome/UUID e preservam mutações
  externas. Ledger de scoreboard compartilhado com contagem de consumidores.
- Testes de config/numéricos, pacotes, dimensão, conflitos e limpeza; regressões
  anteriores preservadas. Schema SQLite6 e dependências permanecem.
- Sem release ou alteração/publicação de configuração do ambiente.

# 0.11.0-dev

- Presets estáticos por ID com permissões, escolhas persistidas de títulos pessoais e
  nome/sigla de clã independentes; fallback/recuperação sem apagar IDs.
- Schema6 transacional com backup, CAS, auditoria, FK/cascade e caches por sessão.
- /estilo e seletores no perfil/clã; pipeline rico compartilhado de chat/TAB/sidebar.
- Editor textual de identidade do clã, convites enviados/cancelamento e comandos
  administrativos iniciais list/inspect/rename/tag com permissões próprias.
- Novos testes de migração, concorrência, restart, perda de permissão, menus e LP real.
- Sem mudanças de dependências/mixins, releases ou configuração do ambiente.

# 0.10.0-dev — apresentação segura

Cabeçalhos de chat, menus de perfil/clã e títulos recebem templates condicionais
com cores/HEX/gradientes. Mesmo título formatado em TAB/sidebar; composição de Text
sem reanalisar valores, sem ações importadas e sem mutar cache. Corpo/assinatura/
máscara do chat normal preservados; menções notificam separadamente e respeitam
filtragem/roteamento. Comandos system podem destacar menções literais.
Configuração presentation.json entra no reload atômico; perda de permissão visual
usa fallback sem apagar seleção/regra. Escolhas pessoais/editor continuam pendentes.
Sem nova dependência, protocolo, mixin ou migração SQLite. Evidências em PROJECT_STATUS.md.

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

## 0.13.2-dev

- Estilo administrativo fixo por ID de título em presentation.titleTemplates;
  mesma renderização em chat, TAB/sidebar e preview, sem herdar preset pessoal.
- Configurações antigas e títulos sem entrada preservados; reload valida todo candidato.

## 0.13.3-dev

- Sidebar recupera jogadores online cujo registro na entrada foi perdido/antecipado.
- /sidebar carrega a preferência antes de alternar mesmo sem View prévia; toggles
  concorrentes continuam serializados. Falhas são registradas no log do servidor.

## 0.13.4-dev

- Mensagens de uso comum com prefixo dourado e componentes literais, cores coerentes
  e confirmações específicas para cada ação de clã, sem dumps de mapas/UUIDs.
- /clan info e members mostram ficha em linhas, nomes online, cargos em português
  e até10 membros; lista completa em /clan menu. UUID fica no hover de diagnóstico.
- Perfil mostra os títulos formatados, ausência como Nenhum/Nenhuma, sem IDs/null.
- Canais de chat e cargos dos menus em português; notificações de convites, editor,
  estilos, TAB e sidebar seguem o mesmo padrão. Comandos administrativos preservados.
- Menus de perfil/estilos traduzem estados e seleções; dados técnicos de escopo/revisão saem do seletor de uso comum.
