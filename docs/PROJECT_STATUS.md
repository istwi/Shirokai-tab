# Estado atual — 0.16.7-dev

Cabeças informativas de perfil/clã/estilos usam o GameProfile do jogador que abriu
a tela, incluindo cópia das propriedades de texturas, sem lookup adicional. API
Minecraft1.21.1 ProfileComponent(GameProfile) auditada por javap no JAR mapeado.
Apenas itens PLAYER_HEAD recebem o componente; ícones customizados preservados.

Clã: exclusão movida para19, vidro cinza para não líderes. Ícones de estrela
substituídos por papel. Seta28 volta ao /menu externo; cabeça10 atualiza os dados.
Confirmação/autorização de exclusão mantidas. Layout/JSONs dos demais menus e
SQLite6 preservados. Sem migração nova de config necessária.

Build aprovado:124 testes JUnit e51 verificações Fabric em três boots.
Fixtures verificam UUID da cabeça do observador em perfil e clã,
vidro para não líder, transferência e confirmação da exclusão no slot novo.
Skin renderizada e /menu externo ainda dependem de validação com cliente real.

JAR: build/libs/shirokai-core-0.16.7-dev.jar
SHA256: ae9c9336d976e4c102db615947517387024a8ebfea833c3f7bb3889accf728d4
Base:ec96200. Substituir JAR e reiniciar, sem apagar menus.json/banco.

---

# Estado atual — 0.16.6-dev

Corrige perfil que permanecia em seis linhas por ter título personalizado, como
“✦ PERFIL”. Migração reconhece rows/tagSlots/slots de botões antigos, preservando
título, textos, itens e modelos de tags; move apenas geometria. Botões ainda iguais
aos defaults recebem as novas dicas. Preserva clanMenuEnabled. Backup
menus.json.pre-0.16.6.bak e troca atômica antes de publicar o perfil carregado.
Geometrias com posições personalizadas permanecem intactas.

Build aprovado:124 testes JUnit sem falhas, com regressão para título personalizado,
backup, migração idempotente e preservação de geometria customizada. Sem novo smoke
Fabric nesta correção de carregamento; UI0.16.5 já validada em51 verificações.

JAR: build/libs/shirokai-core-0.16.6-dev.jar
SHA256: adbae9a89b8cbd3377a35e08037aedd70ac6324d51c7a565e5ab312d3bb9097d
Base:07dc803. Substituir JAR e reiniciar, sem apagar menus.json/banco.

---

# Estado atual — 0.16.5-dev

Perfil e estilos compartilham painel de cinco linhas: cabeça10, quadro19, seta28,
vidro cinza na moldura/divisória e15 opções à direita. Paginação36/44 contextual.
Perfil: cabeça atualiza; quadro esquerdo abre estilos, direito limpa tags; seta
usa /menu externo. Estilos: cabeça informa escolha, quadro usa padrão, seta volta
a /perfil ou /clan conforme o escopo. Escolhas por ID/permissões/CAS preservadas.

MenuConfig migra somente o perfil exatamente igual ao padrão antigo, guardando
menus.json.pre-0.16.5.bak e preservando clanMenuEnabled. Troca atômica do arquivo.
Layouts customizados permanecem intactos; examples/menus.json fornece o novo modelo.
Menus abertos guardam o layout exibido. SQLite6/schema JSON1 preservados.

Build:124 testes unitários sem falhas, incluindo migração/backup/preservação.
51 verificações Fabric aprovadas em três boots; perfil/estilos/apresentação
validados nos slots novos, com preservação do layout aberto após reload.
Renderização com cliente real ainda não certificada.

JAR: build/libs/shirokai-core-0.16.5-dev.jar
SHA256: f92e4eb16cc7258eab6bfefad6b57449225ecb3067baebff66f1930748d6a247
Base:e6ea08d. Substituir JAR e reiniciar, sem apagar banco/configurações.

---

# Estado atual — 0.16.4-dev

