# Integrações opcionais — 0.9.0-dev

O Core continua funcionando sem Cobblemon, Impactor, Shirokai Dex, PAPI ou LuckPerms.
Nenhum deles é embutido ou exigido como dependência obrigatória. O driver SQLite
continua incluído. Instale as distribuições originais e suas dependências no servidor.
As classes de cada API ficam isoladas e só são ativadas quando o mod suportado existe.

O runtime inicia em SERVER_STARTED: LuckPerms Fabric termina de habilitar a API durante
SERVER_STARTING. Não usar fallback administrativo vanilla quando LuckPerms está presente
mas falha; falha da API mantém os consumidores bloqueados. Sem LuckPerms, permanece o
fallback administrativo vanilla nível4, sem conceder permissões de gameplay.

## Versões auditadas

| Integração | Distribuição/API auditada | Uso |
|---|---|---|
| Cobblemon | Fabric1.8.3+1.21.1 | Catálogo implementado e Pokédex oficial, eventos de mudança |
| Impactor | Fabric5.3.5+1.21.1 / API economy5.3.5 | Contas existentes e saldos de cada moeda |
| Text Placeholder API | 2.4.2+1.21 | Importar handlers externos e exportar valores nativos |
| Shirokai Dex | release0.4.0-alpha.1+mc1.21.1-cobblemon1.8.3 | Handlers públicos shirokaidex via PAPI; sem dependência de classes internas |
| LuckPerms | Fabric5.4.140 / API5.4 | Permissão/metadata reais; grants/revokes exercitados |
| Scoreboard vanilla | Minecraft1.21.1/Yarn1.21.1+build.3 | Leitura do objetivo de ginásios, sem modificar seus scores/slots |

Cobblemon1.8.3 exige Loader0.17.2+ e Fabric API0.116.6+1.21.1; Language Kotlin está
aninhado na distribuição. O Core mantém seu mínimo0.16.14 para servidores sem Cobblemon.
PAPI também pode vir aninhado em outro mod; a versão efetiva precisa ser a suportada.
Cobblemon/Impactor/PAPI desconhecidos ficam UNSUPPORTED, sem tentar uma API presumida.
O Dex continua em desenvolvimento: seu contrato é o handler registrado, não uma cópia
ou leitura de arquivos/banco internos. Uma futura versão sem esse handler fica indisponível.

## Configuração

`modules.integrations` em core.json recebe defaulttrue em memória quando ausente.
`config/shirokai-core/integrations.json` é criado somente se ausente, com validação
estrita e limite64KiB:

```json
{
  "schemaVersion": 1,
  "placeholderApi": true,
  "cobblemon": true,
  "impactor": true,
  "shirokaiDex": true,
  "refreshTicks": 100,
  "dollarsCurrency": "impactor:dollars",
  "tokensCurrency": "impactor:tokens",
  "gymsObjective": "gyms",
  "dexCollection": "national",
  "pokedexProvider": "cobblemon"
}
```

Moedas são IDs namespaced diferentes. Nunca usa moeda primária como fallback de dollars
ou tokens. Objective usa nome real até16caracteres; collection segue o ID público do Dex.
refreshTicks20..1200 controla snapshots/consultas assíncronas; não é um polling por frame.
Flags de ativação e modules.integrations exigem reinício. Mapeamentos/intervalo/source
aceitam reload atômico; configuração candidata inválida preserva todos os snapshots.
Mudança de mapeamento descarta caches e respostas antigas de operações em andamento.

## Dados disponíveis

| Placeholder | Origem |
|---|---|
| `%shirokai:dollars%` | Saldo da conta existente na moeda dollarsCurrency |
| `%shirokai:tokens%` | Saldo da conta existente na moeda tokensCurrency |
| `%shirokai:pokedex_total%` | Número de espécies implementadas ou total da coleção Dex escolhida |
| `%shirokai:pokedex_owned%` | Espécies obtidas oficialmente ou registered da coleção Dex |
| `%shirokai:pokedex_seen%` | Espécies vistas oficialmente ou seen da coleção Dex |
| `%shirokai:pokedex_percent%` | Progresso do provider explicitamente escolhido |
| `%shirokai:dex_percent%` | `%shirokaidex:percent national%` (ou dexCollection configurada) |
| `%shirokai:gyms%` | Score existente no objetivo gymsObjective; objetivo/score ausente não vira zero |
| `%shirokai:clan_members%` | Snapshot real da associação nativa |
| `%shirokai:clan_limit%` | Limite atual do líder, após preparar permissões offline |
| `%shirokai:clan%`, `%shirokai:clan_tag%` | Clã nativo real |
| `%shirokai:group%`, `%luckperms:prefix%`, `%luckperms:suffix%` | API de permissões atual |

world/ping e as chaves nativas anteriores também estão disponíveis. TAB/sidebar usam o
mesmo serviço para as novas métricas. Administração pode consultar o próprio contexto
com `/shirokaicore placeholders <template>`; console não inventa um jogador para dados
individuais. `/shirokaicore integrations` informa versões, sessões, fila, falhas, provider
selecionado e IDs de moedas; exige shirokaicore.command.integrations.inspect.

### Pokédex e Dex

