package ru.artem.beforethefall.radiation;

import org.bukkit.Bukkit;
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

public final class RadiationManager {
    private final BeforeTheFallPlugin plugin;
    private final Map<UUID, Double> doses = new HashMap<>();
    private final List<FalloutZone> fallout = new ArrayList<>();
    private BukkitTask task;
    private long ticks;

    public RadiationManager(BeforeTheFallPlugin plugin) { this.plugin = plugin; load(); }
    public void start() {
        if (!plugin.getConfig().getBoolean("radiation.enabled", true)) return;
        long interval = Math.max(20L, plugin.getConfig().getLong("radiation.tick-interval", 40L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }
    public void reload() { if (task != null) task.cancel(); start(); }
    public double get(Player player) { return doses.getOrDefault(player.getUniqueId(), 0.0); }
    /** Current local radiation intensity, as measured by the dosimeter. */
    public double getAmbientLevel(Player player) {
        Location location = player.getLocation();
        boolean bunker = plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(location);
        return bunker ? 0.0 : radiationAt(location);
    }
    public void set(Player player, double value) { doses.put(player.getUniqueId(), clamp(value)); applySymptoms(player); }
    public void add(Player player, double value) { set(player, get(player) + value); }

    /** Creates a persistent radioactive fallout source centered on a detonation. */
    public void addFalloutZone(Location center, double intensity, double radius, long durationTicks) {
        if (center.getWorld() == null) return;
        fallout.add(new FalloutZone(center.getWorld().getUID(), center.getX(), center.getY(), center.getZ(),
                Math.max(0.0, intensity), Math.max(1.0, radius), System.currentTimeMillis() + Math.max(0L, durationTicks) * 50L));
        saveFallout();
        plugin.saveConfig();
    }

    public void save() {
        for (Map.Entry<UUID, Double> entry : doses.entrySet())
            plugin.getConfig().set("player-data." + entry.getKey() + ".radiation", entry.getValue());
        saveFallout();
        plugin.saveConfig();
    }
    private void load() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("player-data");
        if (section != null) for (String key : section.getKeys(false)) {
            try { doses.put(UUID.fromString(key), clamp(section.getDouble(key + ".radiation", 0.0))); }
            catch (IllegalArgumentException ignored) { plugin.getLogger().warning("Skipping invalid player UUID: " + key); }
        }
        for (Map<?, ?> z : plugin.getConfig().getMapList("radiation.fallout-zones")) {
            try {
                UUID world = UUID.fromString(String.valueOf(z.get("world")));
                fallout.add(new FalloutZone(world, num(z.get("x")), num(z.get("y")), num(z.get("z")),
                    num(z.get("intensity")), Math.max(1.0, num(z.get("radius"))), (long)num(z.get("expires-at"))));
            } catch (RuntimeException ignored) { plugin.getLogger().warning("Skipping malformed fallout zone."); }
        }
    }
    private void saveFallout() {
        List<Map<String,Object>> values = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (FalloutZone z : fallout) if (z.expiresAt > now) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("world",z.world.toString());m.put("x",z.x);m.put("y",z.y);m.put("z",z.z);
            m.put("intensity",z.intensity);m.put("radius",z.radius);m.put("expires-at",z.expiresAt);values.add(m);
        }
        plugin.getConfig().set("radiation.fallout-zones", values);
    }
    public void contaminateWater(Location center, double radius, long durationTicks) {
        if (center.getWorld() == null) return;
        plugin.getConfig().set("radiation.water-contamination.world", center.getWorld().getUID().toString());
        plugin.getConfig().set("radiation.water-contamination.x", center.getX());
        plugin.getConfig().set("radiation.water-contamination.z", center.getZ());
        plugin.getConfig().set("radiation.water-contamination.radius", radius);
        plugin.getConfig().set("radiation.water-contamination.expires-at", System.currentTimeMillis() + durationTicks * 50L);
        plugin.saveConfig();
    }

    private boolean isInContaminatedWater(Player player) {
        Location loc = player.getLocation();
        boolean wet = loc.getBlock().getType() == Material.WATER || player.getEyeLocation().getBlock().getType() == Material.WATER
                || loc.clone().subtract(0, 1, 0).getBlock().getType() == Material.WATER;
        if (!wet) return false;
        var section = plugin.getConfig().getConfigurationSection("radiation.water-contamination");
        if (section == null || section.getLong("expires-at") <= System.currentTimeMillis()) return false;
        if (!loc.getWorld().getUID().toString().equals(section.getString("world"))) return false;
        double dx = loc.getX() - section.getDouble("x"), dz = loc.getZ() - section.getDouble("z");
        double radius = section.getDouble("radius", 0.0);
        return dx * dx + dz * dz <= radius * radius;
    }

