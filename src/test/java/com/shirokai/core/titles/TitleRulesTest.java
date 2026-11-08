package com.shirokai.core.titles;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class TitleRulesTest {
    static TitleDefinition definition(String id, TitleDefinition.Category category, int priority, String permission) {
        return new TitleDefinition(id, id, "[" + id + "]", "description", category, priority, permission, false, true, true, true);
    }
    static TitleCatalog catalog(TitleDefinition... titles) {
        var definitions = new HashMap<String,TitleDefinition>(); for (var title : titles) definitions.put(title.id(), title);
        return new TitleCatalog(1, definitions);
    }
    @Test void rolesAreNotSelectableAndLegacyVisualSelectionsAreRemoved() {
        var role=definition("vip",TitleDefinition.Category.ROLE,1,"group.vip");
        var event=definition("event",TitleDefinition.Category.SPECIAL,1,null);
        var rules=new TitleRules((id,node)->true);var catalog=catalog(role,event);
        var state=new TitleState(UUID.randomUUID(),3,Map.of("event",0L),"event",List.of("vip","event"));
        var valid=rules.normalize(state,catalog,1);
        assertNull(valid.selectedVisual());assertEquals(List.of("event"),valid.chatIds());
        assertThrows(IllegalArgumentException.class,()->rules.equip(state,catalog,"vip",true,1));
        assertThrows(IllegalArgumentException.class,()->rules.equip(state,catalog,"vip",false,1));
        assertThrows(IllegalArgumentException.class,()->rules.equip(state,catalog,"event",false,1));
        assertTrue(rules.equip(valid,catalog,"event",true,1).chatIds().isEmpty());
    }
    @Test void expirationAndPermissionLossRemoveOnlyInvalidSelections() {
        var permitted = new AtomicBoolean(true);
        var role = definition("vip", TitleDefinition.Category.ROLE, 1, "group.vip");
        var event = definition("event", TitleDefinition.Category.SPECIAL, 1, null);
        var rules = new TitleRules((id,node) -> permitted.get()); var catalog = catalog(role,event);
        var state = new TitleState(UUID.randomUUID(),1,Map.of("event",100L),"event",List.of("vip","event"));
        assertEquals(TitleRules.Availability.UNLOCKED, rules.availability(event,state,99));
        assertEquals(TitleRules.Availability.EXPIRED, rules.availability(event,state,100));
        var expired = rules.normalize(state,catalog,100);
        assertNull(expired.selectedVisual()); assertTrue(expired.chatIds().isEmpty());
        permitted.set(false);
        assertEquals(TitleRules.Availability.UNAVAILABLE,rules.availability(role,state,99));
        assertTrue(rules.normalize(state,catalog,100).chatIds().isEmpty());
    }
    @Test void lockedMissingAndSurfaceRestrictedTitlesCannotBeEquipped() {
        var locked = definition("locked",TitleDefinition.Category.SPECIAL,0,null);
        var chatOnly = new TitleDefinition("chat", "chat", "chat", "",TitleDefinition.Category.SPECIAL,0,null,true,false,false,true);
        var state = new TitleState(UUID.randomUUID(),0,Map.of(),null,List.of()); var catalog=catalog(locked,chatOnly);
        var rules = new TitleRules((id,node)->false);
        assertEquals(TitleRules.Availability.LOCKED,rules.availability(locked,state,1));
        for(String id:List.of("locked","missing","chat")) assertThrows(IllegalArgumentException.class,()->rules.equip(state,catalog,id,false,1));
        assertEquals(List.of("chat"),rules.equip(state,catalog,"chat",true,1).chatIds());
    }
}
