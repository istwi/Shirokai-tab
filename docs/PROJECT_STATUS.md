# Estado real do projeto — 0.11.0-dev

## Entrega atual

Personalização persistida implementada sobre a apresentação0.10, sem substituir
seleções de títulos, sistema de clãs ou grupos LuckPerms. Catálogo administrativo de
presets com IDs estáveis, permissões e três escopos: TITLE pessoal, CLAN_NAME e CLAN_TAG.
As escolhas de clã são independentes, exclusivas do líder e compartilhadas entre membros.
Detalhes, JSON, comandos e semântica em CUSTOMIZATION.md.

- /estilo e seletores no /perfil e /clan. Preview, paginação, estados explícitos,
  reset por campo, bloqueio de ações de inventário/outro jogador/tela fechada.
- ID salvo é preservado quando permissão expira/preset some; fallback básico permitido
  é renderizado. Recuperação de permissão/reintrodução do ID restaura a escolha.
- Um único título continua compartilhado por TAB/sidebar. Chat tem no máximo duas
  identificações, com corpo/assinatura/máscara/gates intactos. CLAN continua associação real.
- Rich slots aplicam escolhas em títulos e placeholders de nome/sigla sem reinterpretar
  dados ou alterar Text compartilhado. Sem SQL por tick/render; cache por sessão/eventos.
- Schema6 cria escolhas/revisões com FK/cascade do clã, auditoria atômica e CAS.
  Migração1..5 faz backup antes da transação; rollback/futuro/restart são testados.
  Escolhas concorrentes não se sobrescrevem. SQL revalida associação e líder do UUID exato.
- Presets compilam junto dos demais snapshots de reload; inválidos rejeitam o candidato
  inteiro. Telas e writes da geração anterior são recusados. Permissão é verificada
  na thread do servidor antes de enfileirar; não há transação distribuída SQLite/LP.
- Editor textual de nome/sigla por bigorna: digitar e clicar no papel à esquerda salva;
  Esc cancela. Sem forja/material/XP; ícones são limpos antes de fechar. Escopo do clã,
  CAS do campo, cargos/configuração/validade/unicidade e cooldowns são revalidados.
- Convites enviados listados por UUID/prazo, com cancelamento autorizado no menu.
- Administração inicial: list/inspect/rename/tag, permissões separadas e CAS/auditoria.
  Correção ignora espera mas preserva prazos dos cooldowns, membros e liderança.
  Não força transferências/exclusões nem faz reparos relacionais automáticos.

Sem mudanças de dependências externas, protocolo ou mixins de produção. Nenhuma
release ou configuração de ambiente foi publicada/reaplicada nesta tarefa.

## Validação executada

- Build/JUnit: **95 testes, 0 falhas, 0 erros, 0 ignorados** na rodada final.
- Smoke sem opcionais: **44 verificações em 3 boots Fabric**, incluindo editor textual,
  convites enviados, estilos persistidos/menus, pacotes TAB/sidebar, fallback/recuperação,
  catálogo removido/tela antiga/liderança antiga e reload inválido de presets.
- Smoke com mods reais: **14 verificações aprovadas**, incluindo permissão real do
  LuckPerms para preset persistido, perda/fallback/recuperação, consulta administrativa
  pelo console e regressões Cobblemon/Impactor/Dex/PAPI.
- Preparação de integrações: cinco mods oficiais e53dependências aninhadas verificados
  sem alterar bytes, hashes, TLS ou versões.
- JAR inspecionado: versão0.11/refmap presentes, sem classes smoke e somente SQLite aninhado.

Evidências: build/reports/tests/test/index.html, build/server-smoke/result.json,
build/integration-smoke/result.json e logs dos runners. Resultados0.10 históricos:
84JUnit,42checks básicos e12com opcionais; não substituem as verificações atuais.

## Artefato

build/libs/shirokai-core-0.11.0-dev.jar — desenvolvimento, não release estável.
SHA-256: b50e8e7edc83a8888eb32b964eab6595cac3d8452f0c45a23b4bd93a48c8c264.
Base remota antes desta etapa: f3db2d040399253e019e93af5491b6005b610689.
Commit final é informado na entrega; não incluir o próprio hash no seu conteúdo.

## Limitações e continuidade

Projeto completo NÃO está entregue. Presets desta etapa são estáticos: não há
animação pessoal/de clã, transições ou personalização separada de nome de conta/menu.
Animações de layouts anteriores continuam. Administração relacional avançada,
listed/vanish por viewer, nametags, BELOW_NAME e bossbar permanecem em NEXT_STEPS.md.

Jogadores/conexões dos smokes são sintéticos: não validam login autenticado,
assinatura criptográfica válida, renderização real, contextos LP de clientes,
reconexão/mudança de mundo de cliente, escala/actionbar ou carga/conflitos universais.
Neste runtime, authlib registrou UnknownHostException para api.minecraftservices.com
na busca de publickeys; online-mode foi preservado. Não há sessão de cliente
Minecraft autenticado fornecida ao teste. Integrações usam somente APIs públicas
fixadas; moedas/fontes de Pokédex continuam explícitas. CI remoto não foi verificado.

## GitHub

Trabalho autorizado em istwi/Shirokai-tab/main, preservando histórico; commit/push
somente após revisão e checks. Publicação de ambiente e release continuam separadas.
