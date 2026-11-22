package com.shirokai.core.cosmetics;

import java.util.*;

/** Stable identifiers, deliberately separate from the Portuguese display names. */
public final class CosmeticCatalog {
    public record Entry(String id,String category,String label,String style) {}
    public static final Map<String,String> CATEGORIES = Collections.unmodifiableMap(new LinkedHashMap<>(Map.ofEntries(
        Map.entry("nick","Cor do nick"),Map.entry("trail","Rastros no chão"),Map.entry("aura","Auras"),
        Map.entry("join","Entrada triunfal"),Map.entry("victory","Efeitos de vitória"),Map.entry("raid","Celebração de raid")
    )));
    public static final List<String> ORDER = List.of("nick","trail","aura","join","victory","raid");
    public static final List<Entry> ENTRIES;
    static {
        var entries=new ArrayList<Entry>();
        entries.add(new Entry("nick_cyan","nick","Ciano","cyan"));
        entries.add(new Entry("nick_gold","nick","Dourado","gold"));
        entries.add(new Entry("nick_rainbow","nick","Arco-íris suave","rainbow"));
        for(String category:ORDER) if(!category.equals("nick")) {
            entries.add(new Entry(category+"_stars",category,"Estelar","stars"));
            entries.add(new Entry(category+"_fire",category,"Fogo","fire"));
            entries.add(new Entry(category+"_water",category,"Água","water"));
            entries.add(new Entry(category+"_nature",category,"Natureza","nature"));
            entries.add(new Entry(category+"_electric",category,"Elétrico","electric"));
            entries.add(new Entry(category+"_soul",category,"Fantasma","soul"));
            entries.add(new Entry(category+"_snow",category,"Neve","snow"));
            entries.add(new Entry(category+"_heart",category,"Corações","heart"));
            entries.add(new Entry(category+"_magic",category,"Magia","magic"));
            entries.add(new Entry(category+"_enchant",category,"Encantado","enchant"));
            entries.add(new Entry(category+"_cloud",category,"Nuvem","cloud"));
            entries.add(new Entry(category+"_cherry",category,"Cerejeira","cherry"));
        }
        ENTRIES=List.copyOf(entries);
    }
    public static Entry find(String id) { return ENTRIES.stream().filter(e->e.id.equals(id)).findFirst().orElse(null); }
    private CosmeticCatalog() {}
}
