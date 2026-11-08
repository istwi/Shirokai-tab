# Motor de estilos — 0.11.0-dev

StyleEngine compila templates administrativos em árvore imutável. Valores de placeholders
são inseridos depois, como texto literal: tags contidas em nomes, prefixos, siglas ou
outros valores não são reanalisadas como formatação/ações. O motor não é MiniMessage completo.

## Sintaxe suportada

```
<gold><bold>SHIROKAI</bold></gold>
<#33ccff>Texto HEX</#33ccff>
<gradient:#ff0000:#0000ff>Gradiente 😀</gradient>
<italic>Texto</italic>
<underlined>Texto</underlined>
<strikethrough>Texto</strikethrough>
<obfuscated>Texto</obfuscated>
```

Cores Minecraft: black, dark_blue, dark_green, dark_aqua, dark_red, dark_purple,
gold, gray, dark_gray, blue, green, aqua, red, light_purple, yellow, white.
Tags devem ser fechadas explicitamente e podem ser aninhadas até16 níveis.
Gradiente interpola RGB por ponto de código Unicode (não divide pares surrogate)
e preserva estilos de negrito/itálico. Gradiente externo colore todos os caracteres
internos, inclusive aqueles com outra cor interna. \\< representa '<' literal.
Tags click/hover/font/run_command e desconhecidas são rejeitadas; não há efeitos executáveis.
&/§ legados e reset implícito não são sintaxe de formatação deste motor.

## Placeholders e animações

%shirokai:player%, %shirokai:online% etc usam resolver da superfície.
%animation:gold_wave% escolhe um frame compartilhado de styles.json:

```json
{
  "schemaVersion": 1,
  "animations": {
    "gold_wave": {
      "frameTicks": 20,
      "frames": [
        "<gold><bold>✦ SHIROKAI ✦</bold></gold>",
        "<gradient:#ffcc33:#fff4b0><bold>✦ SHIROKAI ✦</bold></gradient>"
      ]
    }
  }
}
```

Frames podem usar placeholders, mas não outras animações (evita recursão/ciclos).
Grupos/layouts podem referenciar animações diferentes. Velocidade mínima5ticks;
a superfície pode atualizar mais lentamente e pular frames intermediários.
Animação desativada por jogador usa frame0, não um valor fictício.
Não há transições interpoladas entre frames nem atualização de mensagens antigas do chat.

Limites:50 animações,100frames cada,2048 caracteres de fonte por template,
256KiB no JSON,512 pontos de código por valor dinâmico,2048 expandidos por render.
Controles em template/valor são rejeitados; múltiplas linhas são listas na configuração TAB.
Cache LRU de256compilações e1024 renders, chaves incluem valores e índices de frames,
impedindo compartilhar texto de um jogador com contexto diferente.
Text retornado é compartilhado e deve ser tratado como read-only; consumidores que
precisem alterar devem copiar antes. TAB monta novos roots sem modificar spans do cache.

## Integração atual

TAB/sidebar e a camada de apresentação de chat/perfil/clãs usam o motor.
Templates de presentation.json e chat.json são compilados junto do candidato de reload.
Corpo normal assinado permanece inalterado; comandos system podem destacar menções
sem interpretar markup. Ver PRESENTATION.md para condições, limites e compatibilidade.
Dados dinâmicos não ganham estilo por conter tags; os templates envolvem valores literais.
Escolhas pessoais estáticas e editor de clã estão implementados em0.11; ver CUSTOMIZATION.md.

styles.json, tab.json e scoreboard.json entram no mesmo reload atômico dos demais módulos. Animação
desconhecida, tag inválida ou template estático grande impede publicar o candidato.
Dados dinâmicos que excedem limites preservam o último frame válido e geram diagnóstico.


A partir0.9, variáveis podem incluir argumentos PAPI limitados, como
`%player:objective gyms%` e `%shirokaidex:percent national%`. A ponte importa
somente a String do resultado: eventos/estilos/markup dos dados não são reanalisados.


renderRich compõe slots de Text sem reanalisar markup, mantendo cores/formatos e
removendo click/hover/insertion. Não altera Text da origem/cache. Usado para inserir
identificações já formatadas em cabeçalhos/HUDs. Rich slots não compartilham cache de
strings: evitam colisões entre valores iguais com estilos diferentes.

## Atualização0.11

Escolhas persistidas de presets estáticos foram implementadas: CUSTOMIZATION.md.
O catálogo reusa o mesmo StyleEngine e os mesmos limites/remoção de ações de rich slots.
Presets não aceitam animação ou placeholders extras porque são compartilhados por
identificações de chat e HUDs. Animações existentes dos layouts continuam disponíveis.
