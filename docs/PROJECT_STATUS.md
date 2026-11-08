# Estado real do projeto — 0.10.0-dev

## Situação

Apresentação configurável de cabeçalhos de chat, menus de perfil/clã e identificações
adicionada. Valores continuam literais; seleção/permissões/roteamento são preservados.
TAB/sidebar compõem o mesmo título formatado. Integrações0.9 e módulos anteriores
preservados. O projeto completo NÃO está entregue: escolhas pessoais de estilos,
editor/administração avançada de clãs, recursos extras e cliente real seguem pendentes.

## Implementado nesta etapa

- presentation.json com superfícies, regras por prioridade/permissão/grupo/dimensão,
  flags e permissão de menção. Todos os candidatos, inclusive regras inativas, compilam
  antes do reload atômico; arquivo existente nunca é sobrescrito. Uso em PRESENTATION.md.
- StyleEngine compõe slots Text sem reanalisar dados, remove click/hover/insertion e
  não altera Text compartilhado. Caminho de strings/cache anterior permanece disponível.
- Cabeçalhos juntam as duas identificações selecionadas já validadas/ordenadas e seus
  estilos. Não acrescentam cargo/prefixo fora dos dois slots. Duplicação de tags, corpo
  e animações nos templates de chat são rejeitados; dados não são comandos ou markup.
- Chat normal usa o mesmo SentMessage/withoutUnsigned: corpo, signature field e máscara
  não são alterados para estilizar menções. Menção vira overlay separado apenas para
  destinatário alcançado; destinatário sujeito a filtragem não recebe essa notificação.
- Mensagens system continuam não assinadas. Podem colorir @Nome conhecido sem mudar
  caracteres ou interpretar tags, e corpo não herda o clique do autor. Todos os gates
  anteriores (mute/ignore/spam/alcance/staff/clã/privado) continuam na mesma rota.
- Perfil aplica estilos de menus e preview do título. Cartão do clã permite templates
  independentes para nome e sigla. Ações, proteção de inventário, escopo/CAS e confirmações
  não mudaram. Botão de retorno do perfil mantém identidade exigida pelo pedido original.
- Títulos compartilhados entre TAB/sidebar usam componentes formatados do TitleService.
  Perda de permissão visual aplica fallback sem apagar seleção ou regra administrativa;
  entitlement de título continua independente. Não cria escolha pessoal/editor persistido.
- Nenhuma mudança de schema SQLite (permanece5), dependência, protocolo ou mixin de produção.

## Validação desta etapa

- Build/JUnit:84testes executados,0falhas,0erros,0ignorados na rodada concluída.
- Smoke sem opcionais:42checks aprovados em3boots reais Fabric, incluindo restart,
  backups/schema futuro, regressões de todos os módulos e reload inválido de presentation.
- Smoke com mods reais:12checks aprovados na rodada0.10, sem regressões dos adapters.
- PresentationPlanTest valida arquivo/tipos/condições/fallback, candidatos inválidos,
  proibição de corpo/animações/tags repetidas e rich slots com Unicode/cores/imutabilidade.
- PresentationSmoke verifica cores de duas identificações, corpo/signature field/máscara/
  unsignedContent, filtragem de menção, privado/local/mute, perdas/retorno de permissão,
  remoção de ações de Text, título em TAB/sidebar e previews de perfil/nome/sigla de clã.
- HoverEvent depende dos registries Minecraft; seu teste foi mantido no smoke Fabric
  depois do diagnóstico Not bootstrapped no JUnit cru, sem desativar o cenário.

Evidências atuais: build/reports/tests/test/index.html, build/server-smoke/result.json,
build/integration-smoke/result.json e logs de cada runner. Conexões/jogadores sintéticos,
não autenticação, assinatura criptográfica válida ou cliente gráfico real.

## Artefato

`build/libs/shirokai-core-0.10.0-dev.jar`, de desenvolvimento, NÃO release estável.
SHA-256: `308bb14a5982ec3f09f4b0e73d4fe54dbe618373d564b631c79c918e961a6b12`.
JAR inspecionado: versão/refmap corretos, sem classes smoke e somente SQLite aninhado.

## Limitações e próxima entrega

A política visual desta etapa é administrativa. Preservar uma regra no JSON quando
uma permissão expira não equivale a uma escolha pessoal gravada no DB. O próximo passo
é presets/seleções por jogador/clã com autorização, persistência e testes transacionais/
restart, incluindo estilos independentes de nome/sigla. Não aceitar markup do jogador.

Não há animação de menus/histórico nem reescrita visual do corpo assinado. Menus abertos
reavaliam ao atualizar/reabrir; renderização/escala/fonte/actionbar com cliente real e
carga continuam pendentes. Assinaturas de verdade e contextos LP autenticados não foram
validados; o authlib ainda pode falhar por DNS, sem desativar online-mode.

Contratos/versões de adapters externos em INTEGRATIONS.md. Fonte da Pokédex e moedas
continuam explícitas; não há fallback primário, dados fictícios, acesso privado ou
transação distribuída. Sidebar cede a objetivos externos e teams do TAB seguem opt-in.
Administração/editor de clãs e extras de TAB permanecem em NEXT_STEPS.md. CI remoto
não verificado. Nenhuma release ou configuração de ambiente publicada/reaplicada.

## GitHub

Base desta etapa:f36fd1e em istwi/Shirokai-tab. Salvar progresso em main sem reescrever
histórico, conforme autorização. Configuração de ambiente e publicação continuam separadas.
