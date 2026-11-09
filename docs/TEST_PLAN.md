## Regressão0.15.2 — persistência de teams

Antes: assertion Temporary Core team leaked into world save. Depois: hook real
NBT omite team próprio inalterado sem alterar players/prefixos ao vivo, restaura
save sem vínculo antigo e permite OWNER -> ADMIN sem team leave. Preserva team
externo e alterações externas em prefixo/displayName, membros desconhecidos e
cleanup conservador. Mixin método/field auditados no JAR Yarn1.21.1+build.3.

## Regressão0.15.1 — cash

JUnit: arquivo antigo recebe cash default em memória, não é reescrito, preserva
alias legado já usando impactor:cash; rejeita duplicatas/IDs/tipos inválidos e
permite cash null/remapeamento com reload. Impactor real: três saldos distintos
incluindo zero, registro/exportação PAPI, remapeamento para moeda ausente e
restauração sem cache antigo, conta cash ausente não criada e cleanup no disconnect.

## Regressão0.15.0 — menus JSON e pausa do clã

JUnit valida round-trip/defaults, preservação do arquivo, colisão/faixa de slots,
linhas, campos/tipos/ações desconhecidos, placeholders/markup seguros e valores
literais. Fabric valida menu3linhas, itens/nomes/slotes alterados, paginação,
remapeamento dos cliques, inventário preservado e snapshots após troca de layout.
Clã/editor bloqueados por padrão e tela antiga bloqueia cliques quando desativada;
fixtures habilitam explicitamente o legado para manter regressões de negócio.
Runner valida atomicidade de reload com slot repetido/item não registrado, config
ativa preservada, reload corrigido e arquivo menus.json preservado no reinício.

## Regressão0.14.1 — teams antigos

Reproduzido antes do patch: team skn vazio bloqueia nova nametag após liberação
explícita do membro. Depois do patch: nova faixa de nomes livre, prefixo correto,
OWNER antes de ADMIN, teams externos/antigos preservados. Outra instância simula
restart com teams salvos; não adota membros antigos, recupera após liberação
administrativa e remove somente seus próprios teams no encerramento.

## Regressão 0.14.0 — cargos fixos

- JUnit: seleção ROLE recusada, limpeza de escolhas legadas online, leitura offline
  preservada, cargo principal muda sem equipar, clear não remove cargo, fallback
  para grupo não configurado, wildcard não escolhe outro cargo.
- Migração de tab-extras schema1 default para nametag ligada; schema2 false e
  prefixo antigo personalizado preservados. enabled mantém exigência de restart.
- Fabric: perfil não lista ROLE e ambos os cliques atuam só no chat; proteção de
  inventário/cliques e limite de slots mantidos. TAG especial/clã não vaza ao TAB/sidebar.
- Fabric: teams/packets confirmam [OWNER] -> [ADMIN] automaticamente em TAB/nametag,
  cleanup preserva ownership. Estilos pessoais não mudam ROLE.
- Integração opcional: trocar primary group usando API LuckPerms5.4.140 e verificar
  cargo visual/chat antes de equipar qualquer coisa.
- Cliente real continua necessário: aparência acima da cabeça, grupos/contextos,
  autenticação/assinaturas, múltiplos jogadores e conflitos com mods reais.

# Plano de testes

## Infraestrutura

JUnit: isolamento de falhas de inicialização, limpeza, módulos desativados,
registro duplicado, módulo inexistente, reinicialização indevida e encerramento inverso.
Build deve compilar, executar testes e remapear JAR. Não confundir build com startup.

## Próximas suítes obrigatórias

- SQLite: migração, rollback, concorrência, integridade e restart.
- Títulos: expiração e perda de permissão; título único para TAB/scoreboard;
  zero/uma/duas identificações e rejeição da terceira, ordem e fallback.
- Clãs: unicidade case-insensitive, criação gratuita, sigla manual, capacidade
  na aceitação, expiração, entrada concorrente, cooldowns independentes,
  redução de limite sem expulsão, transferência aceita e exclusão confirmada.
- Chat: alcance por dimensão/distância, canais, staff, ignore, mute, antispam,
  texto literal sem injeção de markup e conflitos de aliases.
- Servidor real Fabric 1.21.1: boot, shutdown, login/reconexão, mundos, pacotes,
  GUI contra cliques manipulados, reload, LuckPerms e integrações exatas.
