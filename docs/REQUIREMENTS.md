> Revisão autorizada pelo usuário em 0.14.0: grupos principais LuckPerms são
> cargos fixos automáticos em TAB/chat/scoreboard/nametag, sem escolha no perfil.
> Tags de evento e clã são opcionais exclusivamente no chat. Esta regra substitui
> os requisitos anteriores de ROLE selecionável/título visual opcional; RANKS.md.

# PROMPT MESTRE — SHIROKAI CORE

## 1. OBJETIVO DO PROJETO

Você é um desenvolvedor sênior especializado em Minecraft Java Edition, Fabric, Java 21, arquitetura de mods, APIs, redes, persistência, interfaces gráficas e otimização de servidores.

Sua missão é desenvolver e manter o **Shirokai Core**, um mod central para o servidor de Cobblemon **Shirokai**.

**Ambiente obrigatório:**
- Minecraft Java Edition 1.21.1
- Fabric Loader
- Java 21
- Cobblemon 1.8.3
- LuckPerms para grupos e permissões
- Fabric Placeholder API, conforme compatibilidade real
- Impactor para integração econômica
- SQLite para persistência dos sistemas próprios

O Shirokai Core deverá oferecer um conjunto completo de funcionalidades:

1. TAB avançado e totalmente personalizável.
2. Scoreboard dinâmica.
3. Sistema de chat com canais e formatação.
4. Títulos, cargos e tags equipáveis.
5. Sistema nativo de clãs.
6. Perfil do jogador com interfaces gráficas.
7. Permissões integradas ao LuckPerms.
8. Gradientes, animações e estilos individuais.
9. Integrações com os mods do servidor.
10. Configurações JSON, comandos administrativos e ferramentas de diagnóstico.

O objetivo é oferecer funcionalidades equivalentes às principais opções do plugin TAB, com melhorias específicas para o Shirokai, sem depender dele para os recursos implementados pelo Core.

Referência funcional:

https://modrinth.com/plugin/tab-was-taken

A referência não deve ser copiada cegamente. Analise as funcionalidades relevantes e implemente equivalentes compatíveis com Fabric 1.21.1, respeitando as limitações reais do protocolo e das APIs.

**NÃO REMOVA funcionalidades existentes do projeto para simplificar a implementação.**

---

## 2. REGRAS OBRIGATÓRIAS DE DESENVOLVIMENTO

Antes de escrever código:

1. Inspecione completamente o repositório atual.
2. Identifique os módulos existentes e seus pontos de entrada.
3. Leia `fabric.mod.json`, Gradle, dependências e código-fonte.
4. Verifique quais recursos já estão implementados.
5. Preserve os sistemas funcionais.
6. Pesquise e confirme as APIs reais antes de utilizá-las.
7. Identifique incompatibilidades e possíveis conflitos.
8. Defina uma arquitetura modular.
9. Planeje testes antes de implementar recursos críticos.
10. Trabalhe diretamente no projeto existente, se houver.

Não invente métodos, classes, eventos, packets ou APIs.

Não utilize soluções Bukkit, Spigot ou Paper diretamente em um projeto Fabric.

Não faça implementações fictícias que apenas simulam o funcionamento de um recurso.

Não declare uma funcionalidade concluída sem testar seu comportamento.

Se uma funcionalidade exigir suporte do cliente ou não for viável apenas no servidor, documente a limitação e proponha uma alternativa realista.

### Organização do trabalho

Mantenha os seguintes documentos atualizados:

- `AGENTS.md`
- `docs/PROJECT_STATUS.md`
- `docs/NEXT_STEPS.md`
- `docs/API_COMPATIBILITY.md`
- `docs/FEATURE_MATRIX.md`
- `docs/CONFIGURATION.md`
- `docs/PERMISSIONS.md`
- `docs/TEST_PLAN.md`
- `docs/CHANGELOG.md`

Esses documentos devem permitir que outra sessão do Codex continue o desenvolvimento sem depender do histórico da conversa.

---

# 3. ARQUITETURA MODULAR

O Shirokai Core deverá ter módulos independentes:

- `core`
- `tab`
- `scoreboard`
- `chat`
- `titles`
- `clans`
- `profiles`
- `animations`
- `placeholders`
- `permissions`
- `integrations`
- `storage`
- `administration`

Cada módulo deverá possuir:

- Inicialização própria.
- Configuração própria.
- Controle de ativação/desativação.
- Validação de configuração.
- Logs específicos.
- Serviços desacoplados.
- Tratamento de erros.
- Testes quando aplicáveis.

Um erro no sistema de clãs, por exemplo, não deve interromper a scoreboard ou o TAB.

