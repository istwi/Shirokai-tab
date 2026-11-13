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
        return partyShare(author,slot,net.minecraft.text.Text.literal(author.getGameProfile().getName()));
    }
    public Optional<net.minecraft.text.Text> partyShare(ServerPlayerEntity author,int slot,net.minecraft.text.Text trainer) {
        if (!author.getServer().isOnThread()) throw new IllegalStateException("Equipe exige thread do servidor");
        if(slot<1||slot>6)return Optional.empty();
        var pokemon=Cobblemon.INSTANCE.getStorage().getParty(author).get(slot-1);
        if(pokemon==null)return Optional.empty();
        String name=pokemon.getDisplayName(false).getString();
        var tooltip=net.minecraft.text.Text.empty().append(label("Treinador: ")).append(trainer.copy())
            .append(label(" • Slot ")).append(value(Integer.toString(slot)))
            .append(label("\nPokémon: ")).append(value(name))
            .append(label(" • Nível: ")).append(value(Integer.toString(pokemon.getLevel())))
            .append(label("\nEspécie: ")).append(value(pokemon.getSpecies().getName()))
            .append(label("\nGênero: ")).append(value(pokemon.getGender().name()))
            .append(label(" • Shiny: ")).append(value(pokemon.getShiny()?"Sim":"Não"))
            .append(label("\nNatureza: ")).append(value(pokemon.getNature().getName().getPath()))
            .append(label(" • Habilidade: ")).append(value(pokemon.getAbility().getName()))
            .append(label("\nIVs (HP/Atk/Def/SpA/SpD/Spe):"))
            .append(value("\n"+ivs(pokemon.getIvs())+" • Total: "+pokemon.getIvs().total()+"/186"));
        return Optional.of(net.minecraft.text.Text.literal("["+name+"]")
            .formatted(net.minecraft.util.Formatting.AQUA).styled(style->style.withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT,tooltip))));
    }
    private static net.minecraft.text.MutableText label(String text){return net.minecraft.text.Text.literal(text).formatted(net.minecraft.util.Formatting.GRAY);}
    private static net.minecraft.text.MutableText value(String text){return net.minecraft.text.Text.literal(text).formatted(net.minecraft.util.Formatting.WHITE);}
    private static String ivs(com.cobblemon.mod.common.pokemon.IVs ivs){
        var stats=List.of(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP,com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK,
            com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE,com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK,
            com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE,com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED);
        return stats.stream().map(stat->Integer.toString(ivs.getOrDefault(stat))).collect(java.util.stream.Collectors.joining(" / "));
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