Com pokedexProvider=cobblemon, conta espécies de PokemonSpecies.getImplemented e consulta
getSpeciesRecord/hasAtLeast(SEEN/OWNED). Não escreve registros nem chama obtain/encounter
para alterar progresso. O percentual é owned/total×100 com duas casas e ponto decimal,
sem símbolo%, removendo zeros finais. Denominador vazio é indisponível, não0%. Formas
não contam separadamente neste resumo. Dados extras de datapacks seguem o registro real;
a integração não autentica capturas nem reconstrói histórico shiny/selvagem.

Com pokedexProvider=shirokaidex, TODAS as quatro métricas delegam a percent/total/registered/
seen da mesma dexCollection, preservando a semântica e formatação desse provider.
Não mistura percentual de uma coleção com denominador de outra. Não há fallback silencioso
para Cobblemon se o Dex estiver ausente, sem permissão, carregando ou sem a coleção.
O Dex pode devolver seu próprio N/D enquanto prepara o snapshot. Sua permissão de coleção
continua valendo. `%shirokai:dex_percent%` sempre consulta esse contrato, independentemente
da fonte escolhida para pokedex_*. O Core não modifica o repositório/implementação do Dex.

Mudanças POKEDEX_DATA_CHANGED_POST invalidam o snapshot; consulta roda na thread do servidor,
coalescida e limitada a um jogador da fila por tick. Há refresh periódico para dados externos.

### Economia

Consultas usam EconomyService.currencies().currency(Key) e hasAccount antes de account.
A API account é fetch-or-create: a verificação evita chamar para contas já ausentes,
mas não oferece operação atômica contra uma exclusão concorrente feita por outro mod.
O Core não deposita, saca, transfere, salva contas nem lê arquivos privados do Impactor.
Não declara transação distribuída ou garantia nova de persistência do provider.

Leitura e validação de identidade de moeda/dono/provider ocorrem na thread do servidor;
futures de I/O são assíncronos, com timeout15s, coalescidos por jogador/moeda. Valores são
BigDecimal plain, sem arredondar para double nem adivinhar símbolo/locale. Erro, conta/moeda
ausente e cache ainda não carregado usam unavailablePlaceholder. Visões podem ficar até
refreshTicks desatualizadas; erro de refresh remove o valor, e disconnect/reload invalida
respostas antigas. Não há consulta de economia por linha/frame.

### PAPI: importação e exportação

Usa Placeholders.parsePlaceholder/PlaceholderContext.of e handlers da API2.4.2.
Templates nativos e StyleEngine aceitam argumentos reais, por exemplo:

```
%player:objective gyms%
%shirokaidex:percent national%
%dexrewards:player_dex_percent%
%cobbledollars:balance%
```

Os dois últimos só funcionam se seus handlers reais estiverem instalados. Não são
aliases fictícios para outro mod. Importação aceita IDs e argumentos limitados; resultado
vira String literal antes de passar ao StyleEngine, removendo ações/estilos externos.
Não interpreta markup ou comandos que apareçam nos dados.

Exporta somente o namespace próprio shirokai, preservando handlers já registrados.
Callbacks rejeitam thread/contexto inadequado e argumentos não suportados. Proteção de
reentrância impede ciclos entre providers. No stop, remove somente os handlers cuja
identidade ainda pertence ao Core; substituições externas permanecem. Nada sobrescreve
namespaces LuckPerms, dexrewards ou cobbledollars.

## Verificação reproduzível

```
python3 tools/gradle_cloud.py build smokeClasses --max-workers=2
python3 tools/server_smoke.py --accept-eula
python3 tools/prepare_integration_mods.py
python3 tools/integration_smoke.py --accept-eula
```

Manifesto de artefatos fixados: tools/integration-mods.json (URLs e SHA256; hashes originais
SHA512 também preservados quando disponíveis). A preparação verifica TLS/checksum e extrai
JARs aninhados sem modificar bytes para que o Loom consiga carregar o runtime de desenvolvimento.
A versão mais nova de cada mod aninhado é selecionada no conjunto fixado; dependências/manifesto
resultantes ficam em build. Arquivos existentes diferentes não são sobrescritos.

Perfil integrationSmoke usa Loader0.17.2, dependências originais, Kotlin2.2.20 para remapear
metadata do Cobblemon e ICU71.1 conforme sua distribuição; as versões básicas de Gradle,
Loom, Yarn e Fabric API permanecem. Proxy/truststore são repassados ao JavaExec mantendo TLS.
Nenhuma configuração de ambiente é publicada e nenhum servidor de produção é acessado.

JUnit cobre mapeamentos/validação/restart, argumentos externos, ciclos e percentual.
Smoke comum verifica operação sem mods opcionais e reload inválido. Smoke de integrações
roda os mods reais em mundo temporário, registra somente um jogador sintético no fixture
de teste e usa comando oficial givepokemonother para testar obtenção (não captura selvagem).
Testa PAPI real, duas moedas/saldos, ausência de conta/moeda, LuckPerms real, Pokédex e Dex.
Resultados da execução atual em PROJECT_STATUS.md e build/integration-smoke/result.json.

Não valida login/autenticação, renderização cliente, contextos LuckPerms de jogadores
realmente conectados, carga ou compatibilidade com versões não auditadas. O authlib do
runtime cloud pode falhar ao buscar chaves Yggdrasil por DNS; online-mode não é desativado.
