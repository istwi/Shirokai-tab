# Chat e MOTD — 0.17.0-dev

Minecraft 1.21.1, Fabric, **Java 21**. Atualização da base 0.16.10-dev;
não é uma migração para outra versão de Minecraft/Java.

## Compartilhar no chat

Escreva `Veja @1!` a `@6` para mostrar o Pokémon do slot da sua equipe.
O texto mostra nome e nível; passar o mouse mostra treinador, slot, espécie,
nível, gênero, shiny, natureza e habilidade. Não move nem entrega Pokémon.
Slot vazio ou integração ausente aparece como indisponível.

`Minha @carteira` mostra Dollars, Tokens e Cash do autor, usando os mapeamentos
existentes de `integrations.json`. Cash desativado não é mostrado. Os saldos são
snapshots do cache da integração, atualizados conforme `refreshTicks`; dados ainda
não carregados ou com erro aparecem como N/D. Nenhuma conta é criada pela consulta.

Não existe argumento de jogador: `@1` e `@carteira` sempre consultam o UUID do
**autor autenticado**, nunca o destinatário ou um nome digitado. Tokens completos,
sem distinguir maiúsculas, aceitam pontuação ao redor; `@12`, `@7`, `mail@1` e
`@carteiraBob` permanecem literais. Até oito expansões por mensagem; tokens
repetidos usam a mesma leitura dentro da renderização.

Requer módulos chat/integrations e providers suportados: Cobblemon 1.8.3+1.21.1
para equipe e Impactor 5.3.5 para carteira. Canais global/local/clã/staff/privado
mantêm suas audiências, permissões, ignore, mute, cooldown e bloqueio de repetição.
Todas as rotas Core usam o mesmo gate. Mensagens enviadas por outros mods fora do
Core dependem do proprietário desse chat.

No chat normal, as expansões são `unsignedContent` visual: o corpo original,
assinatura e máscara permanecem intactos. Clientes podem indicar mensagem
modificada. Destinatários filtrados e máscaras não pass-through não recebem
expansões. Comandos Core continuam sendo mensagens system. Dados do Pokémon e
saldos são texto literal; não são interpretados como tags, comandos ou ações.

## Filtro contra spam e linguagem

Em `config/shirokai-core/chat.json`, por exemplo:

```json
{
  "schemaVersion": 1,
  "cooldownMillis": 1500,
  "duplicateWindowSeconds": 30,
  "maximumLength": 256,
  "blockedWords": ["palavra_proibida", "outra_expressao"]
}
```

Campos omitidos recebem os defaults existentes. Preserve os demais campos do seu
arquivo. Lista vazia não bloqueia palavras; configure os termos usados no servidor.
Filtro literal por ocorrência, ignorando caixa, acentos, caracteres invisíveis de
formatação, variações Unicode e códigos & reconhecidos. Não é um classificador de
linguagem natural e não promete detectar toda grafia disfarçada. Cooldown/repetições
são compartilhados entre os canais. Bypass administrativos existentes continuam:
`shirokaicore.chat.bypass.cooldown` e `shirokaicore.chat.bypass.filter`.

## MOTD da lista de multiplayer

Edite `config/shirokai-core/motd.json`, criado somente se ausente:

```json
{
  "schemaVersion": 1,
  "enabled": true,
  "frameTicks": 60,
  "frames": [
    [
      "<gradient:#ffcc33:#fff4b0><bold>✦ SHIROKAI ✦</bold></gradient>",
      "<aqua>Cobblemon</aqua> <gray>%server:online%/%server:max% online</gray>"
    ],
    [
      "<gold><bold>✦ SHIROKAI ✦</bold></gold>",
      "<green>Explore • Capture • Battle</green>"
    ]
  ]
}
```

Até duas linhas por quadro, até 100 quadros; `frameTicks` entre 5 e 1200.
Cores nomeadas, HEX (`<#55ffaa>texto</#55ffaa>`), gradientes e estilos seguem
`StyleEngine`. Animações existentes de `styles.json` usam `%animation:gold_wave%`.
Placeholders permitidos: `%server:online%`, `%server:max%`, `%server:version%`.
Não há contexto de jogador antes do login; carteiras e Pokémon não são placeholders
de MOTD. Contagem online exclui jogadores ocultos pelo adapter Vanish suportado.

Após editar, execute `/shirokaicore reload` (console: sem `/`). JSON/templates
inválidos rejeitam a configuração candidata sem substituir a ativa. `enabled:false`
preserva o MOTD vanilla/servidor. Arquivos existentes não são reescritos.

O protocolo vanilla entrega **um quadro por consulta**. Atualizar/reconsultar a
lista pode mostrar outro quadro; não há animação contínua numa resposta já recebida.
O Core só substitui a descrição do status: mantém contagem/lista, favicon, versão
e secure-chat do pacote original. Outros mods que controlam MOTD podem conflitar.
Não altera `server.properties` nem desativa online-mode.

Depois de substituir o JAR, reinicie o servidor para carregar o novo mixin.
Preserve JSONs e banco. Desligar apenas o MOTD exige somente reload.
