# Shirokai Holograms · 1.0.0-dev

Mod **independente e server-side** para Minecraft Java 1.21.1 / Fabric / Java 21.
Não exige Shirokai Core, Cobblemon nem instalação no cliente. Pode coexistir com o Core.
Usa um `TextDisplay` nativo por linha e por observador, sem ArmorStands, colisões,
chunks forçados ou entidades salvas no mundo.

JAR compilado desta versão: [baixar](../releases/shirokai-holograms-1.0.0-dev.jar) · [SHA-256](../releases/shirokai-holograms-1.0.0-dev.jar.sha256).

## Instalar e começar

1. Instale Java 21, Fabric Loader e Fabric API para 1.21.1. Com Cobblemon **1.8.3+1.21.1**, use Loader **0.17.2 ou superior**, exigido pelo próprio Cobblemon.
2. Coloque `build/libs/shirokai-holograms-1.0.0-dev.jar` em `mods/` do servidor e reinicie. Não instale o JAR `-sources` como mod.
3. Os três arquivos são criados em `config/shirokai-holograms/`: `config.json`, `holograms.json` e `animations.json`. Não são substituídos por defaults em reinícios.
4. Como administrador dentro do jogo:

```text
/hologram create spawn
/hologram line set spawn 1 <gradient:#ffcc33:#fff4b0><bold>✦ BEM-VINDO AO SHIROKAI ✦</bold></gradient>
/hologram line add spawn <aqua>Olá, %player:name%! Online: %server:online%</aqua>
/hologram line add spawn &6&lSua jornada começa aqui!
/hologram line align spawn 2 left
/hologram line align spawn 3 right
/hologram line animation spawn 3 TYPEWRITER
/hologram scale spawn 1.5
```

`create` e `move` posicionam a primeira linha dois blocos acima dos pés do administrador.
Linhas são numeradas a partir de **1**. A última linha não pode ser removida; apague o holograma.
`edit` mostra as linhas e os comandos de edição em texto; não abre uma GUI.

## Comandos e permissões

| Comando | Permissão `shirokai.holograms.` |
|---|---|
| `/hologram create <id>` | `create` |
| `/hologram delete <id>` | `delete` |
| `/hologram edit <id>` e `/hologram list` | `edit` |
| `/hologram move <id>` e `/hologram teleport <id>` | `move` |
| `/hologram reload` | `reload` |
| `/hologram align <id> left\|center\|right` | `edit` |
| `/hologram line add <id> <texto>` | `edit` |
| `/hologram line set <id> <linha> <texto>` | `edit` |
| `/hologram line remove <id> <linha>` | `edit` |
| `/hologram line align <id> <linha> left\|center\|right` | `edit` |
| `/hologram line animation <id> <linha> <tipo-ou-preset>` | `edit` |
| `/hologram scale <id> <0.25..4>` | `edit` |

`shirokai.holograms.admin` concede todas as ações. Com LuckPerms presente,
o mod consulta a API 5.4 e o contexto do jogador; falhas negam acesso. Sem LuckPerms,
somente operadores vanilla nível 4 e o console de nível 4 administram. Jogadores comuns
não recebem permissões implícitas. Comandos que precisam de posição exigem jogador.

## Texto e placeholders

Formato equivalente de componentes, com escopo explícito:

- `<gold>`, `<red>`, `<aqua>`, outras 16 cores vanilla e `<#12abef>`.
- `<gradient:#ff0000:#0000ff>texto</gradient>`.
- `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>`.
- Alternativa legada: `&0..f`, `&l`, `&o`, `&n`, `&m`, `&k`, `&r`, `&&`.
- Escolha tags **ou** códigos `&` em cada linha; não misture os dois formatos na mesma linha.
- Tags de click/hover/comandos/fontes arbitrárias não são aceitas.

Placeholders nativos: `%player:name%`, `%player:uuid%`, `%player:world%`,
`%server:online%`, `%server:max%`. Aliases `%shirokai:player%`, `%shirokai:uuid%`
e `%shirokai:online%` também funcionam. O contador nativo é o total conectado,
sem integração de vanish; não é uma contagem privada por observador.