/clan e /clan menu abrem o painel somente para quem pertence a um clã. Sem
associação, mostram orientação para /clan create <nome>, /clan invites e
/clan accept <nome>. Membros que entraram por convite também acessam o painel.
Sair/exclusão/expulsão invalida o menu; ações/reload sem associação fecham a tela.
A checagem usa a associação carregada do SQL na abertura e o cache do servidor
em canUse. Comandos textuais de criação/convites/aceitação continuam disponíveis.

Build aprovado:123 testes JUnit e51 verificações Fabric em três boots.
Fixture de clã verifica entrada por convite, navegação e fechamento
após exclusão. Layout0.16.3 preservado. Sem alteração de SQLite6 ou JSONs existentes.
Renderização/login autenticado de cliente real permanecem pendentes.

JAR: build/libs/shirokai-core-0.16.4-dev.jar
SHA256: 33faa6a4be8522cd056b2a632d8b97cfd1df28c2b966ecfb5d975a52d6ac01e4
Base:b92989d. Instalação: substituir JAR e reiniciar, preservando configurações.

---

# Estado atual — 0.16.3-dev

Ajuste visual solicitado: slots24,25,30,31,32,33 do painel principal ficam vazios,
sem pane. Demais ícones, moldura, permissões e telas permanecem como0.16.2.
Build e123 testes unitários aprovados. Sem nova execução Fabric para este ajuste
decorativo; validação de cliques da0.16.2 permanece registrada abaixo.

JAR: build/libs/shirokai-core-0.16.3-dev.jar
SHA256: 14c989801c2b9dbe23fdbecd5200992760f12fba92794ce9a2cfcf05766cd40c
Base:1d9b5f0. Substituir JAR e reiniciar, sem apagar JSONs ou banco.

---

# Estado atual — 0.16.2-dev

Menu principal do clã segue a referência: cinco linhas/45 slots e vidro cinza.
Cabeça10 informa dados do clã, bússola19 atualiza, bloco de redstone28 fecha e
barreira34 exclui com confirmação. Oito estrelas12..16/21..23 oferecem funções;
estilos de nome/sigla reunidos em submenu. Nome e sigla estilizados ficam na lore
da cabeça. Ações indisponíveis deixam o vidro no lugar, sem burlar permissões.

Listas com27 entradas por página, navegação36/44 e atualizar40/voltar41. Ofertas
de liderança usam37/38 somente quando pendentes. Histórico mantém sua tela própria.
Sem alteração de SQLite6 ou JSONs existentes. MENUS.md contém o mapa dos slots.

Build:123 testes unitários sem falhas;51 verificações Fabric aprovadas em três
boots. Fixtures de clã/apresentação validam novos slots, ações e cores na lore.
Renderização em cliente real ainda não certificada.

JAR: build/libs/shirokai-core-0.16.2-dev.jar
SHA256: 3b44eceb5bd9dbd25ffc2e32751cb5086f8d67378c0f2e985141d0ed72b55f42
Base:7ea21c0. Instalação: substituir JAR e reiniciar, preservando configurações.

---

# Estado atual — 0.16.1-dev

Menu de clã redesenhado: moldura escura com detalhes ciano, estrela central e
botões distribuídos por função. Exclusão vermelha isolada, retorno distinto de
fechar, paginação contextual e estado de lista vazia. Itens decorativos sem ações.
Nome/sigla continuam usando apresentação/presets do servidor. MENUS.md lista slots.

Build:123 testes unitários sem falhas;51 verificações Fabric em três boots
aprovadas. Smoke de clãs verifica novas posições, convites, editor, histórico,
transferência e exclusão.
Ainda não há validação visual em cliente real. Layout é inventário vanilla;
não altera a textura da janela nem exige resource pack.

JAR: build/libs/shirokai-core-0.16.1-dev.jar
SHA256: d29ec6d518b027dc0c5545a1d5893537e468df9c56fbc69753ad85aa5e2d3796
Base:eaf29d9. SQLite6 e JSONs existentes preservados; clanMenuEnabled continua
controlando a ativação. Instalação: substituir JAR e reiniciar.

---

# Estado atual — 0.16.0-dev

