package com.shirokai.core.titles;

import java.util.*;
import java.util.function.BiPredicate;

/** Pure authorization and ordering policy; no game/SQL APIs, never changes LuckPerms groups. */
public final class TitleRules {
    public enum Availability { UNLOCKED, LOCKED, EXPIRED, UNAVAILABLE }
    private final BiPredicate<UUID, String> permissions;
    private final BiPredicate<UUID,String> clans;
    public TitleRules(BiPredicate<UUID, String> permissions) { this(permissions,(player,title)->false); }
    public TitleRules(BiPredicate<UUID,String> permissions,BiPredicate<UUID,String> clans){this.permissions=permissions;this.clans=clans;}
    public Availability availability(TitleDefinition title, TitleState state, long now) {
        if (title.category()==TitleDefinition.Category.CLAN) return clans.test(state.player(),title.id())?Availability.UNLOCKED:Availability.UNAVAILABLE;
        if (title.permission() != null && !permissions.test(state.player(), title.permission())) return Availability.UNAVAILABLE;
        if (title.category() == TitleDefinition.Category.ROLE) return Availability.UNLOCKED;
        Long expiry = state.grants().get(title.id());
        if (expiry != null) return expiry == 0 || expiry > now ? Availability.UNLOCKED : Availability.EXPIRED;
        return title.unlockedByDefault() ? Availability.UNLOCKED : Availability.LOCKED;
    }
    public TitleState normalize(TitleState state, TitleCatalog catalog, long now) {
        String selected = null; // Visual rank belongs to LuckPerms, never to a saved player choice.
        var chat = state.chatIds().stream().distinct().map(catalog.definitions()::get)
                .filter(Objects::nonNull).filter(t -> t.category() != TitleDefinition.Category.ROLE && t.chat() && availability(t, state, now) == Availability.UNLOCKED)
                .sorted(Comparator.comparing(TitleDefinition::category).thenComparing(Comparator.comparingInt(TitleDefinition::priority).reversed()).thenComparing(TitleDefinition::id))
                .limit(2).map(TitleDefinition::id).toList();
        return new TitleState(state.player(), state.revision(), state.grants(), selected, chat);
    }
    public TitleState equip(TitleState current, TitleCatalog catalog, String id, boolean chat, long now) {
        var state = normalize(current, catalog, now);
        TitleDefinition title = catalog.definitions().get(id);
        if (title == null || availability(title, state, now) != Availability.UNLOCKED) throw new IllegalArgumentException("Título indisponível: " + id);
        if (title.category() == TitleDefinition.Category.ROLE) throw new IllegalArgumentException("Cargo é automático pelo LuckPerms e não pode ser escolhido");
        if (!chat) throw new IllegalArgumentException("Tags opcionais são exclusivas do chat; use /perfil chat <id>");
        if (chat) {
            if (!title.chat()) throw new IllegalArgumentException("Título não disponível no chat");
            var tags = new ArrayList<>(state.chatIds());
            if (tags.contains(id)) tags.remove(id);
            else { if (tags.size() >= 2) throw new IllegalArgumentException("Máximo de duas identificações no total"); tags.add(id); }
            return normalize(new TitleState(state.player(), state.revision(), state.grants(), state.selectedVisual(), tags), catalog, now);
        }
        throw new IllegalArgumentException("Seleção visual indisponível");
    }
}
