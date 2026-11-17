# Validação de desenvolvimento

Base auditada: `025a56b5e3b093751544631895fd28b5c506d0ae`. A consulta de todas as
branches remotas encontrou `main` e `zorykai`, ambas nesse commit. A última mudança
é uma variante visual **binária** do Core 0.18.0-dev; não há fonte mais nova em outra
branch. Holograms é um novo subprojeto, sem converter ou substituir o Core.

Java 21.0.13, Minecraft 1.21.1, Yarn 1.21.1+build.3, Loom 1.10.5, Gradle 8.12.1.
APIs de TextDisplay, metadata e spawn conferidas no JAR mapeado da versão exata.
O cliente oficial usado para medir fontes tem SHA-256
`499f6897d1837516680f3114072d8106e11c9adcd933fe5cf051b551089b0c99`.
Os assets Unihex foram verificados pelos hashes oficiais no gerador de métricas.

Resultados finais são registrados ao concluir os runners; os arquivos abaixo
contêm os detalhes da execução atual:

- JUnit: `build/test-results/test/TEST-com.shirokai.holograms.HologramsTest.xml`.
- Fabric sem providers opcionais: `build/server-smoke/result.json` e logs dos dois boots.
- Fabric + Cobblemon: `build/server-smoke-cobblemon/result.json` e logs separados.

Os testes de servidor usam registries reais e passam os componentes pelo codec
nativo de metadata. Observadores são jogadores sintéticos com captura de pacotes;
não representam autenticação Mojang, renderer gráfico ou teste de carga.

Ainda exigem validação de cliente gráfico: aparência das bordas/alinhamentos,
suavidade percebida/FPS, fontes alternativas, ângulos de câmera e diferentes packs.
Multiworld é suportado por chave de dimensão e não por integração direta com sua API;
o roteamento entre dimensões vanilla é testado. Compatibilidade com um JAR específico
de Multiworld e a ponte LuckPerms/PAPI instaladas são verificações separadas.

## Resultado final nesta instância

- Core existente: **135 testes JUnit**, sem falhas/erros/skips; build e smokeClasses aprovados, sem mudanças em seu código Java.
- Shirokai Holograms: **19 testes JUnit**, sem falhas/erros/skips; build e smokeClasses aprovados.
- Fabric básico: **40 verificações em dois boots reais**, aprovadas. Inclui gravação atômica com falha, preservação do plano ativo, reload inválido, comandos, permissões vanilla, codecs de metadata, as dez animações, placeholders personalizados, descarte por distância, dimensão Nether e restauração/exclusão após restart.
- Fabric + **Cobblemon 1.8.3+1.21.1 / Loader 0.17.2**: as mesmas **40 verificações em dois boots reais**, aprovadas; registros em `build/server-smoke-cobblemon/`.
- JAR de produção inspecionado: ID próprio, server-side, versão 1.0.0-dev, fontes numéricas incluídas; nenhuma fixture Smoke ou classe do Core.

SHA-256 de `build/libs/shirokai-holograms-1.0.0-dev.jar`:
`9d7adcb58dfa4313af5e5f240662997cbe8df666bcf9c4152ce30e5bb0471527`.

Durante o primeiro ensaio Cobblemon, a ausência do plugin Kotlin no subprojeto
causou metadados de reflexão não remapeados no ambiente Loom de desenvolvimento.
O plugin Kotlin 2.2.20 foi adicionado e somente o cache gerado de remapeamento do
Cobblemon foi reconstruído. O JAR oficial e suas 21 dependências aninhadas foram
mantidos intactos e verificados por SHA-256. A correção é do harness de desenvolvimento;
nenhum checksum, TLS, código de Cobblemon ou autenticação foi desativado.

O workflow `.github/workflows/holograms-build.yml` foi acrescentado para build/JUnit
independente; sua execução remota ainda não foi solicitada. Na implementação inicial não houve push, publicação de release ou alteração/publicação de configuração de ambiente.

O envio posterior ao GitHub foi solicitado pelo usuário. O JAR verificado está em
`releases/shirokai-holograms-1.0.0-dev.jar` na raiz do repositório; resumos dos dois
runners foram preservados em `shirokai-holograms/validation/`.
