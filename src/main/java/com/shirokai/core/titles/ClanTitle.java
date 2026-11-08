package com.shirokai.core.titles;

import com.shirokai.core.clans.ClanSnapshot;
import java.util.*;

/** No grants: availability derives exclusively from current native clan membership. */
public final class ClanTitle {
    private ClanTitle(){}
    public static String id(UUID clan){return "clan_"+clan;}
    public static Optional<TitleDefinition> from(ClanSnapshot clan){
        if(clan.tag()==null)return Optional.empty();
        return Optional.of(new TitleDefinition(id(clan.id()),clan.name(),"["+clan.tag()+"]","Identificação do clã "+clan.name(),TitleDefinition.Category.CLAN,0,null,false,false,false,true));
    }
}
