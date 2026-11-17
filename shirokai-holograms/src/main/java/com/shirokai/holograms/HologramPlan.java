package com.shirokai.holograms;

import com.shirokai.holograms.text.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.*;
import java.util.function.Function;

public final class HologramPlan {
    public record Compiled(Definitions.Hologram definition,List<StyleEngine.Compiled> lines) { }
    private final ConfigStore.Snapshot snapshot;
    private final StyleEngine styles=new StyleEngine(new StyleConfig(1,Map.of()));
    private final Set<String> reported=new HashSet<>();
    private final Map<String,Compiled> entries=new LinkedHashMap<>();
    public HologramPlan(ConfigStore.Snapshot snapshot) {
        this.snapshot=snapshot;
        if(snapshot.holograms().holograms().size()>snapshot.settings().maxHolograms())throw new IllegalArgumentException("Limite maxHolograms excedido");
        for(var h:snapshot.holograms().holograms()) {
            if(h.lines().size()>snapshot.settings().maxLines())throw new IllegalArgumentException(h.id()+": limite maxLines excedido");
            var lines=new ArrayList<StyleEngine.Compiled>();
            for(var line:h.lines()) {
                if(!snapshot.animations().presets().containsKey(line.animation()))throw new IllegalArgumentException(h.id()+": preset desconhecido "+line.animation());
                try{lines.add(styles.compile(legacy(line.text())));}catch(IllegalArgumentException error){throw new IllegalArgumentException(h.id()+": "+error.getMessage(),error);}
            }
            entries.put(h.id(),new Compiled(h,List.copyOf(lines)));
        }
    }
    public ConfigStore.Snapshot snapshot(){return snapshot;}
    public Map<String,Compiled> entries(){return Collections.unmodifiableMap(entries);}
    public List<Text> render(Compiled h,Function<String,String> resolver) {
        var result=new ArrayList<Text>();
        for(int i=0;i<h.lines().size();i++)try{result.add(styles.render(h.lines().get(i),resolver,0,false).copy());}
        catch(IllegalArgumentException error){
            String id=h.definition().id()+":"+(i+1);
            if(reported.add(id))org.slf4j.LoggerFactory.getLogger("shirokai-holograms").warn("[holograms] {}: expansão inválida; fallback literal até próxima atualização: {}",id,error.getMessage());
            result.add(Text.literal(snapshot.settings().unavailablePlaceholder()));
        }
        return List.copyOf(result);
    }
    /** Alternative legacy format. No parsing of placeholder values, and no ambiguous mixed scopes. */
    static String legacy(String source) {
        if(!source.contains("&"))return source;
        if(source.contains("<")) {
            if(source.matches("(?is).*&[0-9a-fklmnor].*"))throw new IllegalArgumentException("Use tags ou códigos & na mesma linha, sem misturar");
            return source;
        }
        var out=new StringBuilder();var part=new StringBuilder();var tags=new ArrayList<String>();
        for(int i=0;i<source.length();i++) {
            char c=source.charAt(i);
            if(c=='&' && i+1<source.length()) {
                char code=Character.toLowerCase(source.charAt(i+1));
                if(code=='&'){part.append('&');i++;continue;}
                var format=Formatting.byCode(code);
                if(format!=null){flush(out,part,tags);i++;if(format.isColor()||format==Formatting.RESET)tags.clear();if(format!=Formatting.RESET&&!tags.contains(format.getName()))tags.add(format.getName());continue;}
            }
            part.append(c);
        }
        flush(out,part,tags);return out.toString();
    }
    private static void flush(StringBuilder out,StringBuilder part,List<String> tags) {
        if(part.isEmpty())return;
        tags.forEach(tag->out.append('<').append(tag).append('>'));out.append(part);part.setLength(0);
        for(int i=tags.size()-1;i>=0;i--)out.append("</").append(tags.get(i)).append('>');
    }
}
