# Títulos e perfil — 0.5.0-dev

## Catálogo real

`config/shirokai-core/titles.json` é criado somente se ausente. Schema 1:

```json
{
  "schemaVersion": 1,
  "definitions": {
    "traveler": {
      "id": "traveler",
      "name": "Viajante",
      "display": "[VIAJANTE]",
      "description": "Explore o mundo Shirokai.",
      "category": "SPECIAL",
      "priority": 0,
      "unlockedByDefault": true,
      "tab": true,
      "scoreboard": true,
      "chat": true
    },
    "vip": {
      "id": "vip",
      "name": "VIP",
      "display": "[VIP]",
      "description": "Cargo visual vinculado ao LuckPerms.",
      "category": "ROLE",
      "priority": 100,
      "permission": "group.vip",
      "unlockedByDefault": false,
      "tab": true,
      "scoreboard": true,
      "chat": true
    }
  }
}
```

Apenas traveler é fornecido por padrão. O exemplo VIP deve ser configurado de acordo
com as permissões reais do servidor; o Core nunca altera grupos LuckPerms.
Campos booleanos, categoria, prioridade, ID, nome, display e descrição são obrigatórios.
`permission` pode ser omitida; ROLE exige uma permissão. ID deve coincidir com chave.
Até 500 títulos/256 KiB; texto é literal nesta etapa, sem parser de gradientes/MiniMessage.
Categoria CLAN não pode ser declarada em titles.json: é derivada automaticamente de
pertencimento ao clã nativo com sigla definida. IDs clan_ são reservados; veja CLANS.md.

## Entitlements e seleção

ROLE é desbloqueado pela permissão LuckPerms especificada.
SPECIAL é desbloqueado por default ou por concessão SQLite; se também possuir
permission, ela é obrigatória além da concessão/default. Concessão pode ser permanente
(expiração 0) ou temporária (timestamp de expiração). Concessão explícita expirada
resulta em EXPIRED; o registro permanece para distinguir expiração de bloqueio.
Estados expostos: UNLOCKED, LOCKED, EXPIRED e UNAVAILABLE (permissão ausente).

Há apenas um selected_visual persistente, destinado simultaneamente a TAB e scoreboard.
Somente títulos disponíveis nas duas superfícies podem ocupá-lo. Os renderizadores TAB/scoreboard compartilham o mesmo título; não há duas seleções independentes.

Chat permite no máximo duas identificações totais, inclusive ROLE. Ordem:
ROLE antes de SPECIAL, depois prioridade decrescente e ID para desempate.
Duas identificações especiais são permitidas. Selecionar uma já equipada a remove.
Selecionar título visual não ocupa slot de chat; são escolhas independentes.
Terceira identificação é rejeitada pelo serviço; SQLite também impõe slots 0/1 e unicidade.

Seleções inválidas são removidas ao carregar perfil online e revalidadas no cache
a cada 100 ticks (5s em 20 TPS). A ação de seleção sempre revalida antes de gravar.
Renderização de placeholder revalida imediatamente e nunca mostra título expirado
ou sem permissão. Sem seleção, usa fallback do consumidor/core.json; não há concessão automática.
Consultas administrativas offline não limpam permissões desconhecidas de usuários
LuckPerms não carregados. Grant/revoke offline mudam apenas os dados solicitados.

Não há SQL por tick: o cache é usado para renderização; refresh de persistência
ocorre em join, ações e detecção de seleções inválidas. Disconnect remove o cache.
Eventos LuckPerms específicos ainda não estão assinados; usa revalidação periódica
mais validação imediata nas ações/leituras.

## Comandos

Jogador:

- `/perfil`: abre inventário vanilla 9x6, sem exigir mod do cliente.
- `/perfil titulo <id>`: seleciona título compartilhado.
- `/perfil chat <id>`: equipa/remove identificação.
- `/perfil limpar`: limpa título e identificações, preservando desbloqueios.

