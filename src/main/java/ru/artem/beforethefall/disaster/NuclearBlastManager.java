package ru.artem.beforethefall.disaster;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import ru.artem.beforethefall.radiation.RadiationManager;
import java.util.concurrent.ThreadLocalRandom;

public final class NuclearBlastManager {
    private final BeforeTheFallPlugin plugin;
    private final RadiationManager radiation;
    public NuclearBlastManager(BeforeTheFallPlugin plugin, RadiationManager radiation) {
        this.plugin = plugin; this.radiation = radiation;
    }
    public void detonate(Location origin, float power) {
        World world = origin.getWorld();
        if (world == null) return;
        float safePower = Math.max(4.0f, Math.min(40.0f, power));
        double radius = safePower * 9.0;
        world.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 8.0f, 0.45f);
        world.spawnParticle(Particle.FLASH, origin, 1);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, origin, 1);
        for (Player player : world.getPlayers()) {
            double distance = player.getLocation().distance(origin);
            if (plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(player.getLocation())) continue;
            if (distance > radius * 1.35) continue;
            double exposure = Math.max(0.0, 1.0 - distance / (radius * 1.35));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int)(80 + exposure * 180), 0, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, (int)(240 + exposure * 600), 0, true, false, true));
            player.damage(Math.max(2.0, 26.0 * exposure));
            double maskFactor = plugin.getGasMaskManager().isProtected(player) ? 0.35 : 1.0;
            radiation.add(player, 35.0 * exposure * maskFactor);
        }
        new BukkitRunnable() {
            int phase = 0;
            @Override public void run() {
                if (phase >= 12) { cancel(); return; }
                double height = phase * 4.0, cloudRadius = 2.0 + phase * 2.3;
                int particles = 100 + phase * 35;
                for (int i = 0; i < particles; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
                    double r = ThreadLocalRandom.current().nextDouble(cloudRadius * 0.35, cloudRadius);
                    Location point = origin.clone().add(Math.cos(angle) * r, height + ThreadLocalRandom.current().nextDouble(-1, 2), Math.sin(angle) * r);
                    world.spawnParticle(Particle.LARGE_SMOKE, point, 1, 0.5, 1.2, 0.5, 0.015);
                    if (i % 4 == 0) world.spawnParticle(Particle.CLOUD, point, 1, 0.8, 1.5, 0.8, 0.01);
                    if (i % 9 == 0) world.spawnParticle(Particle.ASH, point, 1, 1.0, 1.8, 1.0, 0.02);
                }
                if (phase == 1 || phase == 4) world.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.65f);
                phase++;
            }
        }.runTaskTimer(plugin, 2L, 5L);
    }
}