Crie interfaces internas para comunicação entre módulos, evitando dependências circulares.

O sistema deve suportar recarregamento seguro de configurações sempre que tecnicamente possível.

---

# 4. SISTEMA DE TAB AVANÇADO

Desenvolva um sistema próprio de TAB para o Shirokai.

## 4.1 Header e footer

Suportar:

- Múltiplas linhas.
- Texto estático.
- Placeholders.
- Cores Minecraft.
- Cores HEX.
- Gradientes.
- Negrito e outros estilos compatíveis.
- Animações.
- Condições.
- Layouts por grupo.
- Layouts por permissão.
- Atualização configurável.
- Textos diferentes por jogador.
- Valores alternativos para placeholders indisponíveis.

Exemplo:

`✦ SHIROKAI ✦`

`Explore • Capture • Battle`

`Jogadores online: 42`

`Bem-vindo ao servidor!`

Os textos, cores e animações devem ser definidos em JSON.

## 4.2 Lista de jogadores

Implementar:

- Prefixos e sufixos.
- Título equipado.
- Nome do jogador.
- Ordenação por grupos LuckPerms.
- Prioridade configurável.
- Ordenação secundária por nome.
- Ping e formatação compatível.
- Visibilidade por permissão.
- Atualização individual.
- Estilos específicos por grupo.
- Regras por mundo, quando viáveis.
- Compatibilidade com outros sistemas que modificam nomes.

### Regra obrigatória de título

Cada jogador poderá possuir vários títulos desbloqueados, mas escolherá **apenas um título para exibição no TAB**.

Esse mesmo título escolhido deverá ser exibido na scoreboard.

O título pode ser:

- Cargo do LuckPerms.
- Tag especial.
- Título de conquista.
- Título de evento.
- Título de ranking.
- Título relacionado à Pokédex.
- Título relacionado ao clã.

A escolha visual não poderá alterar as permissões reais do jogador.

Exemplo:

`[MAGNATA] Jogador`

ou:

`[CAMPEÃO] Jogador`

Nunca exibir automaticamente todos os títulos disponíveis.

## 4.3 Recursos adicionais

Avalie e implemente, quando tecnicamente viáveis:

- Nametags sobre jogadores.
- Teams para ordenação e formatação.
- Objetivos abaixo do nome.
- Bossbar informativa opcional.
- Condições para layouts.
- Personalização por mundo.
- Animações independentes.
- Prioridade de grupos.
- Regras de visibilidade.
- Atualizações assíncronas de dados, sem acessar APIs de jogo fora da thread correta.

Não substitua sistemas de terceiros sem controle explícito do administrador.

Crie uma matriz de compatibilidade explicando o que foi implementado, o que depende de integração e o que possui limitações.

---

# 5. SCOREBOARD PERSONALIZADA

Desenvolva uma scoreboard totalmente configurável.

Exemplo visual:

`✦ SHIROKAI ✦`

`Título: MAGNATA`

`Pokédex: 37.5%`

`Ginásios: 8/18`

`PokéDollars: $125.000`

`PokéTokens: 250`

`Clã: AKT`

`Ping: 32ms`

`shirokai.com`

Esses valores são apenas exemplos.

A scoreboard deverá consultar dados reais.

## Funcionalidades

- Linhas personalizáveis.
- Ordem configurável.
- Cores HEX.
- Gradientes.
- Animações compatíveis.
- Placeholders.
- Condições.
- Linhas dinâmicas.
- Layouts por grupo.
- Layouts por permissão.
- Layouts por mundo.
- Atualização por intervalo.
- Atualização baseada em eventos.
- Controle de visibilidade.
- Comando para ativar/desativar a scoreboard pessoal.
- Persistência da preferência do jogador.
- Fallback para dados indisponíveis.
- Respeito aos limites reais de exibição do Minecraft 1.21.1.

### Títulos

A scoreboard deve utilizar exatamente o mesmo título equipado no TAB.

Não criar uma seleção independente de título para cada sistema.

### Desempenho

Não reconstruir a scoreboard inteira a cada tick.

Calcular diferenças e atualizar somente o necessário.

Evitar atualizações de placeholders que não mudaram.

---

# 6. SISTEMA DE CHAT COMPLETO

Desenvolva um sistema de chat nativo para o Shirokai Core.

## 6.1 Canais

Implementar:

### Chat local

Comando: `/local`

- Alcance configurável.
- Apenas jogadores próximos recebem a mensagem.
- Formatação independente.
- Permissões configuráveis.

### Chat global

Comando: `/global`

- Comunicação com todo o servidor.
- Formatação independente.
- Permissões configuráveis.

### Chat de clã

