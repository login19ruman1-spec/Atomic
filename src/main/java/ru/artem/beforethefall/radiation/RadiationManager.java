package ru.artem.beforethefall.radiation;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import ru.artem.beforethefall.BeforeTheFallPlugin;

import java.util.*;

public final class RadiationManager implements Listener {
    private final BeforeTheFallPlugin plugin;
    private final Map<UUID, Double> doses = new HashMap<>();
    private final List<FalloutZone> fallout = new ArrayList<>();
    private final List<WaterZone> waterZones = new ArrayList<>();
    private BukkitTask task;
    private long ticks;
    private final Map<UUID, Long> medicineUntil = new HashMap<>();

    public RadiationManager(BeforeTheFallPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("radiation.enabled", true)) return;
        long interval = Math.max(20L, plugin.getConfig().getLong("radiation.tick-interval", 40L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void reload() {
        if (task != null) task.cancel();
        start();
    }

    public double get(Player player) {
        return doses.getOrDefault(player.getUniqueId(), 0.0);
    }

    /** Ambient radiation at an arbitrary entity location. */
    public double getAmbientLevel(Location location) {
        if (location == null || location.getWorld() == null || isProtectedByBunker(location)) return 0.0;
        return radiationAt(location);
    }

    /** Instant local reading, including contaminated water when the player is immersed in it. */
    public double getAmbientLevel(Player player) {
        double level = getAmbientLevel(player.getLocation());
        if (isInContaminatedWater(player)) {
            level += plugin.getConfig().getDouble("radiation.contaminated-water-reading", 4.0);
        }
        return level;
    }

    /** Protective suits are overwhelmed in the immediate blast core. */
    public boolean isAtBlastCore(Location location) {
        if (location == null || location.getWorld() == null) return false;
        for (FalloutZone zone : fallout) {
            if (!zone.world.equals(location.getWorld().getUID())) continue;
            double dx = location.getX() - zone.x;
            double dy = location.getY() - zone.y;
            double dz = location.getZ() - zone.z;
            if (dx * dx + dy * dy + dz * dz <= 50.0 * 50.0) return true;
        }
        return false;
    }

    public boolean isInContaminatedWater(Player player) {
        Location loc = player.getLocation();
        boolean wet = isWater(loc.getBlock().getType())
                || isWater(player.getEyeLocation().getBlock().getType())
                || isWater(loc.clone().subtract(0, 1, 0).getBlock().getType());
        if (!wet) return false;
        long now = System.currentTimeMillis();
        for (WaterZone zone : waterZones) {
            if (zone.expiresAt <= now || !zone.world.equals(loc.getWorld().getUID())) continue;
            double dx = loc.getX() - zone.x;
            double dz = loc.getZ() - zone.z;
            if (dx * dx + dz * dz <= zone.radius * zone.radius) return true;
        }
        return false;
    }

    private boolean isWater(Material material) {
        return material == Material.WATER || material == Material.BUBBLE_COLUMN;
    }

    public void set(Player player, double value) {
        doses.put(player.getUniqueId(), clamp(value));
        applySymptoms(player);
    }

    public void add(Player player, double value) {
        set(player, get(player) + value);
    }

    public void addFalloutZone(Location center, double intensity, double radius, long durationTicks) {
        if (center.getWorld() == null) return;
        fallout.add(new FalloutZone(center.getWorld().getUID(), center.getX(), center.getY(), center.getZ(),
                Math.max(0.0, intensity), Math.max(1.0, radius),
                System.currentTimeMillis() + Math.max(0L, durationTicks) * 50L));
        save();
    }

    public void contaminateWater(Location center, double radius, long durationTicks) {
        if (center.getWorld() == null) return;
        waterZones.add(new WaterZone(center.getWorld().getUID(), center.getX(), center.getZ(),
                Math.max(1.0, radius), System.currentTimeMillis() + Math.max(0L, durationTicks) * 50L));
        save();
    }

    public void save() {
        for (Map.Entry<UUID, Double> entry : doses.entrySet()) {
            plugin.getConfig().set("player-data." + entry.getKey() + ".radiation", entry.getValue());
        }
        long now = System.currentTimeMillis();
        List<Map<String, Object>> falloutValues = new ArrayList<>();
        for (FalloutZone zone : fallout) {
            if (zone.expiresAt <= now) continue;
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("world", zone.world.toString());
            value.put("x", zone.x); value.put("y", zone.y); value.put("z", zone.z);
            value.put("intensity", zone.intensity); value.put("radius", zone.radius);
            value.put("expires-at", zone.expiresAt);
            falloutValues.add(value);
        }
        plugin.getConfig().set("radiation.fallout-zones", falloutValues);

        List<Map<String, Object>> waterValues = new ArrayList<>();
        for (WaterZone zone : waterZones) {
            if (zone.expiresAt <= now) continue;
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("world", zone.world.toString());
            value.put("x", zone.x); value.put("z", zone.z);
            value.put("radius", zone.radius); value.put("expires-at", zone.expiresAt);
            waterValues.add(value);
        }
        plugin.getConfig().set("radiation.water-zones", waterValues);
        plugin.saveConfig();
    }

    private void load() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("player-data");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    doses.put(UUID.fromString(key), clamp(section.getDouble(key + ".radiation", 0.0)));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Skipping invalid player UUID: " + key);
                }
            }
        }
        for (Map<?, ?> value : plugin.getConfig().getMapList("radiation.fallout-zones")) {
            try {
                fallout.add(new FalloutZone(UUID.fromString(String.valueOf(value.get("world"))),
                        num(value.get("x")), num(value.get("y")), num(value.get("z")),
                        num(value.get("intensity")), Math.max(1.0, num(value.get("radius"))),
                        (long) num(value.get("expires-at"))));
            } catch (RuntimeException ignored) {
                plugin.getLogger().warning("Skipping malformed fallout zone.");
            }
        }
        for (Map<?, ?> value : plugin.getConfig().getMapList("radiation.water-zones")) {
            try {
                waterZones.add(new WaterZone(UUID.fromString(String.valueOf(value.get("world"))),
                        num(value.get("x")), num(value.get("z")), Math.max(1.0, num(value.get("radius"))),
                        (long) num(value.get("expires-at"))));
            } catch (RuntimeException ignored) {
                plugin.getLogger().warning("Skipping malformed contaminated-water zone.");
            }
        }
        // Migrate the previous single-zone water contamination format.
        ConfigurationSection legacyWater = plugin.getConfig().getConfigurationSection("radiation.water-contamination");
        if (legacyWater != null && legacyWater.getLong("expires-at") > System.currentTimeMillis()) {
            try {
                waterZones.add(new WaterZone(UUID.fromString(legacyWater.getString("world")),
                        legacyWater.getDouble("x"), legacyWater.getDouble("z"),
                        Math.max(1.0, legacyWater.getDouble("radius")),
                        legacyWater.getLong("expires-at")));
            } catch (RuntimeException ignored) {
                plugin.getLogger().warning("Skipping legacy contaminated-water zone.");
            }
        }
    }

    private void tick() {
        ticks++;
        long now = System.currentTimeMillis();
        boolean expired = fallout.removeIf(zone -> zone.expiresAt <= now);
        expired |= waterZones.removeIf(zone -> zone.expiresAt <= now);

        double decay = Math.max(0.0, plugin.getConfig().getDouble("radiation.decay-per-tick", 0.025));
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location location = player.getLocation();
            boolean bunker = isProtectedByBunker(location);
            double airborne = bunker ? 0.0 : radiationAt(location);

            // A sealed filter reduces inhaled radioactive dust. It does not stop gamma radiation
            // or contamination taken in through water.
            double inhalationFactor = plugin.getGasMaskManager().isProtected(player)
                    ? plugin.getConfig().getDouble("radiation.gas-mask-inhalation-factor", 0.12) : 1.0;
            double suitFactor = plugin.getProtectiveSuitManager() != null
                    && plugin.getProtectiveSuitManager().hasFullSuit(player)
                    && !isAtBlastCore(location)
                    ? plugin.getConfig().getDouble("radiation.full-suit-factor", 0.45) : 1.0;

            double intake = airborne * inhalationFactor * suitFactor;
            if (!bunker && isInContaminatedWater(player)) {
                intake += plugin.getConfig().getDouble("radiation.contaminated-water-intake", 1.25);
            }

            long medicineEnd = medicineUntil.getOrDefault(player.getUniqueId(), 0L);
            double healing = medicineEnd > now ? plugin.getConfig().getDouble("radiation.medicine-decay-per-tick", 0.15) : decay;
            if (medicineEnd <= now) medicineUntil.remove(player.getUniqueId());
            double next = Math.max(0.0, get(player) - healing) + intake;
            doses.put(player.getUniqueId(), clamp(next));

            if (intake > 0.01 && ticks % 2 == 0) {
                player.getWorld().spawnParticle(Particle.ASH,
                        location.clone().add(0, 1.0, 0), 2, .25, .35, .25, .005);
            }
            applySymptoms(player);
        }

        if (expired || ticks % 150 == 0) save();
    }

    private boolean isProtectedByBunker(Location location) {
        return plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(location);
    }

    private double radiationAt(Location location) {
        double result = 0.0;
        for (Map<?, ?> zone : plugin.getConfig().getMapList("radiation.zones")) {
            if (!location.getWorld().getName().equals(String.valueOf(zone.get("world")))) continue;
            double dx = location.getX() - num(zone.get("x"));
            double dy = location.getY() - num(zone.get("y"));
            double dz = location.getZ() - num(zone.get("z"));
            double radius = Math.max(1.0, num(zone.get("radius")));
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > radius) continue;
            double remaining = 1.0 - distance / radius;
            result += num(zone.get("level")) * (0.08 + 0.92 * Math.pow(remaining, 1.35));
        }

        for (FalloutZone zone : fallout) {
            if (!zone.world.equals(location.getWorld().getUID())) continue;
            double dx = location.getX() - zone.x;
            double dy = location.getY() - zone.y;
            double dz = location.getZ() - zone.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > zone.radius) continue;
            double depthLimit = Math.max(1.0, plugin.getConfig().getDouble("radiation.underground-depth-limit", 20.0));
            double depth = zone.y - location.getY();
            if (depth > depthLimit) continue;
            double minFactor = plugin.getConfig().getDouble("radiation.underground-min-factor", 0.15);
            double undergroundFactor = depth <= 0.0 ? 1.0 : 1.0 - (1.0 - minFactor) * (depth / depthLimit);
            double remaining = 1.0 - distance / zone.radius;
            result += zone.intensity * undergroundFactor * (0.08 + 0.92 * Math.pow(remaining, 1.35));
        }
        return result * plugin.getConfig().getDouble("radiation.intake-scale", 0.12);
    }

    private double num(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private void applySymptoms(Player player) {
        double dose = get(player);
        if (dose >= 90) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 1, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, true, false, true));
        } else if (dose >= 75) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 1, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1, true, false, true));
        } else if (dose >= 50) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 1, true, false, true));
        } else if (dose >= 25) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 0, true, false, true));
        } else {
            player.removePotionEffect(PotionEffectType.HUNGER);
            player.removePotionEffect(PotionEffectType.NAUSEA);
        }
        if (dose < 50) player.removePotionEffect(PotionEffectType.WEAKNESS);
        if (dose < 75) player.removePotionEffect(PotionEffectType.POISON);
        if (dose < 90) {
            player.removePotionEffect(PotionEffectType.WITHER);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
        if (dose >= 90 && ticks % 3 == 0) player.damage(1.0);
    }

    public void applyMedicine(Player player) {
        long duration = Math.max(20L, plugin.getConfig().getLong("radiation.medicine-duration-seconds", 300L)) * 1000L;
        medicineUntil.put(player.getUniqueId(), System.currentTimeMillis() + duration);
        int slowTicks = (int)Math.min(1200L, plugin.getConfig().getLong("radiation.medicine-slowness-seconds", 60L) * 20L);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Math.max(20, slowTicks), 0, true, false, true));
        player.sendMessage(org.bukkit.ChatColor.GREEN + "Лекарство действует: радиационная доза снижается быстрее.");
    }

    @EventHandler
    public void onMedicineConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!item.hasItemMeta()) return;
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "radiation_medicine"), PersistentDataType.BYTE);
        if (marker != null && marker == (byte)1) applyMedicine(event.getPlayer());
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private record FalloutZone(UUID world, double x, double y, double z,
                               double intensity, double radius, long expiresAt) { }
    private record WaterZone(UUID world, double x, double z, double radius, long expiresAt) { }
}
