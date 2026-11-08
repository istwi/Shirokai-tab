package com.shirokai.core.presentation;

import com.shirokai.core.chat.*;
import com.shirokai.core.styles.StyleEngine;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.tab.TabPlan;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.*;

/** Compile every candidate, including inactive rules, before publishing any configuration. */
public final class PresentationPlan {
    private record Rule(PresentationConfig.Rule definition,Map<Surface,StyleEngine.Compiled> templates,Map<Channel,StyleEngine.Compiled> chat) { }
    private final Map<String,StyleEngine.Compiled> titleTemplates;
    private final PresentationConfig config;private final StyleEngine styles;
    private final Map<Surface,StyleEngine.Compiled> templates;private final Map<Channel,StyleEngine.Compiled> chat;private final List<Rule> rules;
    private static final Set<String> CHAT_KEYS=Set.of("chat:name","chat:tags","chat:target","chat:channel");
    public PresentationPlan(PresentationConfig config,StyleEngine styles,ChatConfig chat){
        this.config=config;this.styles=styles;
        var fixed=new HashMap<String,StyleEngine.Compiled>();
        config.titleTemplates().forEach((id,source)->fixed.put(id,compile(Map.of(Surface.TITLE,source)).get(Surface.TITLE)));
        titleTemplates=Map.copyOf(fixed);
        templates=compile(config.templates());this.chat=compileChat(chat.nameFormats());
        rules=config.rules().stream().sorted(Comparator.comparingInt(PresentationConfig.Rule::priority).reversed().thenComparing(PresentationConfig.Rule::id)).map(r->new Rule(r,compile(r.templates()),compileChat(r.chatFormats()))).toList();
    }
    private Map<Surface,StyleEngine.Compiled> compile(Map<Surface,String> source){
        var result=new EnumMap<Surface,StyleEngine.Compiled>(Surface.class);source.forEach((key,value)->{
            var compiled=styles.compile(value);
            if(!compiled.variables().equals(Set.of("visual:value"))||count(compiled.nodes(),"visual:value")!=1||!compiled.animations().isEmpty()||styles.literalSize(compiled)>256)throw new IllegalArgumentException("Template visual exige apenas %visual:value%, sem animação");
            result.put(key,compiled);
        });return Map.copyOf(result);
    }
    private Map<Channel,StyleEngine.Compiled> compileChat(Map<Channel,String> source){
        var result=new EnumMap<Channel,StyleEngine.Compiled>(Channel.class);
        source.forEach((key,value)->{
            String converted=value.replace("{name}","%chat:name%").replace("{tags}","%chat:tags%").replace("{target}","%chat:target%").replace("{channel}","%chat:channel%");
            if(converted.contains("{")||converted.contains("}"))throw new IllegalArgumentException("Variável de cabeçalho desconhecida");
            var compiled=styles.compile(converted);
            if(!CHAT_KEYS.containsAll(compiled.variables())||count(compiled.nodes(),"chat:tags")>1||!compiled.animations().isEmpty()||styles.literalSize(compiled)>512)throw new IllegalArgumentException("Cabeçalho de chat inválido; corpo/animação não são permitidos");
            result.put(key,compiled);
        });return Map.copyOf(result);
    }
    private static int count(List<StyleEngine.Node> nodes,String key){
        int count=0;for(var node:nodes){if(node instanceof StyleEngine.Variable v&&v.name().equals(key))count++;else if(node instanceof StyleEngine.Group g)count+=count(g.children(),key);}return count;
    }
    private boolean matches(Rule rule,PermissionService.Metadata metadata,String world,Predicate<String> permissions){return TabPlan.matches(rule.definition().condition(),metadata,world,permissions);}
    public StyleEngine.Compiled template(Surface surface,PermissionService.Metadata meta,String world,Predicate<String> permissions){return rules.stream().filter(r->r.templates().containsKey(surface)&&matches(r,meta,world,permissions)).map(r->r.templates().get(surface)).findFirst().orElse(templates.get(surface));}
    public StyleEngine.Compiled chat(Channel channel,PermissionService.Metadata meta,String world,Predicate<String> permissions){return rules.stream().filter(r->r.chat().containsKey(channel)&&matches(r,meta,world,permissions)).map(r->r.chat().get(channel)).findFirst().orElse(chat.get(channel));}
    public Optional<StyleEngine.Compiled> titleTemplate(String id){return Optional.ofNullable(titleTemplates.get(id));}
    public PresentationConfig config(){return config;}public StyleEngine styles(){return styles;}
}
