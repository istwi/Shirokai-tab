# Essentials 0.3.0 + Shirokai Core/TAB 0.18.0-dev

Dois mods separados, com configurações e bancos preservados. A ponte usa protocolo público 1, com tipos Java padrão e UUID. Não depende de Impactor e não adiciona cobranças. Instalar só um dos mods continua permitido.

## Instalar

1. Substitua os JARs antigos por `shirokai-essentials-0.3.0.jar` e `shirokai-core-0.18.0-dev.jar`, sem manter versões duplicadas.
2. No Essentials, ative `modules.chat` e `features.nicknames` em `config/shirokai-essentials/modules.json`. A configuração existente não é reescrita nem ativada silenciosamente. AFK usa `afk_manual`/`afk_auto` e seu intervalo atual.
3. Mantenha o módulo de chat do Core ativo para que ele controle canais, estilos e mensagens privadas. Reinicie o servidor completo.
4. Conceda `shirokai.essentials.nick` no LuckPerms aos usuários autorizados. Alterar apelido alheio exige também `shirokai.essentials.nick.others`.
5. Teste `/nick Apelido`, `/nick off` e `/afk`. Não é necessário alterar o template padrão do TAB.

`/shirokaiessentials status` mostra `core-chat=true`; o Core registra `[essentials] READY protocol=1` no log. Configurações antigas recebem `coreBridge=true` em memória; para desligar explicitamente, defina `coreBridge=false` em `integrations.json` do Essentials e reinicie. Nesse caso os administradores devem escolher apenas um formatador de chat, pois a ponte não coordena os dois.

## O que é integrado

- Apelido no chat, TAB, scoreboard que usa `shirokai:player`, perfil e identificação do treinador no hover/compartilhamento de Pokémon do Core.
- Indicador `[AFK]` no TAB padrão. Atividade de chat do Core retorna o jogador ao estado disponível. Tags/cargos LuckPerms e limite de identificações permanecem intactos.
- Hover de chat distingue apelido e conta original. Cliques sugerem mensagem para a conta original; UUID, skin, permissões, team membership e autenticação não mudam.
- Mute/tempmute ativos no Essentials são verificados no gate do Core antes de global, local, staff, clã e privado, independentemente da ordem dos eventos.
- Ignore do Essentials também filtra canais do Core. Mensagens privadas verificam ignore/bloqueio de ambos os participantes e as restrições próprias do Core.
- `/msg` e `/r` do Essentials, incluindo aliases próprios, delegam ao roteador privado do Core quando a ponte assume chat. As conversas e respostas passam a compartilhar o estado do Core. Anti-spam/filtros do Core continuam valendo.
- `/unignore` remove também o ignore do Core. O toggle `/chat ignore` considera registros legados do Essentials; remoções são persistidas sem copiar listas nem bancos. Se uma gravação falhar, o bloqueio restante é preservado e a operação comunica a falha.
- Socialspy do Essentials observa somente privados entregues pelo Core, exige ativação/permissão atuais e respeita a visibilidade dos dois participantes.
- Formatação pública do Essentials é suspensa enquanto o Core possui o chat, evitando mensagens duplicadas. Avisos de conexão ficam com o Core; morte e MOTD dentro do jogo continuam com as opções do Essentials. O MOTD da lista de servidores continua pertencendo ao Core.

Punições preexistentes dos dois mods continuam válidas: remover um mute no Essentials não apaga um mute administrativo independente salvo no Core. `/chat ignore` continua sendo toggle; `/unignore` do Essentials é remoção explícita. A ponte não amplia permissões, não assume comandos de outros mods e não ignora conflitos de aliases.

## Placeholders adicionais

Estão disponíveis nos templates internos do Core (TAB, scoreboard e extras). Esta ponte não registra o namespace `essentials` na API externa PAPI:

| Placeholder | Valor |
| --- | --- |
| `%essentials:nickname%` | Apelido, vazio quando não definido/ativado |
| `%essentials:displayname%` | Apelido ou nome original |
| `%essentials:realname%` | Nome real da conta |
| `%essentials:afk%` / `%essentials:afk_suffix%` | `true`/`false` e sufixo ` [AFK]`/vazio |
| `%essentials:homes%` / `%essentials:home_limit%` | Quantidade de homes e limite por permissão |
| `%essentials:mail%` | Quantidade de mensagens armazenadas no correio |
| `%essentials:teleport_seconds%` | Segundos restantes do cooldown de teleporte |
| `%essentials:kits_ready%` | Kits acessíveis ao jogador sem cooldown/entrega pendente |
| `%essentials:kit_seconds%` | Menor espera entre os kits acessíveis; indisponível se não houver kit ou entrega incerta |
| `%essentials:kit_pending%` | Entrega de kit pendente de conferência |
| `%essentials:fly%` / `%essentials:god%` | Estado habilitado pelo Essentials, não o modo criativo vanilla |

Módulo ausente/desligado ou perfil ainda não carregado usa o fallback configurado do Core, sem inventar dados. AFK/fly/god têm estado booleano efetivo quando o perfil está carregado. Templates personalizados que usam nomes de outro provider precisam optar por `essentials:displayname`; o padrão `shirokai:player` já acompanha o apelido. `shirokai:viewer` mantém a identidade original.

Exemplo de linhas para seu layout de scoreboard (insira no formato já existente):

```text
Jogador: %essentials:displayname%
Homes: %essentials:homes%/%essentials:home_limit%
Kits disponíveis: %essentials:kits_ready%
Próximo kit: %essentials:kit_seconds%s
```

Os novos indicadores não são inseridos em layouts existentes automaticamente. Prefira estados administrativos em painéis restritos. Dados dinâmicos são texto literal, sem interpretação de tags/comandos.

## Limites técnicos

O nick acima da cabeça não é substituído: os nametags do Core continuam sendo prefixo/sufixo de team vanilla. É possível configurar AFK no sufixo, mas isso não cria suporte a trocar o nome-base do personagem. Não há implementação de vanish no Essentials.

A fronteira de API aceita somente a thread do servidor. Leituras visuais usam perfis já carregados e cache por tick, sem consulta de arquivos ou JDBC no render; só comandos explícitos alteram persistência. Falha/incompatibilidade com Essentials instalado bloqueia a rota de chat do Core conservadoramente; atualize o par de JARs. Sem Essentials instalado, o Core preserva seu comportamento original.

Validação e limites de cliente real são descritos em TESTES.md. Os clientes de protocolo locais não certificam aparência gráfica ou assinatura criptográfica de um cliente autenticado.
