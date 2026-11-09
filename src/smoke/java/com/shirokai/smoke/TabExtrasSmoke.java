package com.shirokai.smoke;

import com.shirokai.core.tab.*;
import com.shirokai.core.permissions.PermissionService;
import com.shirokai.core.placeholders.PlaceholderService;
import com.shirokai.core.styles.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.boss.*;
import net.minecraft.network.packet.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.scoreboard.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.*;
import static net.minecraft.server.command.CommandManager.literal;

/** Real Fabric packets and global team API, synthetic connections only. */
public final class TabExtrasSmoke implements ModInitializer {
    public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(
        literal("shirokai-tab-extras-smoke").requires(s->s.hasPermissionLevel(4)).executes(c->{
            try{test(c.getSource().getServer());testRanks(c.getSource().getServer());c.getSource().sendFeedback(()->Text.literal("TAB_EXTRAS_SMOKE_PASS"),false);}
            catch(Throwable error){org.slf4j.LoggerFactory.getLogger("tab-extras-smoke").error("TAB_EXTRAS_SMOKE_FAIL",error);c.getSource().sendError(Text.literal("TAB_EXTRAS_SMOKE_FAIL"));}
            return 1;
        })));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private record BossEvent(String kind,UUID id,Text name,float progress){}
    private static List<BossEvent> bosses(List<Packet<?>> packets){
        var events=new ArrayList<BossEvent>();for(var packet:packets)if(packet instanceof BossBarS2CPacket boss)boss.accept(new BossBarS2CPacket.Consumer(){
            public void add(UUID id,Text title,float progress,BossBar.Color color,BossBar.Style style,boolean sky,boolean music,boolean fog){events.add(new BossEvent("add",id,title,progress));}
            public void remove(UUID id){events.add(new BossEvent("remove",id,null,0));}
            public void updateProgress(UUID id,float progress){events.add(new BossEvent("progress",id,null,progress));}
            public void updateName(UUID id,Text text){events.add(new BossEvent("name",id,text,0));}
            public void updateStyle(UUID id,BossBar.Color color,BossBar.Style style){events.add(new BossEvent("style",id,Text.literal(color+":"+style),0));}
        });return events;
    }
    private static List<PlayerListS2CPacket.Entry> listed(List<Packet<?>> packets){
        return packets.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast)
            .filter(p->p.getActions().equals(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_LISTED))).flatMap(p->p.getEntries().stream()).toList();
    }
    private static PlayerListS2CPacket.Entry listing(ServerPlayerEntity player,boolean value){
        var base=new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_LISTED,player).getEntries().getFirst();
        return new PlayerListS2CPacket.Entry(base.profileId(),base.profile(),value,base.latency(),base.gameMode(),base.displayName(),base.chatSession());
    }
    private static String below(List<Packet<?>> packets){
        return packets.stream().filter(ScoreboardDisplayS2CPacket.class::isInstance).map(ScoreboardDisplayS2CPacket.class::cast)
            .filter(p->p.getSlot()==ScoreboardDisplaySlot.BELOW_NAME).reduce((a,b)->b).orElseThrow().getName();
    }
    private static TabExtrasConfig config(boolean listed,boolean tags,boolean below,boolean boss){
        var base=TabExtrasConfig.defaults();return new TabExtrasConfig(1,
            new TabExtrasConfig.Listed(listed,base.listed().hiddenPermission(),base.listed().seeHiddenPermission(),false),
            new TabExtrasConfig.Nametags(tags,"<red>%shirokai:player% </red>","","ALWAYS"),
            new TabExtrasConfig.BelowName(below,"<gray>Points</gray>","%test:score%"),
            new TabExtrasConfig.Bossbar(boss,"Boss %shirokai:player%","%test:progress%","YELLOW","PROGRESS",TabConfig.Condition.any()));
    }
    private static void testRanks(net.minecraft.server.MinecraftServer server){
        var packets=new ArrayList<Packet<?>>();var a=ProfileSmoke.player(server,"RankAlice",packets::add);
        var players=new ArrayList<>(List.of(a));
        var groups=new HashMap<UUID,String>();groups.put(a.getUuid(),"owner");
        var permissions=new PermissionService(){public boolean allowed(UUID id,String node){return true;}public String provider(){return "test";}public Metadata metadata(UUID id){String group=groups.get(id);return new Metadata(group,"","",group.equals("owner")?100:group.equals("admin")?90:0);}};
        var titles=new com.shirokai.core.titles.TitleService(null,com.shirokai.core.titles.TitleCatalog.defaults(),permissions::allowed,server,id->true,java.time.Clock.systemUTC(),(m,e)->{throw new AssertionError(m,e);});
        titles.bindRanks(permissions::metadata);var engine=new StyleEngine(StyleConfig.defaults());
        var tab=new TabRuntime(server,new TabPlan(TabConfig.defaults(),engine),permissions,null,titles,null,null,()->"N/D",org.slf4j.LoggerFactory.getLogger("rank-smoke"),()->players);
        tab.configureExtras(new TabExtrasPlan(TabExtrasConfig.defaults(),engine));
        try{
            tab.update();
            var team=server.getScoreboard().getScoreHolderTeam("RankAlice");
            check(team!=null&&team.getPrefix().getString().equals("[OWNER] "),"Automatic primary rank missing from nametag");
            check(packets.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).flatMap(p->p.getEntries().stream()).anyMatch(e->e.displayName()!=null&&e.displayName().getString().equals("[OWNER] RankAlice")),"Automatic rank missing from TAB");
            check(titles.chatDefinitions(a.getUuid()).getFirst().display().equals("[OWNER]"),"Automatic chat rank missing before profile load");
            packets.clear();groups.put(a.getUuid(),"admin");tab.update();
            check(server.getScoreboard().getScoreHolderTeam("RankAlice").getPrefix().getString().equals("[ADMIN] "),"Rank change did not update nametag");
            check(packets.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).flatMap(p->p.getEntries().stream()).anyMatch(e->e.displayName()!=null&&e.displayName().getString().equals("[ADMIN] RankAlice")),"Rank change did not update TAB");
            var b=ProfileSmoke.player(server,"LateBob");groups.put(b.getUuid(),"default");players.add(b);tab.update();
            check(server.getScoreboard().getScoreHolderTeam("LateBob").getPrefix().getString().equals("[DEFAULT] "),"Late default player has incorrect nametag");
            packets.clear();groups.put(b.getUuid(),"admin");tab.update();
            check(server.getScoreboard().getScoreHolderTeam("LateBob").getPrefix().getString().equals("[ADMIN] "),"Late player default-to-admin rank remained stale");
            check(packets.stream().filter(PlayerListS2CPacket.class::isInstance).map(PlayerListS2CPacket.class::cast).flatMap(p->p.getEntries().stream()).anyMatch(e->e.displayName()!=null&&e.displayName().getString().equals("[ADMIN] LateBob")),"Late player TAB rank remained stale");
            groups.put(b.getUuid(),"owner");tab.update();
            check(server.getScoreboard().getScoreHolderTeam("LateBob").getPrefix().getString().equals("[OWNER] "),"Rank reorder left stale owner nametag");
            check(server.getScoreboard().getScoreHolderTeam("RankAlice").getPrefix().getString().equals("[ADMIN] "),"Rank reorder changed another player nametag");
        }finally{tab.close();}
        check(server.getScoreboard().getScoreHolderTeam("RankAlice")==null,"Rank nametag leaked on stop");
    }
    private static void test(net.minecraft.server.MinecraftServer server){
        var pa=new ArrayList<Packet<?>>();var pb=new ArrayList<Packet<?>>();
        var a=ProfileSmoke.player(server,"ExtraAlice",pa::add);var b=ProfileSmoke.player(server,"ExtraBob",pb::add);var players=new ArrayList<>(List.of(a,b));
        var bossAllowed=new boolean[]{false};var see=new boolean[]{false};var hidden=new boolean[]{true};var progress=new String[]{"1"};var score=new String[]{"7"};
        var permissions=new PermissionService(){public boolean allowed(UUID id,String node){
            return node.equals("boss.allowed")&&bossAllowed[0]||node.equals("shirokaicore.tab.hidden")&&id.equals(a.getUuid())&&hidden[0]||node.equals("shirokaicore.tab.seehidden")&&id.equals(b.getUuid())&&see[0];
        }public String provider(){return "test";}};
        var placeholders=new PlaceholderService((key,error)->{});placeholders.register("test:progress",c->Optional.of(progress[0]));placeholders.register("test:score",c->Optional.of(score[0]));
        var engine=new StyleEngine(StyleConfig.defaults());var base=TabConfig.defaults();
        var tab=new TabRuntime(server,new TabPlan(new TabConfig(1,20,false,false,base.nameTemplate(),base.layouts(),base.nameRules()),engine),
            permissions,placeholders,null,null,null,()->"N/D",org.slf4j.LoggerFactory.getLogger("tab-extras-smoke"),()->players);
        tab.configureExtras(new TabExtrasPlan(config(true,true,true,true),engine));
        var worldBoard=server.getScoreboard();var foreignTeam=worldBoard.addTeam("foreign_"+UUID.randomUUID().toString().substring(0,8));foreignTeam.setPrefix(Text.literal("Foreign"));worldBoard.addScoreHolderToTeam(b.getGameProfile().getName(),foreignTeam);
        Team ownTeam=null;
        try{
            // Outbound vanilla roster and a foreign hidden entry, before the runtime's first update.
            a.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(players));b.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(players));
            a.networkHandler.sendPacket(TabExtrasPackets.listed(List.of(listing(b,false))));
            pa.clear();pb.clear();tab.update();
            check(listed(pb).stream().anyMatch(e->e.profileId().equals(a.getUuid())&&!e.listed()),"Hidden subject not unlisted for viewer");
            check(listed(pa).stream().noneMatch(e->e.profileId().equals(b.getUuid())&&e.listed()),"Foreign hidden entry revealed");
            check(listed(pa).stream().noneMatch(e->e.profileId().equals(a.getUuid())&&!e.listed()),"Viewer hid itself");
            check(pa.stream().noneMatch(PlayerRemoveS2CPacket.class::isInstance)&&pb.stream().noneMatch(PlayerRemoveS2CPacket.class::isInstance),"Roster was removed/recreated");
            String belowA=below(pa),belowB=below(pb);
            check(worldBoard.getNullableObjective(belowA)==null&&worldBoard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME)==null,"Private objective leaked into world");
            check(pb.stream().filter(ScoreboardScoreUpdateS2CPacket.class::isInstance).map(ScoreboardScoreUpdateS2CPacket.class::cast)
                .noneMatch(p->p.scoreHolderName().equals(a.getGameProfile().getName())),"Below name exposed hidden subject");
            UUID bossA=bosses(pa).stream().filter(e->e.kind().equals("add")).findFirst().orElseThrow().id();
            UUID bossB=bosses(pb).stream().filter(e->e.kind().equals("add")).findFirst().orElseThrow().id();
            check(!bossA.equals(bossB),"Personal bars shared identity");
            ownTeam=worldBoard.getScoreHolderTeam(a.getGameProfile().getName());
            check(ownTeam!=null&&ownTeam.getPrefix().getString().equals("ExtraAlice "),"Nametag prefix not applied");
            check(worldBoard.getScoreHolderTeam(b.getGameProfile().getName())==foreignTeam&&foreignTeam.getPrefix().getString().equals("Foreign"),"Foreign team changed");
            pa.clear();pb.clear();tab.update();check(pa.isEmpty()&&pb.isEmpty(),"Unchanged frame emitted packets");
            see[0]=true;tab.update();check(listed(pb).stream().anyMatch(e->e.profileId().equals(a.getUuid())&&e.listed()),"See-hidden permission did not restore baseline");
            check(pb.stream().filter(ScoreboardScoreUpdateS2CPacket.class::isInstance).map(ScoreboardScoreUpdateS2CPacket.class::cast).anyMatch(p->p.scoreHolderName().equals("ExtraAlice")&&p.score()==7),"Visible subject's real numeric value absent");
            // A later external listing decision revokes ownership until reconnection.
            b.networkHandler.sendPacket(TabExtrasPackets.listed(List.of(listing(a,false))));pa.clear();pb.clear();hidden[0]=false;tab.update();
            check(listed(pb).isEmpty(),"External listing update overwritten");
            b.networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(a.getUuid())));pb.clear();tab.update();check(listed(pb).isEmpty(),"Removed roster entry recreated");
            progress[0]="0.5";pa.clear();pb.clear();tab.update();
            check(bosses(pa).stream().anyMatch(e->e.kind().equals("progress")&&e.progress()==.5f)&&bosses(pa).stream().noneMatch(e->e.kind().equals("add")),"Boss progress did not update incrementally");
            score[0]="N/D";progress[0]="NaN";pa.clear();pb.clear();tab.update();
            check(bosses(pa).isEmpty()&&pa.stream().noneMatch(ScoreboardScoreUpdateS2CPacket.class::isInstance),"Invalid numeric frame invented values");
            score[0]="8";progress[0]="0.25";tab.update();check(bosses(pa).stream().anyMatch(e->e.progress()==.25f),"Valid data did not recover");
            var external=new ScoreboardObjective(new Scoreboard(),"external_below",ScoreboardCriterion.DUMMY,Text.literal("Foreign"),ScoreboardCriterion.RenderType.INTEGER,false,null);
            b.networkHandler.sendPacket(new BundleS2CPacket(List.of(new ScoreboardObjectiveUpdateS2CPacket(external,0),new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.BELOW_NAME,external))));
            pb.clear();tab.update();
            check(pb.stream().filter(ScoreboardObjectiveUpdateS2CPacket.class::isInstance).map(ScoreboardObjectiveUpdateS2CPacket.class::cast).noneMatch(p->p.getName().equals("external_below")),"External below-name objective altered");
            check(pb.stream().noneMatch(ScoreboardDisplayS2CPacket.class::isInstance),"Foreign BELOW_NAME replaced");
            b.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.BELOW_NAME,null));pb.clear();tab.update();String resumed=below(pb);check(!resumed.equals(belowB),"Below-name did not resume with fresh identity");
            var collision=new ScoreboardObjective(new Scoreboard(),resumed,ScoreboardCriterion.DUMMY,Text.literal("Replacement"),ScoreboardCriterion.RenderType.INTEGER,false,null);
            b.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(collision,2));
            com.shirokai.core.scoreboard.SidebarPackets.retain(b.networkHandler);com.shirokai.core.scoreboard.SidebarPackets.release(b.networkHandler);
            check(com.shirokai.core.scoreboard.SidebarPackets.state(b.networkHandler).names().contains(resumed),"One consumer erased shared ownership evidence");
            var foreignBoss=new BossBar(bossB,Text.literal("Foreign boss"),BossBar.Color.RED,BossBar.Style.PROGRESS){};
            b.networkHandler.sendPacket(BossBarS2CPacket.updateName(foreignBoss));pb.clear();tab.update();
            check(pb.stream().noneMatch(ScoreboardObjectiveUpdateS2CPacket.class::isInstance)&&bosses(pb).isEmpty(),"Exact foreign identity collision overwritten");
            ownTeam.setPrefix(Text.literal("External replacement"));worldBoard.addScoreHolderToTeam("OutsideFixture",ownTeam);tab.update();
            check(ownTeam.getPrefix().getString().equals("External replacement"),"Externally modified nametag overwritten");
            pb.clear();tab.close();
            check(pb.stream().filter(ScoreboardObjectiveUpdateS2CPacket.class::isInstance).map(ScoreboardObjectiveUpdateS2CPacket.class::cast).noneMatch(p->p.getName().equals(resumed))&&bosses(pb).isEmpty(),"Stop deleted foreign replacement");
            check(worldBoard.getScoreHolderTeam(b.getGameProfile().getName())==foreignTeam&&worldBoard.getTeam(ownTeam.getName())==ownTeam,"Stop removed external teams");
        }finally{
            tab.close();if(ownTeam!=null&&worldBoard.getTeam(ownTeam.getName())==ownTeam)worldBoard.removeTeam(ownTeam);
            if(worldBoard.getTeam(foreignTeam.getName())==foreignTeam)worldBoard.removeTeam(foreignTeam);
            com.shirokai.core.scoreboard.SidebarPackets.forget(a.networkHandler);com.shirokai.core.scoreboard.SidebarPackets.forget(b.networkHandler);
            TabExtrasPackets.forget(a.networkHandler);TabExtrasPackets.forget(b.networkHandler);
        }
        // Fresh viewer connection: normal ownership restores only the baseline it changed.
        var pc=new ArrayList<Packet<?>>();var c=ProfileSmoke.player(server,"ExtraViewer",pc::add);hidden[0]=true;
        c.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(List.of(a,c)));pc.clear();
        var direct=new TabExtrasRuntime(server,new TabExtrasPlan(config(true,false,false,false),engine),permissions,(key,s,v)->Text.literal(""),id->false,org.slf4j.LoggerFactory.getLogger("tab-extras-smoke"));
        direct.update(List.of(a,c),0);check(listed(pc).stream().anyMatch(e->e.profileId().equals(a.getUuid())&&!e.listed()),"Fresh viewer did not receive policy");
        pc.clear();direct.close();check(listed(pc).stream().anyMatch(e->e.profileId().equals(a.getUuid())&&e.listed()),"Owned listing baseline not restored");
        hidden[0]=false;var listedBase=config(true,false,false,false);
        var worldConfig=new TabExtrasConfig(1,new TabExtrasConfig.Listed(true,listedBase.listed().hiddenPermission(),listedBase.listed().seeHiddenPermission(),true),listedBase.nametags(),listedBase.belowName(),listedBase.bossbar());
        var worldRuntime=new TabExtrasRuntime(server,new TabExtrasPlan(worldConfig,engine),permissions,(key,s,v)->Text.literal(""),id->false,org.slf4j.LoggerFactory.getLogger("tab-extras-smoke"));
        try{
            a.setServerWorld(server.getWorld(net.minecraft.world.World.NETHER));pc.clear();worldRuntime.update(List.of(a,c),0);
            check(listed(pc).stream().anyMatch(e->e.profileId().equals(a.getUuid())&&!e.listed()),"Different world did not unlist subject");
            a.setServerWorld(server.getOverworld());pc.clear();worldRuntime.update(List.of(a,c),20);
            check(listed(pc).stream().anyMatch(e->e.profileId().equals(a.getUuid())&&e.listed()),"World return did not restore listing");
        }finally{a.setServerWorld(server.getOverworld());worldRuntime.close();}
        var barBase=config(false,false,false,true);
        var conditional=new TabExtrasConfig(1,barBase.listed(),barBase.nametags(),barBase.belowName(),new TabExtrasConfig.Bossbar(true,"Boss %shirokai:player%","%test:progress%","YELLOW","PROGRESS",new TabConfig.Condition("boss.allowed",null,Set.of("minecraft:overworld"))));
        var bossRuntime=new TabExtrasRuntime(server,new TabExtrasPlan(conditional,engine),permissions,(key,s,v)->Text.literal(key.equals("test:progress")?progress[0]:s.getGameProfile().getName()),id->false,org.slf4j.LoggerFactory.getLogger("tab-extras-smoke"));
        var unrelated=new ServerBossBar(Text.literal("Other mod"),BossBar.Color.RED,BossBar.Style.PROGRESS);c.networkHandler.sendPacket(BossBarS2CPacket.add(unrelated));
        try{
            pc.clear();bossRuntime.update(List.of(c),0);check(bosses(pc).isEmpty(),"Boss condition ignored permission");
            bossAllowed[0]=true;bossRuntime.update(List.of(c),20);check(bosses(pc).stream().anyMatch(e->e.kind().equals("add")&&!e.id().equals(unrelated.getUuid())),"Personal boss did not coexist with unrelated bar");
            pc.clear();c.setServerWorld(server.getWorld(net.minecraft.world.World.NETHER));bossRuntime.update(List.of(c),40);check(bosses(pc).stream().anyMatch(e->e.kind().equals("remove")),"World condition did not remove owned bar");
            c.setServerWorld(server.getOverworld());pc.clear();bossRuntime.update(List.of(c),60);
            var changed=new TabExtrasConfig(1,barBase.listed(),barBase.nametags(),barBase.belowName(),new TabExtrasConfig.Bossbar(true,"<red>Updated</red>","0.75","RED","NOTCHED_10",conditional.bossbar().condition()));
            bossRuntime.replacePlan(new TabExtrasPlan(changed,engine));pc.clear();bossRuntime.update(List.of(c),80);
            check(bosses(pc).stream().anyMatch(e->e.kind().equals("name")&&e.name().getString().equals("Updated"))&&bosses(pc).stream().anyMatch(e->e.kind().equals("progress")&&e.progress()==.75f),"Reload did not update boss name/progress");
            check(bosses(pc).stream().anyMatch(e->e.kind().equals("style")&&e.name().getString().equals("RED:NOTCHED_10")),"Reload did not update boss color/style");
            pc.clear();bossRuntime.close();check(bosses(pc).stream().noneMatch(e->e.id().equals(unrelated.getUuid())),"Cleanup removed unrelated bossbar");
        }finally{c.setServerWorld(server.getOverworld());bossRuntime.close();}
        TabExtrasPackets.forget(a.networkHandler);TabExtrasPackets.forget(c.networkHandler);
    }
}
