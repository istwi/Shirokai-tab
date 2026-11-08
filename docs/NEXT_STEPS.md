# Próximos passos

## Estado atual

0.10.0-dev adiciona apresentação configurável de chat/perfil/clãs/títulos, compondo
cores/HEX/gradientes com valores literais, condições e fallback dinâmico. TAB/sidebar
usam o mesmo título formatado. Chat normal preserva corpo/assinatura/máscara e
menções notificam separadamente; comandos system podem colorir menções.
Regras/configuração em PRESENTATION.md. Integrações0.9 e módulos anteriores preservados.
Schema SQLite permanece5; não foram criadas escolhas pessoais de estilo.

## Próxima entrega

1. Personalização persistida por jogador/clã: presets permitidos, escolha por ID,
   estilos independentes de nome/sigla do clã e preservação da escolha após expiração
   da permissão. Exige autorização fresca, escopo/CAS, transações e testes de migração/
   restart; não aceitar markup do jogador como configuração executável.
2. Ferramentas administrativas/editor textual de clãs, convites enviados e demais
   requisitos completos. Preservar escopo da tela, confirmações e cooldowns independentes.
3. Recursos adicionais de TAB: listed/vanish por viewer, nametags, objetivos abaixo
   do nome e bossbar, sem substituir controle de outros mods.
4. Cliente real: aparência de menus/TAB/sidebar/gradientes/actionbar, autenticação,
   contextos LuckPerms com conexão real, mudanças de mundo, espectador, carga e conflitos.
5. Novas versões do Shirokai Dex e APIs externas: confirmar contrato público antes
   de ampliar a lista de versões suportadas. Fonte da Pokédex deve continuar explícita.

## Limitações preservadas

A apresentação atual é política administrativa. Não declarar escolha pessoal/editor
concluídos nem confundir regras que permanecem no JSON com preferência de jogador no DB.
Não há animação de menus/histórico de chat nem reescrita visual de corpo assinado.
Campos de packets testados não comprovam assinatura criptográfica válida ou renderização.

Adapters externos foram exercitados com JARs reais em0.9, mas login/renderização/carga
continuam pendentes. Authlib pode falhar por DNS; não desabilitar online-mode para mascarar.
Economia não tem fallback primário nem transação distribuída; hasAccount/account não são
atômicos contra exclusão concorrente externa. Não acessar arquivos internos dos providers.

Sidebar cede a SIDEBAR/TEAM_* externo; colisão exata suspende nessa conexão. Teams do TAB
são opt-in. Preservar mixins/refmap e versão de Minecraft; nunca remover dados externos.

Salvar progresso autorizado em istwi/Shirokai-tab/main, sem reescrever histórico.
Não publicar ambiente ou release automaticamente. Não reaplicar setup publicado.
