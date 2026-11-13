## 0.17.0 — Compartilhamento e MOTD

Marcações são disponíveis nos canais em que o autor já pode falar; não aceitam
alvo de outro jogador. Sem novos bypass. Mantidos chat.bypass.cooldown/filter
sob `shirokaicore`. MOTD usa a permissão administrativa existente de reload.
[Configuração](CHAT_SHARES_MOTD.md).

## Permissões0.16

`shirokaicore.command.clans.history`, `shirokaicore.command.clans.diagnose` e
`shirokaicore.command.clans.repair` controlam os novos subcomandos administrativos
(console/OP4 como fallback, conforme os demais comandos administrativos).
`/clan history` exige jogador, módulo ativo e associação atual ao clã.
Presets animados exigem `shirokaicore.style.animation` (pessoal) ou
`shirokaicore.clan.animation` (líder), além das permissões do preset/cores.
Políticas opcionais de área em personalization.json não concedem essas permissões.
Vanish tem permissões próprias do provider: VANISH.md.

# Permissões 0.11.0-dev

LuckPerms é a fonte de verdade quando presente e inicializado. O adaptador usa API 5.4,
usuário carregado e dados de permissão com contextos atuais. Não retém resultado de
permissão entre chamadas; FALSE/UNDEFINED/usuário ausente negam acesso.
Não altera grupos, prefixos ou permissões para fins visuais.

| Nó | Ação |
|---|---|
| shirokaicore.command.status | status e resultado da raiz sem subcomando |
| shirokaicore.command.version | versão |
| shirokaicore.command.modules | estados dos módulos |
| shirokaicore.command.permissions | provedor |
| shirokaicore.command.reload | recarregar configuração |
| shirokaicore.command.debug | integridade SQLite |
| shirokaicore.command.backup | criar backup |
| shirokaicore.command.placeholders | listar/resolver placeholders |
| shirokaicore.command.titles.inspect | listar catálogo e consultar perfil por UUID |
| shirokaicore.command.titles.grant | conceder títulos próprios permanentes/temporários |
| shirokaicore.command.titles.revoke | revogar concessões e seleção correspondente |

Com LuckPerms, nenhum desses nós é concedido automaticamente aos jogadores.
Sem LuckPerms, há fallback somente para comandos administrativos para jogadores
vanilla nível 4. Console usa autoridade vanilla nível 4; command blocks padrão
(nível 2) não ganham acesso. Se administration estiver desativado/bloqueado ou o
runtime não iniciar, apenas status/version permanecem disponíveis ao console nível 4.
Se placeholders estiver desativado, o subcomando correspondente fica indisponível.

A API foi testada por contrato e a distribuição Fabric5.4.140 foi exercitada em0.9
com grants/revokes e prefixo reais. Contextos de jogadores realmente conectados e
compatibilidade com outras distribuições continuam pendentes.

## Clãs implementados

shirokaicore.clan.create autoriza criação; shirokaicore.clan.limit.<n> determina
limite do líder conforme clans.json, usando o maior valor. Demais ações validam
cargos internos no banco. Fallback admin sem LuckPerms não concede criação.

## Nós de estilos previstos no pedido original (estado atual abaixo)

`shirokaicore.clan.color.hex`,
`shirokaicore.clan.color.gradient`, `shirokaicore.clan.animation`,
`shirokaicore.clan.tag.edit`, `shirokaicore.clan.style.*`, canais e bypass de moderação.
Cargos internos de clã não substituem LuckPerms.


/perfil permite gestão do próprio UUID sem permissão administrativa. Cada título
valida seu desbloqueio e a permissão configurada. ROLE exige permission; nunca atribui
esse grupo ao jogador. grant não ignora a permissão exigida por um título.
Detalhes e limites de chat em TITLES.md.


roleActions em clans.json controla invite/kick/promote/demote/rename/tag por cargo
interno. Não concede grupos LuckPerms. Transferência/exclusão só por líder e com
confirmação; regras de hierarquia e clã da tela são reconferidas na transação.
Identificação CLAN deriva de associação nativa, nunca de concessão administrativa.


## Chat0.6

