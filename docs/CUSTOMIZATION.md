> Atualização 0.14.0: cargos ROLE são fixos pelo grupo principal LuckPerms;
> SPECIAL/CLAN são opcionais somente no chat. Veja [RANKS.md](RANKS.md).
> As regras antigas de seleção visual abaixo são históricas e foram substituídas.

# Personalização persistida — 0.11.0-dev

O catálogo administrativo `config/shirokai-core/presets.json` define presets com
IDs estáveis. O banco grava somente IDs, nunca markup fornecido pelo jogador.
Os templates continuam no StyleEngine seguro; dados entram como texto literal.

## Escopos e precedência

- TITLE: um preset pessoal envolve títulos ROLE/SPECIAL já equipados, no preview,
  nas duas identificações do chat e no título visual compartilhado TAB/sidebar.
  Não seleciona ou concede títulos e não cria slots adicionais.
- CLAN_NAME e CLAN_TAG: escolhas independentes do clã, visíveis para seus membros.
  CLAN_TAG também estiliza sua identificação nativa em chat/TAB/sidebar.
  Nome/sigla nos placeholders ricos dos HUDs e no cartão do clã usam essas escolhas.
- Não há escolha pessoal independente de título por HUD. Não há alteração de
  grupos LuckPerms, corpo de mensagem assinada, nome de conta ou markup do jogador.

Sem escolha salva, valem os templates/regras administrativos de presentation.json.
Com escolha válida, vence o preset escolhido. Se a permissão faltar, usa o fallback
gratuito do catálogo. Se o preset foi removido ou deixou de cobrir a superfície,
o estado é REMOVED e também usa fallback. O ID continua salvo em ambos os casos.
Restaurar a permissão ou reintroduzir o mesmo ID restaura o estilo automaticamente.
Um arquivo/template inválido rejeita o reload inteiro: não é publicado parcialmente.
ADMINISTRATIVE, ACTIVE, UNAVAILABLE e REMOVED aparecem no menu.

## Catálogo

Schema1; todos os campos são obrigatórios; até100presets/256KiB. Exemplo completo:

```json
{
  "schemaVersion": 1,
  "fallback": "basic",
  "presets": {
    "basic": {
      "permission": "",
      "surfaces": ["TITLE", "CLAN_NAME", "CLAN_TAG"],
      "template": "<aqua>%visual:value%</aqua>"
    },
    "gold": {
      "permission": "shirokaicore.style.gold",
      "surfaces": ["TITLE", "CLAN_NAME", "CLAN_TAG"],
      "template": "<gold><bold>%visual:value%</bold></gold>"
    },
    "sunset": {
      "permission": "shirokaicore.style.sunset",
      "surfaces": ["TITLE", "CLAN_NAME", "CLAN_TAG"],
      "template": "<gradient:#ff7733:#ffccff>%visual:value%</gradient>"
    }
  }
}
```

Esses são os defaults gerados apenas se o arquivo estiver ausente. IDs:
`[a-z][a-z0-9_-]{0,63}`; `padrao` é reservado. Campo permission vazio é público;
outros nós são definidos pelo administrador, inclusive nós clan.style específicos.
O fallback precisa cobrir as três superfícies, ter permissão vazia e usar somente
formatação básica, sem HEX/gradiente. Templates exigem exatamente um
`%visual:value%`, nenhum outro placeholder/animação, até128caracteres literais;
também se aplicam os limites do StyleEngine. Ações/fontes/tags desconhecidas são rejeitadas.

Além da permission do preset, HEX de clã exige `shirokaicore.clan.color.hex`;
gradiente de clã exige `shirokaicore.clan.color.gradient`. Cores básicas públicas
ficam disponíveis a todos os líderes. A permissão efetiva do estilo compartilhado
é a do líder atual, não a do membro que está olhando. O serviço prepara o usuário
offline do líder ao carregar associação; dados/contextos indisponíveis negam premium.
Não há resultado de permissão congelado no cache de render.

## Comandos e menus

- `/estilo titulo`: abre o seletor pessoal.
- `/estilo nome` e `/estilo sigla`: seletores do clã atual, somente líder.
- Acrescente um ID para salvar diretamente: `/estilo titulo gold`.
- `/estilo sigla padrao`: remove somente essa escolha e volta à política administrativa.
- /perfil oferece botão de estilo; /clan oferece botões independentes de nome/sigla.

Console não escolhe por jogador. /estilo já existente de outro mod é preservado.
O menu lista presets inclusive bloqueados, mostra ID salvo/estado/revisão e preview,
tem paginação e opção de voltar ao padrão. Clique esquerdo escolhe; movimentações,
outro jogador, slots inválidos e tela fechada não alteram preferências.

Escolhas de clã são exclusivas do líder, independentemente de roleActions de edição
de identidade textual. O banco revalida associação, UUID exato do clã e liderança
na transação. Menu antigo de outro clã não pode agir no clã novo. Revisão CAS da linha
impede sobrescrever escolhas concorrentes, inclusive entre nome e sigla.
Mudança de catálogo invalida telas e escritas pendentes da geração anterior.
Permissões/configuração são verificadas na thread do servidor imediatamente antes
de enfileirar; não há transação distribuída SQLite/LuckPerms. Uma revogação posterior
não apaga ID e já impede o estilo premium no próximo render.

## Persistência e ciclo de vida

Schema6 cria player_style_choices e clan_style_choices. Esta última tem FK/cascade
para o UUID do clã. Migração1..5 faz VACUUM INTO antes da transação; schema futuro
é rejeitado. Escolha, revisão e auditoria são atômicas; falha de auditoria faz rollback.
Excluir clã remove preferências; renomear/transferir mantém IDs e reavalia o líder.
Preferências de títulos, animações, sidebar, chat e membros existentes são preservadas.

Caches de seleção só atendem jogadores online/clãs referenciados por suas sessões;
join/ações/eventos de clã carregam dados e disconnect descarta referências.
Nenhuma consulta SQLite por frame, tick ou render de estilo. TAB observa diferenças
no intervalo configurado; sidebar é invalidada em mudanças. /estilo exige
styles/storage/permissions ativos; módulo clans só é necessário para os dois escopos de clã.

## Limites

Esta entrega seleciona presets estáticos de títulos e identidades de clã.
Animação de presets, personalização separada do nome de conta/cabeçalho/menu,
transições e edição arbitrária de cores não foram implementadas. A animação existente
dos layouts de TAB/sidebar continua disponível. Não se reescreve histórico do chat.
Testes em servidor usam jogadores/conexões sintéticos; cliente real, autenticação,
assinatura criptográfica, escala e carga continuam pendentes.

Desde0.13.2, uma entrada por ID em presentation.titleTemplates tem precedência
sobre o preset pessoal para essa identificação. Tags sem entrada e escolhas salvas
continuam preservadas. Ver PRESENTATION.md e examples/presentation-ranks.json.
