package com.shirokai.core.tab;

import net.minecraft.scoreboard.*;
import java.util.*;
import java.util.function.*;

/** Opt-in scoreboard ownership: never adopts existing teams or moves foreign members. */
public final class TeamOwnership implements AutoCloseable{
    public record Player(UUID id,String name,int priority,net.minecraft.text.Text prefix,net.minecraft.text.Text suffix,AbstractTeam.VisibilityRule visibility){
        public Player(UUID id,String name,int priority){this(id,name,priority,null,null,null);}
    }
    private final Scoreboard scoreboard;private final Consumer<String> warning;
    private final Map<String,Team> owned=new HashMap<>();private final Map<String,Team> assigned=new HashMap<>();private final Set<String> warned=new LinkedHashSet<>();
    private record Appearance(net.minecraft.text.Text prefix,net.minecraft.text.Text suffix,AbstractTeam.VisibilityRule visibility,AbstractTeam.VisibilityRule death,AbstractTeam.CollisionRule collision,net.minecraft.util.Formatting color,boolean friendly,boolean invisibles){
        static Appearance of(Team team){return new Appearance(team.getPrefix().copy(),team.getSuffix().copy(),team.getNameTagVisibilityRule(),team.getDeathMessageVisibilityRule(),team.getCollisionRule(),team.getColor(),team.isFriendlyFireAllowed(),team.shouldShowFriendlyInvisibles());}
    }
    private final Map<Team,Appearance> appearances=new IdentityHashMap<>();
    private void revokeChanged(){for(var entry:List.copyOf(appearances.entrySet())){
        var team=entry.getKey();if(scoreboard.getTeam(team.getName())!=team||!entry.getValue().equals(Appearance.of(team))){
            owned.remove(team.getName(),team);assigned.entrySet().removeIf(row->row.getValue()==team);appearances.remove(team);note("Team modificado externamente preservado: "+team.getName());
        }
    }}
    private boolean closed;private long conflicts;
    public TeamOwnership(Scoreboard scoreboard,Consumer<String> warning){this.scoreboard=Objects.requireNonNull(scoreboard);this.warning=warning;}
    public static String teamName(int priority){int bounded=Math.max(-1000000,Math.min(1000000,priority));return String.format(Locale.ROOT,"sk%07d",1000000-bounded);}
    public boolean owns(Team team){return team!=null&&owned.get(team.getName())==team&&scoreboard.getTeam(team.getName())==team;}
    private void note(String key){if(warned.add(key)){conflicts++;warning.accept(key);if(warned.size()>128)warned.remove(warned.iterator().next());}}
    public void reconcile(Collection<Player> players){
        if(closed)return;revokeChanged();var active=new HashSet<String>();for(var player:players)active.add(player.name());
        for(String name:List.copyOf(assigned.keySet()))if(!active.contains(name))release(name);
        var ranks=new HashMap<UUID,Integer>();int rank=0;
        for(var player:players.stream().sorted(Comparator.comparingInt(Player::priority).reversed().thenComparing(Player::name,String.CASE_INSENSITIVE_ORDER).thenComparing(Player::id)).toList())ranks.put(player.id(),rank++);
        for(var player:players){
            var current=scoreboard.getScoreHolderTeam(player.name());if(current!=null&&!owns(current)){assigned.remove(player.name());note("Jogador "+player.name()+" possui team externo "+current.getName());continue;}
            String name=player.prefix()==null?teamName(player.priority()):String.format(Locale.ROOT,"skn%013d",ranks.get(player.id()));var desired=scoreboard.getTeam(name);
            if(desired!=null&&!owns(desired)){note("Nome de team já ocupado: "+name);continue;}
            if(desired==null){desired=scoreboard.addTeam(name);desired.setShowFriendlyInvisibles(false);desired.setFriendlyFireAllowed(true);desired.setCollisionRule(AbstractTeam.CollisionRule.ALWAYS);owned.put(name,desired);}
            if(player.prefix()!=null){
                var target=desired;
                boolean foreignMember=target.getPlayerList().stream().anyMatch(member->assigned.get(member)!=target);
                if(foreignMember){note("Nametag de team com integrantes externos preservado: "+name);release(player.name());continue;}
                if(!Objects.equals(desired.getPrefix(),player.prefix()))desired.setPrefix(player.prefix().copy());
                if(!Objects.equals(desired.getSuffix(),player.suffix()))desired.setSuffix(player.suffix().copy());
                if(desired.getNameTagVisibilityRule()!=player.visibility())desired.setNameTagVisibilityRule(player.visibility());
                appearances.put(desired,Appearance.of(desired));
            }
            if(current!=desired){if(current!=null)scoreboard.removeScoreHolderFromTeam(player.name(),current);scoreboard.addScoreHolderToTeam(player.name(),desired);}assigned.put(player.name(),desired);
        }
        prune();
    }
    private void release(String name){var expected=assigned.remove(name);if(owns(expected)&&scoreboard.getScoreHolderTeam(name)==expected)scoreboard.removeScoreHolderFromTeam(name,expected);}
    private void prune(){for(var entry:List.copyOf(owned.entrySet())){
        if(scoreboard.getTeam(entry.getKey())!=entry.getValue()){owned.remove(entry.getKey());appearances.remove(entry.getValue());continue;}
        if(entry.getValue().getPlayerList().isEmpty()){scoreboard.removeTeam(entry.getValue());owned.remove(entry.getKey());appearances.remove(entry.getValue());}
    }}
    public int ownedTeams(){return owned.size();}public long conflicts(){return conflicts;}
    @Override public void close(){if(closed)return;revokeChanged();for(String name:List.copyOf(assigned.keySet()))release(name);prune();for(var team:owned.values())note("Team com integrantes externos preservado: "+team.getName());owned.clear();appearances.clear();closed=true;}
}
