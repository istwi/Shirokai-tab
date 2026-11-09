# Cargos fixos — 0.14.0-dev

O grupo principal real do LuckPerms é o cargo fixo do jogador. Não depende de
/perfil, concessão SQL ou seleção salva. O Core não modifica grupos do LuckPerms.
Depois que o LuckPerms atualizar seu cache, o próximo chat já usa o cargo atual;
TAB, scoreboard e nametag atualizam no intervalo de tab.json/scoreboard.json.
Sem LuckPerms/metadados disponíveis, não é inventado cargo por permissão wildcard.

## Configuração e comandos

No console (no jogo, acrescente /):

```
lp creategroup owner
lp group owner setweight 100
lp user Nick parent set owner
lp user Nick primarygroup set owner
```

`parent set` substitui os grupos diretos; para preservar outros grupos, use
`parent add owner` e defina `primarygroup set owner`. O Core usa o grupo principal
informado pela API; a política de cálculo desse grupo pertence ao LuckPerms.
Na configuração parents-by-weight, configure os pesos: por exemplo owner100,
admin90, mod80 e vip10. primarygroup set sozinho não substitui essa política.
Se houver owner e vip, aparece somente o grupo principal, nunca todos os herdados.

Sem configuração adicional, o grupo owner aparece como [OWNER]. Para personalizar
o rótulo, adicione uma definição ROLE em titles.json com id owner (ou permission
group.owner). Para cores/gradientes fixos, use titleTemplates.owner em
presentation.json; o mesmo ID também funciona no cargo automático sem catálogo.
Exemplo de template: `<gradient:#ff3333:#ffcc33>%visual:value%</gradient>`.
Textos importados não são interpretados como markup. O prefixo LP não é reanalisado
como template. Presets escolhidos pelo jogador nunca alteram o estilo de ROLE.

## Superfícies e perfil

- TAB, scoreboard e acima da cabeça: somente cargo principal fixo.
- Chat: cargo principal primeiro, depois tag opcional de evento ou clã.
- /perfil lista apenas tags opcionais com chat=true; ambos os botões do mouse
  equipam/removem no chat. Cargos não aparecem na lista nem nas sugestões.
- /perfil chat <id> continua funcionando; /perfil titulo <id> explica que tags
  opcionais são exclusivas do chat. Não permite escolher nem remover cargos.
- /perfil limpar e o balde removem somente escolhas opcionais; o cargo permanece.
- Mantido o limite de duas identificações totais: com cargo, uma opcional; sem
  metadados de cargo, até duas opcionais. Tags exigem concessão/permissão atual;
  clã exige associação nativa real. Nick do chat permanece branco.

A seleção visual antiga deixa de controlar o TAB/sidebar; o campo legado é
normalizado para null ao carregar perfil online. ROLE salvos nos slots opcionais
são removidos; concessões e presets são preservados. Sem migração de schema SQLite.
Se existirem duas tags opcionais antigas e o cargo ocupar um slot, a primeira na
ordem determinística é exibida; a outra escolha fica preservada, podendo ser
removida pelo perfil. Leituras offline não apagam seleções de usuários descarregados.

## Nametag e atualização

Reinicie o servidor depois de substituir o JAR. tab-extras.json agora usa schema2;
nametags.enabled é true por padrão e prefix é %shirokai:title_prefix%.
Arquivos schema1 com nametag exatamente igual ao antigo padrão desligado recebem
esse padrão habilitado em memória. Arquivos personalizados não são sobrescritos.
Para desligar explicitamente, use schemaVersion2 e nametags.enabled=false.
Alterar enabled exige reinício; templates aceitam reload.

Nametags usam teams vanilla: o prefixo aparece na mesma linha do nome. Teams de
outros mods são preservados; o Core não toma o controle deles. Este recurso não
cria texto flutuante em uma linha separada acima do nick.

Testes automatizados e Fabric usam jogadores sintéticos. Renderização e troca de
grupo com cliente autenticado ainda precisam de validação dentro do servidor.

## Diagnóstico de cargos diferentes entre superfícies

Se chat/sidebar exibem ADMIN e TAB/nametag ainda exibem DEFAULT, não conclua que
o peso está errado: confirme primeiro a versão carregada com /shirokaicore version
e o estado com /shirokaicore tab, com o jogador afetado online. conflicts/errors
apontam disputas de ownership ou falhas de atualização; examine também avisos
[tab]. O TAB pode ceder a getPlayerListName ou team de outro mod. Um layout com
id default é apenas o identificador do layout, não um cargo fixo.

A regressão Fabric inclui jogador que entra depois, recebe DEFAULT e passa para
ADMIN, além de troca de peso que reordena os jogadores. Não reproduziu o relato
com configuração padrão e sem outro controlador. Esse teste não comprova o
comportamento do servidor do usuário nem autoriza apagar teams existentes.