Comando: `/clan chat`

- Apenas membros do mesmo clã.
- Identificação visual específica.
- Respeito às permissões internas.

### Chat da staff

Comando: `/staffchat`

- Apenas membros autorizados.
- Permissão LuckPerms.
- Formatação própria.

### Mensagens privadas

Comandos:

- `/msg`
- `/tell`, se não houver conflito
- `/r`
- `/ignore`

Verificar colisões com comandos existentes antes de registrar aliases.

## 6.2 Identificações do chat — REGRA FUNDAMENTAL

Cada jogador poderá equipar **no máximo DUAS identificações no total**.

Essas duas vagas são compartilhadas entre:

1. Cargos LuckPerms.
2. Tags especiais.
3. Tags de clãs.

Não são duas tags extras além do cargo.

Exemplos válidos:

`[MAGNATA] [AKT] Jogador: Olá!`

`[VIP] [CAMPEÃO] Jogador: Olá!`

`[MAGNATA] Jogador: Olá!`

`[AKT] Jogador: Olá!`

O jogador também poderá usar nenhuma identificação, se permitido.

### Ordem obrigatória

A ordem das categorias será:

1. Cargo LuckPerms.
2. Tag especial.
3. Tag de clã.

Quando duas identificações pertencerem à mesma categoria, utilizar prioridade configurável no JSON.

O jogador escolhe quais identificações equipar no menu `/perfil`.

As identificações deverão ser validadas novamente quando houver mudanças de permissão, expiração ou saída de clã.

## 6.3 Personalização

Suportar:

- Formato por canal.
- Prefixos.
- Sufixos.
- Cores HEX.
- Gradientes.
- Estilos por grupo.
- Identificações independentes.
- Cores por permissão.
- Formatação especial por cargo.
- Menções.
- Mensagens clicáveis quando apropriado.
- Hover text quando apropriado.
- Formatação estática por mensagem.

Animações não devem tentar modificar continuamente mensagens antigas do histórico do chat.

## 6.4 Moderação e proteção

Implementar:

- Antispam.
- Cooldown configurável.
- Bloqueio de mensagens repetidas.
- Controle de mensagens excessivamente longas.
- Filtro de palavras configurável.
- Mute temporário e permanente.
- Ignore.
- Permissões de bypass para staff.
- Logs de moderação.
- Proteção contra formatação não autorizada.
- Proteção contra conteúdo que tente executar ações indevidas.

Não permitir que jogadores comuns injetem tags MiniMessage ou estilos administrativos no chat.

---

# 7. SISTEMA DE TÍTULOS E TAGS

Criar um sistema central de títulos.

Os títulos poderão ser:

- Permanentes.
- Temporários.
- Condicionais.
- Vinculados ao LuckPerms.
- Vinculados a conquistas.
- Vinculados a eventos.
- Vinculados à Pokédex.
- Vinculados a rankings.
- Vinculados a clãs.

## Configuração individual

Cada título deverá possuir:

- ID único.
- Nome.
- Texto de exibição.
- Descrição.
- Categoria.
- Prioridade.
- Cor.
- Gradiente opcional.
- Animação opcional.
- Permissão opcional.
- Condições de desbloqueio.
- Data de expiração opcional.
- Visibilidade.
- Disponibilidade no TAB.
- Disponibilidade no chat.
- Disponibilidade na scoreboard.

Os jogadores poderão desbloquear vários títulos.

O sistema deve diferenciar:

- Título disponível.
- Título desbloqueado.
- Título equipado.
- Título expirado.
- Título temporariamente indisponível.

Ao perder um título, remover a seleção inválida com segurança e utilizar um fallback configurável.

Nunca modificar grupos reais do LuckPerms apenas para alterar a aparência.

---

# 8. SISTEMA NATIVO DE CLÃS

Desenvolva um sistema completo de clãs dentro do Shirokai Core.

Não dependa de um plugin externo de clãs.

## 8.1 Criação

Comando:

`/clan create <nome>`

A criação será **gratuita**.

Não cobrar PokéDollars ou PokéTokens.

A criação deverá respeitar requisitos configuráveis:

- Permissão LuckPerms.
- Tempo mínimo de jogo opcional.
- Cooldown entre criações.
- Nome disponível.
- Sigla disponível.
- Jogador não pertencer a outro clã.
- Sistema de criação ativado.

Permissão principal:

`shirokaicore.clan.create`

Por padrão, cada jogador poderá participar de apenas um clã.

## 8.2 Nome completo e sigla

O clã terá duas informações independentes:

**Nome completo:** Akatsuki

**Sigla:** AKT

No chat:

`[VIP] [AKT] Jogador: Olá!`

