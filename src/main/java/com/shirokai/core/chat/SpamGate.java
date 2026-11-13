package com.shirokai.core.chat;

import java.text.Normalizer;
import java.util.*;

/** Session-local bounded spam history. Used by every Core send path. */
public final class SpamGate {
    private record Entry(String text,long at){}
    private final Deque<Entry> recent=new ArrayDeque<>();
    private Long last;
    public static String normalize(String value){return Normalizer.normalize(value,Normalizer.Form.NFKD).replaceAll("[\\p{M}\\p{Cf}]", "").strip().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
    public void admit(String body,ChatConfig config,long now,boolean cooldownBypass,boolean filterBypass){
        if(body.isBlank()||body.codePointCount(0,body.length())>config.maximumLength()||body.codePoints().anyMatch(Character::isISOControl)||body.indexOf('§')>=0)
            throw new IllegalArgumentException("Mensagem vazia, longa ou com caracteres proibidos");
        String canonical=LegacyChatColors.plain(body);
        if(canonical.isBlank())throw new IllegalArgumentException("Mensagem vazia após códigos de cor");
        String normalized=normalize(canonical);
        if(normalized.isBlank())throw new IllegalArgumentException("Mensagem vazia após normalização");
        if(!filterBypass&&config.blockedWords().stream().map(SpamGate::normalize).anyMatch(normalized::contains))throw new IllegalStateException("Mensagem bloqueada pelo filtro");
        while(!recent.isEmpty()&&now-recent.getFirst().at()>=config.duplicateWindowSeconds()*1000)recent.removeFirst();
        if(!cooldownBypass){
            if(last!=null&&Math.max(0,now-last)<config.cooldownMillis())throw new IllegalStateException("Aguarde o cooldown do chat");
            if(config.duplicateWindowSeconds()>0&&recent.stream().anyMatch(entry->entry.text().equals(normalized)))throw new IllegalStateException("Mensagem repetida bloqueada");
        }
        recent.addLast(new Entry(normalized,now));while(recent.size()>20)recent.removeFirst();last=now;
    }
}
