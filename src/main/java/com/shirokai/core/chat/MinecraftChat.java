package com.shirokai.core.chat;

import com.shirokai.core.clans.ClanService;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.storage.SqliteStorage;
import com.shirokai.core.titles.TitleService;
import net.minecraft.network.message.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import org.slf4j.Logger;
import java.time.Clock;
import java.util.*;
import java.util.function.Supplier;

public final class MinecraftChat {
    private final MinecraftServer server;private final ChatService service;private final Supplier<Collection<ServerPlayerEntity>> online;private final Logger logger;private final PermissionService permissions;
    private final TitleService titles;
    private final ChatShares shares=new ChatShares();
    public ChatShares shares(){return shares;}
    private ChatAppearanceConfig appearance=ChatAppearanceConfig.defaults();
    public ChatAppearanceConfig appearance(){return appearance;}
    public void appearance(ChatAppearanceConfig config){appearance=config;}
    public MinecraftChat(MinecraftServer server,SqliteStorage storage,PermissionService permissions,TitleService titles,ClanService clans,ChatConfig config,Logger logger){
        this(server,storage,permissions,titles,clans,config,logger,()->List.copyOf(server.getPlayerManager().getPlayerList()));
    }
    public MinecraftChat(MinecraftServer server,SqliteStorage storage,PermissionService permissions,TitleService titles,ClanService clans,ChatConfig config,Logger logger,Supplier<Collection<ServerPlayerEntity>> online){
        this.server=server;this.online=online;this.logger=logger;this.permissions=permissions;this.titles=titles;
        service=new ChatService(new ChatRepository(storage,Clock.systemUTC()),permissions,server,
            ()->online.get().stream().filter(player->!player.isDisconnected()).map(player->{
                UUID clan=clans==null?null:clans.cached(player.getUuid()).map(snapshot->snapshot.id()).orElse(null);
                return new ChatService.Participant(player.getUuid(),com.shirokai.core.integrations.EssentialsAccess.display(player),player.getWorld().getRegistryKey().getValue().toString(),player.getX(),player.getY(),player.getZ(),clan);
            }).toList(),id->titles==null?List.of():titles.chatDefinitions(id).stream().map(def->def.display()).toList(),
            Clock.systemUTC(),config,(id,reason)->logger.warn("[chat] Mensagem rejeitada de {}: {}",id,reason));
        service.contentValidator(shares::validate);
        service.visibility((viewer,actor)->com.shirokai.core.visibility.VisibilityService.canSee(server,viewer,actor));
    }
    private com.shirokai.core.presentation.PresentationRuntime presentation;
    public void presentation(com.shirokai.core.presentation.PresentationRuntime value){presentation=value;}
    public ChatService service(){return service;}
    private ServerPlayerEntity player(UUID id){return online.get().stream().filter(player->player.getUuid().equals(id)&&!player.isDisconnected()).findFirst().orElse(null);}
    private Text label(ChatService.Envelope message){
        var author=player(message.author());String name=author==null?message.author().toString():author.getGameProfile().getName();
        return (presentation==null||!presentation.plan().config().chatEnabled()?service.header(message):presentation.header(message)).copy().styled(style->style.withHoverEvent(appearance.playerSummary()?new HoverEvent(HoverEvent.Action.SHOW_TEXT,playerSummary(message.author())):appearance.playerHoverInfo()?new HoverEvent(HoverEvent.Action.SHOW_TEXT,Text.literal("Jogador: "+name)):null)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/chat msg "+name+" ")));
    }
    private java.util.function.BiFunction<UUID,String,String> summaryValues=(id,key)->"N/D";
    public void summaryValues(java.util.function.BiFunction<UUID,String,String> provider){summaryValues=Objects.requireNonNull(provider);}
    public Text rank(UUID id){
        if(titles!=null&&presentation!=null)return safe(titles.visualPresentation(id,com.shirokai.core.customization.VisualTarget.CHAT,0,false));
        if(titles!=null)return titles.rank(id).map(def->Text.literal(def.display()).formatted(net.minecraft.util.Formatting.AQUA)).orElseGet(Text::empty);
        String group=permissions.metadata(id).group();
        return group==null?Text.empty():Text.literal("["+group.toUpperCase(Locale.ROOT)+"]").formatted(net.minecraft.util.Formatting.AQUA);
    }
    public Text trainer(UUID id){
        var author=player(id);var result=Text.empty();var cargo=rank(id);
        if(!cargo.getString().isEmpty())result.append(cargo).append(" ");
        return result.append(Text.literal(author==null?"Indisponível":com.shirokai.core.integrations.EssentialsAccess.display(author)).formatted(net.minecraft.util.Formatting.WHITE));
    }
    public Text playerSummary(UUID id){
        var text=Text.empty().append(Text.literal("Treinador").formatted(net.minecraft.util.Formatting.AQUA,net.minecraft.util.Formatting.BOLD));
        text.append(Text.literal("\n").setStyle(Style.EMPTY)).append(trainer(id));
        var account=player(id);if(account!=null&&!com.shirokai.core.integrations.EssentialsAccess.display(account).equals(account.getGameProfile().getName()))text.append(Text.literal("\nConta: "+account.getGameProfile().getName()).setStyle(Style.EMPTY));
        String dex=summaryValues.apply(id,"shirokai:pokedex_percent"),gyms=summaryValues.apply(id,"shirokai:gyms");
        text.append(Text.literal("\nPokédex: ").setStyle(Style.EMPTY.withColor(net.minecraft.util.Formatting.WHITE)))
            .append(Text.literal(dex.equals("N/D")?dex:dex+"%").setStyle(Style.EMPTY.withColor(net.minecraft.util.Formatting.RED)));
        return text.append(Text.literal("\nGinásios: ").setStyle(Style.EMPTY.withColor(net.minecraft.util.Formatting.WHITE)))
            .append(Text.literal(gyms).setStyle(Style.EMPTY.withColor(net.minecraft.util.Formatting.AQUA)));
    }
    private static Text safe(Text value){
        var result=Text.empty();value.visit((style,part)->{result.append(Text.literal(part).setStyle(style.withHoverEvent(null).withClickEvent(null).withInsertion(null)));return Optional.empty();},Style.EMPTY);return result;
    }
    private boolean colors(UUID author){return appearance.enabled()&&appearance.legacyColors()&&(appearance.colorPermission().isEmpty()||permissions.allowed(author,appearance.colorPermission()));}
    private Text body(ChatService.Envelope message,UUID viewer){
        return shares.render(message.author(),message.body(),fragment->plainBody(message,viewer,fragment));
    }
    private Text plainBody(ChatService.Envelope message,UUID viewer,String fragment){
        var base=appearance.enabled()?appearance.color(message.channel()):net.minecraft.util.Formatting.WHITE;
        if(colors(message.author())&&LegacyChatColors.changes(fragment))return LegacyChatColors.render(fragment,base);
        return (presentation==null?Text.literal(fragment):presentation.body(message.author(),viewer,fragment)).copy().formatted(base);
    }
    private void mention(ChatService.Envelope message,ServerPlayerEntity recipient){
        if(presentation==null)return;
        var normalized=colors(message.author())?new ChatService.Envelope(message.author(),message.label(),LegacyChatColors.plain(message.body()),message.channel(),message.target()):message;
        presentation.notifyMention(normalized,recipient);
    }
    public MessageType.Parameters parameters(Channel channel,Text name){
        var key=appearance.enabled()?net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.MESSAGE_TYPE,net.minecraft.util.Identifier.of("shirokai-core","chat/"+appearance.color(channel).getName())):MessageType.CHAT;
        return MessageType.params(key,server.getRegistryManager(),name);
    }
    public void system(UUID target,ChatService.Envelope message){var recipient=player(target);if(recipient!=null){
        var name=label(message).copy().formatted(appearance.enabled()?appearance.color(message.channel()):net.minecraft.util.Formatting.WHITE);var body=body(message,recipient.getUuid());
        recipient.sendMessage(Text.empty().append(name).append(Text.empty().append(Text.literal(": ").formatted(appearance.enabled()?appearance.color(message.channel()):net.minecraft.util.Formatting.WHITE)).append(body)),false);
        mention(message,recipient);
    }}
    public boolean signed(ServerPlayerEntity sender,SignedMessage incoming){
        try{
            if(!server.isOnThread())throw new IllegalStateException("Roteamento de chat exige thread do servidor");
            if(incoming.hasSignature()&&!incoming.canVerifyFrom(sender.getUuid()))throw new IllegalStateException("Autor da mensagem assinada inválido");
            // Keep signed body/signature/filter mask; visual & decoration is checked per recipient below.
            var sent=SentMessage.of(incoming.withoutUnsigned());
            service.send(sender.getUuid(),service.selected(sender.getUuid()),incoming.getSignedContent(),(target,envelope)->{
                var recipient=player(target);if(recipient!=null){boolean filtered=sender.shouldFilterMessagesSentTo(recipient);
                    var outgoing=sent;
                    // User-requested & colors decorate only unfiltered recipients; signed body/signature remain unchanged.
                    if((ChatShares.contains(envelope.body())||colors(sender.getUuid())&&LegacyChatColors.changes(envelope.body()))&&!filtered&&incoming.filterMask().isPassThrough())
                        outgoing=SentMessage.of(incoming.withoutUnsigned().withUnsignedContent(body(envelope,recipient.getUuid())));
                    recipient.sendChatMessage(outgoing,filtered,parameters(envelope.channel(),label(envelope)));
                    // Do not reveal a mention hidden by filtering. Notification is a separate overlay, never unsigned chat decoration.
                    if(!filtered)mention(envelope,recipient);
                }
            });
        }catch(RuntimeException error){sender.sendMessage(Text.literal("Chat: "+error.getMessage()),false);}
        // Suppress only this vanilla broadcast; recipient sends do not re-enter the broadcast event.
        return false;
    }
}
