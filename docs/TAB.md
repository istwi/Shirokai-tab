# TAB nativo — 0.7.0-dev

## Implementado

- Header/footer multilinha com templates compilados, cores/HEX/gradientes e frames.
- Layout por permissão, grupo primário LuckPerms e dimensão, com prioridade configurável.
- Nome individual com título visual único já escolhido no perfil; não seleciona todos os títulos.
- Prefixo/sufixo e peso de grupo consultados pela API LuckPerms quando disponível.
- Regras de nome por condições, prioridade e atualização em mudanças de grupo/mundo/dados.
- Comparação por componentes: header e nomes só enviam pacotes quando mudam.
- Atualização de nomes usa UPDATE_DISPLAY_NAME; não recria roster, ping, perfil, sessão ou listagem.
- Ordenação por teams opt-in, sem mover jogadores de teams externos nem adotar nomes existentes.
- Preferência de animações por jogador persistida, com cache limpo no disconnect.
- Diagnóstico com contagem real de pacotes/entradas, teams/conflitos e falhas.

## Configuração

config/shirokai-core/tab.json, schemaVersion=1. Default contém updateTicks20,
managePlayerNames=true, manageTeams=false, nameTemplate=%shirokai:title_prefix%%shirokai:player%,
layouts com default sem condições e nameRules vazio. Todos os campos estruturais
são validados; não sobrescreve arquivo existente. styles.json fornece animações.

Exemplo de layout adicional:

```json
{
  "id": "vip",
  "priority": 100,
  "condition": {
    "permission": "shirokaicore.layout.vip",
    "group": "vip",
    "worlds": ["minecraft:overworld"]
  },
  "header": ["<gradient:#ffaa00:#ffffff>✦ SHIROKAI ✦</gradient>"],
  "footer": ["<gold>Bem-vindo, %shirokai:player%</gold>"]
}
```

permission/group omitidos são opcionais; worlds vazio aceita qualquer dimensão.
Quando mais de uma condição está preenchida, todas devem passar. Grupo é primário
LuckPerms (para heranças use condição de permissão), comparação sem caixa.
Layouts ordenam por prioridade decrescente e ID para desempate; configure prioridade
maior que0 para sobrepor o default. default sem condições é obrigatório.
Até50layouts,50regras,12linhas por header e12footer. Texto expandido limitado a2048
por header/footer; nome limitado a256. Templates estáticos excessivos são rejeitados antes do reload.

nameRules usa id/priority/condition/template. Prioridade da regra determina ordenação
quando casa; sem regra, usa peso do grupo primário LuckPerms. Mesma prioridade compartilha team.

## Dados disponíveis

- shirokai:player, uuid, viewer, online, world, ping.
- shirokai:selected_title: display do título escolhido; vazio se não há escolha.
- shirokai:title_prefix: título +espaço ou vazio, para template de nome sem espaço sobrando.
- shirokai:clan e clan_tag: snapshot de associação real, com fallback quando ausente.
- shirokai:group: grupo real ou fallback; luckperms:prefix e suffix: metadados atuais ou vazio.
- Demais chaves válidas %namespace:chave% são encaminhadas ao serviço nativo de placeholders.
  Com integrations ativo, moedas/Pokédex/ginásios/PAPI têm providers reais opcionais (INTEGRATIONS.md); ausência usa unavailablePlaceholder.

Valores são literais. Prefixo LuckPerms com markup/legacy não é interpretado; aplique
estilo fora do placeholder. O serviço revalida título antes de renderizar, mantendo
a mesma chave de seleção compartilhada com a scoreboard visual (SCOREBOARD.md).

## Ordenação e propriedade

No cliente vanilla1.21.1, teams participam da ordenação; não há packet list-order
custom equivalente a versões posteriores. O Core não inventa esse campo.
manageTeams=false é default: não controla ordenação. Para assumir ordenação, habilite
e reinicie, escolhendo Core como dono dos teams dos jogadores gerenciados.
Team names skNNNNNNN usam prioridade invertida; mesmo rank compartilha team e o
cliente desempata por nome de perfil. Peso é limitado a±1000000. Espectadores ainda
seguem a precedência vanilla; não é possível prometer outra posição sem cliente/modificação adicional.

Teams são globais no scoreboard e podem afetar mecânicas que consultem pertencimento:
friendly fire permanece permitido, invisibilidade amigável desativada e colisão ALWAYS.
Isso não remove todos os efeitos possíveis de usar teams: a opção é explícita.

O Core só remove/move dentro de teams criados nesta instância e identificados por
referência, nunca pelo prefixo sk apenas. Team de outro sistema, colisão de nome ou
replacement é preservado e gera diagnóstico. Integrantes externos adicionados a um
team do Core também são preservados; não deleta team que ficou com eles.
No stop, libera seus integrantes/teams antes do save de mundos por SERVER_STOPPING.
Após crash, team persistido desconhecido é preservado e pode exigir limpeza administrativa.

Nome com getPlayerListName não-nulo de outro mod ou team externo é respeitado. Se o
Core o gerenciava antes, faz uma restauração para o valor atual e para de sobrescrever.
Outros mods que enviam nomes/header diretamente por pacotes não são detectáveis por
esse getter: escolha um dono e use managePlayerNames=false/modules.tab=false quando necessário.
Header/footer são do Core enquanto módulo ativo; não há detecção universal de ownership de pacotes.

## Rede, atualização e comandos

updateTicks5..1200. Valores/metadata/regras são calculados por jogador e compartilhados
na atualização; nomes sem dependência do viewer são reutilizados para as duas variantes
de animação. O cache por viewer compara diffs, evitando rebuild de toda lista de rede.
Nenhum SQLite por frame; preferências carregam no join e gravam só ao alterar.

- /tab ou /tab refresh: atualiza visão do próprio jogador.
- /tab animations: alterna animações e persiste preferência.
- /shirokaicore tab: diagnóstico; permissão shirokaicore.command.tab.inspect.

Sem preferência carregada/DB disponível, animações congelam no frame0. Para nomes,
ambos (viewer e subject) precisam de animação ativa; header depende só do viewer.
Preferência de scoreboard existente é preservada. Schema5 adiciona animations_enabled
com defaulttrue; migração1/2/3/4 faz backup antes de alterar e mantém dados anteriores.

Configuração recompila/valida antes de publicar reload. manageTeams e managePlayerNames
exigem reinício; demais alterações aceitam reload. Erro de frame fica isolado e preserva
última visão válida; falhas não devem interromper clãs/chat/outros módulos.

## Limitações e evidências

MixinAccessor mínimo, fixado em MC1.21.1, permite montar entries do packet vanilla;
nenhuma ação desconhecida, fake player ou ping falso é enviada. Refmap acompanha o JAR.
Smokes verificam pacote real, componentes, diffs, ownership, título, grupos e animação
com ServerPlayerEntity sintéticos/conexão sem transporte. Ordenação/aparência com
cliente real e runtime LuckPerms Fabric ainda não validados.

Não implementados ainda: controle de listed/vanish por viewer, nametags customizados,
objetivos abaixo do nome, bossbar, controles por grupo contextual além das condições
atuais e adaptadores externos completos. Nada disso foi marcado como concluído.

## Personalização0.11

Título compartilhado incorpora preset pessoal/clã por rich slot; placeholders
clan/clan_tag também compõem nome/sigla formatados sem reanálise de markup.
Nenhum SQL por frame; escolhas são carregadas no join/eventos/ações. Ownership,
ordenação e limitações listed/vanish/nametags/BELOW_NAME/bossbar não foram alterados.
