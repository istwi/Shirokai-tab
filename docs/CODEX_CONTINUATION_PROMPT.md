# Prompt para continuar o Shirokai Core em outro Codex

Copie o texto abaixo para o outro Codex, com acesso ao repositório
`istwi/Shirokai-tab` na branch `main`.

---

Continue o desenvolvimento do Shirokai Core no repositório
https://github.com/istwi/Shirokai-tab, a partir da versão mais recente
da branch main. O projeto ainda não está concluído. Trabalhe no checkout existente
e preserve alterações locais e o histórico remoto. Confira git status, branch e
remoto antes de sincronizar; não use reset destrutivo, force push ou reescrita de
histórico. Está autorizado salvar o progresso com commits e push para main.
Não publique releases nem alterações de configuração do ambiente na nuvem.

## Primeiro, recupere o contexto

Leia integralmente AGENTS.md, docs/REQUIREMENTS.md, docs/PROJECT_STATUS.md e
docs/NEXT_STEPS.md. Leia também docs/API_COMPATIBILITY.md, FEATURE_MATRIX.md,
CONFIGURATION.md, PERMISSIONS.md, TEST_PLAN.md, TAB.md e STYLES.md antes das
respectivas mudanças. Os caminhos abreviados desta lista ficam em docs/.
REQUIREMENTS.md contém o pedido original completo; este resumo não o substitui.
Se o estado atual tiver avançado, use o código e as evidências mais recentes e
atualize este documento. Não reaplique etapas já concluídas.

## Ponto de partida atualizado

Versão0.10.0-dev adiciona presentation.json e composição de estilos em cabeçalhos de
chat, menus de perfil/clã e identificações. TAB/sidebar usam o mesmo título formatado.
Menções do chat normal são notificações separadas; corpo/assinatura/máscara não mudam.
Mensagens system podem colorir menções sem reanalisar markup. Regras condicionais são
configuração administrativa, NÃO escolhas individuais persistidas. Ver PRESENTATION.md.

Integrações0.9 e demais módulos preservados. Schema permanece5. Testes/evidências/SHA
atuais em PROJECT_STATUS.md. Validação atual:84JUnit sem falhas/erros/ignorados e42checks
em3boots Fabric; resultado dos providers em PROJECT_STATUS. Não reaplicar fases anteriores nem setup de ambiente.

## Próxima entrega

Siga NEXT_STEPS.md: escolhas persistidas de presets por jogador/clã, nome/sigla
independentes, comandos/GUI/autorização com escopo e CAS. Preservar o ID escolhido
quando permissão expira, mostrando fallback sem apagar preferências. Exige migração
transacional/restart se mudar schema; dados do jogador nunca viram templates/markup.
Depois administração/editor de clãs e recursos adicionais de TAB.

Preserve apresentação: rich slots removem click/hover/insertion, não alteram Text do
cache, e cabeçalho não duplica as duas identificações. Corpo assinado continua no
mesmo SentMessage; notificações só após roteamento e nunca revelam menção filtrada.
HoverEvent precisa de registries reais: seus testes rodam no smoke Fabric.

Preserve integrações opcionais/isoladas, moeda explícita, fonte/coleção consistente
e handlers PAPI próprios sem substituir terceiros. Comandos de testes adicionais:
python3 tools/prepare_integration_mods.py e python3 tools/integration_smoke.py --accept-eula.
Não alterar JARs/checksums/TLS para passar. Core inicia em SERVER_STARTED após LP.

Objetivos da scoreboard continuam privados, fora do board salvo do mundo. Observer
respeita SIDEBAR/TEAM_* externo; colisão exata revoga ownership até reconectar.
Não cancelar/reescrever pacotes externos, preservar /scoreboard vanilla/aliases.

## Regras que devem permanecer verdadeiras

- Java21, Minecraft1.21.1 e Fabric; nenhuma API Bukkit/Paper. Confirme versões e APIs
  externas antes de implementar. LuckPerms é a fonte de grupos/permissões; escolhas
  visuais nunca alteram grupos reais. Moedas: impactor:dollars e impactor:tokens.
