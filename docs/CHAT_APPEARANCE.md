## Tooltip do jogador — 0.16.10

`playerHoverInfo` em config/shirokai-core/chat-appearance.json controla o tooltip
ao passar o mouse no nome. Padrão false, inclusive em arquivo antigo sem o campo:

```json
"playerHoverInfo": false
```

false remove o tooltip simples do Core e o SHOW_ENTITY vanilla de jogadores
(tipo/UUID), preservando clique para responder, inserção, cores e mensagem.
Nomes vanilla usados em avisos/conquistas também deixam de incluir esse tooltip.
true restaura o comportamento anterior: tooltip simples no Core e vanilla com
informações de entidade. Pode mudar por /shirokaicore reload.

A ponte só fica ativa enquanto o módulo de chat do Core está instalado/ativo.
Mobs/itens não são afetados. Textos anteriores não são reescritos; só novos nomes.
Mods que constroem seu próprio HoverEvent sem usar o nome vanilla não são
interceptados. Isso é uma preferência visual, não remoção do UUID do protocolo.
Corpo/assinatura/máscara de mensagens permanecem intactos; não há reenvio de chat.

# Chat compacto e cores — 0.13.1-dev

Defaults solicitados:

```text
[G] [TAG] Nick: mensagem
[L] [TAG] Nick: mensagem
[+] Nick
[-] Nick
```

GLOBAL usa cinza e LOCAL amarelo no indicador e corpo. Tags selecionadas mantêm
seus próprios estilos; não há cargo extra fora das duas identificações permitidas.
Nick do autor e do destinatário privado recebe branco explícito, separado das cores
do canal, tags e corpo. O fallback sem apresentação também usa componentes de nick
brancos. A correção0.13.1 muda somente essa cor, inclusive nos avisos de conexão. Não há os
delimitadores vanilla `<nome>`; o tipo de chat sincronizado usa `%s: %s`.
Sem tags selecionadas, não há espaço/identificação fictícia.

## Comandos

/g [mensagem] é alternativa a /global [mensagem]; /l [mensagem] a /local [mensagem].
Sem texto seleciona o canal persistente; com texto envia uma vez nesse canal.
Passam pelo mesmo gate de mute, cooldown, filtro, ignore, permissões e alcance.
Aliases externos são preservados; nesse caso use /chat global ou /chat local.
Registro após reload de datapacks mantém os mesmos aliases e moderação.

## Cores do jogador

```text
/l Olá pessoal
/g &7Mensagem cinza
/l &aVerde &7cinza &rvolta ao amarelo
```

Reconhece somente `&0..&9`, `&a..&f` (maiúsculas também) e `&r`.
Reset volta à cor do canal. `&&` representa um & literal; por exemplo `&&7`
mostra &7. Códigos de negrito/ofuscação/HEX, tags, JSON, ações e comandos não são
interpretados. §, controles e quebras de linha continuam proibidos.
Comprimento máximo é conferido no texto original, incluindo códigos.
Filtro e duplicação usam também o texto sem códigos válidos: trocar cor não
contorna cooldown/duplicação e inserir &7 no meio de palavra bloqueada não a libera.
Mensagem vazia após remoção dos códigos é recusada.

## Assinaturas e filtragem

Cores padrão por canal usam os tipos de chat do registry sincronizado de Minecraft
1.21.1. Mantêm signedBody, signature e filterMask originais, sem unsignedContent
adicional. Não se converte chat normal em mensagem system para trocar aparência.
As traduções de decoração possuem formato literal `%s: %s`, sem resource pack/mod
obrigatório no cliente; narração mantém a tradução vanilla conhecida.

Interpretar &7 exige mudar a apresentação visual dos caracteres digitados.
Pela solicitação desta entrega, somente quando houver código/escape válido e
autorizado é adicionado unsignedContent com Text literal colorido. O corpo original
assinado, a assinatura e a máscara permanecem intactos; o cliente pode exibir
indicador de mensagem modificada. Não se afirma assinatura criptográfica do texto
visual substituído.