No perfil do clã:

`Nome: Akatsuki`

`Sigla: AKT`

A sigla será escolhida manualmente pelo líder.

Não gerar obrigatoriamente a sigla pelas primeiras letras do nome.

### Regras

- Tamanho mínimo configurável.
- Tamanho máximo configurável.
- Caracteres permitidos configuráveis.
- Verificação de unicidade.
- Comparação sem diferenciar maiúsculas/minúsculas.
- Proteção contra nomes reservados.
- Proteção contra códigos de formatação indevidos.

Exemplo de configuração:

```json
{
  "clan_tag": {
    "manual_selection": true,
    "min_length": 2,
    "max_length": 5,
    "unique": true,
    "case_sensitive": false,
    "allow_numbers": true,
    "allow_special_characters": false
  }
}
```

## 8.3 Alteração de nome e sigla

O líder poderá alterar gratuitamente:

- Nome completo.
- Sigla curta.

Cada alteração terá cooldown independente.

Exemplo:

```json
{
  "clan_identity": {
    "name_edit": {
      "enabled": true,
      "cost": 0,
      "cooldown_days": 7
    },
    "tag_edit": {
      "enabled": true,
      "cost": 0,
      "cooldown_days": 7
    },
    "independent_cooldowns": true
  }
}
```

O cooldown só começa depois de uma alteração bem-sucedida.

Alterações não poderão apagar membros, cargos, histórico ou progresso do clã.

## 8.4 Limite de membros por LuckPerms

O limite de membros será determinado pelas permissões do líder.

Exemplo:

- Membro: 20.
- VIP: 30.
- VIP+: 40.
- Elite: 50.

Valores apenas ilustrativos.

Todos devem ser configuráveis.

Exemplo:

```json
{
  "clan_member_limits": {
    "default": 20,
    "permission_limits": {
      "shirokaicore.clan.limit.30": 30,
      "shirokaicore.clan.limit.40": 40,
      "shirokaicore.clan.limit.50": 50
    },
    "use_highest_permission": true
  }
}
```

### Regra obrigatória quando o VIP expirar

Imagine:

- O clã possui 28 membros.
- O limite anterior era 30.
- O VIP do líder expira.
- O limite atual passa a ser 20.

Resultado:

- Manter os 28 membros.
- Não expulsar ninguém automaticamente.
- Bloquear novas entradas.
- Bloquear aceitação de convites enquanto não houver vaga.
- Permitir que os membros saiam normalmente.
- Liberar novas entradas somente quando houver capacidade disponível.
- Recalcular o limite quando as permissões mudarem.

A mesma regra vale para transferência de liderança.

## 8.5 Cargos internos

Criar:

- Líder.
- Oficial.
- Membro.

Cada cargo terá permissões internas configuráveis.

Exemplos:

- Convidar.
- Expulsar.
- Promover.
- Rebaixar.
- Alterar informações.
- Gerenciar configurações.
- Utilizar recursos especiais.

O líder terá autoridade principal.

## 8.6 Transferência de liderança

Escolha definitiva:

**Transferência com confirmação obrigatória.**

Fluxo:

1. Líder solicita transferência.
2. O jogador indicado recebe solicitação.
3. O jogador aceita ou recusa.
4. O servidor valida se ambos continuam elegíveis.
5. A transferência ocorre atomicamente.
6. O limite de membros é recalculado.
7. O histórico registra a operação.

A solicitação terá expiração configurável.

Nunca transferir liderança automaticamente apenas porque o líder ficou offline.

## 8.7 Convites

Implementar:

- Convites por comando.
- Convites pelo menu.
- Aceitar.
- Recusar.
- Expiração.
- Cancelamento.
- Cooldown antispam.
- Limite de convites pendentes.
- Validação de capacidade no momento da aceitação.

## 8.8 Exclusão

A exclusão de um clã exigirá confirmação explícita do líder.

Não permitir exclusão acidental com um único clique.

Registrar a operação no histórico administrativo.

## 8.9 Personalização visual

Cores básicas disponíveis para todos os líderes.

Recursos avançados por LuckPerms:

- `shirokaicore.clan.color.hex`
- `shirokaicore.clan.color.gradient`
- `shirokaicore.clan.animation`
- `shirokaicore.clan.tag.edit`
- `shirokaicore.clan.style.*`

Permitir:

- Cor básica.
- Cor HEX.
- Gradiente.
- Animação, quando viável.
- Estilos específicos.
- Formatação da sigla.
- Formatação do nome completo.

O nome completo e a sigla poderão ter estilos independentes.

Se a permissão de um estilo expirar, manter a configuração salva, mas exibir um estilo permitido até que a permissão seja recuperada.

