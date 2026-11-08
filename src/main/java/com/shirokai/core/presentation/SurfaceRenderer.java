package com.shirokai.core.presentation;
import net.minecraft.text.Text;
import java.util.UUID;
@FunctionalInterface
public interface SurfaceRenderer {
    Text render(UUID subject, Surface surface, String value);
    default Text renderClan(UUID viewer,UUID leader,Surface surface,String value){return render(leader,surface,value);}
    default boolean customizationEnabled(){return false;}
    default void openCustomization(net.minecraft.server.network.ServerPlayerEntity player,Surface surface,UUID expectedClan){}
    default boolean menusEnabled(){return true;}
    static SurfaceRenderer literal() { return (id, surface, value) -> Text.literal(value); }
}
