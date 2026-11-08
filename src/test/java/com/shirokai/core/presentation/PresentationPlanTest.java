package com.shirokai.core.presentation;

import com.google.gson.*;
import com.shirokai.core.chat.*;
import com.shirokai.core.styles.*;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.tab.TabConfig.Condition;
import net.minecraft.text.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PresentationPlanTest {
    @TempDir Path directory;
    @Test void strictFilesRemainUnchangedAndAllCandidatesCompile()throws Exception{
        var file=directory.resolve("presentation.json");assertEquals(PresentationConfig.defaults(),PresentationConfig.load(file));String source=Files.readString(file);
        assertThrows(Exception.class,()->PresentationConfig.parse(source.replace("\"chatEnabled\": true","\"chatEnabled\": \"true\"")));
        for(String bad:List.of("<click:run_command:/op>%visual:value%</click>","%animation:gold_wave%","%visual:other%","%visual:value%%visual:value%")){
            var config=new Gson().toJsonTree(PresentationConfig.defaults()).getAsJsonObject();config.getAsJsonObject("templates").addProperty("TITLE",bad);
            assertThrows(Exception.class,()->new PresentationPlan(PresentationConfig.parse(config.toString()),new StyleEngine(StyleConfig.defaults()),ChatConfig.defaults()));
        }
        Files.writeString(file,"broken");assertThrows(Exception.class,()->PresentationConfig.load(file));assertEquals("broken",Files.readString(file));
    }
    @Test void rulePermissionRevocationFallsBackWithoutMutatingConfiguredStyle(){
        var base=PresentationConfig.defaults();var condition=new Condition("visual.vip","vip",Set.of("minecraft:overworld"));
        var rule=new PresentationConfig.Rule("vip",100,condition,Map.of(Surface.TITLE,"<gradient:#ff0000:#0000ff>%visual:value%</gradient>"),Map.of(Channel.GLOBAL,"<gold>{tags}{name}</gold>"));
        var config=new PresentationConfig(1,true,true,true,"",base.templates(),List.of(rule));var styles=new StyleEngine(StyleConfig.defaults());var plan=new PresentationPlan(config,styles,ChatConfig.defaults());
        var meta=new PermissionService.Metadata("VIP","","",0);
        assertTrue(plan.template(Surface.TITLE,meta,"minecraft:overworld",node->true).source().contains("gradient"));
        assertEquals(base.templates().get(Surface.TITLE),plan.template(Surface.TITLE,meta,"minecraft:overworld",node->false).source());
        assertEquals(base.templates().get(Surface.TITLE),plan.template(Surface.TITLE,meta,"minecraft:the_end",node->true).source());
        assertEquals(config,plan.config());
    }
    @Test void duplicateTagsUnknownBodyAndAnimatedChatAreRejected(){
        var defaults=PresentationConfig.defaults();
        for(String bad:List.of("{tags}{tags}{name}","%chat:body%","%animation:gold_wave%")){
            var rule=new PresentationConfig.Rule("invalid",1,Condition.any(),Map.of(),Map.of(Channel.GLOBAL,bad));
            assertThrows(IllegalArgumentException.class,()->new PresentationPlan(new PresentationConfig(1,true,true,true,"",defaults.templates(),List.of(rule)),new StyleEngine(StyleConfig.defaults()),ChatConfig.defaults()));
        }
    }
    @Test void richSlotsKeepColorsAndUnicodeWithoutParsingOrCopyingActions(){
        var engine=new StyleEngine(StyleConfig.defaults());var template=engine.compile("<bold>%test:value%</bold>");
        Text input=Text.literal("<op>😀</op>").styled(s->s.withColor(0x123456).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/op me")));
        Text result=engine.renderRich(template,key->input,0,false);assertEquals(input.getString(),result.getString());
        result.visit((style,part)->{assertNull(style.getClickEvent());assertNull(style.getHoverEvent());assertTrue(style.isBold());assertEquals(0x123456,style.getColor().getRgb());return Optional.empty();},Style.EMPTY);
        assertNotNull(input.getStyle().getClickEvent());
        var plain=engine.render(engine.compile("<red>%test:value%</red>"),key->"x",0,false);
        engine.renderRich(template,key->plain,0,false);assertEquals("x",plain.getString());
    }
    @Test void fixedTitlesAreIndependentStrictAndSurviveReload()throws Exception{
        var json=new Gson().toJsonTree(PresentationConfig.defaults()).getAsJsonObject();
        // Existing schema1 files remain valid without the new optional map.
        json.remove("titleTemplates");assertTrue(PresentationConfig.parse(json.toString()).titleTemplates().isEmpty());
        var fixed=new JsonObject();fixed.addProperty("owner","<gradient:#ff3333:#ffcc33>%visual:value%</gradient>");
        fixed.addProperty("admin","<gradient:#3355ff:#33ffff>%visual:value%</gradient>");json.add("titleTemplates",fixed);
        var file=directory.resolve("presentation.json");Files.writeString(file,json.toString());
        var config=PresentationConfig.load(file);var styles=new StyleEngine(StyleConfig.defaults());
        var plan=new PresentationPlan(config,styles,ChatConfig.defaults());
        assertNotEquals(plan.titleTemplate("owner").orElseThrow().source(),plan.titleTemplate("admin").orElseThrow().source());
        assertTrue(plan.titleTemplate("traveler").isEmpty());
        assertEquals(config,PresentationConfig.load(file));
        for(String bad:List.of("%visual:other%","%animation:gold_wave%","<click:run_command:/op>%visual:value%</click>")){
            fixed.addProperty("owner",bad);
            assertThrows(Exception.class,()->new PresentationPlan(PresentationConfig.parse(json.toString()),styles,ChatConfig.defaults()));
        }
        fixed.addProperty("owner",true);assertThrows(Exception.class,()->PresentationConfig.parse(json.toString()));
    }

}
