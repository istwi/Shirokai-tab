> 0.15.2-dev: evita persistência de teams temporários entre saves/reinícios;
> [instalação e recuperação de vínculos antigos](docs/RANKS.md).

> 0.15.1-dev: terceira moeda independente [cash](docs/INTEGRATIONS.md), com
> `%shirokai:cash%` e `cashCurrency` em integrations.json.

> 0.15.0-dev: [menus configuráveis](docs/MENUS.md); menu/editor de clã pausados
> por padrão. Exemplo completo em [examples/menus.json](examples/menus.json).

> 0.14.1-dev: [cargos automáticos LuckPerms e nametags](docs/RANKS.md).

# Shirokai Core

Mod server-side Fabric para Minecraft 1.21.1 / Java 21, em desenvolvimento.
A versão 0.13.1-dev implementa infraestrutura de configuração, SQLite, permissões,
placeholders nativos, administração, títulos, perfil, clãs e chat/moderação.
TAB nativo, scoreboard visual incremental e motor de estilos estão implementados; adaptadores opcionais de Cobblemon, Impactor, Shirokai Dex e PAPI estão disponíveis. Validação visual com cliente real e recursos restantes permanecem pendentes.
Esta versão não é uma release estável.

Chat compacto [G]/[L], cores &7 e avisos de conexão: [CHAT_APPEARANCE](docs/CHAT_APPEARANCE.md).

Extras opcionais de TAB (listagem, nametags, BELOW_NAME e bossbar): [TAB_EXTRAS](docs/TAB_EXTRAS.md).

Personalização persistida, comandos e presets: [CUSTOMIZATION](docs/CUSTOMIZATION.md).
Editor textual, convites enviados e administração inicial: [CLANS](docs/CLANS.md).

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

Artefato: `build/libs/shirokai-core-0.13.1-dev.jar`. Requer Fabric API;
o driver SQLite é incluído. LuckPerms é opcional nesta etapa: sem ele, somente
administradores vanilla nível 4 recebem acesso aos comandos administrativos.
Não há concessão automática de permissões de gameplay.

Configuração e comandos: [CONFIGURATION](docs/CONFIGURATION.md),
[PERMISSIONS](docs/PERMISSIONS.md). Continuidade: [NEXT_STEPS](docs/NEXT_STEPS.md).
