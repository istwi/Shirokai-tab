> Atualização 0.14.0: cargos ROLE são fixos pelo grupo principal LuckPerms;
> SPECIAL/CLAN são opcionais somente no chat. Veja [RANKS.md](RANKS.md).
> As regras antigas de seleção visual abaixo são históricas e foram substituídas.

# Apresentação de chat, menus e identificações — 0.11.0-dev

O módulo styles agora compila presentation.json junto dos demais snapshots. Esse
arquivo controla templates administrativos e regras por permissão/grupo/dimensão.
Não cria permissões, grupos, títulos, clãs nem novas escolhas persistidas. A regra
premium permanece na configuração quando uma permissão expira: render usa o fallback
enquanto não autorizado e volta à regra quando a permissão retorna.

## Configuração

`config/shirokai-core/presentation.json`, schemaVersion1, até256KiB. Criado apenas
se ausente; nenhum arquivo personalizado é sobrescrito. Todos os campos abaixo são
obrigatórios. Até50regras, IDs únicos, prioridade±1000000. Todas as regras, inclusive
inativas para os jogadores atuais, são compiladas antes de aceitar reload.

```json
{
  "schemaVersion": 1,
  "chatEnabled": true,
  "menusEnabled": true,
  "mentionsEnabled": true,
  "mentionPermission": "",
  "templates": {
    "MENU_NAME": "<aqua>%visual:value%</aqua>",
    "MENU_LORE": "<gray>%visual:value%</gray>",
    "MENU_TITLE": "<gold><bold>%visual:value%</bold></gold>",
    "TITLE": "<gold>%visual:value%</gold>",
    "CLAN_NAME": "<aqua>%visual:value%</aqua>",
    "CLAN_TAG": "<aqua>%visual:value%</aqua>",
    "MENTION": "<yellow>%visual:value%</yellow>"
  },
  "rules": [{
    "id": "vip",
    "priority": 100,
    "condition": {
      "permission": "shirokaicore.visual.vip",
      "group": "vip",
      "worlds": ["minecraft:overworld"]
    },
    "templates": {
      "TITLE": "<gradient:#ffcc33:#fff4b0>%visual:value%</gradient>",
      "CLAN_NAME": "<gradient:#ff0000:#0000ff>%visual:value%</gradient>",
      "CLAN_TAG": "<dark_purple>%visual:value%</dark_purple>"
    },
    "chatFormats": {
      "GLOBAL": "<green>[GLOBAL] {tags}</green><white>{name}</white>"
    }
  }]
}
```

Default real contém rules vazio; o VIP acima é um exemplo que deve usar permissões
reais do servidor. Condições são as mesmas do TAB: todas devem casar; group é o
primário LuckPerms sem distinguir caixa, worlds vazio aceita qualquer dimensão.
As regras ordenam por prioridade decrescente e ID. Para cada superfície/canal,
vence a primeira regra elegível que forneça aquele template; o resto usa defaults.
Uma regra pode fornecer apenas as superfícies/canais que deseja alterar.

Templates visuais devem conter exatamente uma ocorrência de `%visual:value%`,
sem outro placeholder. Isso envolve o dado literal existente, sem reanalisar seu
conteúdo. Chat e menus desta etapa não usam animações; fontes/ações/tags não
allowlisted continuam rejeitadas. Limites do StyleEngine continuam valendo.

`chatEnabled=false` preserva o cabeçalho literal antigo e desativa menções do Core.
`menusEnabled=false` preserva aparência anterior dos menus de perfil/clã. Os estilos
dos títulos em TAB/sidebar continuam independentes dessa opção. `styles=false`
desativa o módulo inteiro como antes (TAB/sidebar dependem dele).

Todas as opções de presentation aceitam reload. A troca do candidato acontece na
thread do servidor, junto de core/chat/styles/catalogs/TAB/sidebar/integrações.
Qualquer template inválido rejeita o candidato inteiro. Menus já abertos mudam quando
forem atualizados/reabertos, sem trocar sua lógica ou reaproveitar ações de tela antiga.

## Chat

`chat.json.nameFormats` agora aceita as tags seguras do StyleEngine quando styles está
ativo, mantendo `{channel}`, `{tags}`, `{name}` e `{target}`. Também são aceitas as
formas `%chat:channel%`, `%chat:tags%`, `%chat:name%` e `%chat:target%`. Outros placeholders,
corpo de mensagem, animações e duplicação de `{tags}` são rejeitados. Não acrescentamos
prefixo/cargo LuckPerms automaticamente além das duas identificações escolhidas.
A regra opcional chatFormats substitui o formato somente naquele canal e condição.
Cabeçalho/corpo usam contexto visual do autor; a notificação usa o do destinatário,
mas mentionPermission é sempre conferida para o remetente.

Compatibilidade: formatos administrativos antigos com texto `<...>` literal precisam
escapar o `<` como `\\<` para não serem lidos como tags. Displays e descrições de títulos,
nomes de jogadores e dados de clãs continuam literais, mesmo quando contêm markup.
Não interpretar MiniMessage colocado nesses dados: configure o estilo fora do valor.

O cabeçalho monta componentes de no máximo duas identificações revalidadas. ROLE,
SPECIAL e CLAN continuam usando as mesmas seleções e ordenação do TitleService.
Somente a identificação do autor recebe hover e SUGGEST_COMMAND para `/chat msg`;
nenhum componente importado de dados mantém click/hover/insertion. O corpo não herda
esse clique e nunca executa comandos escritos pelo jogador.

