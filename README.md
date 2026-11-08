# Shirokai Core

Mod server-side Fabric para Minecraft 1.21.1 / Java 21, em desenvolvimento.
A versão 0.10.0-dev implementa infraestrutura de configuração, SQLite, permissões,
placeholders nativos, administração, títulos, perfil, clãs e chat/moderação.
TAB nativo, scoreboard visual incremental e motor de estilos estão implementados; adaptadores opcionais de Cobblemon, Impactor, Shirokai Dex e PAPI estão disponíveis. Validação visual com cliente real e recursos restantes permanecem pendentes.
Esta versão não é uma release estável.

Apresentação de chat/menus/títulos: [PRESENTATION](docs/PRESENTATION.md).

Integrações e versões suportadas: [INTEGRATIONS](docs/INTEGRATIONS.md).

Scoreboard: [SCOREBOARD](docs/SCOREBOARD.md).

TAB: [TAB](docs/TAB.md) · Estilos: [STYLES](docs/STYLES.md).

Chat: [CHAT](docs/CHAT.md).

Clãs: [CLANS](docs/CLANS.md).

Títulos, seleções e menu: [TITLES](docs/TITLES.md).

Requisitos completos: [REQUIREMENTS](docs/REQUIREMENTS.md).
Estado e evidências: [PROJECT_STATUS](docs/PROJECT_STATUS.md).
Prompt para outro Codex: [CODEX_CONTINUATION_PROMPT](docs/CODEX_CONTINUATION_PROMPT.md).

## Desenvolvimento

```sh
bash ./gradlew --no-daemon build
# Na nuvem, com JDK/proxy/CA preparados:
python3 tools/gradle_cloud.py build
# Servidor de desenvolvimento (a EULA deve ser aceita pelo responsável):
python3 tools/gradle_cloud.py runServer --args=nogui
# Teste automatizado em servidor isolado, com aceitação explícita da EULA:
python3 tools/server_smoke.py --accept-eula
```

Artefato: `build/libs/shirokai-core-0.10.0-dev.jar`. Requer Fabric API;
o driver SQLite é incluído. LuckPerms é opcional nesta etapa: sem ele, somente
administradores vanilla nível 4 recebem acesso aos comandos administrativos.
Não há concessão automática de permissões de gameplay.

Configuração e comandos: [CONFIGURATION](docs/CONFIGURATION.md),
[PERMISSIONS](docs/PERMISSIONS.md). Continuidade: [NEXT_STEPS](docs/NEXT_STEPS.md).
