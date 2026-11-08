# Estado real do projeto — 0.13.2-dev

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
