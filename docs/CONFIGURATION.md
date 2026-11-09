> 0.16: personalization.json controla presets/animações por área (CUSTOMIZATION.md).
> menus.json novo habilita o menu de clã; false existente permanece até alteração
> explícita. Presets aceitam frames/frameTicks sem mudar schema1. SQLite continua6.

# Configuração 0.11.0-dev

Arquivo real: `config/shirokai-core/core.json`, relativo ao diretório do servidor.
Criado somente se ausente. Arquivos existentes, inclusive inválidos, não são sobrescritos.

```json
{
  "schemaVersion": 1,
  "modules": {
    "storage": true,
    "permissions": true,
    "placeholders": true,
    "administration": true,
    "titles": true,
    "profiles": true,
    "clans": true,
    "chat": true,
    "styles": true,
    "tab": true,
    "scoreboard": true,
    "integrations": true
  },
  "databaseFile": "shirokai.db",
  "unavailablePlaceholder": "N/D"
}
```

`schemaVersion` é obrigatório e deve ser o número inteiro 1. Campos opcionais ausentes
recebem defaults em memória, sem reescrever o arquivo. Campos desconhecidos, tipos
incorretos, caminhos de banco com diretórios e JSON inválido são rejeitados com diagnóstico.
Limite: 64 KiB. Versões diferentes são rejeitadas até haver migração explicitamente implementada.

`databaseFile`: somente nome .db, até 64 caracteres antes da extensão, sem `..`.
Dados ficam em `shirokai-core/<databaseFile>` no diretório do servidor.
Backups ficam em `shirokai-core/backups/`; não substituem arquivos existentes.

## Reload

`/shirokaicore reload` lê e valida no worker de IO e publica um snapshot imutável
na thread do servidor. `unavailablePlaceholder` aceita alteração a quente.
Mudanças em `modules` ou `databaseFile` exigem reinício: reload as rejeita integralmente,
preservando o snapshot ativo. JSON inválido também preserva o estado ativo.
No próximo startup, configuração inválida impede o runtime do Core de iniciar;
o servidor Minecraft continua e o console pode usar status/version para diagnóstico.

O módulo administration depende de permissions; se a dependência não iniciar, fica BLOCKED.
`RUNNING` do módulo storage significa worker iniciado; o status SQLite separado informa
STARTING/READY/FAILED. Uma falha de banco não interrompe módulos independentes.

## Comandos implementados

- `/shirokaicore status`: versão, lifecycle e prontidão/fila SQLite.
- `/shirokaicore version`: versão do mod.
- `/shirokaicore modules`: estados dos módulos registrados.
- `/shirokaicore permissions`: provedor de permissões.
- `/shirokaicore reload`: reload seguro, sem trocar banco ou módulos ativos.
- `/shirokaicore debug`: SQLite integrity_check fora da thread do servidor.
- `/shirokaicore backup`: snapshot consistente via VACUUM INTO.
- `/shirokaicore placeholders [template]`: lista ou resolve placeholders nativos.

Ações administrativas bem-sucedidas reload/backup são auditadas no SQLite quando
storage está ativo. Falhas de auditoria são registradas no log. A ponte opcional Text Placeholder API2.4.2 está documentada em INTEGRATIONS.md. Placeholders implementados: `%shirokai:online%`,
`%shirokai:player%`, `%shirokai:uuid%`. Tokens indisponíveis usam fallback; substituição
é literal e não recursiva, sem interpretar markup, click events ou comandos.

titles.json é funcional: ver TITLES.md. Ele participa do reload junto de core.json;
se qualquer arquivo for inválido, ambos os snapshots ativos são preservados.
Novas chaves de módulos ausentes em core.json antigo recebem default true em memória;
não reescrevemos a configuração personalizada. titles depende de storage/permissions;
profiles depende de titles. Desabilitar exige reinício.
Placeholders adicionais: selected_title, chat_tag_1 e chat_tag_2 do namespace shirokai.
clans.json é funcional: ver CLANS.md; participa do reload atômico junto dos outros catálogos.
chat.json é funcional: ver CHAT.md. tab.json e styles.json são funcionais: ver TAB/STYLES. scoreboard.json também é funcional: ver SCOREBOARD.md.


clans.json inclui roleActions a partir0.5; campo ausente em arquivos antigos usa
matriz padrão em memória sem overwrite. Alterações passam pelo mesmo reload atômico.
Títulos CLAN não são JSON estático: dependem de associação e sigla reais (ver CLANS/TITLES).


chat.json participa do mesmo reload atômico de core/titles/clans. A troca de
ownership takeVanillaPrivateCommands exige reinício; demais campos validados aceitam
reload. Novo defaultChannel vale para perfis carregados sem canal salvo; não muda
sessões em andamento em ações não relacionadas. Texto de formato é literal nesta versão.


styles/tab são snapshots validados junto de core/titles/clans/chat. Candidato compila
animações/templates antes de publicar. managePlayerNames/manageTeams exigem reinício.
Nenhum arquivo existente é substituído por default; estilos de placeholders são
aplicados pelo template, nunca por reinterpretação do valor.


scoreboard participa do mesmo candidato atômico de reload. Layouts/linhas/estilos
são compilados antes de publicar qualquer snapshot. modules.scoreboard exige reinício;
demais opções de scoreboard aceitam reload. No schema6 atual, a visibilidade
usa scoreboard_enabled existente, independente de animations_enabled. Sem leitura
da preferência, sidebar fica oculto. Ownership por conexão e comandos em SCOREBOARD.md.


integrações0.9: integrations.json participa do mesmo candidato atômico. Flags de
ativação exigem reinício; moedas/objetivo/coleção/provider/intervalo aceitam reload,
descartando caches e respostas antigas. Defaults/campos e semântica em INTEGRATIONS.md.
O runtime agora inicia em SERVER_STARTED para respeitar o enable do LuckPerms Fabric;
SQLite continua assíncrono e seu status separado precisa atingir READY.


presentation.json participa de styles e do reload atômico, sem novo módulo ou migração.
Templates de chat/menus/títulos/clãs e regras de condição em PRESENTATION.md. Chat
nameFormats agora compila tags seguras quando styles ativo; texto dinâmico segue literal.
Configuração candidata inválida preserva todos os snapshots, inclusive presentation.

## Atualização0.11

presets.json/schema1 participa do mesmo reload atômico, com defaults apenas quando
ausente, limites e exemplos completos em CUSTOMIZATION.md. Não muda schemas de
presentation/titles/clans. SQLite passa para schema6 com backup antes da migração
e duas tabelas de IDs/revisões; preferências anteriores são preservadas.
styles controla apresentação/presets; escolhas exigem storage/permissions, e clã
ativo para nome/sigla. Configuração inválida preserva todos os snapshots.

## Extras0.12

tab-extras.json/schema1 é criado só se ausente e compila no mesmo candidato de reload.
Estrutura completa em TAB_EXTRAS.md. listed/nametags/belowName/bossbar.enabled começam
false e exigem reinício para mudar ownership. Demais campos aceitam reload, inclusive
condições/permissões/templates. Dado numérico indisponível não é convertido em0.
Não há nova migração SQLite: schema6 permanece.

## Chat0.13

chat-appearance.json/schema1 participa do reload atômico; cores por canal, interpretação
& e avisos de conexão. Defaults/JSON e semântica em CHAT_APPEARANCE.md. Não muda
schema de chat.json/SQLite6. Arquivos existentes são preservados; somente os dois
formatos exatamente iguais aos defaults antigos mudam em memória para [G]/[L].
Primeira instalação exige reinício para carregar mixins e chat_type do datapack do mod.
