package com.shirokai.core.menus;

import com.google.gson.*;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuConfigTest {
    @TempDir Path directory;
    private JsonObject defaults(){return JsonParser.parseString(MenuConfig.json(MenuConfig.defaults())).getAsJsonObject();}
    @Test void defaultLayoutRoundTripsAndClansAreAvailable()throws Exception{
        var config=MenuConfig.parse(MenuConfig.json(MenuConfig.defaults()));assertEquals(MenuConfig.defaults(),config);
        assertTrue(config.clanMenuEnabled());assertEquals(15,config.profile().tagSlots().size());
        assertEquals(40,config.profile().buttons().get("clear").slot());new MenuPlan(config);
    }
    @Test void existingFilesAreNeverOverwrittenAndInvalidFilesFail()throws Exception{
        var file=directory.resolve("menus.json");MenuConfig.load(file);var original=Files.readString(file);
        MenuConfig.load(file);assertEquals(original,Files.readString(file));
        Files.writeString(file,"invalid");assertThrows(Exception.class,()->MenuConfig.load(file));assertEquals("invalid",Files.readString(file));
    }
    @Test void legacyDefaultMigratesWithBackupAndCustomLayoutIsPreserved()throws Exception{
        var file=directory.resolve("menus.json");var legacy=MenuConfig.legacyDefaults();
        String source=MenuConfig.json(new MenuConfig(1,false,legacy.profile()));Files.writeString(file,source);
        var migrated=MenuConfig.load(file);assertFalse(migrated.clanMenuEnabled());assertEquals(MenuConfig.defaults().profile(),migrated.profile());
        assertEquals(source,Files.readString(file.resolveSibling("menus.json.pre-0.16.6.bak")));
        String updated=Files.readString(file);MenuConfig.load(file);assertEquals(updated,Files.readString(file));
        String custom=MenuConfig.json(legacy).replace("PERFIL SHIROKAI","MEU PERFIL");Files.writeString(file,custom);
        var customFile=directory.resolve("custom.json");Files.writeString(customFile,custom);
        var migratedCustom=MenuConfig.load(customFile);assertEquals(5,migratedCustom.profile().rows());assertTrue(migratedCustom.profile().title().contains("MEU PERFIL"));
        assertEquals(custom,Files.readString(customFile.resolveSibling("custom.json.pre-0.16.6.bak")));
        var modified=JsonParser.parseString(custom).getAsJsonObject();var buttons=modified.getAsJsonObject("profile").getAsJsonObject("buttons");
        buttons.getAsJsonObject("header").addProperty("slot",47);buttons.getAsJsonObject("styles").addProperty("slot",4);
        Files.writeString(file,modified.toString());assertEquals(6,MenuConfig.load(file).profile().rows());assertEquals(modified.toString(),Files.readString(file));
    }
    @Test void collisionsOutOfRangeRowsAndDuplicateTagSlotsAreRejected(){
        for(int slot:new int[]{10,45,54,-1}){var root=defaults();root.getAsJsonObject("profile").getAsJsonArray("tagSlots").add(slot);assertThrows(Exception.class,()->MenuConfig.parse(root.toString()));}
        var root=defaults();root.getAsJsonObject("profile").getAsJsonArray("tagSlots").add(12);assertThrows(Exception.class,()->MenuConfig.parse(root.toString()));
        for(int rows:new int[]{0,7,3}){var candidate=defaults();candidate.getAsJsonObject("profile").addProperty("rows",rows);assertThrows(Exception.class,()->MenuConfig.parse(candidate.toString()));}
    }
    @Test void unknownActionsFieldsAndWrongTypesAreRejected(){
        var root=defaults();root.getAsJsonObject("profile").getAsJsonObject("buttons").add("op",new JsonObject());String unknown=root.toString();assertThrows(Exception.class,()->MenuConfig.parse(unknown));
        root=defaults();root.addProperty("clanMenuEnabled","false");String bad=root.toString();assertThrows(Exception.class,()->MenuConfig.parse(bad));
        root=defaults();root.getAsJsonObject("profile").getAsJsonObject("buttons").getAsJsonObject("clear").addProperty("command","op @s");String commands=root.toString();assertThrows(Exception.class,()->MenuConfig.parse(commands));
    }
    @Test void unsafeTemplatesUnknownPlaceholdersAndAnimationsAreRejected()throws Exception{
        for(String title:List.of("<click:run_command:/op>x</click>","%menu:unknown%","%animation:gold_wave%")){
            var root=defaults();root.getAsJsonObject("profile").addProperty("title",title);var parsed=MenuConfig.parse(root.toString());assertThrows(Exception.class,()->new MenuPlan(parsed));
        }
    }
    @Test void configuredMarkupIsRenderedButPlayerValuesRemainLiteral()throws Exception{
        var root=defaults();root.getAsJsonObject("profile").addProperty("title","<red>%menu:player%</red>");var plan=new MenuPlan(MenuConfig.parse(root.toString()));
        var rendered=plan.text(plan.config().profile().title(),Map.of("menu:player",Text.literal("<click:/op>literal</click>")));
        assertEquals("<click:/op>literal</click>",rendered.getString());assertNull(rendered.getStyle().getClickEvent());
    }
}
