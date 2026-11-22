package com.shirokai.core.cosmetics;

import com.google.gson.*;
import com.shirokai.core.storage.SqliteStorage;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** All SQL and legacy/config IO run on the existing bounded SQLite worker. */
public final class CosmeticRepository {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final SqliteStorage storage;
    private final Path legacy;
    public record Profile(long revision,CosmeticState state){}
    public CosmeticRepository(SqliteStorage storage,Path legacy){this.storage=storage;this.legacy=legacy;}
    public CompletableFuture<CosmeticSettings> initialize(Path config,Path oldConfig){return storage.transact(c->{
        try(var s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS cosmetic_profiles(player_uuid TEXT PRIMARY KEY NOT NULL,revision INTEGER NOT NULL DEFAULT 0,state_json TEXT NOT NULL,legacy_imported INTEGER NOT NULL DEFAULT 0 CHECK(legacy_imported IN(0,1)))");}
        if(!Files.exists(config)&&Files.exists(oldConfig)){
            var value=CosmeticSettings.load(oldConfig);writeConfig(config,value);
        }
        return CosmeticSettings.load(config);
    });}
    public CompletableFuture<CosmeticSettings> reload(Path file){return storage.transact(c->CosmeticSettings.load(file));}
    public static CosmeticSettings readSettings(Path path)throws IOException {
        if(!Files.exists(path))writeConfig(path,new CosmeticSettings());
        if(Files.size(path)>1024*1024)throw new IOException("Configuração maior que1MiB");
        try{var cfg=JSON.fromJson(Files.readString(path),CosmeticSettings.class);if(cfg==null)throw new IOException("Configuração vazia");return cfg;}catch(JsonParseException e){throw new IOException("JSON inválido",e);}
    }
    private static void writeConfig(Path path,CosmeticSettings value)throws IOException{
        Files.createDirectories(path.getParent());var tmp=Files.createTempFile(path.getParent(),"cosmetics-",".tmp");
        try{Files.writeString(tmp,JSON.toJson(value)+"\n",StandardCharsets.UTF_8);Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(tmp);}
    }
    public CompletableFuture<Profile> load(UUID id){return storage.transact(c->{
        try(var s=c.prepareStatement("SELECT revision,state_json FROM cosmetic_profiles WHERE player_uuid=?")){s.setString(1,id.toString());try(var row=s.executeQuery()){if(row.next()){var value=JSON.fromJson(row.getString(2),CosmeticState.class);value.validate();return new Profile(row.getLong(1),value);}}}
        var state=new CosmeticState();var file=legacy.resolve(id+".json");boolean imported=Files.isRegularFile(file);
        if(imported){
            if(Files.size(file)>2*1024*1024)throw new IOException("Perfil legado muito grande");
            var object=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(object.has("cosmetics"))state.cosmetics=JSON.fromJson(object.get("cosmetics"),new com.google.gson.reflect.TypeToken<TreeMap<String,String>>(){}.getType());
            if(object.has("cosmeticsHidden"))state.cosmeticsHidden=object.get("cosmeticsHidden").getAsBoolean();
            if(object.has("cosmeticPreferences"))state.cosmeticPreferences=JSON.fromJson(object.get("cosmeticPreferences"),CosmeticState.Preferences.class);
        }
        state.validate();
        try(var s=c.prepareStatement("INSERT INTO cosmetic_profiles(player_uuid,revision,state_json,legacy_imported) VALUES(?,0,?,?)")){s.setString(1,id.toString());s.setString(2,JSON.toJson(state));s.setBoolean(3,imported);s.executeUpdate();}
        return new Profile(0,state);
    });}
    public CompletableFuture<Profile> save(UUID id,long revision,CosmeticState state){
        final CosmeticState copy=state.copy();try{copy.validate();}catch(IOException e){return CompletableFuture.failedFuture(e);}
        return storage.transact(c->{try(var s=c.prepareStatement("UPDATE cosmetic_profiles SET state_json=?,revision=revision+1 WHERE player_uuid=? AND revision=?")){s.setString(1,JSON.toJson(copy));s.setString(2,id.toString());s.setLong(3,revision);if(s.executeUpdate()!=1)throw new IllegalStateException("Seleção mudou; reabra o menu");}
            try(var s=c.prepareStatement("INSERT INTO admin_audit(actor_uuid,action,created_at) VALUES(?,?,?)")){s.setString(1,id.toString());s.setString(2,"cosmetics:preferences");s.setString(3,java.time.Instant.now().toString());s.executeUpdate();}
            return new Profile(revision+1,copy);
        });
    }
}
