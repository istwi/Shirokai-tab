package com.shirokai.core.storage;

import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import com.shirokai.core.titles.TitleState;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** One connection confined to one bounded-admission worker. No JDBC on the game thread. */
public final class SqliteStorage implements AutoCloseable {
    @FunctionalInterface private interface Query<T> { T run(Connection connection) throws Exception; }
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> new Thread(task, "shirokai-sqlite"));
    private final AtomicInteger pending = new AtomicInteger();
    private final Path file;
    private final CompletableFuture<Void> ready;
    private Connection connection;
    private boolean closed;
    private CompletableFuture<Void> closing;
    public SqliteStorage(Path file) {
        this.file = file.toAbsolutePath();
        ready = CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(this.file.getParent());
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + this.file);
                try (Statement s = connection.createStatement()) {
                    s.execute("PRAGMA foreign_keys=ON");
                    s.execute("PRAGMA busy_timeout=5000");
                    s.execute("PRAGMA journal_mode=WAL");
                    s.execute("PRAGMA synchronous=FULL");
                }
                migrate(connection);
            } catch (Exception failure) {
                if (connection != null) try { connection.close(); } catch (SQLException close) { failure.addSuppressed(close); }
                connection = null;
                throw new CompletionException(failure);
            }
        }, worker);
    }
    public CompletableFuture<Void> ready() { return ready.copy(); }
    public int pendingOperations() { return pending.get(); }
    private void migrate(Connection c) throws Exception {
        int version;
        try (Statement s = c.createStatement(); ResultSet rows = s.executeQuery("PRAGMA user_version")) {
            if (!rows.next()) throw new SQLException("Schema não retornado"); version = rows.getInt(1);
        }
        if (version > 6) throw new SQLException("Banco mais recente que o mod: schema " + version);
        if (version == 6) return;
        if (version >= 1 && version <= 5) {
            Path backup = file.resolveSibling(file.getFileName() + ".schema" + version + "-" + UUID.randomUUID() + ".bak.db");
            try (PreparedStatement s = c.prepareStatement("VACUUM INTO ?")) { s.setString(1, backup.toString()); s.execute(); }
        }
        final int previous = version;
        transaction(c, db -> {
            try (Statement s = db.createStatement()) {
                if (previous == 0) {
                    s.execute("CREATE TABLE player_preferences (player_uuid TEXT PRIMARY KEY NOT NULL, scoreboard_enabled INTEGER NOT NULL DEFAULT 1 CHECK(scoreboard_enabled IN (0,1)))");
                    s.execute("CREATE TABLE admin_audit (id INTEGER PRIMARY KEY, actor_uuid TEXT, action TEXT NOT NULL, created_at TEXT NOT NULL)");
                }
                if (previous < 2) {
                s.execute("CREATE TABLE title_profiles (player_uuid TEXT PRIMARY KEY NOT NULL, revision INTEGER NOT NULL DEFAULT 0, selected_visual TEXT)");
                s.execute("CREATE TABLE title_grants (player_uuid TEXT NOT NULL REFERENCES title_profiles(player_uuid) ON DELETE CASCADE, title_id TEXT NOT NULL, expires_at INTEGER NOT NULL DEFAULT 0 CHECK(expires_at>=0), PRIMARY KEY(player_uuid,title_id))");
                s.execute("CREATE TABLE chat_identifications (player_uuid TEXT NOT NULL REFERENCES title_profiles(player_uuid) ON DELETE CASCADE, slot INTEGER NOT NULL CHECK(slot IN (0,1)), title_id TEXT NOT NULL, PRIMARY KEY(player_uuid,slot), UNIQUE(player_uuid,title_id))");
                }
                if(previous < 3)ClanSchema.migrate(s);
                if(previous < 4)ChatSchema.migrate(s);
                if(previous < 5)s.execute("ALTER TABLE player_preferences ADD COLUMN animations_enabled INTEGER NOT NULL DEFAULT 1 CHECK(animations_enabled IN(0,1))");
                com.shirokai.core.customization.StyleChoiceRepository.migrate(s);
                s.execute("PRAGMA user_version=6");
            }
            return null;
        });
    }
    private static <T> T transaction(Connection c, Query<T> operation) throws Exception {
        c.setAutoCommit(false);
        try { T result = operation.run(c); c.commit(); return result; }
        catch (Exception failure) { try { c.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); } throw failure; }
        finally { c.setAutoCommit(true); }
    }
    private synchronized <T> CompletableFuture<T> query(Query<T> query) {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Storage encerrado"));
        if (pending.get() >= 256) return CompletableFuture.failedFuture(new RejectedExecutionException("Fila SQLite cheia"));
        pending.incrementAndGet();
        var result = new CompletableFuture<T>();
        // Cancellation of a caller's future never drops an already accepted write or leaks admission slots.
        worker.execute(() -> {
            try {
                if (connection == null) throw new IllegalStateException("SQLite indisponível", ready.handle((v,e) -> e).join());
                result.complete(query.run(connection));
            } catch (Exception failure) { result.completeExceptionally(failure); }
            finally { pending.decrementAndGet(); }
        });
        return result;
    }
    @FunctionalInterface public interface DatabaseWork<T> { T run(Connection connection) throws Exception; }
    /** Trusted repositories only: callback is confined to the JDBC worker; never access Minecraft APIs here. */
    public <T> CompletableFuture<T> transact(DatabaseWork<T> operation) {
        return query(connection -> transaction(connection, operation::run));
    }
    public CompletableFuture<PlayerPreferences> preferences(UUID id) {
        java.util.Objects.requireNonNull(id);
        return query(c -> {
            try (PreparedStatement s = c.prepareStatement("SELECT scoreboard_enabled,animations_enabled FROM player_preferences WHERE player_uuid=?")) {
                s.setString(1, id.toString());
                try (ResultSet rows = s.executeQuery()) { boolean exists=rows.next();return new PlayerPreferences(id,!exists||rows.getBoolean(1),!exists||rows.getBoolean(2)); }
            }
        });
    }
    public CompletableFuture<Void> setScoreboard(UUID id, boolean enabled) {
        java.util.Objects.requireNonNull(id);
        return query(c -> {
            try (PreparedStatement s = c.prepareStatement("INSERT INTO player_preferences(player_uuid,scoreboard_enabled) VALUES(?,?) ON CONFLICT(player_uuid) DO UPDATE SET scoreboard_enabled=excluded.scoreboard_enabled")) {
                s.setString(1, id.toString()); s.setBoolean(2, enabled); s.executeUpdate(); return null;
            }
        });
    }
    public CompletableFuture<Void> setAnimations(UUID player,boolean enabled){
        return query(c->{try(var s=c.prepareStatement("INSERT INTO player_preferences(player_uuid,animations_enabled) VALUES(?,?) ON CONFLICT(player_uuid) DO UPDATE SET animations_enabled=excluded.animations_enabled")){
            s.setString(1,player.toString());s.setBoolean(2,enabled);s.executeUpdate();return null;
        }});
    }
    public CompletableFuture<TitleState> titleState(UUID player) {
        return query(c -> transaction(c, db -> readTitles(db, player)));
    }
    private static TitleState readTitles(Connection c, UUID player) throws SQLException {
        long revision = 0; String visual = null;
        try (var s = c.prepareStatement("SELECT revision,selected_visual FROM title_profiles WHERE player_uuid=?")) {
            s.setString(1, player.toString()); try (var rows = s.executeQuery()) { if (rows.next()) { revision = rows.getLong(1); visual = rows.getString(2); } }
        }
        var grants = new HashMap<String, Long>();
        try (var s = c.prepareStatement("SELECT title_id,expires_at FROM title_grants WHERE player_uuid=?")) {
            s.setString(1, player.toString()); try (var rows = s.executeQuery()) { while (rows.next()) grants.put(rows.getString(1), rows.getLong(2)); }
        }
        var chat = new ArrayList<String>();
        try (var s = c.prepareStatement("SELECT title_id FROM chat_identifications WHERE player_uuid=? ORDER BY slot")) {
            s.setString(1, player.toString()); try (var rows = s.executeQuery()) { while (rows.next()) chat.add(rows.getString(1)); }
        }
        return new TitleState(player, revision, grants, visual, chat);
    }
    private static void ensureProfile(Connection c, UUID player) throws SQLException {
        try (var s = c.prepareStatement("INSERT OR IGNORE INTO title_profiles(player_uuid) VALUES(?)")) { s.setString(1, player.toString()); s.executeUpdate(); }
    }
    public CompletableFuture<TitleState> saveTitles(TitleState state) {
        if (state.chatIds().size() > 2 || new HashSet<>(state.chatIds()).size() != state.chatIds().size())
            return CompletableFuture.failedFuture(new IllegalArgumentException("Máximo de duas identificações únicas"));
        return query(c -> transaction(c, db -> {
            ensureProfile(db, state.player());
            var chosen=new ArrayList<>(state.chatIds());if(state.selectedVisual()!=null)chosen.add(state.selectedVisual());
            for(String id:chosen)if(id.startsWith("clan_")){
                String clan=UUID.fromString(id.substring(5)).toString();
                try(var check=db.prepareStatement("SELECT 1 FROM clan_members JOIN clans ON clans.id=clan_members.clan_id WHERE player_uuid=? AND clan_id=? AND tag IS NOT NULL")){
                    check.setString(1,state.player().toString());check.setString(2,clan);try(var rows=check.executeQuery()){if(!rows.next())throw new IllegalStateException("Identificação de clã não pertence mais ao jogador");}
                }
            }
            try (var s = db.prepareStatement("UPDATE title_profiles SET selected_visual=?,revision=revision+1 WHERE player_uuid=? AND revision=?")) {
                s.setString(1, state.selectedVisual()); s.setString(2, state.player().toString()); s.setLong(3, state.revision());
                if (s.executeUpdate() != 1) throw new ConcurrentModificationException("Perfil atualizado simultaneamente; tente novamente");
            }
            try (var s = db.prepareStatement("DELETE FROM chat_identifications WHERE player_uuid=?")) { s.setString(1, state.player().toString()); s.executeUpdate(); }
            try (var s = db.prepareStatement("INSERT INTO chat_identifications(player_uuid,slot,title_id) VALUES(?,?,?)")) {
                for (int slot = 0; slot < state.chatIds().size(); slot++) {
                    s.setString(1, state.player().toString()); s.setInt(2, slot); s.setString(3, state.chatIds().get(slot)); s.executeUpdate();
                }
            }
            return readTitles(db, state.player());
        }));
    }
    public CompletableFuture<TitleState> grantTitle(UUID player, String title, long expiresAt, UUID actor) {
        if (expiresAt < 0 || !title.matches("[a-z0-9][a-z0-9_-]{0,63}")) throw new IllegalArgumentException("Concessão inválida");
        return query(c -> transaction(c, db -> {
            ensureProfile(db, player);
            try (var s = db.prepareStatement("INSERT INTO title_grants(player_uuid,title_id,expires_at) VALUES(?,?,?) ON CONFLICT(player_uuid,title_id) DO UPDATE SET expires_at=excluded.expires_at")) {
                s.setString(1, player.toString()); s.setString(2, title); s.setLong(3, expiresAt); s.executeUpdate();
            }
            try (var s = db.prepareStatement("UPDATE title_profiles SET revision=revision+1 WHERE player_uuid=?")) { s.setString(1, player.toString()); s.executeUpdate(); }
            auditRow(db, actor, "titles grant " + player + " " + title + " expires=" + expiresAt);
            return readTitles(db, player);
        }));
    }
    public CompletableFuture<TitleState> revokeTitle(UUID player, String title, UUID actor) {
        if (!title.matches("[a-z0-9][a-z0-9_-]{0,63}")) throw new IllegalArgumentException("ID inválido");
        return query(c -> transaction(c, db -> {
            ensureProfile(db, player);
            for (String sql : List.of("DELETE FROM title_grants WHERE player_uuid=? AND title_id=?", "DELETE FROM chat_identifications WHERE player_uuid=? AND title_id=?")) {
                try (var s = db.prepareStatement(sql)) { s.setString(1, player.toString()); s.setString(2, title); s.executeUpdate(); }
            }
            try (var s = db.prepareStatement("UPDATE title_profiles SET selected_visual=CASE WHEN selected_visual=? THEN NULL ELSE selected_visual END, revision=revision+1 WHERE player_uuid=?")) {
                s.setString(1, title); s.setString(2, player.toString()); s.executeUpdate();
            }
            auditRow(db, actor, "titles revoke " + player + " " + title);
            return readTitles(db, player);
        }));
    }
    private static void auditRow(Connection c, UUID actor, String action) throws SQLException {
        try (var s = c.prepareStatement("INSERT INTO admin_audit(actor_uuid,action,created_at) VALUES(?,?,?)")) {
            s.setString(1, actor == null ? null : actor.toString()); s.setString(2, action); s.setString(3, Instant.now().toString()); s.executeUpdate();
        }
    }
    public CompletableFuture<String> integrityCheck() {
        return query(c -> {
            try (Statement s = c.createStatement(); ResultSet rows = s.executeQuery("PRAGMA integrity_check")) {
                StringBuilder result = new StringBuilder();
                while (rows.next()) { if (!result.isEmpty()) result.append(';'); result.append(rows.getString(1)); }
                return result.toString();
            }
        });
    }
    public CompletableFuture<Void> audit(UUID actor, String action) {
        if (action == null || action.length() > 1024) throw new IllegalArgumentException("Invalid audit action");
        return query(c -> {
            try (PreparedStatement s = c.prepareStatement("INSERT INTO admin_audit(actor_uuid,action,created_at) VALUES(?,?,?)")) {
                s.setString(1, actor == null ? null : actor.toString()); s.setString(2, action);
                s.setString(3, Instant.now().toString()); s.executeUpdate(); return null;
            }
        });
    }
    public CompletableFuture<Path> backup(Path destination) {
        Path target = destination.toAbsolutePath();
        return query(c -> {
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) throw new FileAlreadyExistsException(target.toString());
            try (PreparedStatement s = c.prepareStatement("VACUUM INTO ?")) { s.setString(1, target.toString()); s.execute(); }
            return target;
        });
    }
    /** Drains accepted writes before closing; waiting occurs only during server shutdown. */
    @Override public void close() throws Exception {
        synchronized (this) {
            if (closed) return;
            closed = true;
            closing = CompletableFuture.runAsync(() -> {
                if (connection != null) try { connection.close(); }
                catch (SQLException failure) { throw new CompletionException(failure); }
            }, worker);
            worker.shutdown();
        }
        if (!worker.awaitTermination(30, TimeUnit.SECONDS)) throw new IllegalStateException("SQLite ainda encerrando; operações preservadas na fila");
        closing.get();
    }
}
