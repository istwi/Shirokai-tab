package com.shirokai.core.customization;

import com.shirokai.core.presentation.Surface;
import com.shirokai.core.styles.*;
import net.minecraft.text.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PresetFramesTest {
    private PresetCatalog catalog(){return new PresetCatalog("basic",Map.of(
        "basic",new PresetCatalog.Definition("",PresetCatalog.SURFACES,"<white>%visual:value%</white>"),
        "pulse",new PresetCatalog.Definition("style.pulse",PresetCatalog.SURFACES,"<white>%visual:value%</white>",List.of("<red>%visual:value%</red>","<blue>%visual:value%</blue>"),5)),new StyleEngine(StyleConfig.defaults()));}
    private boolean color(Text text,int rgb){boolean[] seen={false};text.visit((style,value)->{if(!value.isEmpty()&&style.getColor()!=null&&style.getColor().getRgb()==rgb)seen[0]=true;return Optional.empty();},Style.EMPTY);return seen[0];}
    @Test void framesRespectTickAndDisabledAnimationUsesStaticTemplate(){
        var catalog=catalog();var selected=catalog.resolve("pulse",Surface.TITLE,node->true);
        assertTrue(color(catalog.render(selected,"name",0,true),0xff5555));assertTrue(color(catalog.render(selected,"name",5,true),0x5555ff));
        assertTrue(color(catalog.render(selected,"name",5,false),0xffffff));assertEquals("name",catalog.render(selected,"name",5,true).getString());
    }
    @Test void animationPermissionAndFallbackKeepTheSavedId(){
        var catalog=catalog();var selected=catalog.resolve("pulse",Surface.CLAN_TAG,node->!node.equals("shirokaicore.clan.animation"));
        assertEquals(PresetCatalog.Status.UNAVAILABLE,selected.status());assertEquals("pulse",selected.saved());assertTrue(color(catalog.render(selected,"tag",5,true),0xffffff));
    }
    @Test void malformedFramesAndAnimatedFallbackAreRejected(){
        var basic=new PresetCatalog.Definition("",PresetCatalog.SURFACES,"%visual:value%");
        for(String frame:List.of("%visual:value%%visual:value%","%animation:gold_wave%","<click:/op>%visual:value%</click>"))assertThrows(Exception.class,()->new PresetCatalog("basic",Map.of("basic",basic,"bad",new PresetCatalog.Definition("",PresetCatalog.SURFACES,"%visual:value%",List.of(frame),5)),new StyleEngine(StyleConfig.defaults())));
        assertThrows(Exception.class,()->new PresetCatalog("pulse",catalog().presets().entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().definition())),new StyleEngine(StyleConfig.defaults())));
    }
}
