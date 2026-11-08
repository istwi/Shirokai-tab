# Auditoria de compatibilidade

## Confirmado por consulta a fontes

- Java local: OpenJDK 21.0.13.
- Maven Fabric lista Loom 1.10.5, Loader 0.16.14 e Fabric API 0.116.17+1.21.1.
- Build declara Minecraft 1.21.1 e Yarn 1.21.1+build.3. Resultado do build no PROJECT_STATUS.
- Fonte oficial LuckPerms expõe `getUserManager`, `getEventBus`, `getContextManager`.
  Isso NÃO confirma ainda a versão de runtime Fabric nem implementa integração.
- README oficial da branch 1.21 do TextPlaceholderAPI documenta `%modid:type%`
  e `%modid:type/data%`. Registro e resolução ainda precisam de auditoria de código.

## Não confirmado

- Cobblemon 1.8.3: consulta a `api.modrinth.com` bloqueada pelo proxy (CONNECT 403).
  Não afirmar compatibilidade binária nem substituir silenciosamente por outra versão.
- Impactor: URL candidata `ImpactorMC/Impactor/main/README.md` retornou 404.
  Confirmar repositório/documentação oficial e APIs para moedas `impactor:dollars` e `impactor:tokens`.
- Shirokai Dex, dexrewards e cobbledollars: sem artefatos ou código no checkout.
- LuckPerms Fabric: validar distribuição para MC 1.21.1, assinatura e callbacks da versão escolhida.
- SQLite: driver, versão e política de empacotamento ainda não definidos.

## Fontes consultadas

- https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml
- https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml
- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml
- https://raw.githubusercontent.com/LuckPerms/LuckPerms/master/api/src/main/java/net/luckperms/api/LuckPerms.java
- https://raw.githubusercontent.com/Patbox/TextPlaceholderAPI/1.21/README.md

Não usar fontes de branch móvel como evidência de compatibilidade de uma versão específica.
Nenhuma integração externa foi implementada ou testada nesta etapa.


## Atualização 0.2.0-dev

- Build e bootstrap operacional confirmados no Fabric1.21.1/Loader0.16.14/API0.116.17.
- CommandRegistrationCallback.EVENT e ServerLifecycleEvents SERVER_STARTING/SERVER_STOPPED
  conferidos nos source JARs das dependências exatas e exercitados no servidor real.
- SQLite JDBC3.49.1.0 obtido do Maven Central e incluído via Loom include. SQL real
  VACUUM INTO, WAL, user_version e rollback exercitados nos testes. Driver carregou no dev server.
- LuckPerms API5.4 source JAR Maven Central auditado. getUserManager/getUser,
  getCachedData/getPermissionData/checkPermission e Tristate usados e compilados.
  Teste de contrato com proxies verifica revogação, UNDEFINED e usuário ausente;
  não equivale a testar a distribuição LuckPerms Fabric em runtime.
- Gson2.10.1 usa leitor estrito e adapter JsonElement para não aceitar JSON leniente.
- Nenhum adaptador Impactor/Cobblemon/Dex/PAPI foi implementado ainda.


## Atualização 0.3.0-dev

Assinaturas Yarn1.21.1+build.3 conferidas com javap antes do uso:
GenericContainerScreenHandler(type,syncId,PlayerInventory,Inventory,rows), onSlotClick,
quickMove, canUse, onClosed; SimpleNamedScreenHandlerFactory; DataComponentTypes
CUSTOM_NAME/LORE; LoreComponent(List<Text>); ServerPlayerEntity.openHandledScreen.
JOIN/DISCONNECT de ServerPlayConnectionEvents e END_SERVER_TICK conferidos nos
source JARs Fabric da versão fixada. Compilação não equivale a inspeção visual cliente.
Teste de GUI usa source set smoke separado, com jogadores sintéticos e conexão
sem transporte; ele não está no JAR de produção nem substitui login/rede de cliente.


## Atualização0.4

