# Shirokai Core

Leia `docs/REQUIREMENTS.md`, `docs/PROJECT_STATUS.md` e `docs/NEXT_STEPS.md` antes de alterar o projeto.
Trabalhe no checkout existente; não crie worktree sem solicitação explícita.
Java 21, Minecraft 1.21.1, Fabric. Nunca use APIs Bukkit/Paper.
Não invente integrações: confirme artefatos e assinaturas da versão exata antes de implementar.
Não marque esqueletos como funcionalidades entregues. Atualize documentação e resultados dos testes.
Não publique releases ou configuração de ambiente sem solicitação.
Não acesse APIs Minecraft fora da thread do servidor. Persistência deve usar executor próprio.
Título visual único compartilhado entre TAB e scoreboard; no máximo duas identificações totais no chat.
Clãs gratuitos; sigla manual; cooldowns independentes; redução de limite nunca expulsa membros.
Transferência exige aceitação; exclusão exige confirmação; toda ação revalidada no servidor.
Build: `bash ./gradlew --no-daemon build`; na nuvem: `python3 tools/gradle_cloud.py build`.

Smoke real isolado: `python3 tools/server_smoke.py --accept-eula`; a aceitação atual
foi autorizada explicitamente pelo usuário. Não alterar online-mode para contornar rede.
Configuração de runtime: docs/CONFIGURATION.md; novos schemas precisam de testes
transacionais e de restart. Ao atualizar artefatos, atualizar SHA/versão em PROJECT_STATUS.

Título/perfil: docs/TITLES.md. Usar uma única selected_visual persistente para TAB/scoreboard.
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
Text do cache é read-only. Ordenação teams é opt-in, não adotar team por prefixo de nome.
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
