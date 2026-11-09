# Vanish opcional — 0.16

O Core sozinho não oculta o personagem. A integração usa **Drex Vanish
1.5.9+1.21.1**, mod Fabric separado (MIT), responsável pela remoção da lista,
tracking e visibilidade da entidade. Não precisa de mod no cliente.

1. Pare o servidor e faça backup.
2. Instale o [Vanish1.5.9+1.21.1 oficial](https://github.com/DrexHD/Vanish/releases/tag/1.5.9%2B1.21.1)
   na pasta mods junto do Core. SHA256 do vanish-1.5.9+1.21.1.jar:
   `728fcc7c39e9c045924c58f50004f4f42d1ea1dc3995da34556d83441a0bc9a1`.
3. Reinicie. `/shirokaicore tab` deve informar `vanish=READY 1.5.9+1.21.1`.
4. Use `/vanish`. Permissões do provider: `vanish.command.vanish`,
   `vanish.command.vanish.other`, `vanish.command.vanish.reload` e
   `vanish.feature.view` para enxergar ocultos. Não conceda view ao grupo default.
   Há fallback OP do provider; teste com jogador comum sem OP.

Teste isolado usou Loader0.17.2, Fabric API e Fabric PAPI2.4.2+1.21 junto das
bibliotecas internas originais do provider. Não substitua uma versão mais nova já
instalada sem verificar a compatibilidade do restante dos mods. A versão1.6.16
não foi certificada e não é aceita por este adaptador. Suporte é fixado à versão
auditada; ampliação exige nova auditoria/testes.

## Comportamento e limites

- Sem provider: status ABSENT e comportamento normal; `/vanish` não é criado pelo Core.
- Com provider suportado: o Core consulta a API para destinatários de chat, alvos
  privados, menções, nomes TAB, contagem online por observador, seletores/autocomplete de convites e
  estado online nos cartões de membros de clã.
  Ignorar filtros não permite ignorar visibilidade.
- Ocultos não entram nos teams globais do Core, porque esses pacotes revelariam
  nomes a todos. Staff autorizado pode vê-los sem o prefixo global do Core enquanto
  ocultos. Ao sair do vanish, voltam às regras normais de ownership.
- Listed/TAB oculto não equivale a vanish. A entidade é ocultada pelo mod externo.
- Provider reconhecido mas incompatível/erro de API: estado UNSUPPORTED/FAILED e
  visões públicas do Core falham fechadas; consulte o log e corrija a instalação.
- Não garante privacidade em comandos vanilla, server-list ping, plugins/mods
  externos ou canais que não passem pelo Core. Histórico de mensagens não é apagado.
- Nick, times e aparência ainda cedem a ownership externo. Não remover teams alheios.

## Auditoria e reprodução

API pública no artefato exato: `me.drex.vanish.api.VanishAPI`:
`isVanished(Entity)`, `setVanish(ServerPlayerEntity, boolean)` e
`canSeePlayer(ServerPlayerEntity actor, ServerPlayerEntity observer)`.
A ordem dos argumentos é actor primeiro. EntityMixin usa essa consulta em
canBeSpectated e isInvisibleTo, verificados no bytecode do JAR e na fixture real.
A ponte opcional usa reflection; o mod externo não é incluído no JAR do Core.

```sh
python3 tools/prepare_vanish.py
python3 tools/vanish_smoke.py --accept-eula
```

O runner inicia servidor Fabric real em diretório isolado, online-mode preservado,
com o provider original, e usa jogadores sintéticos para checar API/mixins,
remoção de roster, gates de entidade, destinatários, contagens por observador e
restauração ao desocultar. Não é login autenticado nem renderização com cliente real.
Logs: build/vanish-smoke/server.log; resultado: build/vanish-smoke/result.json.
