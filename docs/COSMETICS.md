# Cosméticos nativos do Core — 0.19.2-dev

Migração solicitada do catálogo/menu do Essentials0.6.12-teleport-speed.2 para o Core. Catálogo atual com48 IDs, seis categorias e os ajustes de partículas. O Core passa a persistir/aplicar as escolhas; o Essentials continua com homes, comandos e teleportes, incluindo a correção de `/speed`.

## Instalação e migração

1. Pare o servidor e guarde backup dos JARs, diretório `shirokai-core`, configurações e dados do mundo.
2. Substitua o Core por `shirokai-core-0.19.2-dev.jar` e o Essentials por `shirokai-essentials-0.6.13-core-cosmetics.1-zorykai-visual.jar`, sem duplicados. São candidatos de desenvolvimento, não publicação de release estável.
3. Preserve os arquivos existentes. Conceda as permissões **novas** do Core descritas abaixo. Reinicie o servidor completo.
4. Consulte `/cosmeticos`. Se houver outro controlador desse comando, existe `/shirokaicore:cosmeticos`.

O Core também funciona sem o Essentials. Se o Essentials estiver instalado, use o JAR atualizado do par: a ponte de cosméticos confirma um único controlador e libera os teams antigos que a instância do Essentials possui. Uma versão antiga sem essa ponte impede ativar o novo componente Core, com diagnóstico de atualização; não tenta executar os dois sistemas silenciosamente.

No primeiro carregamento de cada UUID, o worker SQLite consulta `world/shirokai-essentials/players/<uuid>.json`, sob o diretório real do mundo. Copia somente `cosmetics`, `cosmeticsHidden` e `cosmeticPreferences`. Não importa homes, apelidos, punições, dinheiro ou permissões. Não escreve nem apaga o arquivo legado; o Essentials pode continuar atualizando seus próprios dados por outras funções normalmente.

Uma escolha já existente no Core nunca é substituída pelo legado em reconexão/reinício. Perfis offline são importados quando carregam pela primeira vez. IDs antigos/indisponíveis são preservados como escolhas inativas, sem conceder acesso. JSON legado inválido ou acima de2MiB é recusado e preservado; corrija a origem e tente abrir novamente. Alterações feitas no Essentials depois da migração não sobrescrevem o Core.

Quando `config/shirokai-core/cosmetics.json` ainda não existe, uma configuração válida de `config/shirokai-essentials/cosmetics.json` é copiada, preservando limites, `enabled`, `nickEnabled` e opções desativadas. Arquivos Core existentes nunca são sobrescritos automaticamente.

O componente requer os módulos `storage` e `permissions` ativos. A publicação das escolhas em memória ocorre somente depois de salvar. O SQLite existente recebe a tabela adicional `cosmetic_profiles`, com revisão CAS e payload schema1. Não há alteração destrutiva do schema6 de títulos/clãs; perfis, concessões e grupos LP permanecem independentes. Backups do banco devem incluir essa tabela. SQL/configuração/legado usam o worker do storage; renderização usa apenas cache na thread do servidor.

## Permissões LuckPerms

As permissões antigas `shirokai.essentials.cosmetics.*` **não concedem** acesso no Core. Nenhum grupo do LuckPerms é modificado automaticamente.

| Permissão | Acesso |
|---|---|
| `shirokai.core.cosmetics.use` | Abrir menu e usar opções concedidas |
| `shirokai.core.cosmetics.customize` | Velocidade, tamanho, densidade, direção e prévia |
| `shirokai.core.cosmetics.nick_cyan` | Ciano |
| `shirokai.core.cosmetics.nick_gold` | Dourado |
| `shirokai.core.cosmetics.nick_rainbow` | Arco-íris suave |
| `shirokai.core.cosmetics.<id>` | Uma opção do catálogo abaixo |
| `shirokai.core.cosmetics.admin` | Reload, status e celebração administrativa |

Exemplo para um grupo de jogadores:

```text
lp group magma permission set shirokai.core.cosmetics.use true
lp group magma permission set shirokai.core.cosmetics.customize true
lp group magma permission set shirokai.core.cosmetics.nick_gold true
lp group magma permission set shirokai.core.cosmetics.trail_fire true
lp group magma permission set shirokai.core.cosmetics.aura_stars true
```

Para administração, conceda `.admin` ao grupo administrativo apropriado. O wildcard `shirokai.core.cosmetics.*` inclui esse acesso administrativo; prefira os IDs específicos para VIPs. O console pode usar comandos administrativos. Remover permissão suspende a opção, preservando a escolha; conceder novamente a reativa sem reequipar.

IDs de efeitos: combine uma categoria `trail`, `aura`, `join`, `victory` ou `raid` com `_` e um estilo: `stars`, `fire`, `water`, `nature`, `soul`, `snow`, `heart`, `magic`, `cherry`. Exemplo: `raid_cherry`, `join_fire`. Os três IDs `nick_*` são os listados acima.

## Funcionalidades preservadas

