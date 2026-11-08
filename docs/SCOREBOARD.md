# Sidebar nativo — 0.8.0-dev

`modules.scoreboard` ativa o módulo. O padrão é true; a chave ausente em core.json
antigo recebe o default em memória. Desabilitar módulo exige reinício.
`config/shirokai-core/scoreboard.json` é criado somente se ausente; arquivo existente
nunca é sobrescrito. Schema1, até256KiB, updateTicks5..1200, até50layouts.

```json
{
  "schemaVersion": 1,
  "updateTicks": 20,
  "layouts": [{
    "id": "default",
    "priority": 0,
    "condition": {"worlds": []},
    "title": "<gold>✦ SHIROKAI ✦</gold>",
    "lines": [
      {"id": "title", "condition": {"worlds": []}, "text": "Título: %shirokai:selected_title%"},
      {"id": "clan", "condition": {"worlds": []}, "text": "Clã: %shirokai:clan_tag%"},
      {"id": "online", "condition": {"worlds": []}, "text": "Online: %shirokai:online%"},
      {"id": "money", "condition": {"worlds": []}, "text": "PokéDollars: %shirokai:dollars%"}
    ]
  }]
}
```

IDs seguem `[a-z][a-z0-9_-]{0,63}` e não podem se repetir dentro de seu escopo.
Layouts ordenam por prioridade decrescente (±1000000), depois ID. Um layout `default`
sem condições é obrigatório. `condition` aceita permission, group e worlds, com a
mesma semântica do TAB: todas as condições devem passar; grupo primário sem caixa;
worlds vazio aceita todas as dimensões. Condições também existem por linha.
Linhas aparecem na ordem da lista; a seleção é refeita no intervalo, incluindo
mudanças de mundo, grupo e permissão. IDs não são nomes de jogadores nem comandos.

São permitidas até15linhas por layout, incluindo as condicionais, conforme o limite
do HUD vanilla1.21.1. Título tem limite local128pontos de código e linha256; isso
é uma política do Core, não promessa de largura em pixels. O cliente pode cortar
conteúdo conforme resolução/escala/fonte. Linhas idênticas ou vazias continuam
independentes. Scores descendentes controlam ordem; BlankNumberFormat oculta números.
Não usamos teams de formatação nem jogadores falsos. O TAB pode gerenciar seus
teams opt-in independentemente.

Templates usam StyleEngine/STYLES.md, inclusive HEX, gradientes e animações de
styles.json. Dados dinâmicos são literais. Placeholders: player, viewer, uuid,
online, world, ping, selected_title, clan, clan_tag, group, luckperms:prefix/suffix;
demais tokens seguem o serviço nativo. Dados de dinheiro/Pokédex/ginásios usam os adaptadores opcionais0.9 descritos em
INTEGRATIONS.md; sem provider/conta/objetivo carregado, exibem unavailablePlaceholder.
`selected_title` usa exatamente o mesmo TitleService/selected_visual do TAB.
Não há seleção separada de título nem mudança de grupo para aparência.

O frame é validado integralmente antes dos envios. Configuração candidata de
scoreboard e styles é compilada antes do reload de todos os módulos; qualquer
falha preserva todos os snapshots. Mudanças de layout/título/linhas removem sobras
e enviam somente componentes/scores alterados; não recriam o objetivo por frame.
Falha de dado dinâmico preserva a última visão válida, com diagnóstico limitado.
Join, reload, mudança de visibilidade e eventos de título/clã invalidam a visão;
as demais alterações são observadas no intervalo configurado. Sem consultas SQLite
por frame: preferências carregam no join e gravam só ao alternar.

## Visibilidade e animações

- `/sidebar` ou `/sidebar toggle`: alterna somente a própria visibilidade.
- `/sidebar refresh`: solicita atualização (sem recriar objetivos inalterados).
- `/shirokai:sidebar` tem os mesmos subcomandos e serve como alternativa se o alias
  curto já pertence a outro mod. Ambos preservam comandos já registrados.
- `/scoreboard` vanilla permanece intacto.
- `/shirokaicore scoreboard`: contadores de viewers, objetivos, pacotes, conflitos e erros;
  exige `shirokaicore.command.scoreboard.inspect`, com o fallback administrativo existente.

`scoreboard_enabled` persiste no schema5 existente, independente de animations_enabled.
Sem preferência carregada ou banco disponível, sidebar permanece oculto, evitando
sobrepor a escolha salva. Toggles concorrentes são serializados e só aparecem após
gravação bem-sucedida. Reconexão descarta estado de rede e carrega a preferência.
Com TAB ativo, scoreboard usa a mesma preferência viva de `/tab animations`.
Sem TAB, usa a preferência de animação carregada no join (não fornece novo comando
para animações). Preferência desconhecida congela frame0; não muda a escolha de visibilidade.

## Propriedade e convivência

Cada conexão recebe objetivo privado com nome aleatório de16caracteres e entradas
únicas. Nada é registrado no scoreboard global/salvo do mundo. Não tomamos sidebar
de outro mod: qualquer slot SIDEBAR ou TEAM_* externo faz o Core ceder por segurança,
mesmo que o team colorido não esteja ativo para esse viewer. LIST/BELOW_NAME não bloqueiam.

Mixin observador em `ServerCommonNetworkHandler.send(Packet,PacketCallbacks)` mantém
estado dos objetivos e slots enviados por conexão, inclusive bundles e envios anteriores
a JOIN. Ele não cancela ou reescreve pacotes. Envios próprios são distinguidos por
identidade; acesso ao ledger é sincronizado, sem consultar jogo/SQLite no hook.
O scoreboard global também é consultado para detectar slots/nomes externos.
Ao liberar o sidebar externo, o Core pode voltar a mostrar um objetivo novo.

Se outro sistema usar o nome exato do objetivo privado (colisão/replacement), o Core
abandona esse objetivo e suspende sua sidebar nessa conexão até reconectar. Não
remove a substituição externa. Se o ledger exceder4096nomes conhecidos, também
suspende a conexão conservadoramente. Limpeza normal, toggle e stop removem somente
objetivo próprio ainda reconhecido; remoção do objetivo limpa seu slot no cliente,
sem enviar um clear global que poderia apagar a sidebar externa. Disconnect limpa
cache/ledger; chaves de conexão são fracas. SERVER_STOPPING encerra recursos próprios.

Limitação: mods que enviam diretamente por ClientConnection, substituem o send()
sem chamar vanilla, ou alteram pacotes depois do observador podem escapar da detecção.
Não há garantia universal de compatibilidade; nesses casos escolha um dono e desative
modules.scoreboard. O Core não bloqueia o pacote externo nem corrige mods que usam
APIs de jogo fora da thread do servidor. Observação de pacote não prova entrega de rede.

## Evidências

ScoreboardPlanTest valida arquivo/defaults/limites, condições e compilação de todos
os templates. ScoreboardSmoke roda no Fabric real e captura pacotes com jogadores
sintéticos: título compartilhado, linhas vazias/repetidas, ordenação/números ocultos,
valor literal/fallback, diffs, remoção/reload, frames inválidos, animações/freeze,
visibilidade/reconexão, comandos, sidebar externo/bundle/slots coloridos e colisão.
O runner também rejeita scoreboard inválido dentro de reload atômico real.
Resultados executados ficam em PROJECT_STATUS.md. Isso não valida aparência,
login, autenticação, transporte real ou compatibilidade com todos os mods.
