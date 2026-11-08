package ru.artem.beforethefall.radiation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import ru.artem.beforethefall.BeforeTheFallPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RadiationManager {
    private final BeforeTheFallPlugin plugin;
    private final Map<UUID, Double> levels = new HashMap<>();
    private BukkitTask task;

    public RadiationManager(BeforeTheFallPlugin plugin) { this.plugin = plugin; load(); }

    public void start() {
        if (!plugin.getConfig().getBoolean("radiation.enabled", true)) return;
        long interval = Math.max(20L, plugin.getConfig().getLong("radiation.tick-interval", 40L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void reload() { if (task != null) task.cancel(); start(); }
    public double get(Player player) { return levels.getOrDefault(player.getUniqueId(), 0.0); }
    public void set(Player player, double value) { levels.put(player.getUniqueId(), clamp(value)); applySymptoms(player); }
    public void add(Player player, double value) { set(player, get(player) + value); }

    public void save() {
        for (Map.Entry<UUID, Double> entry : levels.entrySet()) plugin.getConfig().set("player-data." + entry.getKey() + ".radiation", entry.getValue());
        plugin.saveConfig();
    }

    private void load() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("player-data");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try { UUID id = UUID.fromString(key); levels.put(id, section.getDouble(key + ".radiation", 0.0)); }
            catch (IllegalArgumentException ignored) { plugin.getLogger().warning("Skipping invalid player UUID: " + key); }
        }
    }

    private void tick() {
        double decay = plugin.getConfig().getDouble("radiation.decay-per-tick", 0.1);
        double ambient = plugin.getConfig().getDouble("radiation.ambient-per-tick", 0.15);
        for (Player player : Bukkit.getOnlinePlayers()) {
            double current = Math.max(0.0, get(player) - decay) + radiationAt(player.getLocation()) * ambient;
            if (wearingProtection(player)) current *= 0.75;
            levels.put(player.getUniqueId(), clamp(current));
            applySymptoms(player);
        }
    }

    private double radiationAt(Location location) {
        double level = 0.0;
        for (Map<?, ?> zone : plugin.getConfig().getMapList("radiation.zones")) {
            Object worldName = zone.get("world");
            if (worldName == null || location.getWorld() == null || !location.getWorld().getName().equals(String.valueOf(worldName))) continue;
            double x = number(zone.get("x")), y = number(zone.get("y")), z = number(zone.get("z"));
            double radius = Math.max(1.0, number(zone.get("radius")));
            Location center = new Location(location.getWorld(), x, y, z);
            if (location.distanceSquared(center) <= radius * radius) level = Math.max(level, number(zone.get("level")));
        }
        return level;
    }

    private double number(Object value) { return value instanceof Number n ? n.doubleValue() : 0.0; }

    private boolean wearingProtection(Player player) {
        String[] names = {
            player.getInventory().getHelmet() == null ? "" : player.getInventory().getHelmet().getType().name(),
            player.getInventory().getChestplate() == null ? "" : player.getInventory().getChestplate().getType().name(),
            player.getInventory().getLeggings() == null ? "" : player.getInventory().getLeggings().getType().name(),
            player.getInventory().getBoots() == null ? "" : player.getInventory().getBoots().getType().name()
        };
        int pieces = 0;
        for (String name : names) if (name.contains("LEATHER") || name.contains("IRON") || name.contains("GOLDEN") || name.contains("DIAMOND") || name.contains("NETHERITE")) pieces++;
        return pieces >= 4;
    }

    private void applySymptoms(Player player) {
        double value = get(player);
        double warning = plugin.getConfig().getDouble("radiation.warning-threshold", 20.0);
        double sick = plugin.getConfig().getDouble("radiation.sickness-threshold", 45.0);
        double severe = plugin.getConfig().getDouble("radiation.severe-threshold", 75.0);
        if (value >= severe) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0, true, false, true));
        } else if (value >= sick) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, true, false, true));
        } else if (value >= warning) player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 0, true, false, true));
    }

    private double clamp(double value) { return Math.max(0.0, Math.min(100.0, value)); }
}
