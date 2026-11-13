package com.shirokai.core.chat;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatConfigAndGateTest {
    @TempDir Path directory;
    @Test void strictConfigAndCustomFilesArePreserved()throws Exception{
        Path file=directory.resolve("chat.json");assertEquals(ChatConfig.defaults(),ChatConfig.load(file));
        for(String json:new String[]{"null","[]","{schemaVersion:1}","{\"schemaVersion\":1,\"maximumLength\":\"10\"}","{\"schemaVersion\":1,\"unknown\":true}","{\"schemaVersion\":1,\"localRange\":0}"})assertThrows(Exception.class,()->ChatConfig.parse(json));
        Files.writeString(file,"broken");assertThrows(Exception.class,()->ChatConfig.load(file));assertEquals("broken",Files.readString(file));
    }
    @Test void cooldownDuplicateWindowAndMandatoryBoundsApply()throws Exception{
        var gate=new SpamGate();var config=ChatConfig.defaults();gate.admit("Hello",config,100000,false,false);
        assertThrows(IllegalStateException.class,()->gate.admit("Different",config,100001,false,false));
        assertThrows(IllegalStateException.class,()->gate.admit("  HELLO ",config,102000,false,false));
        gate.admit("New",config,102000,false,false);gate.admit("Hello",config,131000,false,false);
        assertThrows(IllegalArgumentException.class,()->gate.admit("line\nbreak",config,132000,true,true));
        assertThrows(IllegalArgumentException.class,()->gate.admit("§aadmin",config,132000,true,true));
    }
    @Test void filterUsesLiteralNormalizedWordsAndBypassIsExplicit()throws Exception{
        var json=new Gson().toJsonTree(ChatConfig.defaults()).getAsJsonObject();var words=new JsonArray();words.add("blocked");json.add("blockedWords",words);
        var config=ChatConfig.parse(json.toString());var gate=new SpamGate();
        assertThrows(IllegalStateException.class,()->gate.admit("ＢＬＯＣＫＥＤ",config,1,true,false));
        assertThrows(IllegalStateException.class,()->gate.admit("blócked",config,1,true,false));
        assertThrows(IllegalStateException.class,()->gate.admit("blo\u200bcked",config,1,true,false));
        assertThrows(IllegalStateException.class,()->gate.admit("bl&ao&bcked",config,1,true,false));
        gate.admit("blocked",config,2,true,true);
    }
}