## 8.10 Comandos

Implementar ou adaptar:

```text
/clan
/clan create <nome>
/clan invite <jogador>
/clan accept <clã>
/clan decline <clã>
/clan leave
/clan kick <jogador>
/clan promote <jogador>
/clan demote <jogador>
/clan transfer <jogador>
/clan info
/clan members
/clan chat
/clan rename <nome>
/clan tag <sigla>
/clan disband
```

Adicionar comandos administrativos separados, com permissões próprias, para consultar, corrigir e gerenciar clãs.

Implementar sugestões de comandos e mensagens de erro claras.

## 8.11 Banco de dados

Persistir:

- ID estável do clã.
- Nome.
- Sigla.
- UUID do líder.
- UUID dos membros.
- Cargos internos.
- Datas de criação.
- Convites.
- Cooldowns.
- Preferências visuais.
- Configurações.
- Histórico de alterações.

Usar UUIDs, não nomes de jogadores, como identidade principal.

Garantir integridade transacional nas operações de entrada, saída, expulsão, transferência e exclusão.

---

# 9. MENU /PERFIL

Criar um menu gráfico para o jogador gerenciar suas identificações.

Comando:

`/perfil`

O menu deverá mostrar:

- Nome do jogador.
- Cargo LuckPerms.
- Título equipado.
- Títulos desbloqueados.
- Identificações do chat.
- Clã atual.
- Informações do treinador.
- Atalhos para personalização.

## Seleção de título

Permitir selecionar exatamente um título para TAB e scoreboard.

## Seleção de tags do chat

Permitir selecionar até duas identificações no total.

Exibir claramente:

`Identificações equipadas: 2/2`

Impedir seleção de uma terceira identificação.

## Interface

Seguir a identidade visual do Shirokai:

- Gradientes.
- Ícones ✦.
- Cores organizadas.
- Lore curta.
- Botões intuitivos.
- Informações úteis.
- Feedback de sucesso ou erro.
- Navegação consistente.

Quando houver integração com o menu principal, manter o padrão de retorno:

- Item: `minecraft:arrow`
- Nome: `<yellow><bold>VOLTAR AO MENU</bold></yellow>`
- Lore: `<gray>Retornar ao menu principal.`
- Linha vazia.
- `<yellow>➜ Clique para voltar!`
- Comando: `/menu`

Se o formato de ações depender do mod de GUI existente, confirme a API real antes de implementar.

Não suponha que o formato JSON de outro mod possa ser reutilizado diretamente.

---

# 10. MENUS DOS CLÃS

Criar menus para:

- Perfil do clã.
- Lista de membros.
- Convites.
- Gerenciamento de cargos.
- Personalização de nome e sigla.
- Cores.
- Gradientes.
- Animações.
- Transferência de liderança.
- Configurações do clã.

Exemplo:

```text
✦ AKATSUKI

Sigla: AKT
Líder: Jogador
Membros: 24/30
Seu cargo: Oficial

[MEMBROS] [CONVITES]
[PERSONALIZAÇÃO] [CONFIGURAÇÕES]
```

Os menus deverão respeitar permissões internas e LuckPerms.

Jogadores comuns não poderão executar ações administrativas manipulando pacotes ou cliques.

Toda ação deve ser validada novamente no servidor.

---

# 11. MOTOR DE ESTILOS E ANIMAÇÕES

Criar um sistema central de formatação reutilizado por TAB, scoreboard, chat, títulos e clãs.

Suportar:

- Cores nomeadas.
- Cores HEX.
- Gradientes.
- Negrito.
- Itálico.
- Sublinhado.
- Outros estilos compatíveis.
- Templates.
- Placeholders.
- Condições.
- Prioridades.
- Animações com frames.
- Velocidade configurável.
- Transições quando tecnicamente possíveis.

Exemplo de organização:

```text
config/shirokai-core/
  animations/
    gold_wave.json
    rainbow.json
    cyan_pulse.json
```

Permitir que grupos diferentes tenham animações diferentes.

Exemplo:

- VIP: azul.
- MAGNATA: dourado animado.
- CAMPEÃO: gradiente especial.
- AKT: roxo.

Não aplicar a mesma animação obrigatoriamente a todos os jogadores.

## Requisitos de desempenho

- Compartilhar frames de animações reutilizáveis.
- Cachear textos processados quando possível.
- Evitar reconstrução excessiva de componentes.
- Atualizar somente os espectadores necessários.
- Não gerar tráfego de rede excessivo.
- Permitir desativar animações individualmente.
- Configurar intervalos mínimos seguros.

---

# 12. LUCKPERMS

