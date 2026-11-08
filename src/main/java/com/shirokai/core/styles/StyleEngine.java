package com.shirokai.core.styles;

import net.minecraft.text.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;

/** Strict template-only formatting. Resolved values are literal; cached Text is shared read-only. */
public final class StyleEngine {
    public record Compiled(String source,List<Node> nodes,Set<String> variables,Set<String> animations){
        public Compiled{nodes=List.copyOf(nodes);variables=Set.copyOf(variables);animations=Set.copyOf(animations);}
    }
    public sealed interface Node permits Literal,Variable,Group,Animated{}
    public record Literal(String text)implements Node{}
    public record Variable(String name)implements Node{}
    public record Animated(String name)implements Node{}
    public record Group(String name,List<Node> children,Integer from,Integer to)implements Node{public Group{children=List.copyOf(children);}}
    private record Span(String text,Style style){}
    private record Key(String source,Map<String,String> variables,Map<String,Integer> frames){}
    private final StyleConfig config;
    private final Map<String,List<Compiled>> animations=new HashMap<>();
    private final Map<String,Compiled> compiled=lru(256);
    private final Map<Key,Text> rendered=lru(1024);
    private static <K,V>Map<K,V> lru(int capacity){return new LinkedHashMap<K,V>(capacity,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<K,V> entry){return size()>capacity;}};}
    private static final Map<String,Integer> COLORS=Map.ofEntries(Map.entry("black",0x000000),Map.entry("dark_blue",0x0000aa),Map.entry("dark_green",0x00aa00),Map.entry("dark_aqua",0x00aaaa),Map.entry("dark_red",0xaa0000),Map.entry("dark_purple",0xaa00aa),Map.entry("gold",0xffaa00),Map.entry("gray",0xaaaaaa),Map.entry("dark_gray",0x555555),Map.entry("blue",0x5555ff),Map.entry("green",0x55ff55),Map.entry("aqua",0x55ffff),Map.entry("red",0xff5555),Map.entry("light_purple",0xff55ff),Map.entry("yellow",0xffff55),Map.entry("white",0xffffff));
    public StyleEngine(StyleConfig config){
        this.config=config;
        for(var entry:config.animations().entrySet()){var frames=new ArrayList<Compiled>();for(String frame:entry.getValue().frames())frames.add(parse(frame,false));animations.put(entry.getKey(),List.copyOf(frames));}
    }
    public Compiled compile(String source){return compiled.computeIfAbsent(source,s->parse(s,true));}
    private Compiled parse(String source,boolean allowAnimation){
        if(source==null||source.length()>2048||source.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Template inválido/excede2048");
        var parser=new Parser(source,allowAnimation);var nodes=parser.nodes(null,0);var variables=new HashSet<String>();var animationIds=new HashSet<String>();dependencies(nodes,variables,animationIds);
        for(String id:animationIds){var frames=animations.get(id);if(frames==null)throw new IllegalArgumentException("Animação desconhecida: "+id);for(var frame:frames)variables.addAll(frame.variables());}
        return new Compiled(source,nodes,variables,animationIds);
    }
    private static void dependencies(List<Node> nodes,Set<String> variables,Set<String> animations){for(Node node:nodes){if(node instanceof Variable v)variables.add(v.name());else if(node instanceof Animated a)animations.add(a.name());else if(node instanceof Group g)dependencies(g.children(),variables,animations);}}
    private final class Parser{
        private final String source;private final boolean allowAnimation;private int position;
        Parser(String source,boolean allowAnimation){this.source=source;this.allowAnimation=allowAnimation;}
        List<Node> nodes(String end,int depth){
            if(depth>16)throw new IllegalArgumentException("Estilos aninhados demais");var result=new ArrayList<Node>();var text=new StringBuilder();
            while(position<source.length()){
                char c=source.charAt(position);
                if(c=='\\'&&position+1<source.length()&&source.charAt(position+1)=='<'){text.append('<');position+=2;continue;}
                if(c=='<'){
                    flush(text,result);int close=source.indexOf('>',position);if(close<0)throw new IllegalArgumentException("Tag sem fechamento");String tag=source.substring(position+1,close).toLowerCase(Locale.ROOT);position=close+1;
                    if(tag.startsWith("/")){if(!tag.substring(1).equals(end))throw new IllegalArgumentException("Fechamento inesperado: "+tag);return result;}
                    Integer from=null,to=null;String name=tag;
                    if(tag.startsWith("gradient:")){var parts=tag.split(":");if(parts.length!=3)throw new IllegalArgumentException("Gradiente exige duas cores HEX");from=hex(parts[1]);to=hex(parts[2]);name="gradient";}
                    else modify(Style.EMPTY,tag); // Validates allowlisted styles; click/hover/font/etc are rejected.
                    result.add(new Group(name,nodes(name,depth+1),from,to));continue;
                }
                if(c=='%'){
                    int close=source.indexOf('%',position+1);
                    if(close>position){String key=source.substring(position+1,close);if(key.matches(com.shirokai.core.placeholders.PlaceholderService.KEY_PATTERN)){
                        flush(text,result);if(key.startsWith("animation:")){if(!allowAnimation)throw new IllegalArgumentException("Frame não pode referenciar animação");result.add(new Animated(key.substring(10)));}else result.add(new Variable(key));position=close+1;continue;
                    }}
                }
                text.append(c);position++;
            }
            flush(text,result);if(end!=null)throw new IllegalArgumentException("Tag não fechada: "+end);return result;
        }
    }
    private static void flush(StringBuilder text,List<Node> result){if(!text.isEmpty()){result.add(new Literal(text.toString()));text.setLength(0);}}
    private static int hex(String color){if(!color.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Cor HEX inválida: "+color);return Integer.parseInt(color.substring(1),16);}
    private static Style modify(Style style,String tag){
        if(COLORS.containsKey(tag))return style.withColor(COLORS.get(tag));if(tag.startsWith("#"))return style.withColor(hex(tag));
        return switch(tag){case "bold"->style.withBold(true);case "italic"->style.withItalic(true);case "underlined"->style.withUnderline(true);case "strikethrough"->style.withStrikethrough(true);case "obfuscated"->style.withObfuscated(true);default->throw new IllegalArgumentException("Tag não permitida: "+tag);};
    }
    private static int interpolate(int from,int to,int index,int total){double ratio=total<=1?0:(double)index/(total-1);int rgb=0;for(int shift:new int[]{16,8,0})rgb|=((int)Math.round(((from>>shift)&255)*(1-ratio)+((to>>shift)&255)*ratio))<<shift;return rgb;}
    public Text render(Compiled template,Function<String,String> resolver,long tick,boolean animate){
        var values=new HashMap<String,String>();for(String key:template.variables()){String value=Objects.requireNonNullElse(resolver.apply(key),"");if(value.codePoints().anyMatch(Character::isISOControl)||value.codePointCount(0,value.length())>512)throw new IllegalArgumentException("Placeholder excede512 caracteres: "+key);values.put(key,value);}
        var frames=new HashMap<String,Integer>();for(String id:template.animations()){var animation=config.animations().get(id);frames.put(id,animate?(int)Math.floorMod(Math.floorDiv(tick,animation.frameTicks()),animation.frames().size()):0);}
        var key=new Key(template.source(),Map.copyOf(values),Map.copyOf(frames));var previous=rendered.get(key);if(previous!=null)return previous;
        var spans=new ArrayList<Span>();int[] remaining={2048};var richValues=new HashMap<String,List<Span>>();values.forEach((name,value)->richValues.put(name,List.of(new Span(value,Style.EMPTY))));evaluate(template.nodes(),Style.EMPTY,richValues,frames,spans,remaining);
        var output=Text.empty();for(var span:spans)output.append(Text.literal(span.text()).setStyle(span.style()));rendered.put(key,output);return output;
    }
    /** Compose trusted formatted slots without reparsing data. Strip actions from slots; caller-owned Text is never mutated. */
    public Text renderRich(Compiled template,Function<String,Text> resolver,long tick,boolean animate){
        var values=new HashMap<String,List<Span>>();
        for(String name:template.variables()){
            Text value=Objects.requireNonNullElseGet(resolver.apply(name),Text::empty);String plain=value.getString();
            if(plain.codePoints().anyMatch(Character::isISOControl)||plain.codePointCount(0,plain.length())>512)throw new IllegalArgumentException("Placeholder excede512 caracteres: "+name);
            var parts=new ArrayList<Span>();
            value.visit((style,content)->{parts.add(new Span(content,style.withClickEvent(null).withHoverEvent(null).withInsertion(null)));return Optional.empty();},Style.EMPTY);
            values.put(name,List.copyOf(parts));
        }
        var frames=new HashMap<String,Integer>();for(String id:template.animations()){var animation=config.animations().get(id);frames.put(id,animate?(int)Math.floorMod(Math.floorDiv(tick,animation.frameTicks()),animation.frames().size()):0);}
        var spans=new ArrayList<Span>();evaluate(template.nodes(),Style.EMPTY,values,frames,spans,new int[]{2048});
        var output=Text.empty();for(var span:spans)output.append(Text.literal(span.text()).setStyle(span.style()));return output;
    }
    private static void append(List<Span> spans,String text,Style style,int[] remaining){int length=text.codePointCount(0,text.length());remaining[0]-=length;if(remaining[0]<0)throw new IllegalArgumentException("Texto expandido excede2048 caracteres");if(!text.isEmpty())spans.add(new Span(text,style));}
    private void evaluate(List<Node> nodes,Style style,Map<String,List<Span>> values,Map<String,Integer> frames,List<Span> output,int[] remaining){
        for(Node node:nodes){
            if(node instanceof Literal value)append(output,value.text(),style,remaining);
            else if(node instanceof Variable value)for(var span:values.get(value.name()))append(output,span.text(),span.style().withParent(style),remaining);
            else if(node instanceof Animated value)evaluate(animations.get(value.name()).get(frames.get(value.name())).nodes(),style,values,frames,output,remaining);
            else if(node instanceof Group group){
                if(group.from()==null)evaluate(group.children(),modify(style,group.name()),values,frames,output,remaining);
                else{var spans=new ArrayList<Span>();evaluate(group.children(),style,values,frames,spans,remaining);int total=spans.stream().mapToInt(s->s.text().codePointCount(0,s.text().length())).sum(),index=0;
                    for(var span:spans)for(int point:span.text().codePoints().toArray())output.add(new Span(new String(Character.toChars(point)),span.style().withColor(interpolate(group.from(),group.to(),index++,total))));
                }
            }
        }
    }
    public int literalSize(Compiled template){return literalSize(template.nodes());}
    private int literalSize(List<Node> nodes){int size=0;for(Node node:nodes){
        if(node instanceof Literal literal)size+=literal.text().codePointCount(0,literal.text().length());
        else if(node instanceof Group group)size+=literalSize(group.children());
        else if(node instanceof Animated animation)size+=animations.get(animation.name()).stream().mapToInt(this::literalSize).max().orElse(0);
    }return size;}
    public int cachedRenders(){return rendered.size();}
}
