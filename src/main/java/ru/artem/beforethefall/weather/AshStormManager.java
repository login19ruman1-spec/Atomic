package ru.artem.beforethefall.weather;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import java.util.concurrent.ThreadLocalRandom;

/** Radioactive ash weather with particles and thin carpet-like deposits (never snow). */
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
        int count = Math.max(20, Math.min(500, plugin.getConfig().getInt("ash-storm.particle-count", 320)));
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
            if (plugin.getConfig().getBoolean("ash-storm.deposit-ash", true)) {
                int attempts = 5;
                int r = Math.max(4, plugin.getConfig().getInt("ash-storm.deposit-radius", 18));
                double chance = Math.max(0.0, Math.min(1.0, plugin.getConfig().getDouble("ash-storm.deposit-chance", 0.06)));
                for (int i = 0; i < attempts; i++) {
                    if (random.nextDouble() > chance) continue;
                    int x = center.getBlockX() + random.nextInt(-r, r + 1);
                    int z = center.getBlockZ() + random.nextInt(-r, r + 1);
                    Block ground = world.getHighestBlockAt(x, z);
                    Block layer = ground.getRelative(0, 1, 0);
                    if (!ground.getType().isSolid() || ground.isLiquid() || ground.getType().name().contains("LEAVES")) continue;
                    if (!layer.getType().isAir() || layer.getY() >= world.getMaxHeight() - 1) continue;
                    layer.setType(Material.LIGHT_GRAY_CARPET, false);
                }
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
