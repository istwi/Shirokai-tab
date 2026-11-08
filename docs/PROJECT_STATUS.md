# Estado real do projeto — 0.8.0-dev

## Situação

Fase7 implementa scoreboard/sidebar real por viewer. Núcleo, títulos/perfil, clãs,
chat, motor de estilos e TAB anteriores preservados. O projeto completo NÃO está
entregue: integrações externas, estilos nas demais superfícies, administração avançada,
recursos adicionais de TAB e cliente real continuam pendentes (NEXT_STEPS.md).
SQLite permanece schema5; visibilidade reutiliza scoreboard_enabled existente.

## Implementado nesta etapa

- Objetivos privados por conexão, linhas reais do protocolo1.21.1 e números ocultos.
  Nada é registrado no scoreboard salvo do mundo; nenhum team de formatação/fake player.
- Layouts por prioridade/permissão/grupo/dimensão e condições por linha, até15linhas.
  Linhas iguais/vazias são independentes e ordenadas. Limites locais128/256pontos de código.
- StyleEngine para HEX/gradientes/animações e valores literais; fallback para dados
  indisponíveis. Não há saldo/Pokédex/ginásios fictícios.
- Mesma selected_visual/TitleService do TAB, sem seleção separada nem alteração de grupos.
- Atualizações por componente/score: sem pacotes quando frame não muda; remoção de
  sobras de linhas/layouts. Frame inválido não publica conteúdo parcial.
- Visibilidade persistida, toggles serializados, carregamento seguro e limpeza no disconnect.
  scoreboard_enabled é independente de animations_enabled. Sem preferências, sidebar oculta.
- Reload de scoreboard/styles compila candidato antes de alterar qualquer snapshot.
  Eventos de títulos/clãs, join, toggle e reload invalidam visão; demais dados usam intervalo.
- Observer Mixin de send(Packet,PacketCallbacks), incluindo bundles/pre-JOIN, sem cancelar
  ou modificar pacotes. Cede a sidebar/slots coloridos externos; colisão do nome exato
  revoga ownership até reconectar. Remove somente recursos próprios ainda reconhecidos.
- /sidebar e /shirokai:sidebar com toggle/refresh; aliases externos e /scoreboard vanilla
  preservados. /shirokaicore scoreboard fornece diagnóstico com permissão própria.
- CoreConfig suporta11módulos sem limite de Map.of; modules.scoreboard ausente usa default
  em memória. Nenhuma migração SQLite ou nova dependência externa.

## Validação desta etapa

- `python3 tools/gradle_cloud.py build smokeClasses --max-workers=2`: PASSOU (build final).
- JUnit: 76 testes executados, 0 falhas, 0 erros, 0 ignorados no build final desta etapa.
- Smoke final:39verificações passaram em3boots reais Fabric, incluindo os testes
  ampliados de animação/comandos/reset e rejeição atômica de candidato scoreboard/core inválido.
- ScoreboardPlanTest cobre defaults/arquivo preservado, tipos/limites/duplicatas,
  condições/prioridade e compilação integral antes do reload.
- ScoreboardSmoke captura pacotes reais com jogadores sintéticos: título igual ao TAB,
  linhas repetidas/vazias, ordem e número oculto, valores literais/fallback, delta de
  uma linha, remoção condicional/reload, frame inválido, frames/freeze, visibilidade e
  toggles concorrentes/reconexão, ownership externo/bundles/slots coloridos/clear/colisão/stop.
- TabPreferenceTest preserva testes de restart e upgrade4→5 das preferências independentes.
- Regressões de chat, aliases/datapack reload, clãs/menus/títulos, backups e schema futuro
  fazem parte do mesmo smoke real; todos os servidores são encerrados com stop.

Resultados atuais: build/test-results/test, build/reports/tests/test/index.html,
build/server-smoke/result.json e logs first-boot/restart/storage-failure.
Resultados históricos0.7:73JUnit/37checks e SHA1f37125b…; não substituem execução atual.

## Artefato

`build/libs/shirokai-core-0.8.0-dev.jar` (desenvolvimento, NÃO release estável).
SHA-256: `c64a7ca236c4a3fb7dea6903cb6cb3a77c535693bba6f8e10c03c34c0f9a3de2`.
JAR inspecionado: ambos os mixins/refmap presentes; nenhuma classe smoke em produção.

## Limitações

Conexões sintéticas validam transformação Fabric, API e pacotes, não entrega/renderização,
login, assinatura criptográfica ou carga. LuckPerms Fabric/contextos reais, Cobblemon,
Impactor, Dex e PAPI continuam sem validação/integração completa. CI remoto não verificado.

Sidebar preserva qualquer slot colorido externo por segurança, mesmo fora do team do
viewer. Colisão exata ou overflow do ledger suspende gestão na conexão. Mods que enviam
por ClientConnection diretamente/contornam o send vanilla podem escapar do observador;
não prometemos compatibilidade universal. Ver SCOREBOARD.md para configuração/limites.
Ordenação teams do TAB é opt-in; listed/vanish, nametags/below-name/bossbar pendentes.
Styles ainda aguarda chat/menus/personalizações de títulos/clãs.

## Continuidade e GitHub

Base sincronizada por fast-forward até528c692, preservando d9099c0 e histórico anterior.
Progresso autorizado em istwi/Shirokai-tab/main, conforme confirmação do proprietário.
Nenhuma release ou configuração do ambiente é publicada por esta etapa.
