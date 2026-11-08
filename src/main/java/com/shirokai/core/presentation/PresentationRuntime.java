package com.shirokai.core.presentation;

import com.shirokai.core.chat.ChatService;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.titles.TitleService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.*;

/** Pure presentation on the server thread: no IO, policy changes or parsing of dynamic markup. */
public final class PresentationRuntime implements SurfaceRenderer {
    private static final Pattern MENTION=Pattern.compile("(?<![A-Za-z0-9_@])@([A-Za-z0-9_]{1,16})(?![A-Za-z0-9_])");
    private final MinecraftServer server;private final PermissionService permissions;private final TitleService titles;
    private final Supplier<Collection<ServerPlayerEntity>> online;private PresentationPlan plan;
    public PresentationRuntime(MinecraftServer server,PresentationPlan plan,PermissionService permissions,TitleService titles){this(server,plan,permissions,titles,()->List.copyOf(server.getPlayerManager().getPlayerList()));}
    public PresentationRuntime(MinecraftServer server,PresentationPlan plan,PermissionService permissions,TitleService titles,Supplier<Collection<ServerPlayerEntity>> online){this.server=server;this.plan=plan;this.permissions=permissions==null?new PermissionService(){public boolean allowed(UUID id,String node){return false;}public String provider(){return "unavailable";}}:permissions;this.titles=titles;this.online=online;}
    private com.shirokai.core.customization.StyleChoiceService customization;
    public void customization(com.shirokai.core.customization.StyleChoiceService service){customization=service;}
    @Override public boolean customizationEnabled(){return customization!=null;}
    @Override public void openCustomization(ServerPlayerEntity player,Surface surface,UUID expectedClan){if(customization!=null)com.shirokai.core.customization.StyleChoiceMenu.open(player,customization,surface,expectedClan);}
    @Override public Text renderClan(UUID viewer,UUID leader,Surface surface,String value){
        if(!server.isOnThread())throw new IllegalStateException("Apresentação exige thread do servidor");
        if(customization!=null){var selected=customization.render(viewer,surface,value);if(selected.isPresent())return selected.get();}
        return render(leader,surface,value);
    }
    public PresentationPlan plan(){return plan;}public void replacePlan(PresentationPlan candidate){plan=candidate;}
    private ServerPlayerEntity player(UUID id){return online.get().stream().filter(p->p.getUuid().equals(id)&&!p.isDisconnected()).findFirst().orElse(null);}
    @Override public boolean menusEnabled(){return plan.config().menusEnabled();}
    @Override public Text renderTitle(UUID subject,com.shirokai.core.titles.TitleDefinition definition){
        if(!server.isOnThread())throw new IllegalStateException("Apresentação exige thread do servidor");
        if(definition.category()!=com.shirokai.core.titles.TitleDefinition.Category.CLAN){
            var fixed=plan.titleTemplate(definition.id());
            if(fixed.isPresent())return plan.styles().render(fixed.get(),key->definition.display(),0,false);
        }
        return SurfaceRenderer.super.renderTitle(subject,definition);
    }
    @Override public Text render(UUID subject,Surface surface,String value){
        if(!server.isOnThread())throw new IllegalStateException("Apresentação exige thread do servidor");
        if(surface.name().startsWith("MENU_")&&!plan.config().menusEnabled())return Text.literal(value);
        if(customization!=null){var selected=customization.render(subject,surface,value);if(selected.isPresent())return selected.get();}
        var p=player(subject);String world=p==null?"":p.getWorld().getRegistryKey().getValue().toString();
        var template=plan.template(surface,permissions.metadata(subject),world,node->permissions.allowed(subject,node));
        return plan.styles().render(template,key->value,0,false);
    }
    public Text header(ChatService.Envelope message){
        if(!plan.config().chatEnabled())return Text.literal(message.label());
        var author=player(message.author());if(author==null)return Text.literal(message.label());
        var tags=Text.empty();if(titles!=null)for(var tag:titles.chatPresentation(message.author())){if(!tags.getString().isEmpty())tags.append(Text.literal(" "));tags.append(tag);}if(!tags.getString().isEmpty())tags.append(Text.literal(" "));
        var slots=Map.of("chat:name",Text.literal(author.getGameProfile().getName()).formatted(net.minecraft.util.Formatting.WHITE),"chat:channel",Text.literal(message.channel().name()),"chat:target",Text.literal(message.target()).formatted(net.minecraft.util.Formatting.WHITE),"chat:tags",tags);
        var template=plan.chat(message.channel(),permissions.metadata(author.getUuid()),author.getWorld().getRegistryKey().getValue().toString(),node->permissions.allowed(author.getUuid(),node));
        Text result=plan.styles().renderRich(template,key->slots.getOrDefault(key,Text.empty()),0,false);
        if(result.getString().codePointCount(0,result.getString().length())>1024)throw new IllegalArgumentException("Cabeçalho excede1024 caracteres");return result;
    }
    private boolean mentions(UUID author){return plan.config().chatEnabled()&&plan.config().mentionsEnabled()&&(plan.config().mentionPermission().isEmpty()||permissions.allowed(author,plan.config().mentionPermission()));}
    public boolean mentioned(UUID author,ServerPlayerEntity recipient,String body){
        if(!mentions(author)||author.equals(recipient.getUuid()))return false;
        var matcher=MENTION.matcher(body);while(matcher.find())if(matcher.group(1).equalsIgnoreCase(recipient.getGameProfile().getName()))return true;return false;
    }
    /** Commands/system only. Normal SignedMessage bodies are never sent through this renderer. */
    public Text body(UUID author,String body){
        if(!mentions(author))return Text.literal(body);
        var known=new HashSet<String>();for(var p:online.get())if(!p.isDisconnected())known.add(p.getGameProfile().getName().toLowerCase(Locale.ROOT));
        var output=Text.empty();var matcher=MENTION.matcher(body);int cursor=0;
        while(matcher.find()){
            output.append(Text.literal(body.substring(cursor,matcher.start())));
            String value=matcher.group();output.append(known.contains(matcher.group(1).toLowerCase(Locale.ROOT))?render(author,Surface.MENTION,value):Text.literal(value));cursor=matcher.end();
        }
        return output.append(Text.literal(body.substring(cursor)));
    }
    public void notifyMention(ChatService.Envelope message,ServerPlayerEntity recipient){
        if(mentioned(message.author(),recipient,message.body())){
            var author=player(message.author());if(author!=null)recipient.sendMessage(render(recipient.getUuid(),Surface.MENTION,"Menção de "+author.getGameProfile().getName()),true);
        }
    }
}
