package com.shirokai.core.customization;

import com.shirokai.core.presentation.Surface;
import com.shirokai.core.storage.SqliteStorage;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Choices, authorization against the exact clan, CAS and audit share one SQL transaction. */
public final class StyleChoiceRepository {
    private final SqliteStorage storage;
    public StyleChoiceRepository(SqliteStorage storage) { this.storage=storage; }
    public static void migrate(Statement s) throws SQLException {
        s.execute("CREATE TABLE player_style_choices (owner_uuid TEXT PRIMARY KEY NOT NULL, revision INTEGER NOT NULL DEFAULT 0, title_preset TEXT)");
        s.execute("CREATE TABLE clan_style_choices (owner_uuid TEXT PRIMARY KEY NOT NULL REFERENCES clans(id) ON DELETE CASCADE, revision INTEGER NOT NULL DEFAULT 0, name_preset TEXT, tag_preset TEXT)");
    }
    private static String table(boolean clan) { return clan?"clan_style_choices":"player_style_choices"; }
    private static StyleChoices read(Connection c, UUID owner, boolean clan) throws SQLException {
        var selections=new EnumMap<Surface,String>(Surface.class);long revision=0;
        try(var s=c.prepareStatement("SELECT * FROM "+table(clan)+" WHERE owner_uuid=?")) {
            s.setString(1,owner.toString());
            try(var rows=s.executeQuery()) {
                if(rows.next()) {
                    revision=rows.getLong("revision");
                    for(var surface:clan?List.of(Surface.CLAN_NAME,Surface.CLAN_TAG):List.of(Surface.TITLE)) {
                        String id=rows.getString(column(surface));if(id!=null)selections.put(surface,id);
                    }
                }
            }
        }
        return new StyleChoices(owner,clan,revision,selections);
    }
    private static String column(Surface surface) {
        return switch(surface) {case TITLE->"title_preset";case CLAN_NAME->"name_preset";case CLAN_TAG->"tag_preset";default->throw new IllegalArgumentException("Superfície sem escolha");};
    }
    public CompletableFuture<StyleChoices> load(UUID owner, boolean clan) { return storage.transact(c->read(c,owner,clan)); }
    public CompletableFuture<StyleChoices> save(UUID actor,StyleChoices expected,Surface surface,String preset) {
        return save(actor,expected,surface,preset,()->true);
    }
    public CompletableFuture<StyleChoices> save(UUID actor,StyleChoices expected,Surface surface,String preset,java.util.function.BooleanSupplier current) {
        if (!PresetCatalog.SURFACES.contains(surface) || expected.clan()!=(surface!=Surface.TITLE) ||
            preset!=null && !preset.matches("[a-z][a-z0-9_-]{0,63}"))
            return CompletableFuture.failedFuture(new IllegalArgumentException("Escolha inválida"));
        return storage.transact(c->{
            if(!current.getAsBoolean())throw new IllegalStateException("Configuração mudou; tente novamente");
            if(expected.clan()) {
                try(var s=c.prepareStatement("SELECT 1 FROM clans JOIN clan_members ON clan_members.clan_id=clans.id WHERE clans.id=? AND leader_uuid=? AND player_uuid=?")) {
                    s.setString(1,expected.owner().toString());s.setString(2,actor.toString());s.setString(3,actor.toString());
                    try(var rows=s.executeQuery()) { if(!rows.next()) throw new IllegalStateException("Clã/liderança mudou; reabra o menu"); }
                }
            } else if(!actor.equals(expected.owner())) throw new IllegalStateException("Escolha pertence a outro jogador");
            String table=table(expected.clan());
            try(var s=c.prepareStatement("INSERT OR IGNORE INTO "+table+"(owner_uuid) VALUES(?)")) {s.setString(1,expected.owner().toString());s.executeUpdate();}
            try(var s=c.prepareStatement("UPDATE "+table+" SET "+column(surface)+"=?,revision=revision+1 WHERE owner_uuid=? AND revision=?")) {
                s.setString(1,preset);s.setString(2,expected.owner().toString());s.setLong(3,expected.revision());
                if(s.executeUpdate()!=1)throw new ConcurrentModificationException("Estilo atualizado simultaneamente; reabra o menu");
            }
            try(var s=c.prepareStatement("INSERT INTO admin_audit(actor_uuid,action,created_at) VALUES(?,?,?)")) {
                s.setString(1,actor.toString());s.setString(2,"style "+expected.owner()+" "+surface+" "+Objects.toString(preset,"padrao"));
                s.setString(3,java.time.Instant.now().toString());s.executeUpdate();
            }
            return read(c,expected.owner(),expected.clan());
        });
    }
}