Qualquer jogador pode gerenciar o próprio perfil; desbloqueios/permissões continuam
obrigatórios para cada título. Não aceita UUID de terceiros nos comandos de jogador.
O console recebe erro claro ao usar /perfil.

Administração:

- `/shirokaicore titles list`
- `/shirokaicore titles inspect <uuid>`: dados persistentes, sem limpar perfil offline.
- `/shirokaicore titles grant <uuid> <id> [seconds]`: permanente sem seconds;
  temporário de 1 a 31536000 segundos quando informado.
- `/shirokaicore titles revoke <uuid> <id>`

Permissões: shirokaicore.command.titles.inspect/grant/revoke (cada ação separada).
Operações administrativas usam transação com auditoria; falha de auditoria reverte
concessão/revogação. Grant não altera grupo LuckPerms nem remove requisitos de permissão.
Revogar concessão remove esse ID da seleção visual/chat; outros títulos são preservados.

## Menu

Clique esquerdo em título seleciona visual; direito equipa/remove no chat.
Paginação de 36 títulos, estados, seleção e contador n/2. Botão de atualizar consulta
estado real; botão de limpar preserva concessões. Voltar usa minecraft:arrow, nome
amarelo/negrito e lore padronizada; executa /menu como o jogador se o comando existir.
Não eleva permissões e informa quando o menu principal não está disponível.

Itens do menu são somente ícones: movimentação, shift-click, troca hotbar, clone,
throw, quick-craft e pickup-all são ignorados. Slots inválidos, outro jogador e menu
fechado não executam ações. Operações assíncronas não reabrem tela que o jogador já
fechou/trocou. Itens do inventário do jogador não são transferidos para o menu.

O clã atual já aparece no painel. Faltam cargo real e dados do treinador, assim como estilo de
gradientes/animações do motor futuro. A interface atual não simula esses dados.
Validação visual/rede com cliente real ainda é necessária, mesmo após teste do
handler em servidor com jogadores sintéticos.

## Migração e concorrência

Schema SQLite 2 cria title_profiles, title_grants e chat_identifications.
Upgrade de schema1 cria backup consistente `<database>.schema1-<uuid>.bak.db`
antes da transação; preferências e auditoria antigas são preservadas.
Registros usam UUID. Revisão otimista impede duas ações sobre snapshot antigo de
sobrescreverem uma à outra; a ação perdedora recebe pedido de tentar novamente.
Reload valida core.json e titles.json antes de publicar ambos. Catálogo inválido
preserva integralmente estado ativo. Títulos removidos do catálogo têm seleções
invalidadas online; concessões antigas permanecem guardadas, sem serem exibidas.

## Placeholders

%shirokai:selected_title%, %shirokai:chat_tag_1%, %shirokai:chat_tag_2% usam cache
do perfil online e retornam display literal. Dado ausente usa fallback. A sintaxe
é do serviço nativo; registro na Fabric Placeholder API continua pendente.


## Integração de clã0.5

ClanTitle fornece um único título dinâmico do clã atual, sem registrar concessão.
As mesmas regras de duas identificações e título único se aplicam. SQL rejeita
clan_<UUID> de outro clã ou sem sigla; leave/kick/disband removem seleção atomicamente.
Mudanças do serviço de clãs atualizam o cache online; view/equip carregam pertencimento
antes de validar, para não aceitar uma permissão fictícia ou apagar escolha no join
antes de carregar dados. Diagnóstico offline segue sem limpeza baseada em permissão ausente.


## Apresentação0.10

Display/descrição do catálogo continuam literais. presentation.json aplica estilo
TITLE/CLAN_TAG fora do dado, com condições dinâmicas. TAB/sidebar e preview do perfil
compõem esse mesmo título sem alterar a seleção. Revogar permissão visual troca o
estilo por fallback; não revoga título nem apaga a regra configurada. Entitlement do
título continua independente. Escolha pessoal persistida de estilo ainda não existe.
