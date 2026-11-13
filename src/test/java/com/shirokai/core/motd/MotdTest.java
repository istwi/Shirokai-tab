package com.shirokai.core.motd;

import com.shirokai.core.styles.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MotdTest {
    @TempDir Path directory;
    private StyleEngine styles(){return new StyleEngine(StyleConfig.defaults());}
    @Test void framesHaveColorsLiveValuesAndLiteralSubstitution(){
        var plan=new MotdPlan(new MotdConfig(1,true,20,List.of(List.of("<gold>%server:online%/%server:max%</gold>","%server:version%"),List.of("<aqua>Segundo</aqua>"))),styles());
        var first=plan.render(0,2,30,"<red>literal");
        assertEquals("2/30\n<red>literal",first.getString());
        assertEquals("Segundo",plan.render(20,3,30,"1.21.1").getString());
        assertEquals("4/30\n1.21.1",plan.render(40,4,30,"1.21.1").getString());
        assertEquals("2/30\n<red>literal",first.getString());
        assertThrows(Exception.class,()->new MotdPlan(new MotdConfig(1,true,20,List.of(List.of("%shirokai:cash%"))),styles()));
    }
    @Test void loadPreservesFilesAndRejectsInvalidCandidate()throws Exception{
        var file=directory.resolve("motd.json");assertEquals(MotdConfig.defaults(),MotdConfig.load(file));
        String original=Files.readString(file);assertEquals(MotdConfig.load(file),MotdConfig.load(file));assertEquals(original,Files.readString(file));
        for(String invalid:List.of("{}",original.replace("\"enabled\": true","\"enabled\": \"true\""),original.replace("\"frameTicks\": 60","\"frameTicks\": 0"),original.replace("\"schemaVersion\": 1","\"schemaVersion\": 2")))assertThrows(Exception.class,()->MotdConfig.parse(invalid));
        Files.writeString(file,"broken");assertThrows(Exception.class,()->MotdConfig.load(file));assertEquals("broken",Files.readString(file));
    }
}
