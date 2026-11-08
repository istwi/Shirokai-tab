> 0.14.0: schema2 habilita nametags por padrão e promove o padrão schema1 antigo
> em memória. Demais extras continuam opcionais. Veja [RANKS.md](RANKS.md).

# Extras de TAB — 0.12.0-dev

Quatro recursos opcionais do módulo tab, desligados por padrão. Não alteram schemas
de tab.json nem do SQLite (permanece6), seleção de título ou grupos LuckPerms.
Exigem Java21/Fabric/Minecraft1.21.1. Configuração:
`config/shirokai-core/tab-extras.json`, schema1, até64KiB, criada só se ausente.

```json
{
  "schemaVersion": 1,
  "listed": {
    "enabled": false,
    "hiddenPermission": "shirokaicore.tab.hidden",
    "seeHiddenPermission": "shirokaicore.tab.seehidden",
    "sameWorldOnly": false
  },
  "nametags": {
    "enabled": false,
    "prefix": "%shirokai:title_prefix%",
    "suffix": "",
    "visibility": "ALWAYS"
  },
  "belowName": {
    "enabled": false,
    "label": "<gray>ms</gray>",
    "value": "%shirokai:ping%"
  },
  "bossbar": {
    "enabled": false,
    "title": "<gold>✦ SHIROKAI ✦</gold>",
    "progress": "1",
    "color": "YELLOW",
    "style": "PROGRESS",
    "condition": {"worlds": []}
  }
}
```

Campos são obrigatórios, exceto permission/group opcionais dentro de condition.
Campos/tipos/enums desconhecidos são rejeitados. Todos os templates, mesmo de recursos
desligados, compilam antes de aceitar o reload atômico. Cada enabled muda ownership
e exige reinício; demais propriedades aceitam reload junto de styles/tab/configs.
Nenhum arquivo personalizado é sobrescrito. Desligar modules.tab exige reinício.
O updateTicks de tab.json também controla estes recursos.

## Listagem por observador

Com listed.enabled, hiddenPermission no sujeito o oculta da lista de quem não tem
seeHiddenPermission. O próprio sujeito continua se vendo. sameWorldOnly limita
a listagem à dimensão do observador. Ambas as regras somente restringem o baseline
observado: um false/remoção externo nunca vira true por causa da permissão do Core.
As permissões são consultadas novamente a cada atualização, sem alterar grupos LP.

Usa somente UPDATE_LISTED do protocolo1.21.1. Não remove/recria perfis, sessões de
chat, entidades ou ping. Não há campo list-order inventado.
Uma decisão UPDATE_LISTED/ADD/REMOVE externa posterior revoga o controle daquele
sujeito para aquela conexão até reconectar. O Core não recria entradas removidas.
Ao encerrar, restaura somente o baseline que mudou e cuja revisão ainda corresponde.
Estado desconhecido/overflow cede conservadoramente; não assume que todo mundo era listado.

**Isto não é vanish de entidades ou mecanismo de privacidade global.** O perfil pode
continuar conhecido pelo cliente; tracking de entidades, chat, mensagens de entrada,
contagem online e outros plugins não são ocultados. Integração com um mod de vanish
precisa de contrato específico. O Core preserva a listagem que esse mod já enviou.
Não use apenas esta opção para esconder a existência de um usuário.

## Nametags

Prefix/suffix recebem o mesmo StyleEngine/rich slots de títulos/clãs, sem interpretar
markup dos dados e sem alterar Text do cache. Limite128pontos de código por lado.
O nome da conta continua vanilla; os componentes do prefixo/sufixo podem usar
cores/HEX/gradientes e animações dos layouts, conforme a preferência do sujeito.
Contexto é global do sujeito: viewer equivale ao próprio sujeito, não a cada espectador.
visibility aceita ALWAYS, NEVER, HIDE_FOR_OTHER_TEAMS e HIDE_FOR_OWN_TEAM.

Teams são globais e alteram também a ordenação do TAB. Habilitar nametags já autoriza
criar teams próprios, mesmo com manageTeams=false. Rank determinístico usa prioridade
da nameRule/peso LP, nome sem caixa e UUID; nomes skn +13dígitos cabem em16caracteres.
Um team por jogador permite prefixos independentes sem misturar estilos.
Mudanças na lista/rank podem mover vários integrantes; frame inalterado não envia diffs.
Espectadores ainda seguem a precedência do cliente vanilla.