Com **Text Placeholder API 2.4.2+1.21** instalada, os demais `%namespace:chave%`
e `%namespace:chave argumento%` são resolvidos no contexto do observador.
Handlers de outros mods, inclusive providers de Cobblemon/Core, precisam estar realmente instalados.
Não cria moedas, contas ou placeholders de Pokédex fictícios. Valores indisponíveis
usam `unavailablePlaceholder`. Resultados entram como texto literal, sem reinterpretação
de cores, tags ou placeholders, e sem herdar eventos de click/hover do provider.

## Configuração individual e alinhamento

Veja [os três exemplos completos](examples/). `holograms.json` contém
`schemaVersion: 1` e a lista `holograms`; cada item possui `id`, `world`, `x/y/z`,
`alignment`, `lines`, `lineSpacing`, `scale`, `backgroundColor`, `backgroundOpacity`,
`shadow`, `yaw`, `pitch` e `billboard`. Em cada linha, `text` é obrigatório;
`alignment` omitido herda o holograma e `animation` omitido usa `STATIC`.

`LEFT`, `CENTER` e `RIGHT` movem a linha dentro da largura da maior linha renderizada
para aquele observador. O cálculo usa os avanços reais de **114.581 glifos das fontes
bitmap e Unihex vanilla 1.21.1**, incluindo o acréscimo de negrito. Não adiciona espaços.
A mudança do placeholder recalcula a largura. A revelação TYPEWRITER mantém a âncora
completa para não saltar conforme letras aparecem.

**Limite de fontes:** sem um mod no cliente o servidor não conhece as fontes de um
resource pack, a opção de fonte Unicode/Japonês nem o resultado de shaping bidirecional.
O alinhamento calculado é para a fonte padrão e texto da esquerda para a direita.
Fontes substituídas, texto árabe/hebraico e obfuscação podem divergir visualmente.
Use a fonte padrão, desative fontes alternativas e evite obfuscação quando bordas
precisas forem necessárias. Não há alegação de alinhamento universal para qualquer pack.

`lineSpacing` é a distância entre linhas em pixels vanilla antes da escala (9..100).
`scale` é 0.25..4. `backgroundColor` é RGB inteiro (ex.: `1056816` para `#102030`),
`backgroundOpacity` é 0..255, sendo 0 transparente. `shadow` liga a sombra.
Cada linha tem sua própria caixa nativa; não há um painel único atrás de todo o holograma.
`yaw` e `pitch` controlam orientação fixa. `billboard` aceita `FIXED`, `VERTICAL`,
`HORIZONTAL` ou `CENTER`, conforme os modos nativos do Minecraft; no modo de câmera,
os eixos acompanhados pela câmera substituem os respectivos ângulos fixos.

A dimensão é a chave de registro completa (`minecraft:overworld`, `minecraft:the_nether`
ou a chave exposta pelo Multiworld). Dimensões ausentes geram aviso e não são carregadas
à força. Os hologramas passam a aparecer quando a dimensão existir e houver observador
próximo. Não usa nomes de pastas nem APIs de mundos Bukkit.

## Animações

`animations.json` contém presets. Os dez nomes a seguir vêm configurados:

| Tipo | Comportamento |
|---|---|
| `STATIC` | Texto parado |
| `MARQUEE_LEFT` / `MARQUEE_RIGHT` | Texto completo atravessa a área horizontal |
| `SLIDE_LEFT` / `SLIDE_RIGHT` | Entra pelo lado indicado até a âncora |
| `TYPEWRITER` | Revela caracteres, preservando formatação e sequências combinadas |
| `FADE_IN` / `FADE_OUT` | Varia a opacidade nativa do texto |
| `BOUNCE` | Deslocamento vertical suave, amplitude de 10% de `width` |
| `PING_PONG` | Oscilação horizontal suave entre `-width/2` e `+width/2` |

