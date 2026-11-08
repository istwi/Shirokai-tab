# Motor de estilos — 0.7.0-dev

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

O TAB e a scoreboard já usam o motor. Chat continua com corpo/cabeçalho literal por segurança e
compatibilidade de assinatura nesta etapa; migração de estilos de cabeçalho de chat,
menus e personalização individual de títulos/clãs continua pendente.
Dados dinâmicos não ganham estilo por conter tags. Para colorir prefixo, configure
<gold>%luckperms:prefix%</gold> no template, não dependa de interpretar markup no valor.

styles.json, tab.json e scoreboard.json entram no mesmo reload atômico dos demais módulos. Animação
desconhecida, tag inválida ou template estático grande impede publicar o candidato.
Dados dinâmicos que excedem limites preservam o último frame válido e geram diagnóstico.
