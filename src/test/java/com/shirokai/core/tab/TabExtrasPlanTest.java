package com.shirokai.core.tab;

import com.shirokai.core.styles.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TabExtrasPlanTest {
    @TempDir Path directory;
    @Test void defaultsAreDisabledAndExistingConfigurationIsNeverOverwritten()throws Exception{
        var file=directory.resolve("tab-extras.json");var config=TabExtrasConfig.load(file);
        assertEquals(TabExtrasConfig.defaults(),config);assertFalse(config.listed().enabled());assertFalse(config.nametags().enabled());assertFalse(config.belowName().enabled());assertFalse(config.bossbar().enabled());
        String original=Files.readString(file);TabExtrasConfig.load(file);assertEquals(original,Files.readString(file));
        Files.writeString(file,"invalid");assertThrows(Exception.class,()->TabExtrasConfig.load(file));assertEquals("invalid",Files.readString(file));
    }
    @Test void unknownFieldsEnumsTypesAndNumericMarkupFailBeforePublishing()throws Exception{
        var engine=new StyleEngine(StyleConfig.defaults());String valid=new Gson().toJson(TabExtrasConfig.defaults());
        new TabExtrasPlan(TabExtrasConfig.parse(valid),engine);
        for(String bad:List.of(valid.replace("\"enabled\":false","\"enabled\":\"false\""),valid.replace("\"schemaVersion\":1","\"schemaVersion\":2"),
            valid.replace("\"visibility\":\"ALWAYS\"","\"visibility\":\"MAYBE\""),valid.replace("\"color\":\"YELLOW\"","\"color\":\"GOLD\""),
            valid.replace("\"schemaVersion\":1","\"schemaVersion\":1,\"unknown\":true")))
            assertThrows(Exception.class,()->new TabExtrasPlan(TabExtrasConfig.parse(bad),engine));
        var root=JsonParser.parseString(valid).getAsJsonObject();root.getAsJsonObject("bossbar").addProperty("progress","<red>1</red>");
        assertThrows(Exception.class,()->new TabExtrasPlan(TabExtrasConfig.parse(root.toString()),engine));
        root.getAsJsonObject("bossbar").addProperty("progress","1.1");assertThrows(Exception.class,()->new TabExtrasPlan(TabExtrasConfig.parse(root.toString()),engine));
        root.getAsJsonObject("bossbar").addProperty("progress","1");root.getAsJsonObject("nametags").addProperty("prefix","%animation:unknown%");
        assertThrows(Exception.class,()->new TabExtrasPlan(TabExtrasConfig.parse(root.toString()),engine));
    }
    @Test void ownershipFlagsRequireRestartButTemplatesAndConditionsReload()throws Exception{
        var base=TabExtrasConfig.defaults();var root=new Gson().toJsonTree(base).getAsJsonObject();
        for(String feature:List.of("listed","nametags","belowName","bossbar")){
            var changed=root.deepCopy();changed.getAsJsonObject(feature).addProperty("enabled",true);
            assertTrue(TabExtrasConfig.parse(changed.toString()).requiresRestartComparedTo(base));
        }
        root.getAsJsonObject("bossbar").addProperty("title","<red>New</red>");
        assertFalse(TabExtrasConfig.parse(root.toString()).requiresRestartComparedTo(base));
    }
    @Test void numericDataRejectsUnavailableOverflowNaNAndOutOfRangeWithoutInventingValues(){
        assertEquals(Integer.MIN_VALUE,TabExtrasPlan.score("-2147483648"));assertEquals(42,TabExtrasPlan.score("42"));
        for(String value:List.of("N/D","2147483648","1.5","NaN","<red>1</red>"))assertThrows(IllegalArgumentException.class,()->TabExtrasPlan.score(value));
        assertEquals(.25f,TabExtrasPlan.progress("0.25"));assertEquals(1f,TabExtrasPlan.progress("1"));
        for(String value:List.of("NaN","Infinity","N/D","-0.1","1.01","1e0","1".repeat(33)))assertThrows(IllegalArgumentException.class,()->TabExtrasPlan.progress(value));
    }
    @Test void templatesKeepImportedValuesLiteral(){
        var plan=new TabExtrasPlan(TabExtrasConfig.defaults(),new StyleEngine(StyleConfig.defaults()));
        var value=plan.styles().renderRich(plan.prefix(),key->net.minecraft.text.Text.literal("<click:/op>literal</click>"),0,false);
        assertTrue(value.getString().contains("<click:/op>"));assertNull(value.getStyle().getClickEvent());
    }
}
