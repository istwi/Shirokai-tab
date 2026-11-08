# Chat e moderação — 0.10.0-dev

## Canais e comandos

- /global [mensagem], /local [mensagem], /staffchat [mensagem].
- /clan chat [mensagem] ou /chat clan [mensagem].
- /chat global|local|staff [mensagem]; /chat mostra canal atual.
- Sem mensagem, seleciona canal persistente. Com mensagem, envia uma vez sem trocar seleção.
- /chat msg <jogador online ou UUID online> <mensagem>, /chat reply <mensagem>.
- /msg, /tell e /w são moderados quando a implementação vanilla foi assumida pelo Core.
- /r <mensagem> e /ignore <jogador online ou UUID> são aliases quando não há conflito.
- /chat ignore <jogador online ou UUID> alterna ignore; executar novamente remove.

GLOBAL alcança jogadores carregados de todo servidor. LOCAL exige mesma dimensão e
distância euclidiana no raio (incluindo altura). CLAN exige associação nativa atual
igual; STAFF exige permissão tanto do remetente quanto do destinatário a cada envio.
Privado exige ambos online e carregados, com ignore respeitado. /r só aponta para
conversa privada efetivamente entregue; desconexão remove a referência de resposta.

Perder permissão staff ou sair do clã NÃO muda mensagem para global: o envio falha
até o jogador escolher um canal permitido. Alterar o canal padrão na configuração
não troca o canal da sessão em ações de mute/ignore.

## Configuração

config/shirokai-core/chat.json, schemaVersion=1. Criado apenas se ausente, validado
estritamente sem sobrescrever customizações. Campos ausentes usam defaults em memória,
exceto schemaVersion obrigatório. Mapas channelPermissions/nameFormats devem conter
GLOBAL, LOCAL, CLAN, STAFF e PRIVATE quando personalizados.

| Campo | Default |
|---|---|
| defaultChannel | GLOBAL; PRIVATE não pode ser padrão |
| localRange | 100 blocos |
| maximumLength | 256 pontos de código Unicode; chat assinado também tem limites vanilla |
| cooldownMillis | 1000 |
| duplicateWindowSeconds | 30 |
| blockedWords | lista vazia; comparação literal case-insensitive com normalização NFKC |
| channelPermissions | GLOBAL/LOCAL/CLAN/PRIVATE: vazio; STAFF: shirokaicore.chat.staff |
| nameFormats | [GLOBAL]/[LOCAL]/[CLÃ]/[STAFF] {tags}{name}; privado: [PRIVADO] {tags}{name} → {target} |
| takeVanillaPrivateCommands | true; alteração exige reinício |

Variáveis permitidas nos formatos de nome: {channel}, {tags}, {name}, {target}.
Substituição é literal e não recursiva. {tags} contém no máximo as duas identificações
escolhidas, validadas pelo serviço de títulos; não adiciona cargo automaticamente.
Com styles ativo, templates administrativos aceitam cores/HEX/gradientes via StyleEngine (PRESENTATION.md). Mensagem do jogador é sempre
literal, sem eventos clicáveis ou interpretação de tags, JSON ou comandos.

O cabeçalho do remetente tem hover com nome e clique SUGGEST_COMMAND para /chat msg;
nunca RUN_COMMAND. O corpo não herda esse clique. Não há animação de histórico.
Regras por grupo/permissão/dimensão e menções seguras são fornecidas por presentation.json (PRESENTATION.md).

## Assinaturas e entrega

Chat normal é interceptado em ServerMessageEvents.ALLOW_CHAT_MESSAGE. O Core envia
o mesmo SignedMessage, sem decoração unsigned de terceiros, preservando corpo,
assinatura e máscara de filtro, via SentMessage e MessageType.CHAT por destinatário.
Só o nome de apresentação é decorado pelo servidor. A decoração externa usa o formato
CHAT vanilla do cliente (por exemplo <nome> mensagem), não um template de corpo arbitrário.

Mensagens de comandos do Core são GameMessage/system, com cabeçalho estilizado e corpo de caracteres literais (menções podem ganhar cor);
não são apresentadas como mensagens assinadas/reportáveis de jogador. /say e /me
executados por jogadores passam pelo mesmo roteamento/moderação por ALLOW_COMMAND_MESSAGE;
podem ter aparência de chat normal, não a ação vanilla. Console mantém anúncios vanilla.
Testes capturam pacotes sintéticos, mas não validaram assinatura criptográfica/login real.

O evento de broadcast é cancelado após a entrega seletiva, evitando duplicação.
Mods posteriores no mesmo evento podem não receber a mensagem; escolha um proprietário
de chat. Desative modules.chat e reinicie para usar outro sistema. Não removemos mods
nem alteramos configurações de terceiros.