- Múltiplos jogadores: tráfego incremental, caches removidos no disconnect,
  ausência de SQLite por tick e latência da thread principal.

Inventário completo de cenários: seção 19 de REQUIREMENTS.md.
Nunca marcar testes não executados como aprovados.


## Suítes adicionadas na 0.2.0-dev

ConfigServiceTest: defaults sem overwrite, JSON/tipos/schema/caminhos inválidos,
preservação em falha, snapshot imutável e distinção hot reload/restart.
SqliteStorageTest: restart/backup, concorrência/drenagem, rejeição de schema futuro,
rollback de migração e cancelamento de resposta sem perder gravação aceita.
LuckPermsPermissionsTest: contrato API5.4, perda de permissão, UNDEFINED e usuário não carregado.
PlaceholderServiceTest: valores reais do contexto, fallback, substituição literal e não recursiva.
ModuleManagerTest: dependências bloqueiam somente consumidores; suites anteriores mantidas.

Smoke real: `python3 tools/server_smoke.py --accept-eula` cria diretório temporário
sob build/server-smoke, limita bind a loopback com porta dinâmica, exerce comandos,
reload válido/inválido/restart-only, backup, auditoria, reinício e schema futuro.
Preserva logs e resultado JSON; não altera configuração/world do run de desenvolvimento.
EULA aceita nesse teste somente com flag explícita. Nenhum login de jogador é simulado.


## Suítes 0.3.0-dev

TitleRulesTest: zero/uma/duas identificações, terceira rejeitada, slots compartilhados
ROLE/SPECIAL, ordem de categorias/prioridade, título único, permissões/expiração/superfícies.
TitleCatalogTest: defaults preservados, validação de schema/tipos/IDs/categoria suportada.
TitleStorageTest: upgrade schema1 com backup, CAS, seleção/revogação/restart e rollback
quando auditoria falha. TitleServiceTest: executor da thread do servidor, perda de
permissão, expiração/limpeza e diagnóstico offline sem apagar escolhas.

ProfileSmoke é mod de teste em src/smoke, ativado apenas por -PprofileSmoke e fora
do JAR de produção. Cria ServerPlayerEntity sintéticos no servidor real, com conexão
sem transporte. Abre ProfileMenu real e testa quickMove, ações não permitidas,
slots inválidos, outro dono, seleção visual, dois slots, terceiro bloqueado, limpeza,
retorno, menu fechado e preservação de itens. Não simula login ou valida renderização cliente.
Runner server_smoke habilita esse mod somente no primeiro boot e exige PASS explícito.
CI compila smokeClasses; execução de servidor é local nesta sessão, não no GitHub.


## Suítes0.4

ClanRepositoryTest: unicidade case-insensitive, sigla manual, cooldowns independentes,
concorrência na última vaga, redução sem expulsão, transferência confirmada, expiração,
roles contra escalada, rollback de auditoria, persistência e upgrade2→3 com backup.
ClanServiceTest: criação com permissão/tempo mínimo, limites VIP dinâmicos, mudança de
líder para limite menor, recuperação de VIP e validação/preservação de configuração.
ClanSmoke em src/smoke: mesmo serviço/repositório no servidor Fabric real com provedor
de permissões de teste. Não testa LuckPerms real nem representa clãs/multiclientes em produção.


## Suítes0.5

ClanIntegrationTest: cargos configuráveis sem violar hierarquia, escopo de menu antigo
rejeitado após troca de clã, identificação real em duas vagas, falsificação bloqueada,
limpeza atômica na saída/kick/disband. Usa SQLite real e executor equivalente à thread do servidor.
ClanMenuSmoke: aceitar convite no menu, listar membro, promover, pedir transferência
somente após confirmar, aceitar pelo sucessor, pedido de exclusão em duas etapas,
cancelamento que invalida pedido, exclusão final, menu fechado e preservação dos itens.
Runner exige CLAN_MENU_SMOKE_PASS; não substitui teste visual com cliente conectado.


## Suítes0.6

