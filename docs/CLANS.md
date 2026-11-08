# Clãs nativos — 0.5.0-dev

Núcleo por comandos e menus nativos; sigla integrada ao /perfil como identificação CLAN.
Presets estáticos, editor textual e administração inicial estão descritos na atualização0.11 abaixo; animações e reparos relacionais avançados seguem pendentes. Chat de clã está integrado ao módulo chat0.6.

## Regras implementadas

- Criação gratuita: nenhuma API de economia é chamada.
- /clan create <nome> cria nome e ID UUID estável; não gera sigla.
  O líder escolhe a sigla manualmente com /clan tag <sigla>.
- Nome e sigla são únicos sem diferenciar caixa, com limites, regex e reservas configuráveis.
- Cada UUID pertence a no máximo um clã, imposto também pelo banco.
- Membros são identificados por UUID. Nomes são usados apenas para resolver jogadores online.
- ROLE LEADER deriva de um único leader_uuid; OFFICER/MEMBER são cargos internos.
- Líder pode convidar, expulsar, promover, rebaixar, alterar identidade e solicitar transferências.
- Oficial pode convidar/cancelar e expulsar membros; não expulsa outro oficial ou líder.
- Membro pode sair; líder precisa transferir ou confirmar exclusão.
- Matriz de ações internas é configurável em roleActions; liderança e hierarquia são invariantes.
- Nome/sigla têm cooldowns independentes, iniciados somente em alteração bem-sucedida.
- Convites persistem, expiram, permitem recusa/cancelamento, têm cooldown por remetente/alvo
  e limite de pendências por clã e destinatário. Convites ativos são informados no join.
- Capacidade é checada na transação de aceitação, a partir de permissões do líder
  resolvidas na thread do servidor imediatamente antes de enfileirar a operação.
  A liderança é conferida novamente no banco; se mudou, a entrada é rejeitada para tentar de novo.
- Perda de VIP/limite menor não expulsa ninguém: somente novas entradas são bloqueadas.
- Aceitar convites simultâneos não ultrapassa a capacidade e não duplica associação.
- Transferência exige aceitação pelo sucessor membro, dentro do prazo; nunca ocorre por offline.
- Transferência revalida líder/membro e mantém todos os membros, mesmo acima do novo limite.
- Exclusão exige /clan disband e depois /clan disband confirm no prazo e pelo mesmo líder.
- Troca de líder invalida confirmação de exclusão antiga. Alvo que saiu/foi expulso
  perde a solicitação pendente de transferência.
- Histórico e auditoria são gravados na mesma transação; falha reverte a operação.

## Configuração

`config/shirokai-core/clans.json`, schemaVersion=1, gerado apenas se ausente.
Campos desconhecidos/tipos incorretos são rejeitados. Campos ausentes usam defaults
em memória, exceto schemaVersion obrigatório. Participa do reload atômico com core/titles.

| Campo | Default / significado |
|---|---|
| creationEnabled | true |
| minimumPlaySeconds | 0; usa PLAY_TIME do Minecraft dividido por20 |
| creationCooldownSeconds | 86400 |
| nameCooldownSeconds | 604800 |
| tagCooldownSeconds | 604800 |
| minimumNameLength / maximumNameLength | 3 /32 |
| minimumTagLength / maximumTagLength | 2 /5 |
| namePattern | [A-Za-z0-9 _-]+ |
| tagPattern | [A-Za-z0-9]+ |
| reservedNames | admin, staff, shirokai; comparação sem caixa |
| defaultLimit | 20 |
| permissionLimits | shirokaicore.clan.limit.30→30, .40→40, .50→50 |
| invitationSeconds | 300 |
| invitationCooldownSeconds | 30, por remetente/alvo |
| maximumInvitations | 20, por clã e destinatário |
| confirmationSeconds | 60, transferência/exclusão |
| roleActions | LEADER: invite/kick/promote/demote/rename/tag; OFFICER: invite/kick; MEMBER: vazio |

