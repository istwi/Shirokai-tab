# Estado atual — 0.14.1-dev

Reproduzida colisão de teams antigos: após liberar membro com /team leave, o nome
antigo continua ocupado e impede nova nametag/ordenação. Regressão falhava antes
com "Stale team name blocked nametag after explicit release". Agora cada instância
escolhe namespace de nametag livre, sem adotar/remover teams antigos por nome.
Recupera prefixo e ordenação por prioridade/peso depois da liberação administrativa.
Status TAB informa nametags=true/false. Não altera JSONs personalizados ou SQLite6.

Validação: build/smokeClasses aprovados; 105 testes JUnit sem falhas/erros e
48 verificações Fabric em três boots. Inclui team antigo vazio, rank OWNER
antes de ADMIN, preservação externa, restart simulado com teams salvos, recuperação
após liberação e cleanup próprio. Integrações opcionais:14 verificações históricas
0.14.0, não repetidas; nenhum adaptador opcional foi alterado. Cliente real pendente.

Instalação: substituir JAR e reiniciar; liberar uma vez os jogadores ainda presos
nos teams antigos identificados. Depois a atualização periódica normal reassume
nametags/ordenação. RANKS.md contém os comandos e os limites dessa migração.

JAR: build/libs/shirokai-core-0.14.1-dev.jar
SHA256: 94ce2130d9db276076183899b759ac111bd63d7af991061cb3c57ed64b0a664b
Artefato inspecionado: versão correta/refmap e nenhuma fixture smoke.
Base:3bc1c88. Histórico de versões anteriores abaixo.

---

# Estado real do projeto — 0.14.0-dev

Cargo principal do LuckPerms agora é automático e fixo no TAB/sidebar/chat/nametag,
sem seleção no perfil. ROLE configurado personaliza o rótulo; sem definição, usa
[GRUPO] e ID do grupo para titleTemplates. Presets pessoais não alteram ROLE.
Eventos e identificação nativa de clã são opcionais exclusivamente no chat.
Mantido limite de duas identificações totais: cargo + uma opcional (sem metadados,
até duas opcionais). /perfil e sugestões excluem ROLE; limpar não remove cargo.

Nametags agora habilitadas por padrão em tab-extras schema2. Schema1 com nametag
igual ao antigo padrão desligado é promovido em memória; opt-out explícito por
schema2 false e configs personalizadas preservados. Teams externos continuam
preservados. Acima da cabeça usa prefixo vanilla na mesma linha do nome.
SQLite permanece6; seleções visuais antigas e ROLE opcionais são normalizados
online. Concessões/presets preservados; consulta offline não limpa seleções.

## Validação desta entrega

- Build/smokeClasses aprovados, 105 testes JUnit, sem falhas/erros.
- Suite Fabric: 48 verificações, incluindo reinício e falha isolada de storage.
- Integrações opcionais reais: 14 verificações, incluindo mudança de cargo
  via LuckPerms5.4.140, pesos e invalidação de cache da API na fixture. Cinco mods
  oficiais/53 dependências aninhadas revalidados sem alterar artefatos.
- Perfil: ROLE escondido, cliques seguros, tags só no chat; TAB/nametag mudam de
  OWNER para ADMIN sem equipar; estilos pessoais não mudam o padrão de ROLE.
- Artefato inspecionado: versão/refmap presentes, sem classes smoke em produção.

JAR: build/libs/shirokai-core-0.14.0-dev.jar
SHA256: c902d1d27b7948af8496172cde2c933a76b728a178cf57dcbea5ba3230fb4841

Relatórios locais: build/reports/tests/test, build/server-smoke/result.json e
build/integration-smoke/result.json. Testes Fabric usam jogadores sintéticos;
cliente autenticado, visual no jogo, contextos LP de clientes e carga não foram
certificados. DNS/publickeys pode falhar; online-mode permanece habilitado.
RANKS.md documenta comandos, primary group, pesos, migração e configuração.
Base: ef900cd. Histórico abaixo preserva evidências das versões anteriores.

---

# Estado real do projeto — 0.13.4-dev