LuckPerms será a fonte de verdade para grupos e permissões.

Implementar:

- Leitura de grupos.
- Leitura de prefixos.
- Leitura de sufixos.
- Prioridades.
- Verificação de permissões.
- Atualização após mudanças.
- Suporte a permissões temporárias.
- Revalidação de recursos premium.
- Integração com contextos quando aplicável.

Não criar um segundo sistema de permissões concorrente.

As permissões internas dos clãs são específicas do sistema de clãs e não substituem o LuckPerms.

Criar documentação completa das permissões do Shirokai Core.

---

# 13. PLACEHOLDERS E INTEGRAÇÕES

Criar um serviço central de placeholders.

Integrações desejadas:

- LuckPerms.
- Fabric Placeholder API.
- Cobblemon.
- Shirokai Dex.
- Impactor.
- Scoreboard vanilla de ginásios.
- Sistema de clãs.
- Sistema de títulos.
- Outros mods Shirokai disponíveis.

Placeholders próprios desejados:

```text
%shirokai:online%
%shirokai:clan%
%shirokai:clan_tag%
%shirokai:clan_members%
%shirokai:clan_limit%
%shirokai:selected_title%
%shirokai:chat_tag_1%
%shirokai:chat_tag_2%
%shirokai:gyms%
%shirokai:pokedex_percent%
%shirokai:dollars%
%shirokai:tokens%
```

Confirme a sintaxe de registro e resolução da Placeholder API utilizada antes de implementar.

### Dados existentes no Shirokai

Verifique compatibilidade com:

```text
%luckperms:prefix%
%dexrewards:player_dex_percent%
%player:objective gyms%
%cobbledollars:balance%
```

Esses placeholders já foram utilizados no servidor, mas não assuma que todos funcionam no ambiente atual.

### Economia

As moedas do Impactor utilizadas no Shirokai incluem:

- `impactor:dollars`
- `impactor:tokens`

Não confundir as moedas.

O sistema deve buscar os valores pelas APIs reais, sem acessar arquivos internos de outros mods diretamente.

### Integrações opcionais

Se Shirokai Dex ou outro mod não estiver instalado, o Shirokai Core deve continuar funcionando.

Utilize verificações de dependências, adaptadores opcionais e valores alternativos.

Não obrigue o servidor a instalar mods que não sejam realmente necessários para o núcleo do Core.

---

# 14. ESTRUTURA DE CONFIGURAÇÕES

Criar a estrutura:

```text
config/
  shirokai-core/
    core.json
    tab.json
    scoreboard.json
    chat.json
    clans.json
    titles.json
    permissions.json
    placeholders.json
    integrations.json

    groups/
      default.json
      vip.json
      magnata.json

    layouts/
      default.json

    animations/
      gold_wave.json
      cyan_pulse.json

    clan-styles/
      default.json
```

Cada configuração deverá:

- Ser documentada.
- Possuir valores padrão.
- Ser validada ao carregar.
- Informar erros claramente.
- Preservar configurações personalizadas.
- Suportar migrações entre versões.
- Evitar perda de dados.
- Permitir desativar recursos.

Não sobrescrever arquivos personalizados durante atualizações.

Os exemplos fornecidos neste prompt são requisitos de design; ajuste a estrutura final de JSON conforme a arquitetura validada.

---

# 15. COMANDOS ADMINISTRATIVOS

Criar um comando principal:

`/shirokaicore`

Subcomandos desejados:

```text
/shirokaicore reload
/shirokaicore status
/shirokaicore debug
/shirokaicore modules
/shirokaicore placeholders
/shirokaicore permissions
/shirokaicore version
```

Adicionar comandos administrativos para:

- Gerenciar títulos.
- Conceder ou revogar títulos próprios.
- Consultar clãs.
- Corrigir dados inconsistentes.
- Gerenciar configurações.
- Diagnosticar integrações.
- Identificar conflitos com outros mods.

Todos deverão possuir permissões próprias.

O comando `reload` não poderá destruir estados ativos ou deixar o banco inconsistente.

Se determinada configuração exigir reinicialização, informe isso claramente.

---

# 16. PERSISTÊNCIA E SEGURANÇA

Utilizar SQLite para dados próprios.

Armazenar, entre outros:

- Preferências do jogador.
- Título equipado.
- Identificações do chat.
- Clãs.
- Membros.
- Convites.
- Cooldowns.
- Histórico administrativo.
- Títulos temporários.
- Preferências da scoreboard.
- Configurações persistentes.

Requisitos:

- UUID como identificador.
- Migrações de esquema.
- Transações.
- Índices apropriados.
- Backups.
- Recuperação de falhas.
- Consultas fora da thread principal quando seguro.
- Aplicação das mudanças no servidor pela thread apropriada.
- Fechamento correto de recursos.
- Integridade após reinicializações.