Clãs: histórico paginado GUI/CLI, diagnóstico SQLite/FKs e de relações, reparos
seguros com prévia/token120s, fingerprint dos dados, transação e auditoria. Nunca
muda líder, move membros ou exclui clãs por reparo. Problemas inseguros são MANUAL.
Menu/editor ativos por padrão em arquivos novos; false existente é preservado.
Para reativar uma instalação antes pausada, menus.json clanMenuEnabled=true e reload.

Presets pessoais/clã: frames/frameTicks, permissão de animação, previews vivos e
personalization.json para TAB/SCOREBOARD/NAMETAG/CHAT/MENU. ID preservado no SQL6;
ROLE fixo permanece administrativo. Chat captura um quadro sem reescrever histórico.

Vanish: integração opcional real Drex Vanish1.5.9+1.21.1, entidade/tracking a cargo
do provider externo. Sem esse mod, Core não oferece vanish. Visibilidade respeitada
em chat/privado/menções, TAB/contagem por observador e seletores/autocomplete de clã.
Não há garantia de privacidade em outros mods, ping ou comandos vanilla. VANISH.md
contém instalação, hash, API e limitações. Provider não é empacotado no Core.

Validação local:
- build/smokeClasses:123 testes JUnit, zero falhas/erros.
- 51 verificações Fabric em três boots: inclui histórico GUI sem extração,
  frames/snapshot de chat, políticas independentes e regressão de teams/saves.
- 14 verificações com mods opcionais reais: LuckPerms, PAPI, Impactor, Cobblemon e Dex.
- 7 grupos de verificações com Vanish real: API/mixins de entidade, roster, chat,
  autocomplete, contagens/caches por observador, teams e unvanish.
- JAR de produção: versão/refmap conferidos, sem fixtures smoke.

Fixtures usam jogadores/conexões sintéticos. Login autenticado, renderização real,
contextos LP multijogador, carga e conflitos universais continuam pendentes.
Logs/resultados locais: build/server-smoke, build/integration-smoke, build/vanish-smoke.

JAR: build/libs/shirokai-core-0.16.0-dev.jar
SHA256: 77953848d9159f1dd352aaef4edefd1a4fbeb3583ddb7ee80aebb1f4f8625913
Base:2431883. Sem mudança de SQL6 nem reescrita dos JSONs existentes.
0.15.2-dev foi publicada antes desta etapa em releases/tag/v0.15.2-dev.
Histórico de versões anteriores abaixo.

---

# Estado atual — 0.15.2-dev

Corrigida persistência de teams efêmeros durante save da scoreboard. Reproduzido
antes do patch: Temporary Core team leaked into world save. Teams salvos podem
voltar sem ownership na nova instância e manter prefixos/ordenação antigos.
ScoreboardTeamPersistence filtra exclusivamente o NBT de teams ainda controlados
por TeamOwnership, com aparência completa inalterada e sem integrantes externos.
Não altera times/jogadores ao vivo durante save, não adota por prefixo e preserva
teams externos, substituídos, modificados ou mistos. Trackers usam weak references.

Contrato Minecraft1.21.1/Yarn1.21.1+build.3 auditado por javap: ScoreboardState,
field scoreboard, método privado teamsToNbt(RegistryWrapper.WrapperLookup):NbtList.
Mixin server-only/refmap verificados no JAR; hook executado na regressão Fabric.

Validação atual:
- build/smokeClasses: 113 testes JUnit aprovados, sem falhas/erros.
- 49 verificações Fabric em três boots. Save de team próprio com jogador
  online não inclui o team no NBT nem altera a aparência atual; restore do save
  e OWNER -> ADMIN funcionam sem team leave. Teams externos, alterações externas
  de prefixo/displayName e integrantes estrangeiros permanecem persistidos.
- 14 verificações com mods opcionais reais aprovadas novamente.
- JAR: versão/refmap/novo mixin corretos, sem fixtures smoke.

Instalação: substituir JAR/reiniciar sem apagar JSONs. Para membros ainda presos
a teams antigos já gravados, liberação administrativa uma vez (/team leave Nick).
Os novos teams próprios não serão persistidos por saves normais. Conflitos reais
de ownership causados por outro sistema continuam sendo preservados/diagnosticados.
Sem mudanças em SQL6, configurações de moedas ou menus; menu de clã segue pausado.
Cliente autenticado/renderização real ainda não certificados. RANKS.md detalha limites.

