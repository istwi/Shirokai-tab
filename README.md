## Integração Essentials — 0.18.0-dev

Ponte opcional com **Shirokai Essentials 0.3.0**: apelido no chat/TAB/scoreboard/perfil,
AFK no TAB, mute/ignore/bloqueio de privados compartilhados e placeholders de homes,
kits, correio e utilidades. Dois JARs e bancos independentes; não altera cargos LP,
UUIDs ou skins. [Instalação e limites](docs/ESSENTIALS.md).

## Ajustes de chat 0.17.1-dev

Slot vazio avisa somente ao autor. Compartilhamento mostra só o nome do Pokémon;
tooltip inclui IVs e treinador com cargo colorido. Hover no nick mostra cargo,
Pokédex e ginásios. [Detalhes](docs/CHAT_SHARES_MOTD.md).

## Chat: Pokémon, carteira e MOTD (0.17.0-dev)

`@1` a `@6` compartilham seu próprio Pokémon; `@carteira` mostra seus saldos.
Filtro de linguagem e antispam em `chat.json`; MOTD colorido com placeholders e
quadros em `motd.json`. [Configuração e limites](docs/CHAT_SHARES_MOTD.md).

> 0.16.5-dev: /perfil e estilos usam o [mesmo layout de cinco linhas](docs/MENUS.md), com migração do padrão antigo e backup.

> 0.16.2-dev: menu de clã com cinco linhas e ícones/posições da referência; [layout](docs/MENUS.md).

> 0.16.1-dev: painel de clã reorganizado no estilo Shirokai; [layout e navegação](docs/MENUS.md).

> 0.16.0-dev: histórico, diagnóstico e reparos de [clãs](docs/CLANS.md),
> [presets animados e controles por área](docs/CUSTOMIZATION.md) e integração
> opcional de [Vanish](docs/VANISH.md). Configurações existentes são preservadas.
> Para reabrir o menu de clã antes pausado, defina `clanMenuEnabled: true` em menus.json.

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
