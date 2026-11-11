package ru.artem.beforethefall.disaster;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Item;
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
    private final AftermathManager aftermath;

    public NuclearBlastManager(BeforeTheFallPlugin plugin, RadiationManager radiation) {
        this.plugin = plugin;
        this.radiation = radiation;
        this.aftermath = new AftermathManager(plugin, radiation);
    }

    public void detonate(Location origin, float power) {
        World world = origin.getWorld();
        if (world == null) return;
        final float p = Math.max(4.0f, Math.min(40.0f, power));
        final double radius = p * plugin.getConfig().getDouble("nuclear-blast.shockwave-radius-per-power", 14.0);
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
        scheduleSecondaryBursts(world, center);
        // Remove pre-existing loose drops in the immediate destruction zone.
        double cleanupRadius = plugin.getConfig().getDouble("nuclear-blast.block-radius", 90.0);
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(center, cleanupRadius, cleanupRadius, cleanupRadius)) {
            if (entity instanceof Item) entity.remove();
        }
        aftermath.apply(center);
        radiation.addFalloutZone(center, plugin.getConfig().getDouble("nuclear-blast.fallout-intensity", 100.0),
                plugin.getConfig().getDouble("nuclear-blast.fallout-propagation-radius", 3000.0),
                20L * 60L * 60L * plugin.getConfig().getLong("nuclear-blast.fallout-duration-hours", 72L));

        new BukkitRunnable() {
            int tick = 0;
            final DestructionCursor destruction = plugin.getConfig().getBoolean("nuclear-blast.destroy-blocks", true)
                    ? new DestructionCursor(center,
                        Math.max(16, Math.min(160, plugin.getConfig().getInt("nuclear-blast.block-radius", 90))),
                        Math.max(24, Math.min(192, plugin.getConfig().getInt("nuclear-blast.vertical-destruction-radius", 110))))
                    : null;
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
                        if (distance <= 50.0) player.damage(Math.max(2.0, 30.0 * exposure));
                        radiation.add(player, 38.0 * exposure);
                    }
                }

                // Process a bounded number of coordinates per tick. setType(AIR) deliberately
                // creates no drops, including for containers; water and ice evaporate too.
                if (destruction != null) destruction.process(world,
                        Math.max(500, plugin.getConfig().getInt("nuclear-blast.block-edits-per-tick", 5000)));

                if (tick <= 80) spawnMushroomCloud(world, center, tick, p);
                if (tick == 18 || tick == 42) world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 7.0f, 0.7f);
                if (tick > 80 && (destruction == null || destruction.isDone())) cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void scheduleSecondaryBursts(World world, Location center) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int bursts = random.nextInt(8, 13);
        for (int i = 0; i < bursts; i++) {
            double angle = random.nextDouble(0.0, Math.PI * 2.0);
            double distance = random.nextDouble(25.0, 32.0);
            Location burst = center.clone().add(
                    Math.cos(angle) * distance,
                    random.nextDouble(-2.0, 3.0),
                    Math.sin(angle) * distance
            );
            long delay = random.nextLong(1L, 4L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.spawnParticle(Particle.EXPLOSION_EMITTER, burst, 1);
                world.spawnParticle(Particle.FLAME, burst, 18, 1.2, 0.7, 1.2, 0.04);
                world.playSound(burst, Sound.ENTITY_GENERIC_EXPLODE,
                        SoundCategory.BLOCKS, 2.0f, 0.65f);
                for (Player target : world.getPlayers()) {
                    if (target.getLocation().distanceSquared(burst) > 5.0 * 5.0) continue;
                    if (plugin.getBunkerManager() != null
                            && plugin.getBunkerManager().isProtected(target.getLocation())) continue;
                    target.damage(6.0);
                    target.setFireTicks(Math.max(target.getFireTicks(), 60));
                }
                Block fireBase = burst.getBlock();
                if (fireBase.getType().isAir()
                        && fireBase.getRelative(0, -1, 0).getType().isSolid()) {
                    fireBase.setType(Material.FIRE, false);
                }
            }, delay);
        }
    }

    private void playGlobalSoundLayers(World world, Location center) {
        final double maxDistance = plugin.getConfig().getDouble("nuclear-blast.close-sound-radius", 500.0);
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > maxDistance * maxDistance) continue;

            boolean underground = player.getLocation().getY() < 60
                    || player.getLocation().getBlock().getLightFromSky() == 0;
            String sound = underground
                    ? "beforethefall:blast_distant"
                    : "beforethefall:blast_close";

            player.playSound(center, sound, SoundCategory.MASTER, 1.0f, 1.0f);
        }

        long delay = Math.max(20L, plugin.getConfig().getLong("nuclear-blast.shockwave-delay-ticks", 30L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(center) > maxDistance * maxDistance) continue;
                player.playSound(center, "beforethefall:blast_wave", SoundCategory.MASTER, 0.35f, 1.0f);
            }
        }, delay);
    }

    private void startEarthquake(World world, Location center) {
        Map<UUID, float[]> original = new HashMap<>();
        for (Player player : world.getPlayers()) {
            double distance = player.getLocation().distance(center);
            if (distance > plugin.getConfig().getDouble("nuclear-blast.distant-sound-radius", 3000.0)) continue;
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
                    if (distance > plugin.getConfig().getDouble("nuclear-blast.distant-sound-radius", 3000.0)) continue;
                    double closeRadius = plugin.getConfig().getDouble("nuclear-blast.close-sound-radius", 50.0);
                    double waveRadius = plugin.getConfig().getDouble("nuclear-blast.shock-sound-radius", 2000.0);
                    double distantRadius = plugin.getConfig().getDouble("nuclear-blast.distant-sound-radius", 3000.0);
                    // Camera shake follows the three audible blast layers: the pressure wave
                    // is stronger than the distant rumble, while the near-field detonation is strongest.
                    double strength = distance <= closeRadius ? 7.0
                            : distance <= waveRadius ? 3.5
                            : distance <= distantRadius ? 1.15 : 0.0;
                    if (plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(player.getLocation())) strength *= 0.15;
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    float yaw = (float)(Math.sin(tick * 0.8) * strength + random.nextDouble(-strength, strength));
                    float pitch = (float)(Math.cos(tick * 0.65) * strength * .45 + random.nextDouble(-strength * .45, strength * .45));
                    player.setRotation(base[0] + yaw, Math.max(-90f, Math.min(90f, base[1] + pitch)));
                    if (tick % 10 == 0) player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 25, distance <= closeRadius ? 2 : distance <= waveRadius ? 1 : 0, true, false, true));
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
        // Use the actual supplied Blockbench nuclear_explosion model, not a particle placeholder.
        ItemStack sprite = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = sprite.getItemMeta();
        meta.setItemModel(new NamespacedKey(plugin, "nuclear_explosion"));
        sprite.setItemMeta(meta);
        ItemDisplay display = world.spawn(center.clone().add(0, 8, 0), ItemDisplay.class);
        display.setItemStack(sprite);
        display.setBillboard(Display.Billboard.CENTER);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setViewRange(4.0f);
        float scale = Math.max(0.65f, Math.min(1.45f, power / 18.0f));
        display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf()));
        new BukkitRunnable() {
            int age = 0;
            @Override public void run() {
                age++;
                if (!display.isValid() || age > 100) { display.remove(); cancel(); return; }
                float grow = scale * (1.0f + Math.min(5.5f, age / 13.0f));
                display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(grow, grow, grow), new Quaternionf()));
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(2);
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private static final class DestructionCursor {
        private final int cx, cy, cz, radius, verticalRadius;
        private int x, z, y;
        private boolean done;

        private DestructionCursor(Location center, int radius, int verticalRadius) {
            this.cx = center.getBlockX(); this.cy = center.getBlockY(); this.cz = center.getBlockZ();
            this.radius = radius; this.verticalRadius = verticalRadius;
            this.x = -radius; this.z = -radius; this.y = -verticalRadius;
        }

        private boolean isDone() { return done; }

        private void process(World world, int budget) {
            int checked = 0;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            while (!done && checked < budget) {
                int bx = cx + x, bz = cz + z;
                if (y == -verticalRadius && !world.isChunkLoaded(bx >> 4, bz >> 4)) {
                    nextColumn();
                    continue;
                }
                int by = cy + y;
                int localY = y;
                y++;
                checked++;
                if (by < world.getMinHeight() || by >= world.getMaxHeight()) {
                    if (y > verticalRadius) nextColumn();
                    continue;
                }
                double horizontal = (double)(x * x + z * z) / (radius * (double)radius);
                if (horizontal > 1.0) {
                    if (y > verticalRadius) nextColumn();
                    continue;
                }
                double vertical = (double)(localY * localY) / (verticalRadius * (double)verticalRadius);
                double normalized = Math.min(1.0, Math.sqrt(horizontal * 0.72 + vertical * 0.28));
                Block block = world.getBlockAt(bx, by, bz);
                Material type = block.getType();
                if (type.isAir() || type == Material.BEDROCK || type == Material.BARRIER
                        || type == Material.END_PORTAL_FRAME || type == Material.END_PORTAL
                        || type == Material.COMMAND_BLOCK || type == Material.CHAIN_COMMAND_BLOCK
                        || type == Material.REPEATING_COMMAND_BLOCK) {
                    if (y > verticalRadius) nextColumn();
                    continue;
                }
                // A guaranteed pulverized core with irregular surviving pockets farther out.
                boolean waterOrIce = block.isLiquid() || type == Material.ICE
                        || type == Material.FROSTED_ICE || type == Material.PACKED_ICE
                        || type == Material.BLUE_ICE;
                double chance = normalized < 0.12 ? 1.0 : 0.78 - normalized * 0.43;
                if (waterOrIce || random.nextDouble() <= chance) {
                    block.setType(Material.AIR, false);
                    if (!waterOrIce && random.nextDouble() < 0.018) {
                        Block below = block.getRelative(0, -1, 0);
                        if (below.getType().isSolid() && block.getType().isAir()) block.setType(Material.FIRE, false);
                    }
                }
                if (y > verticalRadius) nextColumn();
            }
        }

        private void nextColumn() {
            y = -verticalRadius;
            z++;
            if (z > radius) { z = -radius; x++; }
            if (x > radius) done = true;
        }
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
