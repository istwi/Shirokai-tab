package com.shirokai.holograms;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/** A private native display allocated from the normal entity ID sequence, never added to a ServerWorld. */
public final class DisplayLine {
    private final TextDisplayEntity entity;
    private final ServerPlayerEntity viewer;
    private final Definitions.Hologram hologram;
    private Text previous; private double px=Double.NaN,py;private int alpha;
    private boolean spawned;
    public DisplayLine(ServerPlayerEntity viewer,Definitions.Hologram hologram) {
        this.viewer=viewer;this.hologram=hologram;
        entity=new TextDisplayEntity(EntityType.TEXT_DISPLAY,viewer.getServerWorld());
        entity.setPosition(hologram.x(),hologram.y(),hologram.z());entity.setYaw(hologram.yaw());entity.setPitch(hologram.pitch());
    }
    public int entityId(){return entity.getId();}
    public void update(Text text,double xPixels,double yPixels,int opacity,int interval,boolean resetInterpolation) {
        if(spawned && text.equals(previous) && xPixels==px && yPixels==py && opacity==alpha)return;
        previous=text.copy();px=xPixels;py=yPixels;alpha=opacity;
        var nbt=entity.writeNbt(new NbtCompound());
        nbt.putString("text",Text.Serialization.toJsonString(text,viewer.getRegistryManager()));
        nbt.putInt("line_width",32767);nbt.putInt("background",opacity<4?0:(hologram.backgroundOpacity()<<24)|hologram.backgroundColor());
        nbt.putByte("text_opacity",(byte)Math.max(4,opacity));nbt.putBoolean("shadow",hologram.shadow());
        nbt.putBoolean("see_through",false);nbt.putBoolean("default_background",false);
        nbt.putString("alignment","center");nbt.putString("billboard",hologram.billboard().name().toLowerCase(java.util.Locale.ROOT));
        nbt.putFloat("view_range",4f);nbt.putInt("interpolation_duration",!spawned||resetInterpolation?0:interval);
        nbt.putInt("start_interpolation",0);
        var brightness=new NbtCompound();brightness.putInt("block",15);brightness.putInt("sky",15);nbt.put("brightness",brightness);
        var transform=new NbtCompound();float scale=hologram.scale();
        transform.put("translation",floats((float)(xPixels*.025*scale),(float)(yPixels*.025*scale),0));
        transform.put("scale",floats(scale,scale,scale));transform.put("left_rotation",floats(0,0,0,1));transform.put("right_rotation",floats(0,0,0,1));nbt.put("transformation",transform);
        entity.readNbt(nbt);
        boolean initial=!spawned;
        if(!spawned) {
            viewer.networkHandler.sendPacket(new EntitySpawnS2CPacket(entity.getId(),entity.getUuid(),hologram.x(),hologram.y(),hologram.z(),hologram.pitch(),hologram.yaw(),EntityType.TEXT_DISPLAY,0,Vec3d.ZERO,hologram.yaw()));
            spawned=true;
        }
        var data=new java.util.LinkedHashMap<Integer,net.minecraft.entity.data.DataTracker.SerializedEntry<?>>();
        var changed=initial?entity.getDataTracker().getChangedEntries():null;
        if(changed!=null)changed.forEach(entry->data.put(entry.id(),entry));
        var dirty=entity.getDataTracker().getDirtyEntries();
        if(dirty!=null)dirty.forEach(entry->data.put(entry.id(),entry));
        if(!data.isEmpty())viewer.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(entity.getId(),java.util.List.copyOf(data.values())));
    }
    private static NbtList floats(float...values){var list=new NbtList();for(float f:values)list.add(NbtFloat.of(f));return list;}
    public void destroy(){if(spawned){viewer.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(entity.getId()));spawned=false;}}
}
