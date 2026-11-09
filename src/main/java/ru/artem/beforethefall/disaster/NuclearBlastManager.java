package ru.artem.beforethefall.disaster;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import ru.artem.beforethefall.radiation.RadiationManager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Multi-stage nuclear detonation with a custom resource-pack billboard,
 * expanding shockwave, per-player distant sound layers, camera shake,
 * block damage and persistent fallout.
 */
public final class NuclearBlastManager {
    private final BeforeTheFallPlugin plugin;
    private final RadiationManager radiation;

    public NuclearBlastManager(BeforeTheFallPlugin plugin, RadiationManager radiation) {
        this.plugin = plugin;
        this.radiation = radiation;
    }

    public void detonate(Location origin, float power) {
        World world = origin.getWorld();
        if (world == null) return;
        final float p = Math.max(4.0f, Math.min(40.0f, power));
        final double radius = p * 9.0;
        final Location center = origin.clone();

        // Keep the flash local; long-distance audio is sent directly to each listener
        // so terrain, buildings and caves do not muffle the global warning layers.
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 10.0f, 0.45f);
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 8.0f, 0.55f);
        world.spawnParticle(Particle.FLASH, center, 2);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 80, 1.2, 1.2, 1.2, 0.15);

        playGlobalSoundLayers(world, center);
        startEarthquake(world, center);
        spawnTexturedBlast(world, center, p);
        radiation.addFalloutZone(center, plugin.getConfig().getDouble("nuclear-blast.fallout-intensity", 100.0),
                plugin.getConfig().getDouble("nuclear-blast.fallout-radius", 400.0),
                20L * 60L * 60L * plugin.getConfig().getLong("nuclear-blast.fallout-duration-hours", 72L));

        new BukkitRunnable() {
            int tick = 0;
            final List<Block> blocks = plugin.getConfig().getBoolean("nuclear-blast.destroy-blocks", true)
                    ? collectDestructionBlocks(center, Math.max(2, Math.min(25, plugin.getConfig().getInt("nuclear-blast.block-radius", 25))))
                    : List.of();
            int blockCursor = 0;
            final Set<UUID> hitPlayers = new HashSet<>();

            @Override public void run() {
                tick++;
                double front = Math.min(radius, tick * (radius / 45.0));
                spawnShockRing(world, center, front, tick);

                for (Player player : world.getPlayers()) {
                    double distance = player.getLocation().distance(center);
                    if (distance > radius * 1.35 || distance > front + 2.5) continue;
                    if (plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(player.getLocation())) continue;
                    double exposure = Math.max(0.0, 1.0 - distance / (radius * 1.35));
                    if (hitPlayers.add(player.getUniqueId())) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, true, false, true));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 1, true, false, true));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 1, true, false, true));
                        player.damage(Math.max(2.0, 30.0 * exposure));
                        radiation.add(player, 38.0 * exposure);
                    }
                }

                // Keep edits bounded to avoid freezing the Paper main thread.
                int edits = 0;
                while (blockCursor < blocks.size() && edits < 120) {
                    Block block = blocks.get(blockCursor++);
                    Material type = block.getType();
                    if (type.isAir() || type.getHardness() < 0 || block.getState() instanceof org.bukkit.block.TileState) continue;
                    if (type == Material.BEDROCK || type == Material.END_PORTAL_FRAME) continue;
                    block.breakNaturally();
                    if (ThreadLocalRandom.current().nextDouble() < 0.035
                            && block.getType().isAir() && !block.getRelative(0, -1, 0).getType().isAir()) {
                        block.setType(Material.FIRE, false);
                    }
                    edits++;
                }

                if (tick <= 80) spawnMushroomCloud(world, center, tick, p);
                if (tick == 18 || tick == 42) world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 7.0f, 0.7f);
                if (tick > 80 && blockCursor >= blocks.size()) cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void playGlobalSoundLayers(World world, Location center) {
        for (Player player : world.getPlayers()) {
            if (!player.getWorld().equals(world)) continue;
            double distance = player.getLocation().distance(center);
            if (distance <= plugin.getConfig().getDouble("nuclear-blast.distant-sound-radius", 3000.0))
                player.playSound(player.getLocation(), "atomic:detonation_distant", SoundCategory.MASTER, 1.0f, 1.0f);
            if (distance <= plugin.getConfig().getDouble("nuclear-blast.shock-sound-radius", 2000.0))
                player.playSound(player.getLocation(), "atomic:shockwave", SoundCategory.MASTER, 1.0f, 1.0f);
            if (distance <= plugin.getConfig().getDouble("nuclear-blast.close-sound-radius", 50.0))
                player.playSound(player.getLocation(), "atomic:nuclear_bomb", SoundCategory.MASTER, 1.0f, 1.0f);
        }
    }

    private void startEarthquake(World world, Location center) {
        Map<UUID, float[]> original = new HashMap<>();
        for (Player player : world.getPlayers()) {
            double distance = player.getLocation().distance(center);
            if (distance > 3000.0) continue;
            Location view = player.getLocation();
            original.put(player.getUniqueId(), new float[]{view.getYaw(), view.getPitch()});
        }
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                tick++;
                for (Player player : world.getPlayers()) {
                    float[] base = original.get(player.getUniqueId());
                    if (base == null) continue;
                    double distance = player.getLocation().distance(center);
                    if (distance > 3000.0) continue;
                    double strength = distance <= 50 ? 5.0 : distance <= 2000 ? 2.2 : 0.8;
                    if (plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(player.getLocation())) strength *= 0.15;
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    float yaw = (float)(Math.sin(tick * 0.8) * strength + random.nextDouble(-strength, strength));
                    float pitch = (float)(Math.cos(tick * 0.65) * strength * .45 + random.nextDouble(-strength * .45, strength * .45));
                    player.setRotation(base[0] + yaw, Math.max(-90f, Math.min(90f, base[1] + pitch)));
                    if (tick % 10 == 0) player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 25, distance <= 50 ? 2 : 0, true, false, true));
                }
                if (tick >= 160) {
                    for (Player player : world.getPlayers()) {
                        float[] base = original.get(player.getUniqueId());
                        if (base != null) player.setRotation(base[0], base[1]);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void spawnTexturedBlast(World world, Location center, float power) {
        ItemStack sprite = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = sprite.getItemMeta();
        meta.setCustomModelData(5);
        sprite.setItemMeta(meta);
        ItemDisplay display = world.spawn(center.clone().add(0, 8, 0), ItemDisplay.class);
        display.setItemStack(sprite);
        display.setBillboard(Display.Billboard.CENTER);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setViewRange(2.0f);
        float scale = Math.max(24f, Math.min(72f, power * 2.2f));
        display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf()));
        new BukkitRunnable() {
            int age = 0;
            @Override public void run() {
                age++;
                if (!display.isValid() || age > 100) { display.remove(); cancel(); return; }
                float grow = scale * (1.0f + Math.min(.55f, age / 70.0f));
                display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(grow, grow, grow), new Quaternionf()));
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(2);
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private List<Block> collectDestructionBlocks(Location center, int blockRadius) {
        List<Block> result = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return result;
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        int r2 = blockRadius * blockRadius;
        for (int x = -blockRadius; x <= blockRadius; x++)
            for (int y = -blockRadius; y <= blockRadius; y++)
                for (int z = -blockRadius; z <= blockRadius; z++)
                    if (x*x + y*y + z*z <= r2) {
                        int yy = cy + y;
                        if (yy >= world.getMinHeight() && yy < world.getMaxHeight())
                            result.add(world.getBlockAt(cx + x, yy, cz + z));
                    }
        result.sort(Comparator.comparingDouble(block -> block.getLocation().distanceSquared(center)));
        return result;
    }

    private void spawnShockRing(World world, Location center, double radius, int tick) {
        if (radius <= 0) return;
        int points = Math.min(180, Math.max(36, (int)(radius * 2.2)));
        double y = center.getY() + 0.25 + Math.min(2.0, tick * 0.025);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            Location at = new Location(world, center.getX() + Math.cos(angle) * radius, y,
                    center.getZ() + Math.sin(angle) * radius);
            world.spawnParticle(Particle.CLOUD, at, 1, .18, .12, .18, .015);
            if (i % 3 == 0) world.spawnParticle(Particle.FLAME, at, 1, .08, .08, .08, .005);
        }
    }

    private void spawnMushroomCloud(World world, Location center, int tick, float power) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double height = Math.min(65.0, 12.0 + tick * 0.75);
        double capRadius = Math.min(24.0, 5.0 + tick * 0.24) * (power / 18.0);
        for (int i = 0; i < 90; i++) {
            double angle = random.nextDouble(Math.PI * 2);
            double r = Math.sqrt(random.nextDouble()) * capRadius;
            double y = tick < 28 ? center.getY() + height * random.nextDouble()
                    : center.getY() + height + random.nextDouble(-3.0, 4.0);
            Location at = center.clone().add(Math.cos(angle) * r, y - center.getY(), Math.sin(angle) * r);
            world.spawnParticle(Particle.LARGE_SMOKE, at, 1, .8, 1.4, .8, .012);
            if (i % 3 == 0) world.spawnParticle(Particle.ASH, at, 1, .6, 1.2, .6, .01);
            if (i % 8 == 0) world.spawnParticle(Particle.FLAME, at, 1, .3, .5, .3, .005);
        }
    }
}
