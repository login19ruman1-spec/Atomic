package ru.artem.beforethefall.radiation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.scheduler.BukkitTask;
import ru.artem.beforethefall.BeforeTheFallPlugin;

public final class EnvironmentalHazardManager implements Listener {
    private final BeforeTheFallPlugin plugin;
    private BukkitTask task;

    public EnvironmentalHazardManager(BeforeTheFallPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        double threshold = plugin.getConfig().getDouble("radiation.mob-lethal-threshold", 4.0);
        if (plugin.getRadiationManager().getAmbientLevel(event.getLocation()) >= threshold) {
            event.setCancelled(true);
        }
    }

    private void tick() {
        double lethalThreshold = plugin.getConfig().getDouble("radiation.mob-lethal-threshold", 4.0);
        double rainDamage = plugin.getConfig().getDouble("acid-rain.damage-per-interval", 1.0);
        double rainDose = plugin.getConfig().getDouble("acid-rain.dose-per-interval", 0.15);
        double mobRainDamage = plugin.getConfig().getDouble("acid-rain.mob-damage-per-interval", 0.5);

        for (World world : Bukkit.getWorlds()) {
            boolean raining = world.hasStorm();
            for (LivingEntity entity : world.getLivingEntities()) {
                if (!entity.isValid() || entity.isDead()) continue;
                Location location = entity.getLocation();

                // Non-player creatures and animals cannot survive lethal fallout and are
                // continuously prevented from repopulating contaminated chunks.
                if (!(entity instanceof Player)
                        && plugin.getRadiationManager().getAmbientLevel(location) >= lethalThreshold) {
                    entity.setHealth(0.0);
                    continue;
                }

                if (!raining || !isExposedToSky(location)) continue;
                if (entity instanceof Player player) {
                    if (plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(location)) continue;
                    boolean fullSuit = plugin.getProtectiveSuitManager() != null
                            && plugin.getProtectiveSuitManager().hasFullSuit(player)
                            && !plugin.getRadiationManager().isAtBlastCore(location);
                    double protection = fullSuit ? 0.30 : 1.0;
                    player.damage(rainDamage * protection);
                    plugin.getRadiationManager().add(player, rainDose * protection);
                } else {
                    entity.damage(mobRainDamage);
                }
            }
        }
    }

    private boolean isExposedToSky(Location location) {
        World world = location.getWorld();
        if (world == null) return false;
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int highest = world.getHighestBlockYAt(x, z);
        return location.getBlockY() >= highest;
    }
}