Cada preset define `type`, `speed` (multiplicador 0.1..10), `durationTicks`,
`intervalTicks`, `repetitions` (0 = infinito), `pauseTicks` e `width` em pixels.
A duração efetiva do movimento é `durationTicks / speed`. A pausa segura o último
quadro; repetições finitas ficam no quadro final. Presets customizados podem ser
referenciados por nome; configure um preset diferente para parametrizar uma linha.

O relógio usa ticks do servidor, comum a todos os observadores desde a última
aplicação da configuração. Sob lag, acompanha a simulação, não o relógio de parede.
Amostras são limitadas por `config.updateTicks`; `intervalTicks` é arredondado para
o próximo múltiplo desse intervalo. Translação e opacidade usam interpolação nativa.
Texto TYPEWRITER é discreto e não é interpolado. Repetições reiniciam sem interpolar
através do salto entre fim e começo. Não existe recorte/scissor por área no TextDisplay:
**marquee move o texto inteiro, não corta os glifos nas bordas**. `width` define o trajeto,
não uma máscara. Fade usa opacidade nativa, ocultando texto nos valores em que o vanilla
trataria alfa 0..3 como opaco. Fundo mantém sua opacidade configurada, exceto no quadro
completamente oculto. Nenhum efeito exige shaders ou mod cliente.

## Persistência e desempenho

Edições são validadas antes de salvar `holograms.json`, por arquivo temporário e
rename atômico no executor de IO. Somente após sucesso a nova definição é publicada
na thread do servidor. Edições concorrentes/reload são recusadas enquanto outra operação
está em andamento. Edite arquivos manualmente **comandos parados**, depois use reload
antes de voltar aos comandos, para evitar sobrescrever alterações ainda não carregadas.
Um reload inválido mantém integralmente a configuração e os displays ativos.

Identidades de entidades vêm do alocador vanilla. Nada é adicionado a `ServerWorld`,
logo não há colisão, drops ou entidades duplicadas no save. Distância, desconexão,
respawn/troca de dimensão, reload e parada limpam somente IDs próprios. Novas conexões
recebem novos displays. Definições, não entidades, são restauradas após restart.

O texto de placeholders é cacheado por observador (`placeholderTicks`, padrão 20).
Linhas estáticas sem mudanças não enviam pacotes. Somente observadores na dimensão
correta e dentro de `viewDistance` recebem displays. Limites padrão: 128 hologramas,
20 linhas cada, alcance 48 blocos, atualização a cada 2 ticks. Evite dezenas de linhas
animadas sobrepostas diante de muitos jogadores: custo/pacotes crescem com linhas
visíveis × observadores. O projeto não promete um TPS específico sem teste de carga.

## Compilar e testar

O subprojeto tem seu próprio Gradle Wrapper 8.12.1 com SHA-256 fixado.

```bash
cd shirokai-holograms
# Com JDK 21 completo:
bash ./gradlew --no-daemon build smokeClasses
# Neste ambiente cloud (seleciona JDK, proxy e CA sem desativar TLS):
python3 tools/gradle_cloud.py build smokeClasses
# Somente após aceitação da EULA pelo responsável:
python3 tools/server_smoke.py --accept-eula
# Validação adicional com Cobblemon oficial fixado por SHA-256:
python3 tools/prepare_cobblemon_smoke.py
python3 tools/server_smoke.py --accept-eula --cobblemon
```

Produção: `build/libs/shirokai-holograms-1.0.0-dev.jar`.
Fontes Java: o JAR `-sources.jar`; projeto completo: este diretório.
JUnit: `build/reports/tests/test/index.html`. Testes reais: `build/server-smoke/result.json`
e `build/server-smoke-cobblemon/result.json`. Fixtures smoke ficam em source set separado,
ativado apenas com `-PhologramSmoke`, e não integram o JAR de produção.

As métricas podem ser regeneradas com `tools/generate_font_widths.py <client-1.21.1.jar>`
(Python + Pillow, apenas para manutenção). O script verifica o cliente e os assets
oficiais por hash; somente avanços numéricos são distribuídos, sem texturas/bitmaps.
O motor de componentes foi adaptado do Shirokai Core da mesma base deste repositório.

[Resultados e limites da validação](VALIDATION.md).
