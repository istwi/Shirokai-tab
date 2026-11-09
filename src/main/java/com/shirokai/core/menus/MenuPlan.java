package com.shirokai.core.menus;

import com.shirokai.core.styles.*;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;

/** Compile on config worker; validate registries only on server thread before publication. */
public final class MenuPlan {
    private static final Set<String> KEYS=Set.of("menu:player","menu:rank","menu:clan","menu:count","menu:page","menu:tag","menu:description","menu:availability","menu:category","menu:selection");
    private final MenuConfig config;
    private final StyleEngine styles=new StyleEngine(StyleConfig.defaults());
    private final Map<String,StyleEngine.Compiled> templates;
    public MenuPlan(MenuConfig config){
        this.config=config;var texts=new ArrayList<String>();var profile=config.profile();texts.add(profile.title());
        for(var button:profile.buttons().values()){texts.add(button.name());texts.addAll(button.lore());}
        texts.add(profile.tags().name());texts.addAll(profile.tags().lore());
        var compiled=new HashMap<String,StyleEngine.Compiled>();
        for(String text:texts){var template=styles.compile(text);if(!KEYS.containsAll(template.variables())||!template.animations().isEmpty())throw new IllegalArgumentException("Placeholder/animação de menu inválido: "+text);compiled.put(text,template);}
        templates=Map.copyOf(compiled);
    }
    public MenuConfig config(){return config;}
    public Text text(String source,Map<String,Text> values){return styles.renderRich(templates.get(source),key->values.getOrDefault(key,Text.empty()),0,false);}
    public void validateItems(){
        var items=new ArrayList<String>();config.profile().buttons().values().forEach(b->items.add(b.item()));items.add(config.profile().tags().unlockedItem());items.add(config.profile().tags().lockedItem());
        for(String id:items){var key=Identifier.of(id);if(!Registries.ITEM.containsId(key)||Registries.ITEM.get(key)==Items.AIR)throw new IllegalArgumentException("Item de menu inexistente ou vazio: "+id);}
    }
}