- Menu de categorias, opções bloqueadas/equipadas, remoção por categoria e desativar/reativar tudo. Inventário de visualização protegido: não entrega ícones nem muda itens/XP.
- Ajustes de partículas1–3 para velocidade, tamanho e densidade, direção±1, prévia sem salvar e botão aplicar. Uma tela antiga não sobrescreve preferências de outra revisão.
- Rastros junto aos pés, densidade proporcional ao tipo de partícula, auras e entrada após breve atraso. Limites globais/por jogador e prioridade rotativa mantidos.
- Esconder tudo preserva seleção, oculta os próprios efeitos e as partículas de outros para esse jogador.
- Invisibilidade, espectador e a política de vanish/visibilidade do Core suprimem partículas. A opção de bloquear durante batalha continua; com Essentials, também respeita as restrições dele.
- Vitória por morte PvP de jogador Minecraft, integração/manual, e observação opcional da transição de vitória do Raids. Assim como na origem, não é uma promessa de celebração automática por toda batalha Pokémon comum.

## Cor do nome

Sem escolha válida, o nick continua branco. Com opção concedida, o Core aplica a cor somente ao componente do nome no chat e no nametag gerenciado pelo Core. O nome no TAB permanece branco, inclusive nos placeholders de apelido; cabeçalho, rodapé, cargo e clã mantêm seus estilos. Corpo, assinatura, máscara, cargo principal, tag/clã e identidade de conta permanecem separados. Os dados dinâmicos não viram templates.

A nametag usa o team **já próprio do Core**, sem criar outro time cosmético por cima. A cor não altera o prefixo/sufixo com seus estilos. `nametags.enabled` precisa estar ativo para colorir acima da cabeça. Teams e controladores externos continuam preservados; o Core não rouba seu ownership. Ícones, cores de cargos e grupos LP não são alterados pelo cosmético.

Arco-íris mantém a alternância suave da origem (paleta vanilla, não gradiente por letra). Acima da cabeça isso é uma cor por vez, limitação normal do team vanilla. O nick/apelido segue a ponte de identidade existente; UUID, nome real, skin e seletores não mudam.

## Configuração e comandos

`config/shirokai-core/cosmetics.json` mantém os campos/limites do Essentials: `enabled`, `nickEnabled`, `suppressInBattle`, intervalos e densidade dos rastros, orçamentos, distância, cooldown, máximos de personalização e `disabled`. Reload inválido mantém a configuração ativa. Depois de corrigir uma falha de configuração inicial, o reload também tenta inicializar o componente de forma segura.

```mcfunction
/cosmeticos
/cosmeticos desativar
/cosmeticosadmin status
/cosmeticosadmin reload
/cosmeticosadmin celebrar NomeDoPlayer vitoria
/cosmeticosadmin celebrar NomeDoPlayer raid
```

Aliases protegidos: `/shirokaicore:cosmeticos` e `/shirokaicore:cosmeticosadmin`. Não sobrescrevem comandos existentes. Celebrações respeitam seleção, permissões, limites e cooldown; não concedem recompensas ou vantagens.

## Validação e limites

Evidências e hashes em `docs/cosmetics/0.19.2-dev/`. Testes de importação não destrutiva, CAS concorrente, restart, configuração e rollback; servidores reais com Core sozinho e com o par, LuckPerms real, captura de pacotes e jogador sintético. O cenário de raid usa o JAR real indicado no relatório e chama sua transição de vitória; não completa uma raid inteira. A troca do objeto do jogador simula respawn durante save; não testa morte/login autenticado de cliente humano. Não houve cliente gráfico ou certificação criptográfica; o teste de chat usa mensagem sintética e compara os campos preservados.

O código do catálogo/efeitos/menu foi portado do Essentials, com sua licença MIT preservada em `META-INF/licenses/essentials-cosmetics-MIT.txt`. Nenhum client mod, publicação de ambiente ou alteração do servidor real foi realizada.


## Ajuste 0.19.2-dev

Removidos Nuvem, Encantado e Elétrico em todas as categorias. Seleções antigas desses IDs ficam preservadas no banco, mas inativas; não aparecem no menu nem emitem efeitos. O catálogo tem48 opções. A lore dos itens não exibe os nodes de permissão; o controle de acesso permanece ativo. Rastros de coração durante movimento têm75% menos partículas no ritmo contínuo, sem afetar auras ou celebrações.

Vitória e raid agora cobrem raio de2,8 a4,4 blocos, conforme tamanho escolhido, com espiral de3,5 blocos de altura e som de explosão de fogos. Mantidos os limites de partículas e cooldown; o som respeita ocultação e visibilidade dos efeitos. Sem entidades de fogos, dano ou itens.


## Arco-íris 0.19.3-dev

Troca de cor a cada10 ticks (0,5 segundo a20TPS), antes40 ticks (2 segundos). Frame de nametag atualizado enquanto há arco-íris equipado/autorizado, sem depender do intervalo normal do TAB. TAB permanece branco; mensagens enviadas guardam a cor do momento.