ChatServiceTest: local por dimensão/distância, global/clã/staff, perda de permissão
sem fallback, ignore/privado/reply, mute/unmute/bypass, duas identificações, corpo literal,
perfil ausente e regressão do canal padrão após reload.
ChatConfigAndGateTest: JSON e arquivos preservados, cooldown, duplicação, controles,
normalização/filtro e bypass. ChatRepositoryTest: persistência, expiração, rollback
em auditoria e upgrade3→4 preservando prefs/títulos/clãs e backup legível.
ChatSmoke: métodos de transporte vanilla reais, pacotes capturados, destinatários,
tags e corpo, ignore/mute, perda de staff. Fixtures usam SignedMessage.ofUnsigned:
não validam criptografia/login cliente. Smoke de aliases roda antes/depois da conclusão
END_DATA_PACK_RELOAD; script também testa mute persistente/restart/unmute e reload inválido.


## Suítes0.7

StyleEngineTest: estilos/HEX, gradiente por Unicode, dados literais, tags inválidas,
loops de animação rejeitados, frames/freeze/cache e limites. TabPlanTest: JSON/defaults,
condições e prioridade, ownership/restart e rejeição de templates grandes/anim desconhecida.
TabPreferenceTest: independência/restart e migração4→5 com backup. Contrato de metadataLP
adicionado ao teste existente. TeamOwnershipSmoke mantém3 cenários de teams antes
ensaiados em JUnit: ambiente nomeado cru não aplica bootstrap/acessos reais de Fabric.
TabSmoke captura header/nomes reais, título, grupo/priority, diff de uma entrada,
zero pacotes em frame igual, preservação/restauração de team externo, toggle e
congelamento de animação por viewer, cleanup. Refmap/entry action verificados no JAR/server.
Script também valida rollback de styles/tab e ownership que exige restart.
Cliente/renderização real/carga não testados; não confundir captura de pacotes com visual aprovado.


## Suítes0.8 — sidebar

ScoreboardPlanTest: default sem overwrite, JSON estrito/tipos/limites/duplicatas,
prioridade/condições, limite15linhas e compilação de título/linhas antes de publicar.
ScoreboardSmoke: pacotes reais no Fabric para título único TAB/sidebar, linhas
repetidas/vazias e ordem, números ocultos, placeholders literais/fallback, delta de
uma linha, remoção condicional/reload, frame dinâmico inválido sem publicação parcial,
frames/freeze, comandos sem sobrescrever alias externo, toggles concorrentes,
visibilidade independente de animação/reconexão, nenhuma alteração no board do mundo,
bundle anterior ao join, sidebar colorido externo, retirada/retorno, clear externo,
reset global de holder, colisão do nome exato e stop preservando replacement externo.
Runner real verifica candidato inválido scoreboard+core rejeitado atomicamente.
TabPreferenceTest preserva restart/upgrade4→5 das duas preferências; schema não mudou.
Resultados executados em PROJECT_STATUS.md; cliente real continua pendente.


## Suítes0.9 — providers opcionais

IntegrationConfigTest: arquivo preservado, schema/tipos, moedas distintas, provider
explícito, intervalo, flags restart-only e cálculo percentual com denominador real.
PlaceholderServiceTest: argumentos de PAPI, valor literal e proteção contra recursão.
Smoke comum também rejeita mapeamento inválido/ativação de integração em reload,
preservando snapshots; runtime sem nenhum mod opcional precisa continuar funcional.

IntegrationSmoke com os JARs reais fixados: importar player:objective com argumento,
exportar texto literal, preservar replacement externo, grants/revokes/prefixo LP,
saldos de duas moedas distintas, ausência de moeda/conta sem fallback, registro de
espécies real e progresso após givepokemonother oficial, atualização do Dex via seus
handlers e troca consistente das quatro métricas para a mesma coleção. Disconnect
limpa cache. Fixture altera somente mundo/contas/perfis descartáveis, sem login simulado
como autenticado. Runner tools/integration_smoke.py mantém online-mode e para o servidor.
Relatório build/integration-smoke/result.json inclui SHA256 dos mods efetivamente usados.
Resultados finais e limitações atuais em PROJECT_STATUS.md.


## Suítes0.10