- Um único título visual compartilhado entre TAB e scoreboard. No chat, no máximo
  duas identificações totais entre ROLE, SPECIAL e CLAN, com ordenação configurada.
- Clãs gratuitos, nome e sigla manuais independentes, cooldowns separados. Redução
  de capacidade nunca expulsa membros; apenas bloqueia novas entradas. Transferir
  liderança exige aceitação do destinatário; excluir exige confirmação explícita.
- Toda ação de menu revalida autorização no servidor. Preserve escopo do clã
  exibido contra callbacks antigos e revisão otimista de perfis. O namespace
  clan_ é reservado à associação real; não conceda clãs fictícios como títulos.
- APIs de jogo/rede na thread do servidor; SQLite em executor próprio, sem consulta
  por tick. Migrações transacionais com backup e testes de upgrade/restart, sem
  apagar dados nem aceitar schema futuro silenciosamente.
- Preserve corpo, assinatura e filtro do chat normal; mensagens system de comandos
  não são assinadas. Não interprete markup escrito pelo jogador nem converta canal
  staff/clã indisponível para global. Moderação cobre aliases e datapack reload.
- Dados dinâmicos entram literalmente no StyleEngine. Text em cache é compartilhado
  e somente leitura. Não adote teams externos pelo nome; limpe os próprios antes
  do save em SERVER_STOPPING. Preserve mixin/refmap do packet PlayerList1.21.1.
- Não invente saldos, progresso de Pokédex ou ginásios. Não anuncie compatibilidade,
  conclusão ou percentuais exatos sem evidências. Consulte os requisitos completos.

## Compilação e verificação

Ferramentas fixadas: Gradle8.12.1, Loom1.10.5, Yarn1.21.1+build.3,
Fabric Loader0.16.14 e Fabric API0.116.17+1.21.1. Confira build.gradle,
gradle.properties e wrapper antes de alterar versões.

Na nuvem atual:

```sh
python3 tools/gradle_cloud.py build smokeClasses
python3 tools/server_smoke.py --accept-eula
```

O helper configura JDK/proxy/CA sem expor credenciais. O JDK atual está em
/workspace/toolchains/jdk-21.0.13+9; em outro ambiente, prepare Java21 completo
conforme o helper e a documentação. Fora desta nuvem, com JDK21 configurado:

```sh
bash ./gradlew --no-daemon build smokeClasses
```

O usuário já aceitou explicitamente a EULA para os testes de desenvolvimento.
Não desative online-mode para contornar falhas de rede/autenticação.
O servidor smoke é isolado; -PshirokaiRunDir deve receber caminho relativo.
O source set smoke é habilitado por -PprofileSmoke e não pode entrar no JAR final.
Testes que dependem das transformações Fabric devem rodar no smoke apropriado,
sem desabilitar cenários para fazer a suíte passar.

Artefato atual: build/libs/shirokai-core-0.10.0-dev.jar. Relatórios ficam em
build/reports/tests/test/index.html e build/server-smoke/result.json.
CI de build/upload de JAR está configurado em .github/workflows/build.yml;
não afirme que a execução remota passou sem verificá-la.

## Como entregar o próximo progresso

Implemente e teste comportamentos reais, preservando regressões. Atualize
PROJECT_STATUS, NEXT_STEPS, FEATURE_MATRIX, API_COMPATIBILITY, CONFIGURATION,
PERMISSIONS, TEST_PLAN e CHANGELOG conforme o escopo, com resultados executados,
limitações e versão/SHA do novo artefato quando aplicável. Atualize este prompt
se o ponto de continuidade mudar.

Revise o diff, execute os checks pertinentes e faça commit/push para main sem
reescrever histórico. Confirme que HEAD local corresponde ao remoto. Código,
testes, configuração versionável e documentação vão ao Git; mundos, banco de
runtime, caches, build e segredos ficam fora dele. Não publique release estável
ou ambiente automaticamente. Informe em português o que entregou, quais testes
executou e a próxima pendência concreta. Continue o trabalho autorizado até
concluir a etapa, comunicando bloqueios reais com evidências.
