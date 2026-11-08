# Estado real do projeto — 0.12.0-dev

## Entrega atual

Quatro extras de TAB implementados em tab-extras.json, todos opt-in/desligados por
default. Documentação completa, JSON, efeitos e limites em TAB_EXTRAS.md.

- UPDATE_LISTED por observador: sujeito hidden e viewer seehidden revalidados,
  opção de mesma dimensão, self preservado e baseline falso/roster removido externo
  nunca reexibido. UPDATE_LISTED externo posterior revoga controle naquela conexão.
  Não oculta entidades/chat/contagem online e não é vanish completo.
- Nametags de prefixo/sufixo/visibilidade via teams próprios individuais globais.
  Rank por prioridade/nome/UUID; um team por jogador permite estilos independentes.
  Templates ricos preservam dados literais e título compartilhado. Teams externos,
  nomes ocupados, integrantes externos e mutações de atributos são preservados.
  Friendly fire/colisão/invisíveis têm os efeitos explícitos documentados.
- BELOW_NAME privado por viewer, fora do scoreboard salvo; label e valores int32
  disponíveis, default ping real. Diffs, remoção de linhas e reenvio após reset.
  Não cria jogadores fictícios nem converte dado indisponível em0. Cede a slot externo
  global/pacote/bundle; colisão do nome exato suspende até reconectar, sem deletar replacement.
- Bossbar pessoal com UUID independente, templates/animações/condições por LP/mundo,
  progresso decimal0..1 e diffs de nome/progresso/cor/estilo. Coexiste com outras barras,
  sem registro no bossbarManager salvo ou música/névoa/escurecimento. Colisão de UUID
  cede ao sistema externo até reconectar, inclusive durante limpeza.
- Configs/templates compilam juntos no reload atômico; enabled altera ownership e
  exige reinício. Não sobrescreve configurações personalizadas. Diagnóstico de extras
  está no comando tab existente. Nenhum SQLite por frame/tick; APIs na thread do servidor.
- Observer de send existente cobre roster/bossbar/bundles, sem cancelar ou reescrever
  pacotes. Ledger de objetivos agora tem revisão de BELOW_NAME e retain/release por
  consumidor, preservando evidência de conflito até o último fechar.

Schema SQLite permanece6, tab.json/schema1 e dependências externas permanecem.
Sem novos mixins; o observer existente foi estendido e o refmap mantido.
Personalização/clãs/chat/sidebar e demais recursos0.11 foram preservados.
Nenhuma release ou alteração/publicação de configuração de ambiente nesta tarefa.

## Validação executada

- Build/JUnit: **100 testes,0falhas,0erros,0ignorados** na rodada final.
- Smoke básico: **46 verificações em3boots Fabric**. Inclui os quatro extras, diffs,
  baseline/roster externos, permissão/dimensão sintéticas, BELOW_NAME externo e colisão,
  bossbar alheia/colisão/condição/reload, nametag externa/mutada e limpeza.
  Suítes de títulos/clãs/editor/presets/chat/TAB/sidebar continuam passando.
- Teste antigo de sidebar inicialmente detectou limpeza prematura/compartilhada do
  ledger; retain/release corrigiu o ciclo de vida. A asserção de limpeza foi mantida
  e passou junto da preservação de evidência para consumidores ainda ativos.
- Smoke com mods reais0.12: **14 verificações aprovadas**, incluindo LP real com
  escolha persistida/fallback/recuperação, PAPI, Cobblemon, Impactor, Dex e shutdown.
- Preparação verificou os cinco mods oficiais e53dependências aninhadas fixadas.
- JAR inspecionado: versão0.12 e refmap presentes, sem classes smoke.

Evidências: build/reports/tests/test/index.html, build/server-smoke/result.json,
build/integration-smoke/result.json e logs dos runners. Resultados0.11 são históricos
(95JUnit/44básicos/14opcionais); não substituem os checks após estas mudanças.

## Artefato

build/libs/shirokai-core-0.12.0-dev.jar — desenvolvimento, não release estável.
SHA-256: 73b3542e531f9ea44d22d8bad976ce9ec326930a8a6bff578dce2670cd630fe3.
Base antes desta etapa:422db579bb560489681fdac8019111701dc4d4a8.
Commit final é informado na entrega, sem hash autorreferente neste documento.

## Limitações e próxima etapa

Projeto completo NÃO está entregue. Faltam integração de vanish de entidades,
administração relacional avançada de clãs, animação de presets pessoais/clã e
personalização de outros campos além dos títulos/identidades de clã.

Testes usam jogadores/conexões sintéticos. Não validam login autenticado, assinatura
criptográfica real, renderização/escala/actionbar/nametags/bossbars/BELOW_NAME do
cliente, contextos LP de jogadores conectados, carga ou compatibilidade universal.
A fixture altera a referência de dimensão do jogador sintético; não equivale ao
protocolo completo de transferência de um cliente real. Authlib ainda pode registrar
UnknownHostException para publickeys; online-mode não foi desativado.

Observer não captura mods que contornam send ou reescrevem pacotes depois dele.
Ownership/efeitos globais/overflow e escolha de responsável estão em TAB_EXTRAS.md.
Integrações continuam fixadas/isoladas, moedas distintas e provider/coleção explícitos.
Não há transação distribuída SQLite/LP/providers externos. CI remoto não verificado.

## GitHub

Commit/push autorizados para istwi/Shirokai-tab/main após checks/revisão, preservando
histórico. Releases e publicação de ambiente continuam fora deste trabalho.
