package com.shirokai.core.integrations;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import java.math.*;
import java.util.*;

/** Loaded only for Cobblemon 1.8.3+1.21.1. Read official records; never grant/encounter/create species records. */
public final class CobblemonAccess implements AutoCloseable {
    private com.cobblemon.mod.common.api.reactive.ObservableSubscription<com.cobblemon.mod.common.api.events.pokemon.PokedexDataChangedEvent.Post> subscription;
    public void listen(java.util.function.Consumer<java.util.UUID> changed) {
        subscription = com.cobblemon.mod.common.api.events.CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(
            (java.util.function.Consumer<com.cobblemon.mod.common.api.events.pokemon.PokedexDataChangedEvent.Post>) event -> changed.accept(event.getPlayerUUID()));
    }
    @Override public void close() { if (subscription != null) subscription.unsubscribe(); }

    public Optional<net.minecraft.text.Text> partyShare(ServerPlayerEntity author,int slot) {
        if (!author.getServer().isOnThread()) throw new IllegalStateException("Equipe exige thread do servidor");
        if(slot<1||slot>6)return Optional.empty();
        var pokemon=Cobblemon.INSTANCE.getStorage().getParty(author).get(slot-1);
        if(pokemon==null)return Optional.empty();
        // Do not forward nickname actions or parse nickname/species as markup.
        String name=pokemon.getDisplayName(false).getString();
        var tooltip=net.minecraft.text.Text.literal("Treinador: "+author.getGameProfile().getName()+" • Slot "+slot)
            .append("\n"+name+" • Nível "+pokemon.getLevel())
            .append("\nEspécie: "+pokemon.getSpecies().getName())
            .append("\nGênero: "+pokemon.getGender().name()+" • Shiny: "+(pokemon.getShiny()?"Sim":"Não"))
            .append("\nNatureza: "+pokemon.getNature().getName().getPath()+" • Habilidade: "+pokemon.getAbility().getName());
        return Optional.of(net.minecraft.text.Text.literal("["+name+" Nv. "+pokemon.getLevel()+"]")
            .formatted(net.minecraft.util.Formatting.AQUA).styled(style->style.withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT,tooltip))));
    }

    public Map<String, String> snapshot(ServerPlayerEntity player) {
        if (!player.getServer().isOnThread()) throw new IllegalStateException("Pokédex exige thread do servidor");
        var species = PokemonSpecies.INSTANCE.getImplemented();
        if (species.isEmpty()) return Map.of();
        var pokedex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
        int owned = 0, seen = 0;
        for (var entry : species) {
            var record = pokedex.getSpeciesRecord(entry.getResourceIdentifier());
            if (record == null) continue;
            if (record.hasAtLeast(PokedexEntryProgress.OWNED)) owned++;
            if (record.hasAtLeast(PokedexEntryProgress.SEEN)) seen++;
        }
        return Map.of("shirokai:pokedex_total", Integer.toString(species.size()), "shirokai:pokedex_owned", Integer.toString(owned),
            "shirokai:pokedex_seen", Integer.toString(seen), "shirokai:pokedex_percent", ProgressValues.percentage(owned, species.size()));
    }
}
