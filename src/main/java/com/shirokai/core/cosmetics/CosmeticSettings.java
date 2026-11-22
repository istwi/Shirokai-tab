package com.shirokai.core.cosmetics;


import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class CosmeticSettings {
    public boolean enabled=true, nickEnabled=true, suppressInBattle=true;
    public int intervalTicks=5, trailIntervalTicks=6, trailBaseDensity=4, trailMinimumPerTick=2, maxParticlesPerPlayer=48, maxParticlesPerTick=500, viewDistance=32, celebrationCooldownSeconds=10;
    public int maxSpeed=3, maxSize=3, maxDensity=3;
    public Set<String> disabled=new HashSet<>();
    public static CosmeticSettings load(Path path) throws IOException {
        var cfg=CosmeticRepository.readSettings(path);
        cfg.validate();return cfg;
    }
    public void validate() throws IOException {
        if(intervalTicks<2||intervalTicks>40||trailIntervalTicks<2||trailIntervalTicks>40||trailBaseDensity<1||trailBaseDensity>8||trailMinimumPerTick<1||trailMinimumPerTick>8||maxParticlesPerPlayer<1||maxParticlesPerPlayer>200||maxParticlesPerTick<1||maxParticlesPerTick>5000||viewDistance<4||viewDistance>64||celebrationCooldownSeconds<1||celebrationCooldownSeconds>3600||maxSpeed<1||maxSpeed>3||maxSize<1||maxSize>3||maxDensity<1||maxDensity>3||disabled==null)throw new IOException("Limites de cosméticos inválidos");
        disabled.removeIf(id->CosmeticCatalog.find(id)==null);
    }
}
