package com.shirokai.core.menus;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import java.util.*;

/** Shared five-row frame; only explicit content slots may select an entry. */
public final class PanelLayout {
    private PanelLayout(){}
    public static final List<Integer> CONTENT=List.of(12,13,14,15,16,21,22,23,24,25,30,31,32,33,34);
    public static void frame(SimpleInventory inventory){
        for(int slot=0;slot<45;slot++)if(!CONTENT.contains(slot)){
            var pane=new ItemStack(Items.GRAY_STAINED_GLASS_PANE);pane.set(DataComponentTypes.CUSTOM_NAME,Text.literal(" "));inventory.setStack(slot,pane);
        }
    }
    public static boolean matches(MenuConfig.Profile profile){return profile.rows()==5&&profile.tagSlots().equals(CONTENT)&&profile.buttons().get("header").slot()==10&&profile.buttons().get("styles").slot()==19&&profile.buttons().get("back").slot()==28;}
}