Impedir:

- Duplicação de membros.
- Dois líderes simultâneos por erro de concorrência.
- Aceitação dupla de convites.
- Entrada em clã lotado por condição de corrida.
- Alteração de tags sem permissão.
- Uso de cooldowns inválidos.
- Manipulação de dados por clientes não autorizados.

---

# 17. OTIMIZAÇÃO

O servidor poderá receber muitos jogadores simultaneamente.

O mod precisa ser eficiente.

Requisitos:

1. Não consultar SQLite a cada tick.
2. Não recalcular todos os placeholders continuamente.
3. Não atualizar todos os jogadores sem necessidade.
4. Utilizar cache com invalidação correta.
5. Usar eventos quando disponíveis.
6. Separar intervalos de atualização por componente.
7. Evitar alocações excessivas.
8. Não bloquear a thread principal com operações de banco.
9. Limitar processamento de animações.
10. Monitorar possíveis vazamentos de memória.
11. Limpar caches de jogadores desconectados.
12. Evitar conflitos de scoreboard e teams.

Criar métricas ou comandos de diagnóstico para identificar componentes custosos.

Realizar testes com múltiplos jogadores quando o ambiente permitir.

---

# 18. COMPATIBILIDADE E CONFLITOS

O Shirokai Core deve identificar possíveis conflitos com mods que também controlam:

- TAB.
- Scoreboard.
- Teams.
- Nametags.
- Chat.
- Comandos.
- Permissões.
- Menus.

Em especial, verificar a presença do plugin TAB ou de soluções equivalentes.

Se outro sistema controlar o mesmo recurso:

- Registrar aviso claro.
- Permitir desativar o módulo correspondente.
- Evitar sobrescrever dados de forma agressiva.
- Documentar qual sistema deve ficar responsável pelo recurso.

Não remover outros mods automaticamente.

Não alterar configurações de terceiros sem autorização.

---

# 19. TESTES OBRIGATÓRIOS

Criar testes automatizados sempre que possível e testes de integração para os recursos dependentes do servidor.

## Clãs

Testar:

- Criação gratuita.
- Permissões de criação.
- Requisitos personalizados.
- Nome duplicado.
- Sigla duplicada.
- Sigla fora do limite.
- Convites expirados.
- Convites simultâneos.
- Entrada em clã cheio.
- VIP expirando.
- Limite reduzido sem expulsões.
- Recuperação do VIP.
- Transferência de liderança.
- Transferência para líder com limite menor.
- Cooldown do nome.
- Cooldown independente da sigla.
- Exclusão confirmada.
- Persistência após reinício.

## Chat

Testar:

- Zero identificações.
- Uma identificação.
- Duas identificações.
- Bloqueio de terceira identificação.
- Ordem entre categorias.
- Tags expiradas.
- Perda de permissão.
- Chat local.
- Chat global.
- Chat de clã.
- Chat staff.
- Mensagens privadas.
- Antispam.
- Permissões.

## TAB e scoreboard

Testar:

- Título selecionado.
- Mesmo título nos dois sistemas.
- Mudança de grupo.
- Atualização de placeholders.
- Placeholders ausentes.
- Gradientes.
- Animações.
- Reconexão.
- Troca de mundo.
- Recarregamento de configuração.
- Conflitos com outros mods.

## Banco de dados

Testar:

- Reinicialização.
- Migração.
- Integridade.
- Falha durante operação.
- Dados incompletos.
- Operações concorrentes.

Não declarar testes de integração aprovados se eles não foram executados em um servidor real ou ambiente equivalente.

---

# 20. BUILDS, GITHUB E RELEASES

O projeto deverá compilar com Java 21.

Configurar GitHub Actions para:

- Validar o projeto.
- Executar testes.
- Compilar.
- Gerar o `.jar`.
- Publicar artefatos de build.
- Gerar releases versionadas quando apropriado.

Não publicar uma release estável se o build ou os testes obrigatórios falharem.

Utilizar versionamento consistente.

Documentar cada alteração.

Se houver acesso ao GitHub, trabalhar no repositório autorizado e manter commits organizados.

Não substituir arquivos ou branches existentes sem necessidade.

---

# 21. PLANO DE IMPLEMENTAÇÃO

Implemente por etapas para reduzir riscos.

### Fase 1 — Auditoria e arquitetura

- Inspecionar repositório.
- Confirmar dependências.
- Auditar APIs.
- Criar documentação.
- Definir módulos.
- Configurar build e testes.

### Fase 2 — Infraestrutura