Essa decoração nunca é enviada a destinatário sujeito a filtragem, nem quando a
mensagem tem máscara diferente de pass-through. Nesses casos os códigos permanecem
literais, mantendo posições/caracteres da máscara original. Menções continuam
notificando somente destinatários alcançados e não sujeitos à filtragem.
Sem código & válido, unsignedContent continua nulo como antes.

Mensagens de comandos/privados do Core continuam system e usam o mesmo formato/
cores permitidas. Não são apresentadas como chat assinado. Dados com tags permanecem
literais e o corpo não herda click/hover do nome do autor.

## Configuração e atualização

`config/shirokai-core/chat-appearance.json`, schema1, até32KiB, gerado apenas se ausente:

```json
{
  "schemaVersion": 1,
  "enabled": true,
  "legacyColors": true,
  "colorPermission": "",
  "connectionMessages": true,
  "bodyColors": {
    "GLOBAL": "gray",
    "LOCAL": "yellow",
    "CLAN": "white",
    "STAFF": "white",
    "PRIVATE": "white"
  }
}
```

Campos são obrigatórios, tipos/campos/cores desconhecidos rejeitados. bodyColors
exige os cinco canais e aceita as16cores Minecraft nomeadas, sem HEX. Cores
administrativas de cabeçalho/tags continuam em chat.json/presentation.json/presets.
colorPermission vazio libera & para todos, conforme o pedido; para restringir,
configure por exemplo `shirokaicore.chat.color` e conceda pelo LuckPerms. A permissão
é revalidada a cada envio; perder permissão mostra códigos literalmente.
legacyColors=false desliga interpretação de &, mantendo cores padrão por canal.
enabled=false volta ao tipo CHAT vanilla e desativa interpretação de cores do
jogador. connectionMessages é independente e controla os avisos de conexão.

/shirokaicore reload valida esta configuração junto de todas as demais; falha
preserva o estado ativo. Nenhum arquivo personalizado é sobrescrito. Em chat.json,
somente os formatos exatamente iguais aos antigos defaults [GLOBAL]/[LOCAL] são
promovidos para [G]/[L] em memória, preservando bytes do arquivo e outros templates.
Formatos personalizados/regras de presentation mantêm precedência; se ainda
estiverem exibindo outro indicador, ajuste-os explicitamente.
Primeira instalação desta versão precisa reiniciar para carregar os novos tipos
sincronizados de chat e mixins. SQLite continua schema6, sem migração de dados.

## Entrada e saída

O broadcast vanilla existente é reaproveitado, sem enviar aviso duplicado:
[+] verde na entrada e [-] vermelho na saída, com nome literal branco.
Inclui a variante vanilla de jogador renomeado, mostrando o nome atual.
Os hooks são locais a PlayerManager.onPlayerConnect e ServerPlayNetworkHandler.cleanUp.
Não interceptam anúncios arbitrários, nem reinterpretam nomes como tags.
Sem módulo chat ativo ou com connectionMessages=false, mantém a mensagem original.
Outro mod que cancela/substitui esse caminho precisa de configuração de ownership;
não há garantia universal com plugins de vanish/avisos.

## Evidências e limites

JUnit cobre parser de cores/escape/reset, config estrita, upgrade em memória sem
overwrite, texto literal/sem ações e bypasses de filtro/duplicação.
Smoke real verifica registries e decoração [G]/[L] sem colchetes angulares, cores
de prefixo/corpo, alcance local, unsigned opcional, permissões revogadas e máscara
intacta sem decoração para filtrados. Executa os callbacks já transformados dos
dois mixins com argumentos sintéticos e verifica preservação de aviso externo.
Não realiza login/logout autenticado nem certifica renderização de cliente real.
Resultados atuais em PROJECT_STATUS.md.

0.17.1: `playerSummary` defaulttrue adiciona resumo no nick/cabeçalho Core com cargo,
Pokédex e ginásios. Flag ausente assume true sem reescrever JSON. Independente de
playerHoverInfo, que permanece para identidade vanilla; false no resumo restaura
o comportamento antigo. Clique de resposta, corpo, assinatura e máscara preservados.
