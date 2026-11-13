# Shirokai Core

Leia `docs/REQUIREMENTS.md`, `docs/PROJECT_STATUS.md` e `docs/NEXT_STEPS.md` antes de alterar o projeto.
Trabalhe no checkout existente; não crie worktree sem solicitação explícita.
Java 21, Minecraft 1.21.1, Fabric. Nunca use APIs Bukkit/Paper.
Não invente integrações: confirme artefatos e assinaturas da versão exata antes de implementar.
Não marque esqueletos como funcionalidades entregues. Atualize documentação e resultados dos testes.
Não publique releases ou configuração de ambiente sem solicitação.
Não acesse APIs Minecraft fora da thread do servidor. Persistência deve usar executor próprio.
Cargo visual fixo pelo grupo principal LuckPerms em TAB/scoreboard/nametag; no máximo duas identificações totais no chat (cargo + opcional). Leia docs/RANKS.md.
Clãs gratuitos; sigla manual; cooldowns independentes; redução de limite nunca expulsa membros.
Transferência exige aceitação; exclusão exige confirmação; toda ação revalidada no servidor.
Build: `bash ./gradlew --no-daemon build`; na nuvem: `python3 tools/gradle_cloud.py build`.

Smoke real isolado: `python3 tools/server_smoke.py --accept-eula`; a aceitação atual
foi autorizada explicitamente pelo usuário. Não alterar online-mode para contornar rede.
Configuração de runtime: docs/CONFIGURATION.md; novos schemas precisam de testes
transacionais e de restart. Ao atualizar artefatos, atualizar SHA/versão em PROJECT_STATUS.

Título/perfil: docs/TITLES.md e RANKS.md. selected_visual legado é normalizado para null online; ROLE automático não é selecionável.
Concessão/revogação e auditoria são transacionais. Revisão otimista deve impedir perda
em ações concorrentes. Nunca limpar permissões de perfil offline em consulta diagnóstica.
Source set smoke só com -PprofileSmoke; não incluir mod de teste no artefato de produção.

Clãs: docs/CLANS.md. Schema3 usa líder como membro e associação global única.
Nunca expulsar membros ao reduzir limite. Transferência/exclusão exigem confirmações.
O usuário autorizou salvar progresso no GitHub nesta tarefa; não confundir com
publicação de ambiente ou release estável. Preservar código e histórico remotos.

Menus de clã devem usar ClanService.scoped(idExibido), nunca agir silenciosamente
sobre clã novo a partir de tela antiga. Identificação CLAN exige associação real e
limpeza transacional em leave/kick/disband. Namespace clan_ é reservado e não concedível.

Chat: docs/CHAT.md. Nunca converter staff/clã indisponível para global silenciosamente.
Toda rota Core deve usar o mesmo gate. Preservar corpo/assinatura/filtro do chat normal;
comandos system não devem ser apresentados como mensagens assinadas. Não interpretar
markup do jogador. A árvore de aliases privados deve continuar moderada após datapack reload.

TAB/estilos: docs/TAB.md e STYLES.md. Dados dinâmicos nunca são reanalisados como tags.
Text do cache é read-only. Teams de nametag são padrão schema2; ordenação isolada continua opt-in. Não adotar team por prefixo de nome.
Liberar próprios teams em SERVER_STOPPING antes do save; não remover integrantes externos.
Accessor de PlayerList exige manter versão/refmap; não inventar packet list-order1.21.1.

Scoreboard: docs/SCOREBOARD.md. Objetivos privados por conexão, sem salvar no mundo.
Ceder a SIDEBAR/TEAM_* externo; colisão do nome exato revoga ownership até reconectar.
Observer de send não cancela/reinterpreta pacotes. Limpar somente recursos próprios,
preservar /scoreboard vanilla e aliases externos. Visibilidade e animação são independentes.

Integrações: docs/INTEGRATIONS.md. O runtime começa em SERVER_STARTED para respeitar LP.
Adapters opcionais são isolados; nunca usar moeda primária como fallback de dollars/tokens.
PAPI exporta só shirokai, preserva handlers externos e trata resultados como texto literal.
Pokédex é consulta oficial; dados Dex vêm de seus handlers, sem misturar coleções/providers.
Teste com JARs fixados: prepare_integration_mods.py e integration_smoke.py --accept-eula.
Não confundir o jogador sintético registrado na fixture com login/autenticação real.

Apresentação: docs/PRESENTATION.md. Dado dinâmico nunca vira template. Rich slots
removem ações e não alteram Text do cache. Cabeçalho usa no máximo duas identificações totais: cargo fixo + opcional.
signedBody/assinatura/máscara permanecem intactos; notificação de menção só após roteamento
e nunca para destinatário sujeito a filtragem. Regras de presentation.json são políticas
administrativas; escolhas persistidas por ID são definidas em CUSTOMIZATION.md.

Personalização0.11: leia docs/CUSTOMIZATION.md. Schema6 guarda somente IDs de presets;
perda de permissão/remoção de preset nunca apaga a escolha. CLAN_NAME/CLAN_TAG são
independentes; somente líder atual pode escolher. Revalidar UUID do clã no SQL,
revisão CAS e geração do catálogo; sem JDBC por render. Nunca modificar grupos LP.
Editor textual de clã usa anvil sem itens/XP reais: clique no papel de entrada
confirma, Esc cancela; limpar ícones antes de super.onClosed para não dar itens.
Identidade exige snapshot do campo mostrado e cargo/config atuais. Administração
de identidade tem permissão própria, auditoria/CAS e preserva cooldowns/membros.

