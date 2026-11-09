package ru.artem.beforethefall.disaster;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import ru.artem.beforethefall.radiation.RadiationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Staged nuclear detonation: flash, expanding shock front, delayed player impact,
 * controlled block damage, and a persistent mushroom cloud.
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

        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 10.0f, 0.45f);
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 8.0f, 0.55f);
        world.spawnParticle(Particle.FLASH, center, 2);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 80, 1.2, 1.2, 1.2, 0.15);

        radiation.addFalloutZone(center, Math.min(28.0, p * 1.15), radius * 1.8, 20L * 60L * 60L);

        // The shock front expands over several seconds; nearby players are hit first.
        new BukkitRunnable() {
            int tick = 0;
            final List<Block> blocks = plugin.getConfig().getBoolean("nuclear-blast.destroy-blocks", true)
                    ? collectDestructionBlocks(center, Math.max(2, Math.min(24, plugin.getConfig().getInt("nuclear-blast.block-radius", 12))))
                    : List.of();
            int blockCursor = 0;

            @Override public void run() {
                tick++;
                double front = Math.min(radius, tick * (radius / 45.0));
                spawnShockRing(world, center, front, tick);

                for (Player player : world.getPlayers()) {
                    double distance = player.getLocation().distance(center);
                    if (distance > radius * 1.35 || distance > front + 2.5) continue;
                    if (plugin.getBunkerManager() != null && plugin.getBunkerManager().isProtected(player.getLocation())) continue;
                    double exposure = Math.max(0.0, 1.0 - distance / (radius * 1.35));
                    if (player.getNoDamageTicks() <= 0) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, true, false, true));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 1, true, false, true));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 1, true, false, true));
                        player.damage(Math.max(2.0, 30.0 * exposure));
                        radiation.add(player, 38.0 * exposure);
                    }
                }

                // Limit block edits per tick to avoid a single long main-thread stall.
                int edits = 0;
                while (blockCursor < blocks.size() && edits < 180) {
                    Block b = blocks.get(blockCursor++);
                    if (b.getType().isAir() || b.getType().getHardness() < 0 || b.getState() instanceof org.bukkit.block.TileState) continue;
                    if (b.getType() == Material.BEDROCK || b.getType() == Material.END_PORTAL_FRAME) continue;
                    b.breakNaturally();
                    if (ThreadLocalRandom.current().nextDouble() < 0.025 && b.getRelative(0, 1, 0).getType().isAir())
                        b.getRelative(0, 1, 0).setType(Material.FIRE, false);
                    edits++;
                }

                if (tick <= 80) spawnMushroomCloud(world, center, tick, p);
                if (tick == 18 || tick == 42) world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 7.0f, 0.7f);
                if (tick > 80 && blockCursor >= blocks.size()) cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
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
