package ru.artem.beforethefall.weather;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import ru.artem.beforethefall.BeforeTheFallPlugin;

public final class AshStormManager {
    private final BeforeTheFallPlugin plugin;
    private BukkitTask task;
    private boolean active;

    public AshStormManager(BeforeTheFallPlugin plugin) { this.plugin = plugin; }

    public void start() {
        if (!plugin.getConfig().getBoolean("ash-storm.enabled", true)) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void reload() { stop(); start(); }
    public void stop() { active = false; if (task != null) { task.cancel(); task = null; } }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    private void tick() {
        if (!active) return;
        int count = Math.max(1, plugin.getConfig().getInt("ash-storm.particle-count", 18));
        boolean slow = plugin.getConfig().getBoolean("ash-storm.apply-slowness", true);
        int amplifier = Math.max(0, plugin.getConfig().getInt("ash-storm.slowness-amplifier", 0));
        double damage = Math.max(0.0, plugin.getConfig().getDouble("ash-storm.damage", 0.0));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getWorld().spawnParticle(Particle.ASH, player.getLocation().add(0, 2, 0), count, 5.0, 2.0, 5.0, 0.01);
            if (slow) player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, amplifier, true, false, true));
            if (damage > 0.0) player.damage(damage);
        }
    }
}