JAR: build/libs/shirokai-core-0.15.2-dev.jar
SHA256: d215fc746fe6d45a2ae49f255ed4c4b1fb816d3220f522ff61bf96212a8dee4a
Base:9ec8a67. Histórico das versões anteriores abaixo.

---

# Estado atual — 0.15.1-dev

Terceira moeda independente: cashCurrency em integrations.json (impactor:cash por
padrão) e %shirokai:cash%, exportado também pelo Fabric PAPI. Usa o mesmo adaptador
Impactor5.3.5 auditado: sem criar contas/moedas, sem trocar por saldo de outra moeda,
sem consultas SQL por render. Cash null desativa a terceira consulta. Mapeamento
pode mudar por reload, que invalida sessões/callbacks antigos.

JSON antigo sem cashCurrency continua válido e não é reescrito. Se já usa
impactor:cash em dollarsCurrency/tokensCurrency, preserva esses aliases e deixa
cash desativado, evitando conflito; configure três IDs distintos para habilitar
todos. Schema JSON permanece1, SQLite6; menu de clã segue pausado.

Validação atual:
- build/smokeClasses aprovados; 113 testes JUnit sem falhas/erros.
- 14 verificações de integração reais aprovadas:125.25dollars,42.5tokens e0cash,
  chave/valor PAPI, reload para cash inexistente e restauração sem saldo antigo,
  conta cash ausente não criada e cleanup no disconnect.
- 49 verificações Fabric aprovadas em três boots, incluindo reinício/storage.
- JAR inspecionado: versão correta e nenhuma classe smoke em produção.

JAR: build/libs/shirokai-core-0.15.1-dev.jar
SHA256: 2e6f0d32d90eadb59eaa18f686a1a5b3497d1142f3e5d3f759cd5f89b1d6da15
Documentação: INTEGRATIONS.md; exemplo completo: examples/integrations.json.
Instalação exige substituir JAR/reiniciar. Moeda cash deve existir no Impactor;
configurar o ID não cria moeda. Cliente autenticado/renderização/carga pendentes.
Base:9433297. Histórico das versões anteriores abaixo.

---

# Estado atual — 0.15.0-dev

menus.json configura o /perfil: título,1..6linhas, posições das tags e dos sete
controles, itens, nomes, lore, cores/degradês e placeholders seguros. Ações são
fixas no servidor; ícones não saem do menu, ROLE segue não selecionável e escolhas
são revalidadas pelo TitleService. Planos são snapshots por tela, evitando que
reload altere os cliques de um inventário já aberto. Fechar/reabrir usa o novo layout.

Menu/editor de clã pausados por padrão (clanMenuEnabled=false). Comandos, dados,
membros, identificações de clã e schema SQLite6 preservados. Aberturas assíncronas
revalidam o bloqueio, cliques antigos são recusados e reload para false fecha as
telas desses tipos. Fixtures de GUI habilitam explicitamente o legado para testes.

Validação da entrega:
- build/smokeClasses: 111 testes JUnit sem falhas/erros.
- Fabric: 49 verificações em três boots; layout3linhas, ícones/nomes/slots
  alterados, paginação, ações remapeadas, inventário preservado e snapshot de tela.
- Reload: slots inválidos ou item inexistente rejeitam o reload inteiro, preservam
  config ativa; configuração válida e pausa do clã sobrevivem ao reinício.
- Integrações opcionais reais: 14 verificações aprovadas novamente.
- JAR inspecionado: versão correta, MenuConfig e refmap, sem classes smoke.

JAR: build/libs/shirokai-core-0.15.0-dev.jar
SHA256: fb7c6c5517eb847ed4ad90391defc24539f21afeac1fd1d67a79416a24e96813
Exemplo: examples/menus.json; documentação: MENUS.md. Arquivos existentes não são
sobrescritos. Instalação exige substituir JAR/reiniciar; edição de menus aceita
/shirokaicore reload e reabertura do perfil. Cliente real/visual ainda não certificado.
A investigação de atualização de teams segue pausada; não houve mudança nessa área.
Base:26432be. Histórico das versões anteriores abaixo.

---

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
