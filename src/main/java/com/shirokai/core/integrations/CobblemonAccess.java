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
