package com.shirokai.core.chat;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;

/** Tokens have no target argument: providers receive only the authenticated message author. */
public final class ChatShares {
    private static final Pattern TOKEN = Pattern.compile("(?<![\\p{L}\\p{N}_@])@([1-6]|carteira)(?![\\p{L}\\p{N}_])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private BiFunction<UUID,Integer,Optional<Text>> pokemon = (id,slot) -> Optional.empty();
    private Function<UUID,Optional<Text>> wallet = id -> Optional.empty();
    public void providers(BiFunction<UUID,Integer,Optional<Text>> pokemon, Function<UUID,Optional<Text>> wallet) {
        this.pokemon=Objects.requireNonNull(pokemon);this.wallet=Objects.requireNonNull(wallet);
    }
    public static boolean contains(String body) { return TOKEN.matcher(body).find(); }
    public void validate(UUID author,String body){
        var matcher=TOKEN.matcher(LegacyChatColors.plain(body));var checked=new HashSet<Integer>();int count=0;
        while(matcher.find()&&count++<8){
            String token=matcher.group(1);
            if(token.equalsIgnoreCase("carteira"))continue;
            int slot=Integer.parseInt(token);
            if(checked.add(slot)&&pokemon.apply(author,slot).isEmpty())
                throw new IllegalStateException("Você precisa ter um Pokémon no slot "+slot+" para compartilhá-lo.");
        }
    }
    public Text render(UUID author, String body, Function<String,Text> literal) {
        if(!contains(body))return literal.apply(body);
        Text rendered=literal.apply(body);String plain=rendered.getString();
        var matcher=TOKEN.matcher(plain);var result=Text.empty();int cursor=0,count=0;
        var snapshots=new HashMap<String,Text>();
        while(matcher.find() && count++<8) {
            appendSlice(result,rendered,cursor,matcher.start());
            String key=matcher.group(1).toLowerCase(Locale.ROOT);
            var token=snapshots.computeIfAbsent(key,k->{
                var value=k.equals("carteira")?wallet.apply(author):pokemon.apply(author,Integer.parseInt(k));
                return value.orElseGet(()->Text.literal(k.equals("carteira")?"[Carteira indisponível]":"[Slot "+k+" vazio/indisponível]").formatted(Formatting.GRAY));
            });
            result.append(token.copy());cursor=matcher.end();
        }
        appendSlice(result,rendered,cursor,plain.length());
        return result;
    }
    private static void appendSlice(MutableText output,Text rendered,int from,int to){
        int[] offset={0};
        rendered.visit((style,part)->{
            int start=offset[0];offset[0]+=part.length();
            int left=Math.max(0,from-start),right=Math.min(part.length(),to-start);
            if(left<right)output.append(Text.literal(part.substring(left,right)).setStyle(style));
            return Optional.empty();
        },Style.EMPTY);
    }
}
