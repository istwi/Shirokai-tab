# Próximos passos

## Estado atual

0.12.0-dev entrega listed por viewer, nametags de prefixo/sufixo/visibilidade,
BELOW_NAME privado e bossbar pessoal, opt-in em tab-extras.json. APIs1.21.1 e
ownership foram auditados/testados. Não reaplicar esses recursos nem a personalização,
editor textual, convites e administração inicial0.11. SQLite continua6.
TAB_EXTRAS.md documenta configurações/efeitos/limites; PROJECT_STATUS contém testes/SHA.

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