## Entrega atual

Correção0.13.1 solicitada pelo usuário: somente nick branco em todos os canais,
remetente/destinatário do privado e avisos de conexão. Indicadores, tags, mensagem,
cores &, assinaturas/filtragem e roteamento permanecem como na0.13.0. A apresentação
usa slots brancos explícitos; o fallback literal agora mantém componentes separados
para não herdar a cor do canal no nick. Smoke existente verifica branco e as cores
anteriores dos indicadores/corpo, incluindo todos os canais.


Chat ajustado ao exemplo visual do usuário. Configuração/semântica completas em
CHAT_APPEARANCE.md, sem alterar SQLite6 ou dependências externas.

- [G] cinza e [L] amarelo; formato [canal] [identificações] Nick: mensagem, sem < >
  vanilla. Tags selecionadas preservam seus estilos e continuam limitadas a duas.
  Cor padrão alcança o corpo por MessageType.Decoration sincronizado, sem substituir
  signedBody/signature/filterMask ou adicionar unsignedContent para texto normal.
- /g e /l selecionam/enviam como /global e /local, com mensagem opcional e os mesmos
  gates. Aliases externos são preservados e os atalhos persistem no registro após
  reload de datapacks. Alcance/dimensão do local permanecem os mesmos.
- Cores &0..f/&r solicitadas, escape &&, sem tags/ações/§. Permissão configurável,
  pública por default. Código válido/autorizado adiciona Text colorido unsigned
  somente para destinatários não filtrados e máscara pass-through. Original e
  assinatura permanecem intactos; cliente pode indicar modificação visual.
  Filtrados recebem conteúdo original/códigos literais, sem bypass da máscara.
- Filtro e duplicação usam texto sem códigos válidos, impedindo palavra bloqueada
  ou mensagem repetida disfarçada por cores. Limite de tamanho inclui código bruto;
  mensagem vazia após remoção é recusada. Mute/ignore/staff/clã não são alterados.
- [+] verde na entrada e [-] vermelho na saída, com nome literal cinza. Dois mixins
  locais substituem o Text do broadcast vanilla existente, sem mensagem duplicada.
  Variante joined.renamed também usa nome atual. Flag desativa o override; bridge
  é liberada ao encerrar o módulo. Anúncios externos com outro conteúdo são preservados.
- chat-appearance.json/schema1 é criado somente se ausente e participa do reload
  atômico. Os dois formatos antigos exatamente iguais aos defaults são promovidos
  em memória para [G]/[L], sem reescrever arquivos ou formatos/regras personalizados.
  Primeira instalação precisa reiniciar para carregar chat_type e novos mixins.

Personalização/clãs/TAB/sidebar/integrações0.12 preservados. Sem alteração ou publicação de configuração de ambiente; a entrega de desenvolvimento
será disponibilizada pelo workflow de pré-release já solicitado pelo usuário.

## Verificação executada

- Build/JUnit: **104 testes,0falhas,0erros,0ignorados**.
- Smoke básico: **48 verificações em3boots Fabric**, incluindo registry/decoração
  [G]/[L]/cores reais dos componentes, body/signature/mask originais, unsigned
  opcional/autorização revogada, filtragem/alcance, comandos e callbacks transformados
  de entrada/saída. Reload de configuração inválida preserva o estado ativo.
  Regressões de clãs/presets/editor/chat/TAB/nametags/BELOW_NAME/bossbar/sidebar passam.
- Smoke com mods opcionais reais: **14 verificações históricas da0.13.0**; não
  repetido nesta correção de cor, que não altera adaptadores/dependências.
- Preparação revalidou cinco mods/53dependências aninhadas fixadas sem mudar bytes/hashes/TLS.
- JAR inspecionado: versão0.13.1, refmap e quatro mixins de produção,16chat_types,
  somente SQLite aninhado e nenhuma fixture/classe smoke.

Evidências: build/reports/tests/test/index.html, build/server-smoke/result.json,
build/integration-smoke/result.json e logs dos runners. O workflow de pré-release executa novamente build/JUnit no GitHub antes de anexar
o artefato e publicá-lo. O workflow0.12 anteriormente passou e publicou pré-release.
A pré-release0.12 não contém os ajustes0.13.

