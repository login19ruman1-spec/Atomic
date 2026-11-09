package ru.artem.beforethefall.weather;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import java.util.concurrent.ThreadLocalRandom;

public final class AshStormManager {
    private final BeforeTheFallPlugin plugin;
    private BukkitTask task;
    private boolean active;
    private long ticks;
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
        ticks += 10;
        int count = Math.max(20, Math.min(900, plugin.getConfig().getInt("ash-storm.particle-count", 450)));
        int rings = Math.max(1, plugin.getConfig().getInt("ash-storm.cloud-rings", 3));
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location center = player.getLocation();
            World world = player.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            boolean protectedByMask = plugin.getGasMaskManager().isProtected(player);
            boolean protectedByBunker = plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(center);
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble(0, Math.PI * 2), radius = random.nextDouble(3.0, 22.0);
                Location ash = center.clone().add(Math.cos(angle) * radius, random.nextDouble(-2.0, 15.0), Math.sin(angle) * radius);
                world.spawnParticle(Particle.ASH, ash, 1, 0.18, 0.45, 0.18, 0.012);
                if (i % 3 == 0) world.spawnParticle(Particle.WHITE_ASH, ash, 1, 0.35, 0.7, 0.35, 0.008);
                if (i % 8 == 0) world.spawnParticle(Particle.CLOUD, ash, 1, 0.5, 0.2, 0.5, 0.015);
            }
            for (int ring = 0; ring < rings; ring++)
                world.spawnParticle(Particle.ASH, center.clone().add(0, 5.0 + ring * 3.0, 0), 45, 10.0, 1.8, 10.0, 0.01);
            if (ticks % 40 == 0 && plugin.getConfig().getBoolean("ash-storm.accumulate-ash", true)) depositAsh(world, center);
            if (!protectedByMask && !protectedByBunker && ticks % 40 == 0 && plugin.getConfig().getBoolean("frost.enabled", true)) {
                int freeze = Math.max(0, plugin.getConfig().getInt("frost.freeze-ticks-per-pulse", 80));
                player.setFreezeTicks(Math.min(player.getMaxFreezeTicks(), player.getFreezeTicks() + freeze));
                double frostDamage = Math.max(0.0, plugin.getConfig().getDouble("frost.damage", 1.0));
                if (frostDamage > 0.0) player.damage(frostDamage);
            }
            if (!protectedByMask && !protectedByBunker) {
                if (plugin.getConfig().getBoolean("ash-storm.apply-slowness", true))
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 35, plugin.getConfig().getInt("ash-storm.slowness-amplifier", 0), true, false, true));
                if (plugin.getConfig().getBoolean("ash-storm.blindness", true))
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 25, 0, true, false, true));
                double damage = plugin.getConfig().getDouble("ash-storm.damage", 0.0);
                if (damage > 0) player.damage(damage);
            }
        }
    }
    private void depositAsh(World world, Location around) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < Math.max(0, Math.min(64, plugin.getConfig().getInt("ash-storm.deposits-per-player", 14))); i++) {
            int x = around.getBlockX() + random.nextInt(-16, 17), z = around.getBlockZ() + random.nextInt(-16, 17);
            int y = world.getHighestBlockYAt(x, z);
            Block top = world.getBlockAt(x, y, z), layer = top.getRelative(BlockFace.UP);
            if (top.getType().isSolid() && (layer.getType().isAir() || layer.getType() == Material.SNOW)) {
                if (layer.getType().isAir()) layer.setType(Material.SNOW, false);
                Snow snow = (Snow) layer.getBlockData();
                if (snow.getLayers() < snow.getMaximumLayers()) { snow.setLayers(Math.min(snow.getMaximumLayers(), snow.getLayers() + 1)); layer.setBlockData(snow, false); }
            }
        }
    }
}