ClanCommands compila com Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME) e
ServerStatHandler.getStat na versãoYarn fixada. LuckPerms UserManager.loadUser(UUID)
prepara dados offline sem bloquear a thread do servidor; sem integração Fabric real
instalada ainda não há validação de contextos/expiração do mod no servidor Shirokai.
Testes do serviço usam provedor de permissões de teste e SQLite real.


## Atualização0.5

Menus reutilizam GenericContainerScreenHandler/ScreenHandler vanilla já auditados
na versãoYarn fixa. Não foram adicionados protocolos personalizados ou mod obrigatório
no cliente. Teste ClanMenuSmoke exerce handler real com ServerPlayerEntity sintéticos;
login/rede/renderização de cliente real e runtime LuckPerms continuam não validados.


## Atualização0.6

ServerMessageEvents.ALLOW_CHAT_MESSAGE/ALLOW_COMMAND_MESSAGE conferidos no source
JAR Fabric-message-api da versão fixada. Assinaturas Yarn de SentMessage.of,
SignedMessage.withoutUnsigned, getSignedContent, MessageType.params,
ServerPlayerEntity.sendChatMessage e shouldFilterMessagesSentTo auditadas com javap.
MessageCommand.register bytecode confirmou msg→targets→message e redirects tell/w.
Detecção de callback usa MessageCommand.class.getName(), válido após remapeamento.
MessageArgumentType.getMessage/EntityArgumentType.getPlayers usados somente na
ponte vanilla; exige um destinatário e converte corpo para texto literal.
Teste captura pacotes no servidor sintético, sem comprovar assinatura criptográfica real.


## Atualização0.7

Assinaturas Yarn1.21.1+build.3 auditadas com javap antes de uso: PlayerListHeaderS2CPacket,
PlayerListS2CPacket actions/entries/Entry e Scoreboard/Team. Entries é private final:
Mixin @Mutable/@Accessor aplica somente setter da lista, sem hooks em lógica vanilla.
Refmap gerado e incluído; Minecraft permanece dependência exata1.21.1.
LuckPerms API5.4 sources confirmam CachedMetaData.getPrefix/getSuffix e Group.getWeight;
contrato compilado/testado, sem afirmar runtime Fabric/contextos reais.
Team/HoverEvent precisa de registries Minecraft; JUnit puro dá IllegalAccessError
no bootstrap do jar nomeado. Cenários de ownership foram mantidos e migrados ao
source set smoke no servidor Fabric, com bootstrap/transformações reais.


## Atualização0.8 — MC1.21.1/Yarn1.21.1+build.3

Auditados com javap antes de implementar: ScoreboardObjective(Scoreboard,String,
ScoreboardCriterion,Text,RenderType,boolean,NumberFormat);
ScoreboardObjectiveUpdateS2CPacket(objective,int) ADD/REMOVE/UPDATE_MODE;
ScoreboardDisplayS2CPacket(slot,nullableObjective), getSlot/getName;
ScoreboardScoreUpdateS2CPacket(holder,objective,int,Optional<Text>,Optional<NumberFormat>);
ScoreboardScoreResetS2CPacket(holder,nullableObjective); BlankNumberFormat.INSTANCE.
HUD vanilla renderScoreboardSidebar filtra/ordena e limita15entradas. Os limites
128/256 de título/linha são política conservadora do Core, não limite em pixels.

Bytecode de ServerCommonNetworkHandler.sendPacket confirma delegação para
send(Packet,PacketCallbacks), que chama ClientConnection.send(packet,callbacks,flush).
Mixin HEAD em send apenas observa scoreboard/bundles; não cancela nem altera pacotes.
Refmap acompanha os dois mixins. BundleS2CPacket.getPackets compila/executa na versão fixa.
ScoreboardDisplaySlot inclui SIDEBAR e TEAM_*; Core cede conservadoramente a qualquer
sidebar colorido externo. Objetivos privados não entram no scoreboard global.
A detecção não cobre mods que contornam/reescrevem essa rota; ver SCOREBOARD.md.
Testes Fabric usam conexão sintética; não afirmam renderização/autenticação real.
