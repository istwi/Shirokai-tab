package com.shirokai.holograms;

import com.shirokai.holograms.Definitions.*;
import com.google.gson.*;
import net.minecraft.text.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HologramsTest {
    @TempDir Path directory;
    private Hologram sample(){return Hologram.create("spawn","minecraft:overworld",0,66,0);}
    private HologramPlan plan(Hologram h){return new HologramPlan(new ConfigStore.Snapshot(Settings.defaults(),Animations.defaults(),new Holograms(1,List.of(h))));}
    @Test void defaultsRoundTripAndCustomFilesPreserved()throws Exception{
        var store=new ConfigStore(directory);var first=store.load();assertEquals(first,store.load());
        var json=ConfigStore.JSON.toJson(Settings.defaults()).replace("\"viewDistance\": 48","\"viewDistance\": 32");
        Files.writeString(directory.resolve("config.json"),json);assertEquals(32,store.load().settings().viewDistance());assertEquals(json,Files.readString(directory.resolve("config.json")));
    }
    @Test void atomicPersistenceRestoresFullDefinitions()throws Exception{
        var store=new ConfigStore(directory);store.load();var holograms=new Holograms(1,List.of(sample()));store.save(holograms);assertEquals(holograms,new ConfigStore(directory).load().holograms());
        try(var files=Files.list(directory)){assertEquals(3,files.count());}
    }
    @Test void coercionAndUnknownFieldsRejected()throws Exception{
        String json=ConfigStore.JSON.toJson(Settings.defaults());
        assertThrows(IllegalArgumentException.class,()->ConfigStore.parse(json.replace("\"enabled\": true","\"enabled\": \"true\""),Settings.class));
        assertThrows(IllegalArgumentException.class,()->ConfigStore.parse(json.replace("\"schemaVersion\": 1","\"schemaVersion\": 1, \"surprise\": 2"),Settings.class));
        assertThrows(IllegalArgumentException.class,()->ConfigStore.parse(json.replace("\"viewDistance\": 48","\"viewDistance\": 2.5"),Settings.class));
    }
    @Test void invalidWorldCoordinatesAndDuplicatesRejected(){
        assertThrows(IllegalArgumentException.class,()->sample().moved("bad world",0,0,0));
        assertThrows(IllegalArgumentException.class,()->sample().moved("minecraft:overworld",Double.NaN,0,0));
        assertThrows(IllegalArgumentException.class,()->new Holograms(1,List.of(sample(),sample())));
        assertThrows(IllegalArgumentException.class,()->sample().scaled(0));
    }
    @Test void lineLimitsAndInvalidAnimationAreRejected(){
        assertThrows(IllegalArgumentException.class,()->sample().withLines(List.of()));
        assertThrows(IllegalArgumentException.class,()->new Line("a\nb",null,null));
        assertThrows(IllegalArgumentException.class,()->plan(sample().withLines(List.of(new Line("text",null,"missing")))));
        assertThrows(IllegalArgumentException.class,()->new Motion(Animation.BOUNCE,Double.NaN,80,2,0,20,160));
    }
    @Test void unsafeMarkupAndInvalidColorsRejected(){
        for(String input:List.of("<click:run_command:/op>x</click>","<#oops>x</#oops>","<bold>x"))
            assertThrows(IllegalArgumentException.class,()->plan(sample().withLines(List.of(new Line(input,null,null)))));
    }
    @Test void placeholderOutputIsLiteralNotReparsed(){
        var plan=plan(sample().withLines(List.of(new Line("<gold><bold>%player:name%</bold></gold>",null,null))));
        Text rendered=plan.render(plan.entries().get("spawn"),key->"<red>&l%server:online%").getFirst();
        assertEquals("<red>&l%server:online%",rendered.getString());
        rendered.visit((style,text)->{assertTrue(style.isBold());assertEquals(0xffaa00,style.getColor().getRgb());return Optional.empty();},Style.EMPTY);
    }
    @Test void excessivePlaceholderExpansionUsesLiteralFallback(){
        var plan=plan(sample().withLines(List.of(new Line("%player:name%".repeat(10),null,null))));
        assertEquals("N/D",plan.render(plan.entries().get("spawn"),key->"x".repeat(512)).getFirst().getString());
    }
    @Test void exampleFilesValidateTogether()throws Exception{
        var settings=ConfigStore.parse(Files.readString(Path.of("examples/config.json")),Settings.class);
        var animations=ConfigStore.parse(Files.readString(Path.of("examples/animations.json")),Animations.class);
        var holograms=ConfigStore.parse(Files.readString(Path.of("examples/holograms.json")),Holograms.class);
        assertEquals(3,new HologramPlan(new ConfigStore.Snapshot(settings,animations,holograms)).entries().get("spawn").lines().size());
    }
    @Test void hexGradientAndInheritedStylesWork(){
        var plan=plan(sample().withLines(List.of(new Line("<gradient:#ff0000:#0000ff><bold><italic><underlined>AB</underlined></italic></bold></gradient>",null,null))));
        var colors=new ArrayList<Integer>();plan.render(plan.entries().get("spawn"),k->"").getFirst().visit((style,text)->{if(!text.isEmpty()){colors.add(style.getColor().getRgb());assertTrue(style.isBold()&&style.isItalic()&&style.isUnderlined());}return Optional.empty();},Style.EMPTY);
        assertEquals(List.of(0xff0000,0x0000ff),colors);
    }
    @Test void legacyColorsBoldAndResetWork(){
        var plan=plan(sample().withLines(List.of(new Line("&6&lGold&r plain",null,null))));
        var text=plan.render(plan.entries().get("spawn"),k->"").getFirst();assertEquals("Gold plain",text.getString());
        assertTrue(FontWidths.width(text)>FontWidths.width(Text.literal("Gold plain")));
        assertThrows(IllegalArgumentException.class,()->plan(sample().withLines(List.of(new Line("<bold>&6x</bold>",null,null)))));
    }
    @Test void pixelWidthsDifferAndIncludeBoldAccentsAndUnicode(){
        assertTrue(FontWidths.width(Text.literal("WWW"))>FontWidths.width(Text.literal("iii")));
        assertEquals(6,FontWidths.width(Text.literal("é")));assertEquals(9,FontWidths.width(Text.literal("界")));
        assertEquals(18,FontWidths.width(Text.literal("WWW")));assertEquals(21,FontWidths.width(Text.literal("WWW").styled(s->s.withBold(true))));
    }
    @Test void alignmentUsesPixelEdgesNotSpaces(){
        double wide=FontWidths.width(Text.literal("WWW")),narrow=FontWidths.width(Text.literal("iii"));
        assertEquals(-wide/2,FontWidths.offset(Alignment.LEFT,narrow,wide)-narrow/2);
        assertEquals(wide/2,FontWidths.offset(Alignment.RIGHT,narrow,wide)+narrow/2);
        assertEquals(0,FontWidths.offset(Alignment.CENTER,narrow,wide));
    }
    private Motion motion(Animation type){return new Motion(type,1,100,2,1,20,100);}
    @Test void marqueesMoveInOppositeDirections(){
        assertTrue(AnimationFrame.at(motion(Animation.MARQUEE_LEFT),0,40).x()>AnimationFrame.at(motion(Animation.MARQUEE_LEFT),100,40).x());
        assertTrue(AnimationFrame.at(motion(Animation.MARQUEE_RIGHT),0,40).x()<AnimationFrame.at(motion(Animation.MARQUEE_RIGHT),100,40).x());
    }
    @Test void slidesEndAtAnchorAndPause(){
        for(var type:List.of(Animation.SLIDE_LEFT,Animation.SLIDE_RIGHT)){assertEquals(0,AnimationFrame.at(motion(type),100,10).x());assertEquals(0,AnimationFrame.at(motion(type),110,10).x());}
    }
    @Test void fadesHaveTrueInvisibleEndpoints(){
        assertEquals("",AnimationFrame.at(motion(Animation.FADE_IN),0,1).text(Text.literal("hello")).getString());
        assertEquals(255,AnimationFrame.at(motion(Animation.FADE_IN),100,1).opacity());
        assertEquals("",AnimationFrame.at(motion(Animation.FADE_OUT),100,1).text(Text.literal("hello")).getString());
    }
    @Test void typewriterPreservesGraphemesAndBold(){
        Text source=Text.literal("A\u0301😀B").styled(s->s.withBold(true));
        var text=new AnimationFrame(0,0,255,.67).text(source);assertEquals("A\u0301😀",text.getString());
        text.visit((style,content)->{if(!content.isEmpty())assertTrue(style.isBold());return Optional.empty();},Style.EMPTY);
    }
    @Test void bounceAndPingPongStayBounded(){
        for(int t=0;t<=100;t++){var b=AnimationFrame.at(motion(Animation.BOUNCE),t,20);assertTrue(b.y()>=0&&b.y()<=10.00001);var p=AnimationFrame.at(motion(Animation.PING_PONG),t,20);assertTrue(Math.abs(p.x())<=50.00001);}
    }
    @Test void speedRepetitionsAndInfiniteLoopsAreDeterministic(){
        var fast=new Motion(Animation.TYPEWRITER,2,100,2,0,0,100);
        assertEquals(.5,AnimationFrame.at(fast,25,20).fraction());assertEquals(0,AnimationFrame.at(fast,50,20).fraction());
        assertEquals(1,AnimationFrame.at(motion(Animation.TYPEWRITER),240,20).fraction());
        assertEquals(AnimationFrame.at(fast,25,20),AnimationFrame.at(fast,25,20));
    }
}