- Configurações JSON.
- Permissões.
- Banco SQLite.
- Serviços internos.
- Placeholders.
- Comandos administrativos.

### Fase 3 — Títulos e perfil

- Registro de títulos.
- Desbloqueios.
- Seleção.
- Expiração.
- GUI `/perfil`.
- Persistência.

### Fase 4 — Clãs

- Criação.
- Nome e sigla.
- Convites.
- Membros.
- Cargos.
- Limites LuckPerms.
- Transferência.
- Cooldowns.
- Menus.
- Chat de clã.

### Fase 5 — Chat

- Canais.
- Formatação.
- Duas identificações.
- Moderação.
- Mensagens privadas.
- Integração com clãs e títulos.

### Fase 6 — TAB

- Header/footer.
- Lista de jogadores.
- Ordenação.
- Títulos.
- Layouts.
- Animações.
- Recursos adicionais viáveis.

### Fase 7 — Scoreboard

- Layouts.
- Linhas.
- Placeholders.
- Animações.
- Integrações.
- Otimização.

### Fase 8 — Finalização

- Integrações completas.
- Testes.
- Otimizações.
- Correções.
- Documentação.
- Build.
- Release.

Essas fases representam a ordem de implementação, não autorização para ignorar dependências técnicas ou deixar recursos existentes quebrados.

Se o repositório já possuir funcionalidades prontas, aproveite-as e adapte o plano.

---

# 22. CRITÉRIOS DE ACEITAÇÃO

O projeto somente poderá ser considerado completo quando:

- O mod iniciar corretamente no Fabric 1.21.1.
- O Java 21 for utilizado.
- Os módulos funcionarem individualmente.
- O LuckPerms for integrado corretamente.
- O TAB funcionar sem depender do plugin TAB.
- A scoreboard exibir informações reais.
- O chat respeitar o máximo de duas identificações.
- TAB e scoreboard compartilharem o mesmo título escolhido.
- Os clãs utilizarem nome completo e sigla independente.
- A sigla for escolhida manualmente.
- Nome e sigla tiverem cooldowns independentes.
- A criação de clãs for gratuita.
- Os requisitos de criação forem configuráveis.
- O limite de membros depender das permissões do líder.
- A expiração de VIP não remover membros.
- A transferência de liderança exigir aceitação.
- Os estilos respeitarem as permissões.
- Os dados persistirem após reinício.
- Os menus validarem todas as ações no servidor.
- Os JSONs forem documentados e funcionais.
- Os testes pertinentes passarem.
- O `.jar` for gerado com sucesso.

Recursos inviáveis tecnicamente devem ser documentados com evidências e alternativas, nunca marcados falsamente como concluídos.

---

# 23. INSTRUÇÕES FINAIS PARA O CODEX

Quero que você trabalhe como responsável técnico pelo Shirokai Core.

Não quero apenas exemplos de código.

Quero uma implementação real, organizada, testável e utilizável no servidor.

**Prioridades:**

1. Preservar funcionalidades existentes.
2. Respeitar todas as regras estabelecidas.
3. Garantir compatibilidade com Fabric 1.21.1 e Java 21.
4. Verificar APIs reais.
5. Desenvolver arquitetura modular.
6. Garantir segurança e persistência.
7. Priorizar desempenho.
8. Criar configurações altamente personalizáveis.
9. Implementar interfaces profissionais.
10. Manter documentação e continuidade do projeto.

**IMPORTANTE:** Não simplifique o sistema removendo recursos solicitados. Se algo exigir desenvolvimento adicional, registre a dependência e implemente na etapa correta.

Ao final de cada fase, informe:

- O que foi implementado.
- Quais arquivos foram alterados.
- Quais testes foram executados.
- Quais testes passaram ou falharam.
- Quais funcionalidades ainda faltam.
- Quais limitações foram encontradas.
- Qual será a próxima etapa.
- Onde encontrar o `.jar` ou artefato, quando houver build.

Mantenha o `PROJECT_STATUS.md` e o `NEXT_STEPS.md` sempre atualizados para permitir continuidade por outras sessões do Codex.

**COMECE AGORA:**

1. Analise o repositório existente.
2. Identifique o estado real do projeto.
3. Audite as APIs e dependências.
4. Crie a arquitetura e os documentos de continuidade.
5. Implemente a primeira fase necessária.
6. Execute os testes disponíveis.
7. Continue pelas fases seguintes, respeitando as dependências e o tempo disponível.
8. Não marque o projeto como concluído antes de cumprir os critérios de aceitação.

O resultado esperado é o **Shirokai Core**, um mod central completo e altamente personalizável para o servidor Shirokai.
