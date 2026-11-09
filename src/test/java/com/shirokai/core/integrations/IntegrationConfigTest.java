package com.shirokai.core.integrations;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationConfigTest {
    @TempDir Path directory;
    @Test void defaultsPreserveFilesAndRejectCurrencyMixupsAndTypes() throws Exception {
        var file = directory.resolve("integrations.json"); assertEquals(IntegrationConfig.defaults(), IntegrationConfig.load(file));
        String source = Files.readString(file);
        for (String invalid : List.of("null", "[]", source.replace("\"impactor\": true", "\"impactor\": \"true\""), source.replace("impactor:tokens", "impactor:dollars"), source.replace("\"refreshTicks\": 100", "\"refreshTicks\": 0"), source.replace("\"pokedexProvider\": \"cobblemon\"", "\"pokedexProvider\": \"auto\""))) assertThrows(Exception.class, () -> IntegrationConfig.parse(invalid));
        Files.writeString(file, "invalid"); assertThrows(Exception.class, () -> IntegrationConfig.load(file)); assertEquals("invalid", Files.readString(file));
    }
    @Test void enablementRequiresRestartButDataMappingCanReload() throws Exception {
        var file = directory.resolve("integrations.json"); IntegrationConfig.load(file); var text = Files.readString(file);
        var defaults = IntegrationConfig.defaults();
        assertTrue(IntegrationConfig.parse(text.replace("\"impactor\": true", "\"impactor\": false")).requiresRestartComparedTo(defaults));
        assertFalse(IntegrationConfig.parse(text.replace("impactor:dollars", "impactor:other")).requiresRestartComparedTo(defaults));
        assertFalse(IntegrationConfig.parse(text.replace("\"pokedexProvider\": \"cobblemon\"", "\"pokedexProvider\": \"shirokaidex\"")).requiresRestartComparedTo(defaults));
    }
    @Test void oldFilesGetCashDefaultWithoutBeingOverwritten()throws Exception{
        var root=new Gson().toJsonTree(IntegrationConfig.defaults()).getAsJsonObject();root.remove("cashCurrency");
        var file=directory.resolve("legacy.json");String original=root.toString();Files.writeString(file,original);
        assertEquals("impactor:cash",IntegrationConfig.load(file).cashCurrency());assertEquals(original,Files.readString(file));
        root.addProperty("dollarsCurrency","impactor:cash");
        var legacy=IntegrationConfig.parse(root.toString());assertEquals("impactor:cash",legacy.dollarsCurrency());assertNull(legacy.cashCurrency());
    }
    @Test void cashIsDistinctTypedAndCanReloadOrBeDisabled()throws Exception{
        var root=new Gson().toJsonTree(IntegrationConfig.defaults()).getAsJsonObject();
        for(String value:List.of("impactor:dollars","impactor:tokens","Invalid key")){
            root.addProperty("cashCurrency",value);String invalid=root.toString();assertThrows(Exception.class,()->IntegrationConfig.parse(invalid));
        }
        root.addProperty("cashCurrency",true);String invalid=root.toString();assertThrows(Exception.class,()->IntegrationConfig.parse(invalid));
        root.addProperty("cashCurrency","custom:cash");assertFalse(IntegrationConfig.parse(root.toString()).requiresRestartComparedTo(IntegrationConfig.defaults()));
        root.add("cashCurrency",JsonNull.INSTANCE);assertNull(IntegrationConfig.parse(root.toString()).cashCurrency());
    }
    @Test void progressUsesRealDenominatorAndRejectsUnknownTotals() {
        assertEquals("0.11", ProgressValues.percentage(1, 905));
        assertEquals("100", ProgressValues.percentage(905, 905));
        assertEquals("0", ProgressValues.percentage(0, 905));
        assertThrows(IllegalArgumentException.class, () -> ProgressValues.percentage(0, 0));
        assertThrows(IllegalArgumentException.class, () -> ProgressValues.percentage(906, 905));
    }
}