Sempre usa o maior limite permitido, pelo menos defaultLimit. Não há custo configurável:
criação/alterações são gratuitas. Alterar clans em core.json exige reinício.
Aumentar/reduzir limites em clans.json aceita reload; não remove membros existentes.
Renomear um clã preserva ID, membros, convites, progresso e histórico.

## Comandos

```
/clan
/clan menu
/clan info
/clan members
/clan create <nome>
/clan tag <sigla>
/clan rename <nome>
/clan invite <jogador online ou UUID>
/clan invites
/clan accept <nome do clã>
/clan decline <nome do clã>
/clan cancel <jogador online ou UUID>
/clan leave
/clan kick <jogador online ou UUID>
/clan promote <jogador online ou UUID>
/clan demote <jogador online ou UUID>
/clan transfer <jogador online ou UUID>
/clan transferaccept
/clan transferdecline
/clan disband
/clan disband confirm
/clan disband cancel
```

Criação exige shirokaicore.clan.create no LuckPerms. Sem LuckPerms o fallback administrativo
não concede essa permissão de gameplay. Limites usam shirokaicore.clan.limit.<n> configurados.
Usuário offline do líder é preparado pela API LuckPerms antes de avaliar limite;
integração com a distribuição Fabric/contextos reais continua pendente de teste.

Comando conflitante /clan de outro mod é preservado e o Core registra aviso sem sobrescrever.
Feedback usa texto literal, sem interpretar formatação/comandos enviados pelo jogador.
UUIDs podem ser usados para alvos offline; convites/transferências ainda obedecem prazo.

## Banco e testes

Schema3 adiciona clans, clan_members, clan_invites, cooldowns, transferências,
confirmações e histórico. FK diferida composta garante líder membro do próprio clã;
leader_uuid único por linha e PK global de membro impedem múltipla associação.
Migração de schema1/2 gera backup consistente antes da transação; dados de títulos/prefs são preservados.
Schema futuro é rejeitado sem reset. Não há SQL por tick para clãs.

Testes usam SQLite real e provedor de permissões de teste para simular VIP/expiração.
Smoke do componente roda no Fabric real, sem autenticação falsa nem desativação de online-mode.
Isso não equivale a testar LuckPerms Fabric instalado, interfaces de clãs ou múltiplos clientes.


## Menus0.5

/clan e /clan menu abrem menu vanilla 9x6. Páginas de perfil, membros/cargos,
convites recebidos, jogadores online para convidar, gestão do membro e confirmações.
Página de membros usa UUID para alvos offline e nome somente para quem está online.
Convites: clique esquerdo aceita, direito recusa; capacidade/validade são checadas novamente.
Transferência: líder escolhe membro e confirma solicitação; destinatário ainda deve
aceitar em seu próprio menu. Exclusão: primeiro clique registra pedido e abre outra tela;
segundo clique confirma. Cancelar também revoga o pedido no banco.
Nome/sigla têm editor textual desde0.11; instruções e segurança na atualização abaixo.
Convites enviados podem ser cancelados por comando ou pela página adicionada em0.11.

Menus são somente leitura de itens, com bloqueio de swaps/shift/throw/clone/craft,
outro usuário e tela fechada. Toda ação com clã é vinculada ao ID mostrado na tela:
se o jogador mudou de clã, o banco rejeita a ação antiga dentro da transação.
Mesmo se um botão antigo estiver visível após mudar cargo/configuração, a ação usa
configuração e cargo atuais, sem confiar no estado visual anterior.

roleActions é mapa de LEADER/OFFICER/MEMBER para listas de invite/kick/promote/demote/rename/tag.
A chave roleActions substitui a matriz inteira quando fornecida; omita para usar defaults.
Pode restringir líder nas ações configuráveis; transferir/excluir continuam exclusivas do líder.
Ninguém pode expulsar/rebaixar líder, operar contra outro clã ou agir sobre si mesmo
por comandos de alvo. Não líder só pode gerir alvos de hierarquia inferior: configurar
promote para oficial permite promover membro; não autoriza rebaixar/expulsar um par.

