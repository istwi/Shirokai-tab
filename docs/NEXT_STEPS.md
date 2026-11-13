## Continuidade 0.18.0-dev

Ponte com Essentials 0.3.0 implementada em ESSENTIALS.md. Instalar o par de JARs;
revalidar com cliente gráfico real, especialmente tooltips, TAB e layouts customizados.
Não converter apelidos em identidades de conta/seletores nem apagar punições do outro mod.
PAPI externa continua no namespace shirokai; essentials:* é nativo dos templates Core.

## Continuidade 0.17.1-dev

Validar no cliente real os novos tooltips e IVs. Preservar validação de slot antes
de qualquer entrega e resumo independente do hover vanilla; CHAT_SHARES_MOTD.md.

## Continuidade 0.17.0-dev

Marcações próprias e MOTD implementados: [guia](CHAT_SHARES_MOTD.md).
Validar hover/cores da mensagem e indicador de modificação num cliente real;
consultar multiplayer em momentos diferentes para ver os quadros. Não prometer
animação contínua do protocolo vanilla. Preservar destinatários filtrados e
contexto do autor. Não publicar ambiente/release sem nova solicitação.

> 0.16 entregou as três etapas solicitadas: histórico/diagnóstico/reparos seguros
> de clãs, presets animados/políticas por área e integração opcional Drex Vanish.
> As etapas1..3 listadas abaixo são o plano histórico, não tarefas a refazer.
> Próximo: cliente real, carga, reconexões e conflitos de mods. Não afirmar
> vanish com Core sozinho nem ampliar versões do provider sem auditoria.

> 0.15.2 evita gravar teams efêmeros próprios no save; RANKS.md documenta os
> critérios de ownership e a liberação única dos vínculos antigos já persistidos.

> 0.15.1 acrescenta terceira moeda cash independente; INTEGRATIONS.md documenta
> defaults, compatibilidade e consulta sem criação de contas.

> 0.15.0: perfil configurável em menus.json; menu/editor de clã pausados por
> padrão até terminar essa área. Não reativar automaticamente. Consulte MENUS.md.

> 0.14.1 corrige colisão de nomes de teams após liberação do jogador; RANKS.md
> documenta migração manual dos vínculos antigos. Não reintroduzir nomes globais skn.

> 0.14.0 substitui ROLE selecionável por cargo principal fixo. Não reaplicar
> a regra antiga de título visual opcional. Consulte RANKS.md para migração.

# Próximos passos

## Estado atual

0.13.1-dev fixa nick branco sem outras mudanças; a0.13.0 acrescentou chat compacto [G]/[L], cores &, /g e /l e avisos [+]/[-].
CHAT_APPEARANCE.md define configuração, assinatura e filtragem. Os extras0.12 já entregam listed por viewer, nametags de prefixo/sufixo/visibilidade,
BELOW_NAME privado e bossbar pessoal, opt-in em tab-extras.json. APIs1.21.1 e
ownership foram auditados/testados. Não reaplicar esses recursos nem a personalização,
editor textual, convites e administração inicial0.11. SQLite continua6.
TAB_EXTRAS.md documenta os extras, CHAT_APPEARANCE.md o novo layout; PROJECT_STATUS contém testes/SHA.

## Próximas etapas de implementação

1. Administração/configurações de clãs restantes conforme REQUIREMENTS.md: diagnóstico
   de integridade, reparos relacionais explícitos/transacionais e histórico consultável.
   Nunca transferir liderança/excluir por atalho que contorne aceitação/confirmação.
   list/inspect/rename/tag já existem com permissões próprias, auditoria e CAS.
2. Ampliar personalização de forma segura: animação de presets pessoais/clã quando
   viável, campos separados além dos títulos e controles/permissões por superfície.
   Não animar histórico assinado nem confundir animação de layout com preset persistido.
3. Integração legítima de vanish de entidades, se necessária: confirmar API do mod
   real escolhido. UPDATE_LISTED da0.12 só altera a lista; não oculta tracking/chat/
   contagem online. Não presumir privacidade global nem revelar listagens externas.
4. Cliente real: menus/editor anvil, listagem/nametags/BELOW_NAME/bossbar/sidebar,
   gradientes/escala/actionbar, autenticação/assinaturas e contextos LP. Testar
   reconexão/troca de mundo de cliente, multijogador, desempenho e conflitos de mods.
5. Novas versões de providers: auditar artefatos/APIs antes de ampliar suporte.
   Pokédex mantém provider/coleção explícitos; dollars/tokens são moedas distintas.

## Regras de continuidade

Pedido visual0.13 autoriza cores &0..f/&r limitadas: não usar parser de markup ou
formatos com ações. unsignedContent visual só para códigos autorizados e não filtrados/
máscara pass-through; original/signature/mask não mudam. Cor padrão usa chat_type.
Moderar texto canônico sem códigos; /g e /l não podem ignorar os gates dos canais.
Join/quit reusam broadcast vanilla, não duplicar avisos. Teste dos callbacks merged
não equivale a login/logout de cliente. Atualização do JAR exige reinício para tipos
sincronizados de chat e mixins; não sobrescrever arquivos personalizados.


Preservar título único TAB/sidebar e até duas identificações, associação clan_ real,
perda de permissão visual sem apagar IDs, CAS/geração de catálogo e UUID de clã da tela.
Clãs gratuitos; cooldowns independentes; redução de limite não expulsa; transferência
aceita e exclusão confirmada. Editor anvil não move ícones/XP, Esc cancela.

Extras são desligados por padrão; enabled exige reinício e templates/condições
aceitam reload atômico. Teams de nametag são globais e influenciam ordenação/mecânicas;
mutação externa revoga ownership. BELOW_NAME e sidebar privados cedem a slots externos;
colisão exata suspende até reconectar. Bossbars têm identidades independentes.
Ledger de objetivos usa retain/release: não apagar evidência do outro consumidor.
Hooks observam sem IO/jogo; game/network/caches só na thread do servidor, sem SQL por frame.

Testes Fabric sintéticos não comprovam cliente/login/assinatura/contextos/carga.
Falha de DNS/publickeys não autoriza desativar online-mode. Sem desabilitar TLS,
mudar checksums/JARs ou fabricar integrações para passar. Preserve mixins/refmap1.21.1.
Salvar progresso autorizado em main sem reescrever histórico; não publicar ambiente
ou release automaticamente, nem reaplicar o setup publicado.

0.13.2 acrescenta titleTemplates por ID (PRESENTATION.md), com precedência sobre
presets pessoais. Conferir cores no cliente real com examples/presentation-ranks.json.
