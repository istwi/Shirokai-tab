package com.shirokai.core.integrations;

import com.google.gson.*;
import com.shirokai.core.styles.JsonValidation;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public record IntegrationConfig(int schemaVersion, boolean placeholderApi, boolean cobblemon, boolean impactor,
        boolean shirokaiDex, int refreshTicks, String dollarsCurrency, String tokensCurrency, String cashCurrency,
        String gymsObjective, String dexCollection, String pokedexProvider) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    /** Source-compatible constructor for existing integrations/config fixtures. */
    public IntegrationConfig(int schemaVersion,boolean placeholderApi,boolean cobblemon,boolean impactor,
            boolean shirokaiDex,int refreshTicks,String dollarsCurrency,String tokensCurrency,
            String gymsObjective,String dexCollection,String pokedexProvider){
        this(schemaVersion,placeholderApi,cobblemon,impactor,shirokaiDex,refreshTicks,dollarsCurrency,tokensCurrency,legacyCash(dollarsCurrency,tokensCurrency),gymsObjective,dexCollection,pokedexProvider);
    }
    private static String legacyCash(String dollars,String tokens){return "impactor:cash".equals(dollars)||"impactor:cash".equals(tokens)?null:"impactor:cash";}
    public IntegrationConfig {
        if (schemaVersion != 1 || refreshTicks < 20 || refreshTicks > 1200
                || !currency(dollarsCurrency) || !currency(tokensCurrency) || dollarsCurrency.equals(tokensCurrency)
                || (cashCurrency != null && (!currency(cashCurrency) || cashCurrency.equals(dollarsCurrency) || cashCurrency.equals(tokensCurrency)))
                || gymsObjective == null || !gymsObjective.matches("[a-zA-Z0-9_.-]{1,16}")
                || dexCollection == null || !dexCollection.matches("[a-z][a-z0-9_]{0,47}")
                || !Set.of("cobblemon", "shirokaidex").contains(pokedexProvider)) throw new IllegalArgumentException("Configuração de integrações inválida");
    }
    private static boolean currency(String key) { return key != null && key.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") && key.length() <= 128; }
    public static IntegrationConfig defaults() { return new IntegrationConfig(1, true, true, true, true, 100, "impactor:dollars", "impactor:tokens", "gyms", "national", "cobblemon"); }
    public static IntegrationConfig load(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) try { Files.writeString(file, GSON.toJson(defaults()) + "\n", StandardOpenOption.CREATE_NEW); } catch (FileAlreadyExistsException ignored) { }
        if (Files.size(file) > 65536) throw new IOException("integrations.json excede 64KiB");
        try { return parse(Files.readString(file)); } catch (Exception error) { throw new IOException(file + ": " + error.getMessage(), error); }
    }
    public static IntegrationConfig parse(String text) throws IOException {
        var root = JsonValidation.object(text);
        JsonValidation.fields(root, Set.of("schemaVersion", "placeholderApi", "cobblemon", "impactor", "shirokaiDex", "refreshTicks", "dollarsCurrency", "tokensCurrency", "cashCurrency", "gymsObjective", "dexCollection", "pokedexProvider"));
        JsonValidation.integer(root, "schemaVersion"); JsonValidation.integer(root, "refreshTicks");
        for (String key : List.of("placeholderApi", "cobblemon", "impactor", "shirokaiDex")) JsonValidation.bool(root, key);
        for (String key : List.of("dollarsCurrency", "tokensCurrency", "gymsObjective", "dexCollection", "pokedexProvider")) JsonValidation.string(root, key, false);
        if(!root.has("cashCurrency")){
            String cash=legacyCash(root.get("dollarsCurrency").getAsString(),root.get("tokensCurrency").getAsString());
            root.addProperty("cashCurrency",cash);
        }
        JsonValidation.string(root,"cashCurrency",true);
        return GSON.fromJson(root, IntegrationConfig.class);
    }
    public boolean requiresRestartComparedTo(IntegrationConfig previous) {
        return placeholderApi != previous.placeholderApi || cobblemon != previous.cobblemon || impactor != previous.impactor || shirokaiDex != previous.shirokaiDex;
    }
}
