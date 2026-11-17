# Shirokai Holograms

Subprojeto independente: Java 21, Fabric/Minecraft 1.21.1. Não depende do Core.
Trabalhe no checkout existente; não crie worktrees sem pedido explícito.
Não publique configuração de ambiente, releases ou faça push sem solicitação.

Build: `python3 tools/gradle_cloud.py build smokeClasses` (ou `bash ./gradlew --no-daemon build smokeClasses`).
Smoke: `python3 tools/server_smoke.py --accept-eula`; aplica-se a autorização de EULA registrada no AGENTS.md pai.
Compatibilidade: `python3 tools/prepare_cobblemon_smoke.py` e `python3 tools/server_smoke.py --accept-eula --cobblemon`.
Não desative online-mode/TLS/checksums para contornar falhas.

Game APIs e envio de pacotes somente na thread do servidor. IO de configuração
no executor próprio; publicar planos apenas depois de validação e persistência.
Preserve o plano ativo em erro de reload. Nunca acrescente as entidades virtuais
à ServerWorld ou remova entidades externas. Use IDs alocados pelo vanilla.

Texto vindo de placeholders é literal, nunca template. Não reutilize Text mutável
compartilhado. Preserve o envio dos campos voltando ao default e start_interpolation=0
nas atualizações de metadata (getChangedEntries sozinho omite defaults).

Alinhamento usa avanços da fonte vanilla, incluindo Unihex/negrito, não espaços.
Documente limites com packs de fontes, bidi e clipping. TYPEWRITER preserva estilos
sem cortar surrogate pairs/combining sequences. Fixtures jamais entram no JAR final.
Consulte README.md e VALIDATION.md antes de alterar o comportamento.