## Identificação nativa de clã

Depois de escolher sigla, o perfil oferece um título CLAN derivado: id clan_<UUID>.
Nome segue o clã e display é [SIGLA]; não há concessão manual nem dados fictícios.
Participa das mesmas duas vagas totais de chat (ROLE, SPECIAL, CLAN nessa ordem),
ou pode ser o título visual único compartilhado pelos futuros TAB/scoreboard.
Mudança de sigla atualiza display preservando o ID estável.

Sair, ser expulso ou excluir clã limpa a seleção correspondente na mesma transação
que remove a associação. Save de seleção também verifica associação/tag no SQL,
impedindo que uma tela antiga reequipe uma sigla depois da saída. Cache online atualiza
por eventos do serviço, sem ler SQLite por tick. Desconexão remove cache.
Dados de clã aparecem no cabeçalho do /perfil mesmo antes de escolher sigla.
Títulos CLAN continuam proibidos em titles.json: somente pertencimento nativo os produz.


/clan chat [mensagem] usa o mesmo gate de mute/ignore/spam do restante do Core;
sem mensagem, seleciona CLAN. Sair do clã bloqueia envio, sem fallback público.
Só membros do mesmo UUID de clã recebem. Ver CHAT.md para configurações e limitações.


## Apresentação0.10

Templates administrativos CLAN_NAME e CLAN_TAG são independentes e envolvem valores
literais, com regras de grupo/permissão/mundo. Cartão do menu usa contexto do líder;
identificação equipada usa contexto do jogador. Não há editor/escolha persistida de
estilo do próprio clã ainda. Escopo, autorização e confirmações anteriores permanecem.

## Atualização0.11

Presets persistidos de nome/sigla, independentes e somente por líder, em
CUSTOMIZATION.md. Cores básicas gratuitas; HEX/gradientes com permissões e fallback
sem apagar ID. Transferir liderança preserva escolhas e reavalia permissões do sucessor.

O menu agora oferece editor textual para nome/sigla: digite no campo da bigorna e
clique no papel de entrada à esquerda para salvar; Esc cancela. O resultado de forja
não é usado: nenhum XP/material é cobrado. Ícones não entram no inventário, inclusive
ao fechar. Todas as formas de mover itens são bloqueadas. O campo exibido funciona
como CAS: alterações concorrentes pedem reabrir; o outro campo continua independente.
Revalida UUID do clã, cargo, config, validade/unicidade e cooldown na transação.
Aparência/interação com cliente real ainda precisa ser verificada.

Página Convites enviados lista destinatários por UUID/nome online e prazo. Clique
cancela usando o mesmo serviço, que revalida cargo/config/clã. Convites vencidos não
aparecem. Todos os membros podem consultar a lista do próprio clã; somente cargos
autorizados a invite podem cancelar. Convites recebidos/aceitação continuam intactos.

Administração inicial, separada das ações de jogador:

- /shirokaicore clans list [page]: 20itens por página, começando em1.
- /shirokaicore clans inspect <id>: UUID ou nome sem espaço; UUID é recomendado.
- /shirokaicore clans rename <id> <nome>
- /shirokaicore clans tag <id> <sigla>

Consulta exige shirokaicore.command.clans.inspect. Correção exige
shirokaicore.command.clans.identity, revalidada após leitura assíncrona e antes de
enfileirar. Correção ignora a espera do cooldown, mas preserva seus prazos, membros,
seleções e liderança. Valida regras/unicidade, CAS do campo e auditoria atômica;
não força exclusão/transferência nem repara integridade relacional automaticamente.
Console usa o fallback administrativo existente; clan_history identifica console pelo
UUID zero, admin_audit mantém actor_uuid NULL. Falha de auditoria reverte correção.