    private void tick() {
        ticks++;
        long now = System.currentTimeMillis();
        boolean removedZones = fallout.removeIf(z -> z.expiresAt <= now);
        if (removedZones) save();
        double decay = Math.max(0.0, plugin.getConfig().getDouble("radiation.decay-per-tick", 0.025));
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            boolean bunker = plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(loc);
            double source = bunker ? 0.0 : radiationAt(loc);
            // A gas-mask filter blocks inhaled radioactive dust, not penetrating gamma radiation.
            double inhalationFactor = plugin.getGasMaskManager().isProtected(player) ? 0.18 : 1.0;
            double suitFactor = plugin.getProtectiveSuitManager() != null && plugin.getProtectiveSuitManager().hasFullSuit(player) ? 0.45 : 1.0;
            double intake = source * inhalationFactor * suitFactor;
            if (!bunker && isInContaminatedWater(player)) intake += plugin.getConfig().getDouble("radiation.contaminated-water-intake", 2.5);
            double oldDose = get(player);
            double next = Math.max(0.0, oldDose - decay) + intake;
            doses.put(player.getUniqueId(), clamp(next));
            if (intake > 0.01 && ticks % 2 == 0) {
                World w = player.getWorld();
                w.spawnParticle(Particle.WITCH, loc.clone().add(0, 1.0, 0), 2, .25, .35, .25, .01);
            }
            applySymptoms(player);
        }
        // Persist player doses periodically, not on every gameplay tick.
        if (ticks % 150 == 0) save();
    }
    private double radiationAt(Location loc) {
        double result = 0.0;
        for (Map<?, ?> zone : plugin.getConfig().getMapList("radiation.zones")) {
            if (!loc.getWorld().getName().equals(String.valueOf(zone.get("world")))) continue;
            double dx=loc.getX()-num(zone.get("x")),dy=loc.getY()-num(zone.get("y")),dz=loc.getZ()-num(zone.get("z"));
            double radius=Math.max(1.0,num(zone.get("radius"))),d2=dx*dx+dy*dy+dz*dz;
            if(d2<=radius*radius) result=Math.max(result,num(zone.get("level"))/(1.0+d2/64.0));
        }
        for(FalloutZone z:fallout) if(z.world.equals(loc.getWorld().getUID())) {
            double dx=loc.getX()-z.x,dy=loc.getY()-z.y,dz=loc.getZ()-z.z,d2=dx*dx+dy*dy+dz*dz;
            if(d2<=z.radius*z.radius) result+=z.intensity/(1.0+d2/100.0);
        }
        return result * plugin.getConfig().getDouble("radiation.intake-scale", 0.12);
    }
    private double num(Object o){return o instanceof Number n?n.doubleValue():0.0;}
    private void applySymptoms(Player p) {
        double v=get(p);
        if(v>=90){p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,100,1,true,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,100,1,true,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,100,1,true,false,true));}
        else if(v>=75){p.addPotionEffect(new PotionEffect(PotionEffectType.POISON,100,0,true,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,100,1,true,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,100,1,true,false,true));}
        else if(v>=50){p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,100,0,true,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,100,0,true,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER,100,1,true,false,true));}
        else if(v>=25){p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER,100,0,true,false,true));}
        else { p.removePotionEffect(PotionEffectType.HUNGER); p.removePotionEffect(PotionEffectType.NAUSEA); }
        if(v<50) p.removePotionEffect(PotionEffectType.WEAKNESS);
        if(v<75) p.removePotionEffect(PotionEffectType.POISON);
        if(v<90) { p.removePotionEffect(PotionEffectType.WITHER); p.removePotionEffect(PotionEffectType.SLOWNESS); }
        if(v>=90 && ticks%3==0)p.damage(1.0);
    }
    private double clamp(double v){return Math.max(0.0,Math.min(100.0,v));}
    private record FalloutZone(UUID world,double x,double y,double z,double intensity,double radius,long expiresAt){}
}