## Ownership de comandos e conflitos

Com takeVanillaPrivateCommands=true, o Core só substitui o callback do /msg reconhecido
como implementação vanilla MessageCommand; /tell e /w redirecionados ao mesmo nó passam
pelo mesmo gate. Detecta pela referência da classe remapeável, não nome hardcoded de Yarn.
Reaplica no registro de comandos após reload de datapacks, para evitar bypass de mute.
Seletores múltiplos não fazem broadcast privado: exige um destinatário.

Comandos de outros mods são preservados. Aliases em conflito podem exigir os comandos
/chat correspondentes. Com takeVanillaPrivateCommands=false, /msg vanilla permanece
independente e não é moderado pelo Core. Comandos próprios de outro mod, envio direto
por outro mod e conflito de ownership precisam de decisão administrativa; não assumir
cobertura global de moderação em canais que o Core não controla.
Console não usa os comandos de chat de jogador; pode moderar por comandos administrativos.

## Proteções

Todas as rotas Core passam por mute, limites, filtro, cooldown e duplicação. Cooldown
e histórico são compartilhados entre canais e privados; histórico é limitado a20 entradas
por sessão e removido no disconnect. Controles/quebras de linha/section sign são rejeitados.
Filtro de palavras usa strings literais, não regex fornecido pelo jogador.

Ignore persiste por UUID (máximo1000 alvos). Bloqueia recepção e conversas privadas;
ignorar a si mesmo é rejeitado. Bypass de ignore permite remetente autorizado ultrapassar
ignore do destinatário; não remove a preferência salva. Sua própria lista continua
impedindo iniciar privado com alguém que você ignorou.

Sem perfil carregado, envio é recusado e destinatário não recebe. Falha de storage
não libera chat vanilla silenciosamente enquanto o módulo Core ativo controla o evento.
Não há SQL por mensagem/tick no roteamento; dados são carregados no join/ações.

## Mute e administração

```
/shirokaicore chat inspect <uuid>
/shirokaicore chat mute <uuid> <seconds> [reason]
/shirokaicore chat unmute <uuid>
```

seconds=0 é permanente; 1..31536000 é temporário. Expiração é conferida a cada envio,
sem job de SQLite por tick. Registro vencido pode continuar para consulta histórica.
Razão padrão Moderação, até256 caracteres sem controles. Mudança e auditoria fazem
parte da mesma transação: falha de auditoria reverte o mute/unmute.
Ações atualizam o cache online após commit; não alteram canal selecionado.
Logs de rejeição têm UUID/motivo e limitação de frequência; não gravam corpo de privados.
Não há histórico completo de mensagens/spy de privados implementado.

Permissões administrativas: shirokaicore.command.chat.inspect/mute/unmute, independentes.
Bypasses explícitos: shirokaicore.chat.bypass.mute, .cooldown (inclui duplicação), .filter,
.ignore. Não são concedidos automaticamente. Sem LuckPerms, somente administração
vanilla nível4 possui fallback; global/local continuam públicos por default.
Bypass não autoriza exceder tamanho ou inserir controles/formatos proibidos.

## Persistência e testes

Schema4: chat_profiles, chat_ignores e chat_mutes. Upgrade1/2/3 cria backup consistente
antes da migração, preservando prefs/títulos/clãs. Dados de sessão/reply/cooldown não
persistem após disconnect/restart; canal escolhido, ignore e mute persistem.

Unitários exercitam roteamento, staff revogado, clã/dimensão, ignore, privado, mute,
spam, texto literal, regressão de default reload e migração/auditoria. ChatSmoke no
Fabric real usa jogadores sintéticos e captura ChatMessage/GameMessage packets.
Teste de aliases roda antes e após END_DATA_PACK_RELOAD com a nova árvore pronta.
Validação com cliente conectado, assinaturas criptográficas e LuckPerms real ainda pendentes.


## Apresentação0.10

Cabeçalhos usam slots de identificação formatados sem duplicar os dois slots selecionados.
Sem animação de histórico e sem acrescentar metadados de cargo fora dessas escolhas.
Chat assinado mantém seu corpo/unsignedContent/máscara; menção usa overlay separado,
somente após entrega autorizada e sem notificar destinatário que usa filtragem.
Mensagens system destacam @Nome conhecido sem interpretar tags nem mudar caracteres.
Detalhes, permissões de menção, formatos e testes atuais em PRESENTATION.md.

## Personalização0.11

Identificações ROLE/SPECIAL usam o preset pessoal TITLE; CLAN usa CLAN_TAG do clã.
Não há identificação extra, alteração de corpo/assinatura/máscara ou reanálise de
markup. Permissão visual é independente de entitlement e dos gates de roteamento.
