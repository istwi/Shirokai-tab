# Estado real do projeto — 0.9.0-dev

## Situação

Integrações opcionais reais adicionadas: Cobblemon1.8.3, Impactor5.3.5,
Text Placeholder API2.4.2, Shirokai Dex pelos seus handlers públicos e ginásios
vanilla. LuckPerms Fabric5.4.140 foi exercitado. Os módulos anteriores, incluindo
TAB/sidebar, permanecem; o projeto completo NÃO está entregue. Estilos nas demais
superfícies, ferramentas de clãs, recursos adicionais e cliente real seguem em NEXT_STEPS.md.

## Implementado

- integrations.json estrito, módulo opcional e diagnóstico `/shirokaicore integrations`.
  Flags exigem reinício; mapeamentos/provider/intervalo passam pelo reload atômico.
  Respostas de sessões/configurações antigas são descartadas. Nenhuma migração SQLite;
  schema permanece5 e preferências de visibilidade/animação continuam independentes.
- Pokédex oficial consulta catálogo implementado e registros SEEN/OWNED. Snapshots
  coalescidos na thread do servidor, invalidação por evento oficial e fila limitada.
  Não altera registros nem transforma obtenções em estatística de capturas selvagens.
- Impactor consulta cada moeda explicitamente, verifica existência da conta, identidade
  de dono/moeda/provider e lê BigDecimal. I/O assíncrono, timeout, cache por jogador/moeda;
  sem deposit/withdraw/save, comandos de economia, leitura privada ou fallback primário.
- PAPI importa handlers reais com argumentos e exporta só shirokai em Text.literal.
  Proteção de reentrância, thread/contexto e ownership; não sobrescreve provider externo
  nem remove sua substituição no stop. Dados não viram markup/ações no StyleEngine.
- Shirokai Dex delega a percent/total/registered/seen da mesma coleção. Provider explícito
  troca as quatro métricas juntas; ausência não troca silenciosamente a fonte. Sem
  depender de classes/banco internos, copiar cálculo ou modificar o repositório do Dex.
- Ginásios leem objetivo/score existentes sem mudar seus valores/slots. Placeholders
  adicionais de clãs usam associação e permissões reais do líder.
- Runtime inicia em SERVER_STARTED para respeitar o enable de LuckPerms Fabric durante
  SERVER_STARTING. Essa ordem foi corrigida após falha real de API ainda não carregada.
  Sem LP, fallback administrativo anterior; LP presente com falha não vira fallback op.
- Core funciona sem instalar qualquer mod opcional. Compatibilidade de outras versões
  não é presumida; versões desconhecidas de API ficam UNSUPPORTED.

## Versões e validação

Contratos/configuração: INTEGRATIONS.md; auditoria: API_COMPATIBILITY.md.
Manifesto tools/integration-mods.json fixa URLs/hashes dos JARs originais.

- Build/JUnit:80testes,0falhas,0erros,0ignorados no build concluído desta etapa.
- Smoke sem opcionais:40checks aprovados em3boots reais, incluindo restart, schema futuro e reload inválido.
- Smoke com mods reais:12checks aprovados na rodada final ampliada, com encerramento limpo.
- Testes ampliados incluem aquisição por givepokemonother oficial, atualização não-zero
  da Pokédex/Dex, duas moedas/saldos, ausência de moeda/conta, provider consistente,
  handlers com argumentos/valores literais, LP grant/revoke/prefixo e limpeza de cache.
- As fixtures registram jogador/conexão sintéticos no mundo descartável. Isso não é login
  autenticado. Test-only mixin/accessors/conexão não são incluídos no JAR de produção.
- O smoke comum preserva testes de clãs/menus/títulos/chat/aliases/TAB/sidebar/reloads,
  backups, restart e schema futuro. A checagem agora aguarda a conclusão assíncrona do
  SQLite antes de pedir READY/FAILED, sem alterar o banco ou tornar assertions opcionais.

Resultados atuais: build/reports/tests/test/index.html, build/server-smoke/result.json,
build/integration-smoke/result.json e logs de cada execução. Manifesto de dependências
aninhadas extraídas sem alteração fica em build/integration-dependencies.json.

Gradle8.12.1/Loom1.10.5/Yarn1.21.1+build.3/Fabric API0.116.17 preservados. Kotlin plugin
2.2.20 habilita metadata remapping. O perfil opcional usa Loader0.17.2 exigido por
Cobblemon/Dex e ICU71.1. Core sem Cobblemon mantém mínimo Loader0.16.14. TLS e hashes
foram preservados; nenhuma política/configuração do ambiente foi publicada/reaplicada.

## Artefato

`build/libs/shirokai-core-0.9.0-dev.jar`, de desenvolvimento, NÃO release estável.
SHA-256: `d0cc5be06a978eddf331c6720e90c7dc0a311da471859920ce558b83e0f3c759`.
JAR inspecionado: mixins/refmap corretos, nenhum código/fixture smoke e somente SQLite aninhado.
Build final padrão aprovado;80JUnit e os resultados40+12 acima pertencem a esta etapa.

## Limites e pendências

As versões auditadas foram exercitadas com JARs reais e jogador sintético, mas cliente
real/renderização/carga/login e contextos LuckPerms com conexão autenticada continuam
pendentes. Authlib/Yggdrasil no cloud apresentou UnknownHostException ao buscar chaves;
online-mode permaneceu ativo. Não declarar autenticação validada por esses smokes.

EconomyService.account é fetch-or-create: hasAccount evita consultar contas já ausentes,
mas não cria atomicidade contra exclusão concorrente por outro mod. Não há transação
entre bancos/providers nem nova garantia de persistência. Fonte/coleção da Pokédex é
explícita; Dex pode retornar seu N/D enquanto carrega ou nega acesso. Novas versões do
Dex dependem de preservar o handler público, não da presença de uma classe interna.

Sidebar continua cedendo a objetivos externos; teams do TAB seguem opt-in. Contextos
reais/assinaturas de chat, estilos em chat/menus/títulos/clãs, administração avançada e
recursos adicionais de TAB continuam pendentes. Execução remota de CI não verificada.

## GitHub

Base desta etapa:3afcddf, em istwi/Shirokai-tab. Preservar histórico e salvar progresso
em main conforme autorização. Nenhuma release ou configuração de ambiente publicada.
