package com.shirokai.core.chat;

import com.google.gson.*;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatAppearanceTest {
    @TempDir Path directory;
    @Test void exactOldDefaultsUpgradeInMemoryWithoutOverwritingCustomFormats()throws Exception{
        var file=directory.resolve("chat.json");var root=new Gson().toJsonTree(ChatConfig.defaults()).getAsJsonObject();
        root.getAsJsonObject("nameFormats").addProperty("GLOBAL","[GLOBAL] {tags}{name}");
        root.getAsJsonObject("nameFormats").addProperty("LOCAL","[LOCAL] {tags}{name}");
        Files.writeString(file,root.toString());String original=Files.readString(file);
        var config=ChatConfig.load(file);
        assertEquals("[G] {tags}{name}",config.nameFormats().get(Channel.GLOBAL));assertEquals("[L] {tags}{name}",config.nameFormats().get(Channel.LOCAL));
        assertEquals(original,Files.readString(file));
        root.getAsJsonObject("nameFormats").addProperty("GLOBAL","Custom {name}");
        assertEquals("Custom {name}",ChatConfig.parse(root.toString()).nameFormats().get(Channel.GLOBAL));
    }
    @Test void appearanceDefaultsAndStrictConfigPreserveExistingFiles()throws Exception{
        var file=directory.resolve("chat-appearance.json");var config=ChatAppearanceConfig.load(file);String original=Files.readString(file);
        assertEquals(Formatting.GRAY,config.color(Channel.GLOBAL));assertEquals(Formatting.YELLOW,config.color(Channel.LOCAL));
        assertEquals(config,ChatAppearanceConfig.load(file));assertEquals(original,Files.readString(file));
        for(String invalid:List.of(original.replace("\"enabled\": true","\"enabled\": \"true\""),original.replace("\"gray\"","\"rainbow\""),original.replace("\"schemaVersion\": 1","\"schemaVersion\": 2")))
            assertThrows(Exception.class,()->ChatAppearanceConfig.parse(invalid));
        Files.writeString(file,"broken");assertThrows(Exception.class,()->ChatAppearanceConfig.load(file));assertEquals("broken",Files.readString(file));
    }
    @Test void playerHoverOptionIsOptionalStrictAndPreservesLegacyFile()throws Exception{
        var root=new Gson().toJsonTree(ChatAppearanceConfig.defaults()).getAsJsonObject();root.remove("playerHoverInfo");
        var file=directory.resolve("legacy-appearance.json");String legacy=root.toString();Files.writeString(file,legacy);
        assertFalse(ChatAppearanceConfig.load(file).playerHoverInfo());assertEquals(legacy,Files.readString(file));
        root.addProperty("playerHoverInfo",true);assertTrue(ChatAppearanceConfig.parse(root.toString()).playerHoverInfo());
        root.addProperty("playerHoverInfo","false");assertThrows(Exception.class,()->ChatAppearanceConfig.parse(root.toString()));
    }
    @Test void summaryDefaultsOnForLegacyFilesButExplicitFalseIsPreserved()throws Exception{
        var root=new Gson().toJsonTree(ChatAppearanceConfig.defaults()).getAsJsonObject();root.remove("playerSummary");
        Path file=directory.resolve("summary.json");String legacy=root.toString();Files.writeString(file,legacy);
        assertTrue(ChatAppearanceConfig.load(file).playerSummary());assertEquals(legacy,Files.readString(file));
        root.addProperty("playerSummary",false);assertFalse(ChatAppearanceConfig.parse(root.toString()).playerSummary());
        root.addProperty("playerSummary","true");assertThrows(Exception.class,()->ChatAppearanceConfig.parse(root.toString()));
    }
    @Test void colorsResetAndEscapingNeverInterpretMarkupOrActions(){
        var text=LegacyChatColors.render("Base &aGreen &7Gray &rDefault &&a <click:/op>literal</click>",Formatting.YELLOW);
        assertEquals("Base Green Gray Default &a <click:/op>literal</click>",text.getString());
        var colors=new ArrayList<Integer>();
        text.visit((style,part)->{assertNull(style.getClickEvent());assertNull(style.getHoverEvent());assertNull(style.getInsertion());if(!part.isEmpty())colors.add(style.getColor().getRgb());return Optional.empty();},Style.EMPTY);
        assertTrue(colors.contains(0x55ff55));assertTrue(colors.contains(0xaaaaaa));assertEquals(0xffff55,colors.getLast());
        assertEquals("&kHidden &lBold &xHex",LegacyChatColors.plain("&kHidden &lBold &xHex"));
        assertEquals("😀",LegacyChatColors.plain("&A😀&r"));
    }
    @Test void colorCodesCannotBypassFilterDuplicateOrEmptyMessageChecks()throws Exception{
        var root=new Gson().toJsonTree(ChatConfig.defaults()).getAsJsonObject();var words=new JsonArray();words.add("blocked");root.add("blockedWords",words);
        var config=ChatConfig.parse(root.toString());var gate=new SpamGate();
        assertThrows(IllegalStateException.class,()->gate.admit("blo&7cked",config,100000,true,false));
        gate.admit("&aHello",config,100000,false,false);
        assertThrows(IllegalStateException.class,()->gate.admit("&7HELLO",config,102000,false,false));
        assertThrows(IllegalArgumentException.class,()->gate.admit("&7&r",config,103000,true,true));
        assertThrows(IllegalArgumentException.class,()->gate.admit("§aNot allowed",config,103000,true,true));
    }
}
