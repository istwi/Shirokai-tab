package com.shirokai.smoke;

import com.shirokai.core.tab.TeamOwnership;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;

import net.minecraft.scoreboard.Scoreboard;
import java.util.*;

public final class TeamOwnershipSmoke implements ModInitializer{
    @Override public void onInitialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(literal("shirokai-teams-smoke").requires(s->s.hasPermissionLevel(4)).executes(context->{
        staleNametagTeamsDoNotBlockReleasedPlayers();samePrioritySharesTeamAndForeignPlayersAreNeverMoved();foreignNameCollisionAndReplacementAreNotAdoptedOrDeleted();changedPriorityAndDisconnectPruneOnlyEmptyOwnedTeams();
        context.getSource().sendFeedback(()->Text.literal("TEAM_OWNERSHIP_SMOKE_PASS"),false);return 1;
    })));}
    private static void staleNametagTeamsDoNotBlockReleasedPlayers(){
        var board=new Scoreboard();var stale=board.addTeam("skn0000000000000");
        stale.setPrefix(Text.literal("[DEFAULT] "));board.addScoreHolderToTeam("Alice",stale);
        var manager=new TeamOwnership(board,message->{});
        var alice=new TeamOwnership.Player(UUID.randomUUID(),"Alice",100,Text.literal("[OWNER] "),Text.empty(),net.minecraft.scoreboard.AbstractTeam.VisibilityRule.ALWAYS);
        var bob=new TeamOwnership.Player(UUID.randomUUID(),"Bob",90,Text.literal("[ADMIN] "),Text.empty(),net.minecraft.scoreboard.AbstractTeam.VisibilityRule.ALWAYS);
        manager.reconcile(List.of(alice,bob));assertSame(stale,board.getScoreHolderTeam("Alice"));
        board.removeScoreHolderFromTeam("Alice",stale); // Explicit administrator /team leave.
        manager.reconcile(List.of(alice,bob));
        var owner=board.getScoreHolderTeam("Alice");var admin=board.getScoreHolderTeam("Bob");
        if(owner==null||admin==null)throw new AssertionError("Stale team name blocked nametag after explicit release");
        if(!owner.getPrefix().getString().equals("[OWNER] ")||owner.getName().compareTo(admin.getName())>=0)throw new AssertionError("LP weight order or nametag not restored");
        manager.close();assertSame(stale,board.getTeam(stale.getName()));
        // Simulate a saved team from an unclean previous session without claiming it by name.
        var previous=new TeamOwnership(board,message->{});previous.reconcile(List.of(alice,bob));
        var orphan=board.getScoreHolderTeam("Alice");var orphanBob=board.getScoreHolderTeam("Bob");
        var restarted=new TeamOwnership(board,message->{});restarted.reconcile(List.of(alice,bob));assertSame(orphan,board.getScoreHolderTeam("Alice"));
        board.removeScoreHolderFromTeam("Alice",orphan);board.removeScoreHolderFromTeam("Bob",orphanBob);
        restarted.reconcile(List.of(alice,bob));
        if(board.getScoreHolderTeam("Alice")==null||board.getScoreHolderTeam("Bob")==null)throw new AssertionError("Previous session prevented fresh nametags");
        restarted.close();assertSame(orphan,board.getTeam(orphan.getName()));
    }
    private static void assertSame(Object expected,Object actual){if(expected!=actual)throw new AssertionError("Identity mismatch");}
    private static void assertNull(Object actual){if(actual!=null)throw new AssertionError("Expected null");}
    private static void assertTrue(boolean value){if(!value)throw new AssertionError("Expected true");}
    private static void samePrioritySharesTeamAndForeignPlayersAreNeverMoved(){
        var scoreboard=new Scoreboard();var foreign=scoreboard.addTeam("external");scoreboard.addScoreHolderToTeam("Alice",foreign);
        var manager=new TeamOwnership(scoreboard,message->{});
        manager.reconcile(List.of(new TeamOwnership.Player(UUID.randomUUID(),"Alice",100),new TeamOwnership.Player(UUID.randomUUID(),"Bob",100),new TeamOwnership.Player(UUID.randomUUID(),"Carol",100)));
        assertSame(foreign,scoreboard.getScoreHolderTeam("Alice"));assertSame(scoreboard.getScoreHolderTeam("Bob"),scoreboard.getScoreHolderTeam("Carol"));
        assertTrue(TeamOwnership.teamName(100).compareTo(TeamOwnership.teamName(0))<0);assertTrue(TeamOwnership.teamName(-1000000).length()<=16);
        manager.close();assertSame(foreign,scoreboard.getScoreHolderTeam("Alice"));assertNull(scoreboard.getScoreHolderTeam("Bob"));assertSame(foreign,scoreboard.getTeam("external"));
    }
    private static void foreignNameCollisionAndReplacementAreNotAdoptedOrDeleted(){
        var scoreboard=new Scoreboard();var collision=scoreboard.addTeam(TeamOwnership.teamName(100));var manager=new TeamOwnership(scoreboard,message->{});
        manager.reconcile(List.of(new TeamOwnership.Player(UUID.randomUUID(),"Alice",100)));assertNull(scoreboard.getScoreHolderTeam("Alice"));
        manager.reconcile(List.of(new TeamOwnership.Player(UUID.randomUUID(),"Bob",0)));var original=scoreboard.getScoreHolderTeam("Bob");scoreboard.removeTeam(original);var replacement=scoreboard.addTeam(original.getName());scoreboard.addScoreHolderToTeam("Bob",replacement);
        manager.reconcile(List.of(new TeamOwnership.Player(UUID.randomUUID(),"Bob",0)));manager.close();assertSame(collision,scoreboard.getTeam(collision.getName()));assertSame(replacement,scoreboard.getScoreHolderTeam("Bob"));
    }
    private static void changedPriorityAndDisconnectPruneOnlyEmptyOwnedTeams(){
        var scoreboard=new Scoreboard();var manager=new TeamOwnership(scoreboard,message->{});UUID id=UUID.randomUUID();
        manager.reconcile(List.of(new TeamOwnership.Player(id,"Alice",0)));String old=scoreboard.getScoreHolderTeam("Alice").getName();manager.reconcile(List.of(new TeamOwnership.Player(id,"Alice",100)));assertNull(scoreboard.getTeam(old));
        var team=scoreboard.getScoreHolderTeam("Alice");scoreboard.addScoreHolderToTeam("ExternalHolder",team);manager.close();assertNull(scoreboard.getScoreHolderTeam("Alice"));assertSame(team,scoreboard.getScoreHolderTeam("ExternalHolder"));
    }
}
