package com.shirokai.core.presentation;
import net.minecraft.text.Text;
import java.util.UUID;
@FunctionalInterface
public interface SurfaceRenderer {
    Text render(UUID subject, Surface surface, String value);
    default boolean menusEnabled(){return true;}
    static SurfaceRenderer literal() { return (id, surface, value) -> Text.literal(value); }
}
