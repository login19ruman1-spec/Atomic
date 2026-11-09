package ru.artem.beforethefall.weather;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import java.util.concurrent.ThreadLocalRandom;

/** Ash is a visual/weather effect only. It never places snow or changes terrain. */
public final class AshStormManager {
    private final BeforeTheFallPlugin plugin;
    private BukkitTask task;
    private boolean active;
    public AshStormManager(BeforeTheFallPlugin plugin) { this.plugin = plugin; }
    public void start() {
        if (!plugin.getConfig().getBoolean("ash-storm.enabled", true)) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }
    public void reload() { stop(); start(); }
    public void stop() { active = false; if (task != null) { task.cancel(); task = null; } }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    private void tick() {
        if (!active) return;
        int count = Math.max(20, Math.min(500, plugin.getConfig().getInt("ash-storm.particle-count", 220)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location center = player.getLocation();
            World world = player.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            boolean protectedByMask = plugin.getGasMaskManager().isProtected(player);
            boolean protectedByBunker = plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(center);
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble(0, Math.PI * 2), radius = random.nextDouble(3.0, 20.0);
                Location ash = center.clone().add(Math.cos(angle) * radius, random.nextDouble(-2.0, 14.0), Math.sin(angle) * radius);
                world.spawnParticle(Particle.ASH, ash, 1, 0.16, 0.42, 0.16, 0.012);
                if (i % 4 == 0) world.spawnParticle(Particle.WHITE_ASH, ash, 1, 0.28, 0.65, 0.28, 0.008);
                if (i % 12 == 0) world.spawnParticle(Particle.CLOUD, ash, 1, 0.45, 0.18, 0.45, 0.012);
            }
            if (!protectedByMask && !protectedByBunker) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 35, 0, true, false, true));
                if (plugin.getConfig().getBoolean("ash-storm.blindness", true))
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 25, 0, true, false, true));
                double damage = plugin.getConfig().getDouble("ash-storm.damage", 0.0);
                if (damage > 0) player.damage(damage);
                if (random.nextDouble() < 0.25) plugin.getRadiationManager().add(player, 0.015);
            }
        }
    }
}