shirokaicore.chat.staff é requisito padrão para enviar E receber STAFF, revalidado
em cada envio. channelPermissions em chat.json configura requisitos dos demais canais.
Global/local/clã/privado são públicos por default, com associação nativa exigida para CLAN.
shirokaicore.command.chat.inspect/mute/unmute autorizam administração separadamente.
shirokaicore.chat.bypass.mute/cooldown/filter/ignore são bypasses explícitos, nunca
automáticos. Cooldown inclui duplicação; limites de tamanho/caracteres permanecem.
Comandos de jogador não aceitam remetente arbitrário e não elevam permission level.


## TAB0.7

shirokaicore.command.tab.inspect permite diagnóstico global. /tab refresh/animations
só operam sobre a própria visão/preferência. Condições em tab.json usam permissões
LuckPerms configuradas, grupo primário e dimensão. Peso/prefixo/sufixo são dados atuais
do LP; ausência de provider não inventa grupo ou saldo. Ownership de teams é opção
administrativa em JSON/reinício, não permissão que jogador possa conceder a si.


## Scoreboard0.8

shirokaicore.command.scoreboard.inspect autoriza `/shirokaicore scoreboard`.
`/sidebar` e `/shirokai:sidebar` operam somente a preferência/visão do próprio jogador,
sem conceder ownership de sidebar externo. Condições por layout/linha usam os nós
LuckPerms configurados; não há grupo fictício nem permissão administrativa implícita.
Comandos curtos/namespaced existentes são preservados; `/scoreboard` vanilla permanece.


## Integrações0.9

shirokaicore.command.integrations.inspect permite `/shirokaicore integrations`.
Handlers do Shirokai Dex continuam exigindo a permissão da coleção do próprio Dex.
A ponte não contorna essa autorização nem modifica grupos/permissões por aparência.
O teste de integração concede nós só ao usuário sintético no mundo descartável.


## Apresentação0.10

Regras de presentation.json consultam sua permission/grupo/mundo em cada render.
Não concedem entitlement de título nem permissões de clã. mentionPermission controla
destaque/notificação de menções; vazio é público. Não ignora mute, ignore ou alcance.
Perda de permissão visual usa fallback sem apagar regra ou seleção de título existente.
A atualização0.11 abaixo acrescenta editor/persistência por ID separado dessas regras.

## Atualização0.11

Catálogo presets.json define permission por preset; vazio é público. Defaults
shirokaicore.style.gold e shirokaicore.style.sunset autorizam esses presets. O
administrador pode usar nós clan.style específicos. HEX de clã exige adicionalmente
shirokaicore.clan.color.hex; gradiente exige shirokaicore.clan.color.gradient.
Nome/sigla usam permissão atual do líder e somente ele pode salvar presets; roleActions
continua controlando identidade textual, convites e membros. Sem LuckPerms não há
premium automático; básicos vazios funcionam para jogador/líder.

shirokaicore.command.clans.inspect permite list/inspect;
shirokaicore.command.clans.identity permite correção administrativa rename/tag.
Permissões administrativas não autorizam escolhas pessoais arbitrárias, transferência
forçada ou remoção de clãs. clan.animation controla os presets animados0.16; clan.tag.edit pode ser
configurado como requisito de preset, não é um bypass da política de cargos.

## Extras0.12

Com listagem opt-in, shirokaicore.tab.hidden oculta o sujeito apenas na lista de
quem não tem shirokaicore.tab.seehidden. O próprio sujeito continua listado para si,
e um baseline falso externo não é revelado. Nós podem ser alterados em tab-extras.json.
Isto não autoriza esconder entidades ou contornar mod de vanish. Nametags/BELOW_NAME
são políticas administrativas globais/por conexão, sem comando de autoelevação.
Bossbar usa condição permission/grupo/mundos de TAB; nenhuma permissão é concedida
pelo Core. Diagnóstico continua em shirokaicore.command.tab.inspect.

## Chat visual0.13

/g e /l usam as mesmas permissões de GLOBAL/LOCAL e não ignoram moderação.
chat-appearance.json.colorPermission vazio permite cores &0..f/&r para todos. Para
restringir, configure shirokaicore.chat.color ou outro nó e conceda via LuckPerms.
A permissão é revalidada por envio; não é bypass de filtro/cooldown/mute/ignore.
Cor padrão do canal e avisos [+]/[-] são política do servidor, sem elevar autoridade.

0.17.1: nenhum nó novo. Resumo e cargo respeitam audiência existente do chat;
slot vazio não pode ser anunciado nem usando os bypass de moderação.