Extras0.12: leia docs/TAB_EXTRAS.md antes de mudar listagem/nametags/BELOW_NAME/bossbar.
Nametags são padrão schema2; demais extras opt-in em tab-extras.json. enabled exige reinício. UPDATE_LISTED não é
vanish de entidades; nunca reexibir baseline falso/roster removido por terceiros.
Alteração externa revoga ownership da listagem nessa conexão. Nametags usam teams
globais próprios, com efeitos/ordenação documentados; mutação externa revoga ownership.
BELOW_NAME é objetivo privado, cede ao slot externo e colisão exata suspende até
reconectar. Bossbars têm UUID por viewer: coexistem com outras, não removem replacement.
SidebarPackets tem retain/release por consumidor: não apagar evidência antes da última
liberação. Hooks observam somente, sem jogo/IO; ações/caches na thread do servidor.

Chat0.13 (pedido visual do usuário): leia docs/CHAT_APPEARANCE.md. [G] cinza/[L]
amarelo e formato Nick: mensagem usam chat_type sincronizado, preservando signedBody.
Códigos &0..f/&r são interpretação limitada solicitada, sem tags/ações/§; só quando
autorizados e sem filtragem adicionam unsignedContent visual. Cliente pode marcar
modificação; não afirmar assinatura do texto decorado. Nunca enviar essa decoração
para destinatário filtrado ou máscara não-pass-through. Moderar também texto sem
códigos para impedir bypass de filtro/duplicação. /g e /l compartilham o mesmo gate.
Join/quit substituem apenas o broadcast vanilla existente, [+] verde/[-] vermelho;
não duplicar avisos e liberar bridge no stop. Não confundir callback sintético com login real.

Nick0.13.1: nick branco é componente separado em todos os canais/privado/avisos;
não aplicar WHITE ao cabeçalho inteiro ou mudar cor de tags, indicador ou corpo.

Correção0.14.1: nomes de nametag usam namespace livre por instância, não índices
globais skn reutilizados entre sessões. Times antigos não são adotados por nome.
Liberação administrativa de jogador deve permitir criar novo vínculo mesmo com
team antigo vazio ou salvo após parada anormal. Preservar ordenação e ownership.

Menus0.15: leia docs/MENUS.md. Perfil usa menus.json com posições/itens/textos e
ações fixas. Validar slots/tipos/templates na leitura e registries na thread do
servidor ANTES de publicar reload. Telas guardam snapshot do layout exibido.
0.16 reativa clã/editor por padrão em arquivos novos; preservar false em arquivos
existentes. Checar flag também após async/na ação de telas antigas. Histórico
exige associação atual ao UUID exibido. Reparos exigem prévia/token/CAS/auditoria;
nunca mover líder, apagar membros ou contornar confirmação de transferência/exclusão.
Personalização por área: personalization.json; frames guardam IDs, não Text/markup
no SQL. ROLE fixo continua administrativo. Vanish opcional: docs/VANISH.md; auditar
versão exata e ordem actor/viewer. Não afirmar vanish sem provider instalado.

Cash0.15.1: cashCurrency opcional e %shirokai:cash% independentes de dollars/tokens.
Nunca criar conta/moeda ao consultar saldo. Legacy sem campo usa impactor:cash;
se ID já mapeado em dollars/tokens, cash=null preserva o mapeamento legado.
Mapeamentos explícitos não podem duplicar IDs. Preservar arquivos e schema1.

Teams0.15.2: ScoreboardTeamPersistence filtra apenas NBT de teams ativos comprovados
por TeamOwnership (instância/aparência/membros), nunca por prefixo. Não remover
players durante save nem omitir teams externos/modificados/mistos. LIVE usa weak
references e libera managers fechados. Testar save + restore + troca de cargo.

Perfil0.16.5: PanelLayout compartilha moldura/15slots com StyleChoiceMenu.
0.16.6: MenuConfig.load reconhece geometria legacy (rows/tagSlots/slots dos
botões), preservando título, itens/textos e tags; backup antes de troca atômica. Preservar clanMenuEnabled e layouts customizados.
Cabeça10/quadro19/seta28; perfil default usa quadro direito para limpar tags.

Hover0.16.10: playerHoverInfo controla tooltips do Core e Entity.getHoverEvent
para ServerPlayerEntity somente, com bridge ativa do chat. Não modificar corpo,
assinatura, máscaras, traduções ou hover de mobs/itens. ChatAppearance schema1
aceita campo ausente como false; true restaura vanilla. Ler CHAT_APPEARANCE.md.

Chat/MOTD0.17: ler docs/CHAT_SHARES_MOTD.md. Tokens não possuem alvo; resolver UUID
do autor somente, após gate e roteamento. Não expandir unsignedContent para
filtrados/máscaras não-pass-through. Saldos usam cache próprio; não criar contas.
MOTD renderiza na thread do servidor e publica snapshot sem IO no Netty; hook
1.21.1 troca apenas description. Não prometer animação contínua no multiplayer.