Friendly fire fica permitido, visibilidade de invisíveis amigáveis desativada e
colisão ALWAYS, como no modo de ordenação anterior. Esses efeitos globais são motivo
para o opt-in explícito. Sem nametags, manageTeams mantém os teams compartilhados anteriores.
Não há promessa de ordenação universal quando outros sistemas também têm teams.

Team externo ou nome ocupado não é adotado. Alteração externa nos atributos de um
team de nametag revoga seu ownership; não se sobrescreve nem remove a substituição.
Integrantes externos são preservados. Antes do save, libera somente recursos próprios.
Após crash, teams salvos desconhecidos continuam externos e podem exigir inspeção administrativa.

## Objetivo abaixo do nome

Cada conexão recebe objetivo privado com UUID no nome (16caracteres), fora do
scoreboard salvo do mundo. Label até128pontos; value é um inteiro int32 literal ou
placeholder disponível, sem tags/animação. Default usa ping real em ms.
Não interpreta N/D como0, não inventa valores e não cria jogadores fictícios.
Entradas usam nomes reais dos perfis; sujeitos removidos/ocultos para esse viewer
não recebem novas linhas deste recurso.

Label é comum ao objetivo do viewer; valores são resolvidos no contexto de cada sujeito.
Falha numérica/dado grande preserva o último frame válido e registra diagnóstico.
Mudanças enviam somente objetivo/valores afetados; reset externo de scores força reenvio.
Dados estão sujeitos à frequência de atualização, sem consultas SQLite por frame.

BELOW_NAME externo, seja em pacote/bundle ou scoreboard global, faz o Core ceder.
Ao liberar o slot, pode criar um novo objetivo próprio. Uso externo do nome exato
do objetivo revoga ownership até reconectar; nenhuma remoção da substituição é enviada.
O Core não envia clear global de slot: remove somente objetivo ainda próprio.
Sidebar/LIST são independentes e podem coexistir. O ledger compartilhado de objetivos
tem contagem de consumidores: fechar sidebar não apaga a evidência usada por BELOW_NAME.

## Bossbar

Uma barra própria por viewer, com UUID independente. Não é registrada no bossbarManager
salvo do servidor e não liga música, névoa ou escurecimento do céu.
condition é a mesma condição de TAB (permission/grupo/mundos, todos precisam casar).
Perda de condição remove somente a barra própria; retorno cria outra identidade.
Título aceita rich slots/animações, até256pontos, com preferência de animação do viewer.

progress é decimal explícito0..1, literal ou placeholder disponível, sem tags/animação.
Por exemplo0.75 é75%; porcentagens0..100 não são divididas silenciosamente.
NaN/infinito/N/D/fora de faixa preservam o último frame válido, sem inventar um progresso.
color: PINK/BLUE/RED/GREEN/YELLOW/PURPLE/WHITE.
style: PROGRESS/NOTCHED_6/NOTCHED_10/NOTCHED_12/NOTCHED_20.
Título/progresso/cor/estilo são comparados e atualizados separadamente.

Barras de outros UUIDs coexistem e não são removidas. Qualquer pacote externo com o
UUID exato da barra própria revoga ownership nessa conexão até reconectar, sem remover
a substituição, inclusive no stop. Não reescreve nem cancela pacotes externos.

## Diagnóstico, ciclo de vida e limites

/shirokaicore tab inclui extrasViewers/extraPackets/extraConflicts/extraErrors além
dos contadores anteriores. Não há comando administrativo novo nem novo schema SQL.
Fontes são as mesmas de TAB: títulos compartilhados, clãs, metadados e placeholders
nativos/providers opcionais. Dados dinâmicos entram literalmente.

Observer existente de ServerCommonNetworkHandler.send foi estendido; não há novo
mixin. Observações são sincronizadas, com chaves fracas de conexão e sem APIs de jogo
ou IO no hook; ações de rede/jogo/caches do runtime ficam na thread do servidor.
Ledgers limitam4096objetivos/UUIDs de bossbar e8192perfis observados por conexão;
overflow cede os recursos em vez de adivinhar ownership. Remoção de sessão libera
views; a última referência de objetivo libera seu ledger compartilhado.

Mods que enviam por ClientConnection diretamente, substituem send ou alteram pacotes
depois do observer podem escapar da detecção. Nessas integrações, escolha um único
responsável e desative o recurso conflitante. Não há compatibilidade universal.

Smokes validam protocolos/handlers com conexões sintéticas, não a renderização do
cliente, transporte real, assinatura/autenticação ou carga. O objetivo tem limite
visual de distância/escala vanilla; prefixos longos e barras múltiplas ainda exigem
teste com cliente real. Resultados executados em PROJECT_STATUS.md.