PresentationPlanTest: arquivo preservado, tipos/schema, regras/condições/fallback,
candidatos inválidos, proibição de corpo/animações/tags duplicadas e rich slots com
cores/Unicode/remoção de ações/imutabilidade. Hover com registries roda em Fabric.
PresentationSmoke: cabeçalho com duas identificações formatadas, corpo/signature field/
unsignedContent/máscara preservados, menções filtradas/fora da rota/mute, corpo privado
literal sem clique herdado, perda/retorno de permissão sem apagar título/regra, estilo
base compartilhado em TAB/sidebar e preview real dos menus de perfil/clã.
Runner também rejeita presentation inválida dentro de um reload atômico completo.
Não equivale a cliente autenticado, assinatura criptográfica válida ou aparência real.

## Suítes0.11

StyleChoiceTest usa SQLite real: upgrade5→6 com backup e preservação de prefs/títulos/
clãs, rollback parcial da migração, auditoria, CAS/concorrência/restart, associação/
liderança/clã correto, cascade de exclusão e ticket de catálogo inválido. Testa
catálogo estrito/defaults preservados, fallback/remoção/recuperação e dados literais.
ClanRepositoryTest acrescenta CAS de identidade, convites enviados com expiração/
autorização/escopo e correção administrativa com unicidade, CAS e rollback de auditoria.

StyleChoiceSmoke: menu real bloqueia movimentação/outro jogador/tela fechada,
revalida permissão no clique, aplica preset persistido a título e pacotes TAB/sidebar,
preserva ID na perda/retorno/remoção, separa nome/sigla compartilhados e rejeita líder
antigo/catálogo antigo. ClanMenuSmoke agora exercita editor textual, cancelamento do
rascunho sem XP/itens, convites enviados e regressões de cargos/confirmações.
Runner também rejeita presets inválidos no reload atômico e aceita candidato corrigido.
IntegrationSmoke acrescenta persistência/fallback/recuperação com LuckPerms real e
runner verifica consulta administrativa de clãs pelo console. Conexões continuam
sintéticas: não comprovam cliente autenticado, assinatura criptográfica, renderização
do editor/gradientes, contextos de mundo reais ou carga. Resultados em PROJECT_STATUS.md.

## Suítes0.12

TabExtrasPlanTest: defaults desligados/config preservada, tipos/enums/campos/ownership,
compilação atômica de templates e números; N/D/overflow/NaN/fora de faixa não viram
valores fictícios. Rich slots mantêm dados literais.
TabExtrasSmoke no Fabric: listagem por viewer/self/seehidden e baseline falso,
cedência a UPDATE_LISTED/REMOVE externos, restauração normal; nametags individuais
e teams externos/mutados; objetivo privado fora do mundo, valores reais, invisíveis
não recebem linhas, BELOW_NAME externo/bundle/clear/colisão e limpeza; bossbar por
viewer, diffs, condição de permissão/mundo, reload de nome/progresso/cor/estilo,
coexistência, colisão exata e stop. Testa frame inválido sem inventar valores,
recuperação e zero pacotes em frame igual.
Suíte sidebar mantém a asserção de ledger vazio após último consumidor fechar;
registro compartilhado não é apagado enquanto outro recurso ainda precisa dele.
Runner rejeita config numérica inválida e enabled que exige restart no reload real.
São conexões sintéticas: não comprovam renderização cliente ou autenticação/carga.

## Suítes0.13

ChatAppearanceTest: migração em memória sem overwrite de [GLOBAL]/[LOCAL], defaults/
tipos/cores estritos, &0..f/&r/escape/Unicode, texto literal sem ações, filtro com
código no meio de palavra, duplicação por texto visível e mensagem vazia pós-código.
ChatAppearanceSmoke: registry real e decoração [G]/[L] Nick: corpo, RGB cinza/amarelo,
assinatura/body intactos, unsigned apenas para códigos permitidos, filtro sem
decoração e máscara preservada, perda/retorno de permissão, caminho de comandos,
alcance, callbacks transformados de join/quit e preservação de anúncio/alias externo.
ChatAliasSmoke exige /g e /l antes/depois de reload de datapacks. Runner também
rejeita cor de configuração inválida dentro do reload atômico real. Não houve
login/logout autenticado nem comprovação de assinatura criptográfica válida.

## Correção0.13.1

ChatAppearanceSmoke também exige nick branco nos componentes reais de global/local,
caminho de comandos, templates dos cinco canais, alvo privado e avisos [+]/[-],
sem alterar indicadores/corpo. Suítes existentes preservam as cores das tags.