## Artefato

build/libs/shirokai-core-0.13.1-dev.jar — desenvolvimento, não release estável.
SHA-256: ee5ad4f9c2e9e4085a61a2dacc6ac237f93daf9c7cfd09b1036d7072cc1385de.
Base desta correção:e56c77666c62a7e5f79f41ef1d4a5ff871b4562c.
Commit final é informado na entrega, sem hash autorreferente aqui.

## Limites e continuidade

Projeto completo NÃO está entregue. Presets animados/administração relacional
avançada/vanish de entidades e validação real continuam em NEXT_STEPS.md.
Não há resource pack obrigatório para o formato de chat, mas renderização/indicador
visual, login/logout/autenticação/assinatura criptográfica e contextos de clientes
reais não foram certificados. Testes usam jogadores/conexões sintéticos; callbacks
merged dos mixins são chamados isoladamente, sem efetuar login autenticado.
Não confundir preservação de signature field com verificação criptográfica real.

Authlib ainda pode falhar por DNS/publickeys; online-mode foi preservado. Outros
mods que substituem avisos/chat precisam de decisão de ownership. Chat do Core
não cria vanish global. Não há transação distribuída SQLite/LP/providers externos.
Código/JAR mantêm Minecraft1.21.1/Fabric/Java21 e APIs externas fixadas.

## GitHub

Commit/push de código/testes/docs autorizados para main sem reescrever histórico.
Publicação de pré-release de desenvolvimento segue a solicitação do usuário;
não publicar release estável nem configuração de ambiente.

0.13.2: titleTemplates opcional por ID, antes de presets/regras, sem migração SQLite.
Build0.13.2 passou:105 testes JUnit, sem falhas. Smoke Fabric de estilos passou
com OWNER/ADMIN independentes e precedência sobre preset salvo.
JAR: build/libs/shirokai-core-0.13.2-dev.jar
SHA256:800ffeb052c2e14520055681e228015b364bea09213ebe248b9970e72362b1ee.
Renderização em cliente real continua pendente.

Suite server_smoke0.13.2:48 verificações aprovadas, incluindo reinício e isolamento
de falha de storage; relatório em build/server-smoke/result.json.

0.13.3: recuperação de View ausente na scoreboard durante update/toggle, sem schema
novo. Regressão Fabric cobre JOIN antes de roster e dois toggles sem View prévia.
Build0.13.3 aprovado (105 testes JUnit). SCOREBOARD_SMOKE_PASS no Fabric,
incluindo recuperação de JOIN antes do roster, toggle sem View e preferência
desativada preservada no reconnect. Cliente real permanece pendente.
SHA256 do JAR local:020bf738c6f60b718be3e91bb178974b4cc4feecd4c18fe1f3d8ec314c7919c9.

Suite server_smoke0.13.3:48 verificações aprovadas, incluindo reinício/storage.

0.13.4 revisa apresentação das mensagens comuns e fichas de clã/perfil.
Não altera schemas, permissões, limite de tags ou confirmações destrutivas.
Build final aprovado:105 testes JUnit. Suite server_smoke:48 verificações
aprovadas; última tradução de menus verificada adicionalmente por quatro smokes
Fabric (perfil, menu de clã, estilos e apresentação), MESSAGES_NATIVE_SMOKE_PASS.
Aparência em cliente real continua pendente.
SHA256 do JAR local:06458291ac369fa3383116ce1e7f2d606a7c1783118a360b609fe17c9ed71c99.

## Investigação: DEFAULT no TAB/nametag após promover novo jogador

Regressão adicionada para entrada tardia, DEFAULT -> ADMIN e reordenação de vários
jogadores por peso; TAB e prefixos globais acompanharam corretamente. Suite
Fabric completa aprovada novamente:48 verificações em três inicializações.
Não houve mudança de produção/JAR nesta investigação. Causa no servidor relatado
ainda não confirmada; aguardando versão e status TAB do servidor afetado.
