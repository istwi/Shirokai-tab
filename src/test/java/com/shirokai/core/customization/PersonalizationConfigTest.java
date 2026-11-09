package com.shirokai.core.customization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class PersonalizationConfigTest {
    @TempDir Path directory;
    @Test void existingPolicyIsPreservedAndAllSurfacesAreRequired()throws Exception{
        Path file=directory.resolve("personalization.json");
        var defaults=PersonalizationConfig.load(file);assertEquals(5,defaults.surfaces().size());
        String policy=Files.readString(file).replace("\"animated\": true","\"animated\": false");
        Files.writeString(file,policy);assertTrue(PersonalizationConfig.load(file).surfaces().values().stream().noneMatch(PersonalizationConfig.Rule::animated));
        assertEquals(policy,Files.readString(file));
        Files.writeString(file,"{\"schemaVersion\":1,\"surfaces\":{}}");assertThrows(IOException.class,()->PersonalizationConfig.load(file));
    }
    @Test void unknownAreasAndInvalidPermissionAreRejected()throws Exception{
        Path file=directory.resolve("bad.json");PersonalizationConfig.load(file);String original=Files.readString(file);
        for(String invalid:new String[]{original.replace("\"CHAT\"","\"UNKNOWN\""),original.replace("\"permission\": \"\"","\"permission\": \"space forbidden\""),original.replace("\"enabled\": true","\"enabled\": \"true\"")}){
            Files.writeString(file,invalid);assertThrows(IOException.class,()->PersonalizationConfig.load(file));
        }
    }
}