Mensagens normais mantêm o mesmo SignedMessage, withoutUnsigned, SentMessage, máscara
e corpo utilizados antes. Nenhuma decoração unsigned é adicionada para colorir palavras.
Quando há `@NomeOnline`, o destinatário realmente alcançado pode receber uma notificação
separada na actionbar. Se aquele destinatário usa filtragem, não há notificação derivada
do corpo, evitando revelar uma menção filtrada. Autenticação real não é inferida dos testes.

Mensagens de comandos do Core continuam system/não assinadas. Nelas, menções conhecidas
podem receber cor em componentes separados, sem mudar nenhum caractere do corpo.
`mentionPermission` vazio libera esse recurso; um nó configurado é revalidado para o
remetente. Limites de spam/mute/ignore/canal/alcance vêm antes da apresentação. Não são
enviadas notificações para destinatários fora da rota. Auto-menções não notificam.
Reconhece nomes exatos de1..16caracteres ASCII, sem distinguir caixa, com limites de palavra;
nomes desconhecidos, endereços de email e `@@Nome` permanecem texto comum.

## Títulos, clãs e menus

TITLE aplica o estilo ao display literal da identificação do próprio jogador. CLAN_TAG
aplica o estilo à identificação derivada da associação real, não a título concedível.
A seleção visual permanece única: TAB e sidebar compõem o mesmo Text do TitleService.
Templates externos dessas superfícies ainda podem aplicar seu próprio gradiente ao
conjunto; isso não muda a seleção. PAPI continua exportando valor literal, sem ações.

Perfil usa MENU_TITLE/MENU_NAME/MENU_LORE e mostra o estilo real das entradas de títulos.
Menus de clã usam esses templates para os controles e CLAN_NAME/CLAN_TAG para o cartão
principal, avaliados no contexto do líder. Nome e sigla são superfícies independentes;
nenhuma string de clã é analisada como um template. Regras do título CLAN equipado são
avaliadas no contexto do jogador que o exibe. Não há escolha pessoal persistida de estilo
nem editor de estilo por clã nesta entrega: essa etapa seguinte exige autorização e
persistência próprias, sem apagar a escolha quando VIP expira.

O botão de retorno do perfil mantém a identidade exigida em REQUIREMENTS.md, e o comando
/menu só é usado quando existe. Não muda inventários, escopo de clã, CAS de perfil,
permissões de ações ou confirmações de transferência/exclusão.

## Testes e limitações

PresentationPlanTest cobre JSON/arquivo preservado, regras/fallback, candidatos inválidos,
rejeição de corpo/animações/tags duplicadas e composição literal de Unicode com cores.
O teste de hover requer registries de Minecraft e roda no smoke Fabric, em vez do JUnit cru.
PresentationSmoke cobre componentes/pacotes reais, cores de títulos/clãs, fonte imutável,
remoção de ações, máscara/corpo/signature field preservados, menções filtradas, privado,
canal local, mute, revogação/retorno de estilo sem perda de título e previews nos menus.
Runner também rejeita presentation inválido dentro do reload atômico real.

As conexões são sintéticas: não validam assinatura criptográfica, aparência com cliente,
fonte/escala, notificações em todos os clientes ou carga. Não há atualização de mensagens
antigas nem animação de menu/histórico. Personalização individual de títulos/clãs e
editor/administração avançada seguem pendentes. Resultados atuais em PROJECT_STATUS.md.

## Personalização0.11

A política acima continua administrativa. Agora CUSTOMIZATION.md define escolhas
separadas por ID persistidas no schema6: TITLE pessoal e CLAN_NAME/CLAN_TAG do clã.
Preset ativo envolve o dado literal; sem escolha usa esta política; escolha bloqueada
ou removida usa fallback básico sem apagar ID. Templates de presets compilam no mesmo
candidato de reload. Corpo/assinatura/máscara/gates/limite de duas identificações
continuam intactos. Não confundir animações de layout com animação de preset pessoal.

## Atualização0.13 solicitada pelo usuário

O formato externo do chat passa a Nick: mensagem via MessageType.Decoration
sincronizado; o cabeçalho continua composto pelas duas identificações autorizadas.
Cor padrão por canal não altera signedBody/signature/filterMask/unsignedContent.
Códigos & válidos/autorizados agora permitem unsignedContent visual somente para
não filtrados/máscara pass-through, preservando dados originais e possível indicador
cliente de modificação. Isto é distinto da reanálise de markup/tags, que segue proibida.
Detalhes e opções em CHAT_APPEARANCE.md.

## Estilo fixo por identificação — 0.13.2

Campo opcional `titleTemplates` no objeto raiz de presentation.json (schema1):

```json
"titleTemplates": {
  "owner": "<gradient:#ff3333:#ffcc33><bold>%visual:value%</bold></gradient>",
  "admin": "<gradient:#3355ff:#33ffff><bold>%visual:value%</bold></gradient>"
}
```

As chaves são IDs do titles.json, não grupos primários LP. Cada identificação
é renderizada separadamente em chat, TAB/sidebar e preview do perfil. A entrada
fixa tem precedência sobre presets pessoais e regras por grupo: um jogador com
OWNER e ADMIN exibe cada tag com suas próprias cores. Permissões e seleção das
tags continuam necessárias; isto não equipa cargos automaticamente. Displays
continuam literais, sem markup. Títulos sem entrada mantêm o comportamento anterior;
CLAN usa o estilo próprio de clã. Nenhuma escolha salva é apagada.

Templates exigem exatamente `%visual:value%` uma vez, sem animação/ações. Todos
compilam no candidato transacional de reload, inclusive IDs ainda não usados.
Até500 entradas. Arquivos antigos sem o campo funcionam sem migração/regravação.
Use `/shirokaicore reload`; nenhum reinício é necessário para trocar as cores.
Exemplo completo: examples/presentation-ranks.json. Cores são sugestões editáveis.
