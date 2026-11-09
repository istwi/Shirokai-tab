# Menus configuráveis — 0.15.0-dev

`config/shirokai-core/menus.json` é criado somente quando ausente. Controla o
layout do /perfil; ícones não são itens entregues ao jogador. Não altera títulos,
permissões, concessões ou cargos LuckPerms. Veja examples/menus.json para o arquivo
completo gerado pelo mod.

## Campos

- schemaVersion:1.
- clanMenuEnabled:true é o padrão de arquivos novos na0.16. Reabre /clan, editor
  e histórico. Arquivos existentes com false são preservados; altere para true e
  use /shirokaicore reload. false continua bloqueando as telas, inclusive antigas,
  sem apagar dados nem bloquear os comandos textuais de clã.
- profile.rows:1..6, cada linha tem nove slots.
- profile.title: título do inventário, com cores/HEX/degradês seguros.
- profile.tagSlots: posições das tags, na ordem da paginação. Pelo menos uma posição.
- profile.buttons: os sete controles header, previous, styles, clear, refresh,
  back e next. Cada um define slot, item, name e lore (lista de linhas).
- profile.tags: unlockedItem, lockedItem, name e lore, modelo para as tags listadas.

Slots começam em0: primeira linha0..8, segunda9..17, terceira18..26, até53 em seis
linhas. Nenhum botão pode ocupar slot de outro botão ou das tags. Todos os sete
controles são obrigatórios; header é somente informação. styles só aparece quando
personalização está disponível. Não existem comandos arbitrários configuráveis;
back usa a integração /menu já existente, se instalada.

IDs de itens usam namespace, por exemplo minecraft:player_head, minecraft:diamond,
minecraft:book ou um item de mod instalado. Itens inexistentes, air, slots fora do
inventário, sobrepostos e tipos/campos desconhecidos rejeitam a configuração.

## Exemplo de alteração

Dentro de profile.buttons, altere clear para:

```json
"clear": {
  "slot": 46,
  "item": "minecraft:blaze_rod",
  "name": "<red><bold>Remover minhas tags</bold></red>",
  "lore": [
    "<gray>Clique para remover suas tags opcionais.</gray>",
    "<gray>Seu cargo fixo continua aparecendo.</gray>"
  ]
}
```

O slot46 está livre no padrão. Se escolher outro slot, remova-o de tagSlots ou
mova o botão que já estiver ali. Mudar rows exige adequar TODOS os slots.

## Textos e placeholders

name, lore e title aceitam os mesmos estilos seguros de cor/HEX/degradê do Core,
como `<gold>texto</gold>` e `<gradient:#ff3333:#ffcc33>texto</gradient>`. Sem click,
hover, comandos ou animações. A apresentação do perfil agora é definida por esses
templates; as tags continuam recebendo os estilos de título/clã em presentation.

Valores são inseridos literalmente, sem interpretar markup fornecido pelo jogador:

| Placeholder | Conteúdo |
|---|---|
| %menu:player% | Nome do jogador |
| %menu:rank% | Nome do cargo fixo LuckPerms |
| %menu:clan% | Nome do clã ou Nenhum |
| %menu:count% | Quantidade de escolhas opcionais salvas |
| %menu:page% | Página atual, começando em1 |
| %menu:tag% | Tag com seu estilo atual (modelo profile.tags) |
| %menu:description% | Descrição da tag |
| %menu:availability% | Disponível/Bloqueado/Expirado/Indisponível |
| %menu:category% | Especial ou Clã |
| %menu:selection% | Estado da seleção no chat |

ROLE permanece fora da lista de opções; configurar um menu não concede títulos,
não libera permissões nem permite mudar o cargo fixo.

## Reload e segurança

Após editar, use `/shirokaicore reload` e reabra /perfil. Um menu aberto conserva o
layout mostrado até fechar; seus cliques continuam usando esse layout. Novos menus
usam o layout atualizado. JSON inválido, slot repetido ou item inexistente rejeitam
o reload inteiro e preservam a configuração ativa. Arquivos existentes nunca são
sobrescritos. Limite64KiB; nomes/linhas até512caracteres; lore até12linhas.

Se clanMenuEnabled for desativado por reload, telas de clã/editor abertas são
fechadas; abertura assíncrona e cliques antigos revalidam o bloqueio. O flag existe
para administração, mas o padrão solicitado nesta entrega é false. Em testes de
regressão, o menu legado é habilitado explicitamente apenas nas fixtures.

Não há migração SQLite nem mudanças nos arquivos de TAB/nametag. A investigação
de atualização de teams permanece pausada conforme solicitado.
